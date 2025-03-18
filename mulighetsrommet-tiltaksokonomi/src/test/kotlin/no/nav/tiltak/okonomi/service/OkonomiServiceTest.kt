package no.nav.tiltak.okonomi.service

import arrow.core.right
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.serialization.json.Json
import no.nav.common.kafka.producer.feilhandtering.StoredProducerRecord
import no.nav.mulighetsrommet.brreg.BrregAdresse
import no.nav.mulighetsrommet.brreg.BrregClient
import no.nav.mulighetsrommet.brreg.BrregHovedenhetDto
import no.nav.mulighetsrommet.brreg.SlettetBrregHovedenhetDto
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.kafka.KafkaProducerRepositoryImpl
import no.nav.mulighetsrommet.ktor.createMockEngine
import no.nav.mulighetsrommet.ktor.decodeRequestBody
import no.nav.mulighetsrommet.model.*
import no.nav.tiltak.okonomi.*
import no.nav.tiltak.okonomi.api.BestillingStatus
import no.nav.tiltak.okonomi.api.FakturaStatus
import no.nav.tiltak.okonomi.db.OkonomiDatabase
import no.nav.tiltak.okonomi.model.*
import no.nav.tiltak.okonomi.oebs.OebsFakturaMelding
import no.nav.tiltak.okonomi.oebs.OebsPoApClient
import java.time.LocalDate

class OkonomiServiceTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    lateinit var db: OkonomiDatabase
    lateinit var kafkaProducerRepository: KafkaProducerRepositoryImpl

    beforeSpec {
        db = OkonomiDatabase(database.db)
        kafkaProducerRepository = KafkaProducerRepositoryImpl(db.db)

        initializeData(db)
    }

    val leverandor = BrregHovedenhetDto(
        organisasjonsnummer = Organisasjonsnummer("123456789"),
        organisasjonsform = "AS",
        navn = "Tiltaksarrangør AS",
        postadresse = null,
        forretningsadresse = BrregAdresse(
            landkode = "NO",
            postnummer = "0170",
            poststed = "OSLO",
            adresse = listOf("Gateveien 1"),
        ),
    )

    val brreg: BrregClient = mockk()

    fun createOkonomiService(oebsTiltakApiClient: OebsPoApClient) = OkonomiService(
        db = db,
        oebs = oebsTiltakApiClient,
        brreg = brreg,
        topics = KafkaTopics("bestilling-status", "faktura-status"),
    )

    context("opprett bestilling") {
        test("feiler når oebs svarer med feil") {
            coEvery { brreg.getHovedenhet(Organisasjonsnummer("123456789")) } returns leverandor.right()
            val service = createOkonomiService(oebsClient(oebsRespondError()))

            val opprettBestilling = createOpprettBestilling("1")
            service.opprettBestilling(opprettBestilling).shouldBeLeft().should {
                it.message shouldBe "Klarte ikke sende bestilling 1 til oebs"
            }
        }

        test("feiler når kontering mangler for bestilling") {
            coEvery { brreg.getHovedenhet(Organisasjonsnummer("123456789")) } returns leverandor.right()
            val service = createOkonomiService(oebsClient(oebsRespondOk()))

            val opprettBestilling = createOpprettBestilling("2").copy(
                periode = Periode.forMonthOf(LocalDate.of(1990, 1, 1)),
            )
            service.opprettBestilling(opprettBestilling).shouldBeLeft().should {
                it.message shouldBe "Kontering for bestilling 2 mangler"
            }
        }

        test("feiler når hovedenhet er slettet") {
            coEvery { brreg.getHovedenhet(Organisasjonsnummer("123456789")) } returns SlettetBrregHovedenhetDto(
                organisasjonsnummer = Organisasjonsnummer("123456789"),
                organisasjonsform = "AS",
                navn = "Tiltaksarrangør AS",
                slettetDato = LocalDate.of(2025, 1, 1),
            ).right()
            val service = createOkonomiService(oebsClient(oebsRespondOk()))

            val opprettBestilling = createOpprettBestilling("3").copy(
                arrangor = OpprettBestilling.Arrangor(
                    hovedenhet = Organisasjonsnummer("123456789"),
                    underenhet = Organisasjonsnummer("234567891"),
                ),
            )
            service.opprettBestilling(opprettBestilling).shouldBeLeft().should {
                it.message shouldBe "Hovedenhet med orgnr ${opprettBestilling.arrangor.hovedenhet} er slettet"
            }
        }

        test("feiler når leverandør mangler adresse") {
            coEvery { brreg.getHovedenhet(Organisasjonsnummer("123456789")) } returns BrregHovedenhetDto(
                organisasjonsnummer = Organisasjonsnummer("123456789"),
                organisasjonsform = "AS",
                navn = "Tiltaksarrangør AS",
                postadresse = null,
                forretningsadresse = null,
            ).right()
            val service = createOkonomiService(oebsClient(oebsRespondOk()))

            val opprettBestilling = createOpprettBestilling("3").copy(
                arrangor = OpprettBestilling.Arrangor(
                    hovedenhet = Organisasjonsnummer("123456789"),
                    underenhet = Organisasjonsnummer("234567891"),
                ),
            )
            service.opprettBestilling(opprettBestilling).shouldBeLeft().should {
                it.message shouldBe "Klarte ikke utlede adresse for leverandør 123456789"
            }
        }

        test("skal opprette bestilling hos oebs og lagrer utgående melding om status for bestilling") {
            coEvery { brreg.getHovedenhet(Organisasjonsnummer("123456789")) } returns leverandor.right()
            val service = createOkonomiService(oebsClient(oebsRespondOk()))

            val opprettBestilling = createOpprettBestilling("1")
            service.opprettBestilling(opprettBestilling).shouldBeRight().should {
                it.bestillingsnummer shouldBe "1"
                it.status shouldBe BestillingStatusType.SENDT
            }

            kafkaProducerRepository.getLatestRecord().should {
                it.topic shouldBe "bestilling-status"
                it.key.toString(Charsets.UTF_8) shouldBe "1"
                it.value.toString(Charsets.UTF_8) shouldBe Json.encodeToString(
                    BestillingStatus(
                        bestillingsnummer = "1",
                        status = BestillingStatusType.SENDT,
                    ),
                )
            }
        }

        test("svarer med eksisterende bestilling når bestillingsnummer allerede er kjent") {
            val opprettBestilling = createOpprettBestilling("10")
            db.session {
                val bestilling = Bestilling.fromOpprettBestilling(opprettBestilling).copy(
                    status = BestillingStatusType.FRIGJORT,
                )
                queries.bestilling.insertBestilling(bestilling)
            }

            val service = createOkonomiService(oebsClient(oebsRespondOk()))

            service.opprettBestilling(opprettBestilling).shouldBeRight().should {
                it.bestillingsnummer shouldBe "10"
                it.status shouldBe BestillingStatusType.FRIGJORT
            }
        }
    }

    context("annuller bestilling") {
        test("annullering feiler når bestilling ikke finnes") {
            val service = createOkonomiService(oebsClient(oebsRespondOk()))

            val annullerBestilling = createAnnullerBestilling("4")
            service.annullerBestilling(annullerBestilling).shouldBeLeft().should {
                it.message shouldBe "Bestilling 4 finnes ikke"
            }
        }

        test("annullering feiler når bestilling er oppgjort") {
            db.session {
                val bestilling = Bestilling.fromOpprettBestilling(createOpprettBestilling("4")).copy(
                    status = BestillingStatusType.FRIGJORT,
                )
                queries.bestilling.insertBestilling(bestilling)
            }

            coEvery { brreg.getHovedenhet(Organisasjonsnummer("123456789")) } returns leverandor.right()
            val service = createOkonomiService(oebsClient(oebsRespondOk()))

            val annullerBestilling = createAnnullerBestilling("4")
            service.annullerBestilling(annullerBestilling).shouldBeLeft().should {
                it.message shouldBe "Bestilling 4 kan ikke annulleres fordi den er oppgjort"
            }
        }

        test("annullering feiler når det finnes fakturaer for bestilling") {
            db.session {
                val bestilling = Bestilling.fromOpprettBestilling(createOpprettBestilling("5"))
                queries.bestilling.insertBestilling(bestilling)

                val faktura = Faktura.fromOpprettFaktura(
                    createOpprettFaktura("5", "5-1"),
                    bestilling.linjer,
                )
                queries.faktura.insertFaktura(faktura)
            }

            coEvery { brreg.getHovedenhet(Organisasjonsnummer("123456789")) } returns leverandor.right()
            val service = createOkonomiService(oebsClient(oebsRespondOk()))

            val annullerBestilling = createAnnullerBestilling("5")
            service.annullerBestilling(annullerBestilling).shouldBeLeft().should {
                it.message shouldBe "Bestilling 5 kan ikke annulleres fordi det finnes fakturaer for bestillingen"
            }
        }

        db.session {
            val bestilling = Bestilling.fromOpprettBestilling(createOpprettBestilling("6"))
            queries.bestilling.insertBestilling(bestilling)
        }

        test("annullering feiler når oebs svarer med feilkoder") {
            coEvery { brreg.getHovedenhet(Organisasjonsnummer("123456789")) } returns leverandor.right()
            val service = createOkonomiService(oebsClient(oebsRespondError()))

            val annullerBestilling = createAnnullerBestilling("6")
            service.annullerBestilling(annullerBestilling).shouldBeLeft().should {
                it.message shouldBe "Klarte ikke annullere bestilling 6 hos oebs"
            }
        }

        test("annullering av bestilling lagrer utgående melding om status for bestilling") {
            coEvery { brreg.getHovedenhet(Organisasjonsnummer("123456789")) } returns leverandor.right()
            val service = createOkonomiService(oebsClient(oebsRespondOk()))

            val annullerBestilling = createAnnullerBestilling("6")
            service.annullerBestilling(annullerBestilling).shouldBeRight().should {
                it.bestillingsnummer shouldBe "6"
                it.status shouldBe BestillingStatusType.ANNULLERT
            }

            kafkaProducerRepository.getLatestRecord().should {
                it.topic shouldBe "bestilling-status"
                it.key.toString(Charsets.UTF_8) shouldBe "6"
                it.value.toString(Charsets.UTF_8) shouldBe Json.encodeToString(
                    BestillingStatus(
                        bestillingsnummer = "6",
                        status = BestillingStatusType.ANNULLERT,
                    ),
                )
            }
        }

        test("noop når bestilling allerede er annullert") {
            val service = createOkonomiService(oebsClient(oebsRespondOk()))

            val annullerBestilling = createAnnullerBestilling("6")
            service.annullerBestilling(annullerBestilling).shouldBeRight().should {
                it.bestillingsnummer shouldBe "6"
                it.status shouldBe BestillingStatusType.ANNULLERT
            }
        }
    }

    context("opprett faktura") {
        val bestillingsnummer = "B-1"

        db.session {
            val bestilling = Bestilling.fromOpprettBestilling(createOpprettBestilling(bestillingsnummer))
            queries.bestilling.insertBestilling(bestilling)
        }

        test("feiler når bestilling ikke finnes") {
            val service = createOkonomiService(oebsClient(oebsRespondOk()))

            val opprettFaktura = createOpprettFaktura("B-2", "F-1")
            service.opprettFaktura(opprettFaktura).shouldBeLeft().should {
                it.message shouldBe "Bestilling B-2 finnes ikke"
            }
        }

        test("feiler når oebs svarer med feilkoder") {
            val service = createOkonomiService(oebsClient(oebsRespondError()))

            val opprettFaktura = createOpprettFaktura(bestillingsnummer, "F-1")
            service.opprettFaktura(opprettFaktura).shouldBeLeft().should {
                it.message shouldBe "Klarte ikke sende faktura F-1 til oebs"
            }
        }

        test("oppretter faktura hos oebs og lagrer utgående melding om status for faktura") {
            val service = createOkonomiService(oebsClient(oebsRespondOk()))

            val opprettFaktura = createOpprettFaktura(bestillingsnummer, "F-2")
            service.opprettFaktura(opprettFaktura).shouldBeRight().should {
                it.fakturanummer shouldBe "F-2"
                it.status shouldBe FakturaStatusType.SENDT
            }

            kafkaProducerRepository.getLatestRecord().should {
                it.topic shouldBe "faktura-status"
                it.key.toString(Charsets.UTF_8) shouldBe "F-2"
                it.value.toString(Charsets.UTF_8) shouldBe Json.encodeToString(
                    FakturaStatus(
                        fakturanummer = "F-2",
                        status = FakturaStatusType.SENDT,
                    ),
                )
            }
        }
    }

    context("frigjor faktura") {
        db.session {
            val bestilling1 = Bestilling.fromOpprettBestilling(createOpprettBestilling("B-2"))
            val bestilling2 = Bestilling.fromOpprettBestilling(createOpprettBestilling("B-3"))
            queries.bestilling.insertBestilling(bestilling1)
            queries.bestilling.insertBestilling(bestilling2)
        }

        test("frigjør bestilling = true setter siste fakturalinje i fakturaen til oebs og oppdaterer bestillingstatus") {
            val mockEngine = createMockEngine {
                post(OebsPoApClient.FAKTURA_ENDPOINT) {
                    val melding = it.decodeRequestBody<OebsFakturaMelding>()

                    melding.fakturaLinjer.last().erSisteFaktura shouldBe true

                    respondOk()
                }
            }
            val service = createOkonomiService(oebsClient(mockEngine))

            val opprettFaktura = createOpprettFaktura("B-2", "F-3")
                .copy(frigjorBestilling = true)
            service.opprettFaktura(opprettFaktura).shouldBeRight().should {
                it.fakturanummer shouldBe "F-3"
                it.status shouldBe FakturaStatusType.SENDT
            }

            kafkaProducerRepository.getLatestRecord(topic = "faktura-status").should {
                it.value.toString(Charsets.UTF_8) shouldBe Json.encodeToString(
                    FakturaStatus(
                        fakturanummer = "F-3",
                        status = FakturaStatusType.SENDT,
                    ),
                )
            }

            db.session {
                val bestilling = queries.bestilling.getByBestillingsnummer("B-2")
                bestilling.shouldNotBeNull().status shouldBe BestillingStatusType.FRIGJORT
            }
        }

        test("frigjør faktura lager en faktura be erSisteLinje = true og setter bestillingen til FRIGJORT") {
            val mockEngine = createMockEngine {
                post(OebsPoApClient.FAKTURA_ENDPOINT) {
                    val melding = it.decodeRequestBody<OebsFakturaMelding>()

                    melding.fakturaLinjer.last().erSisteFaktura shouldBe true

                    respondOk()
                }
            }

            val service = createOkonomiService(oebsClient(mockEngine))

            val frigjorBestilling = createFrigjorBestilling("B-3")
            service.frigjorBestilling(frigjorBestilling).shouldBeRight().should {
                it.fakturanummer shouldBe "B-3-X"
                it.status shouldBe FakturaStatusType.SENDT
            }

            db.session {
                val bestilling = queries.bestilling.getByBestillingsnummer("B-3")
                bestilling.shouldNotBeNull().status shouldBe BestillingStatusType.FRIGJORT
            }

            kafkaProducerRepository.getLatestRecord(topic = "bestilling-status").should {
                it.value.toString(Charsets.UTF_8) shouldBe Json.encodeToString(
                    BestillingStatus(
                        bestillingsnummer = "B-3",
                        status = BestillingStatusType.FRIGJORT,
                    ),
                )
            }
        }
    }
})

private fun KafkaProducerRepositoryImpl.getLatestRecord(topic: String? = null): StoredProducerRecord {
    val records = topic
        ?.let { getRecords(100, listOf(it)) }
        ?: getRecords(100)
    return records.last()
}

private fun oebsRespondError() = createMockEngine {
    post(OebsPoApClient.BESTILLING_ENDPOINT) { respondError(HttpStatusCode.InternalServerError) }

    post(OebsPoApClient.FAKTURA_ENDPOINT) { respondError(HttpStatusCode.InternalServerError) }
}

private fun oebsRespondOk() = createMockEngine {
    post(OebsPoApClient.BESTILLING_ENDPOINT) { respondOk() }

    post(OebsPoApClient.FAKTURA_ENDPOINT) { respondOk() }
}

private fun oebsClient(mockEngine: MockEngine): OebsPoApClient {
    return OebsPoApClient(mockEngine, "http://localhost") { "token" }
}

private fun initializeData(db: OkonomiDatabase) = db.session {
    queries.kontering.insertKontering(
        OebsKontering(
            tilskuddstype = Tilskuddstype.TILTAK_DRIFTSTILSKUDD,
            tiltakskode = Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
            periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2099, 1, 1)),
            statligRegnskapskonto = "12345678901",
            statligArtskonto = "23456789012",
        ),
    )
}

private fun createOpprettBestilling(bestillingsnummer: String) = OpprettBestilling(
    bestillingsnummer = bestillingsnummer,
    tilskuddstype = Tilskuddstype.TILTAK_DRIFTSTILSKUDD,
    tiltakskode = Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
    arrangor = OpprettBestilling.Arrangor(
        hovedenhet = Organisasjonsnummer("123456789"),
        underenhet = Organisasjonsnummer("234567891"),
    ),
    avtalenummer = null,
    belop = 1000,
    behandletAv = OkonomiPart.System(OkonomiSystem.TILTAKSADMINISTRASJON),
    behandletTidspunkt = LocalDate.of(2025, 1, 1).atStartOfDay(),
    besluttetAv = OkonomiPart.System(OkonomiSystem.TILTAKSADMINISTRASJON),
    besluttetTidspunkt = LocalDate.of(2025, 1, 1).atStartOfDay(),
    periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
    kostnadssted = NavEnhetNummer("0400"),
)

private fun createAnnullerBestilling(bestillingsnummer: String) = AnnullerBestilling(
    bestillingsnummer = bestillingsnummer,
    behandletAv = OkonomiPart.System(OkonomiSystem.TILTAKSADMINISTRASJON),
    behandletTidspunkt = LocalDate.of(2025, 1, 1).atStartOfDay(),
    besluttetAv = OkonomiPart.System(OkonomiSystem.TILTAKSADMINISTRASJON),
    besluttetTidspunkt = LocalDate.of(2025, 1, 1).atStartOfDay(),
)

private fun createOpprettFaktura(bestillingsnummer: String, fakturanummer: String) = OpprettFaktura(
    fakturanummer = fakturanummer,
    bestillingsnummer = bestillingsnummer,
    betalingsinformasjon = OpprettFaktura.Betalingsinformasjon(
        kontonummer = Kontonummer("12345678901"),
        kid = null,
    ),
    belop = 1000,
    periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
    behandletAv = OkonomiPart.System(OkonomiSystem.TILTAKSADMINISTRASJON),
    behandletTidspunkt = LocalDate.of(2025, 1, 1).atStartOfDay(),
    besluttetAv = OkonomiPart.System(OkonomiSystem.TILTAKSADMINISTRASJON),
    besluttetTidspunkt = LocalDate.of(2025, 1, 1).atStartOfDay(),
    frigjorBestilling = false,
)

private fun createFrigjorBestilling(bestillingsnummer: String) = FrigjorBestilling(
    bestillingsnummer = bestillingsnummer,
    behandletAv = OkonomiPart.System(OkonomiSystem.TILTAKSADMINISTRASJON),
    behandletTidspunkt = LocalDate.of(2025, 1, 1).atStartOfDay(),
    besluttetAv = OkonomiPart.System(OkonomiSystem.TILTAKSADMINISTRASJON),
    besluttetTidspunkt = LocalDate.of(2025, 1, 1).atStartOfDay(),
)
