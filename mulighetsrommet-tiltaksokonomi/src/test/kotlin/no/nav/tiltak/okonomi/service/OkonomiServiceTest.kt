package no.nav.tiltak.okonomi.service

import arrow.core.right
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.serialization.json.Json
import kotliquery.queryOf
import no.nav.common.kafka.producer.feilhandtering.StoredProducerRecord
import no.nav.mulighetsrommet.brreg.*
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.requireSingle
import no.nav.mulighetsrommet.kafka.toStoredProducerRecord
import no.nav.mulighetsrommet.ktor.createMockEngine
import no.nav.mulighetsrommet.ktor.decodeRequestBody
import no.nav.mulighetsrommet.model.*
import no.nav.tiltak.okonomi.*
import no.nav.tiltak.okonomi.db.OkonomiDatabase
import no.nav.tiltak.okonomi.db.QueryContext
import no.nav.tiltak.okonomi.model.Bestilling
import no.nav.tiltak.okonomi.model.Faktura
import no.nav.tiltak.okonomi.model.OebsKontering
import no.nav.tiltak.okonomi.oebs.OebsBestillingMelding
import no.nav.tiltak.okonomi.oebs.OebsFakturaMelding
import no.nav.tiltak.okonomi.oebs.OebsPoApClient
import org.intellij.lang.annotations.Language
import java.time.LocalDate

class OkonomiServiceTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    lateinit var db: OkonomiDatabase

    beforeSpec {
        db = OkonomiDatabase(database.db)

        initializeData(db)
    }

    val arrangorHovedenhet = BrregHovedenhetDto(
        organisasjonsnummer = Organisasjonsnummer("123456789"),
        organisasjonsform = "AS",
        navn = "Tiltaksarrangør AS",
        overordnetEnhet = null,
        postadresse = null,
        forretningsadresse = BrregAdresse(
            landkode = "NO",
            postnummer = "0170",
            poststed = "OSLO",
            adresse = listOf("Gateveien 1"),
        ),
    )
    val arrangorUnderenhet = BrregUnderenhetDto(
        organisasjonsnummer = Organisasjonsnummer("234567891"),
        organisasjonsform = "BEDR",
        navn = "Tiltaksarrangør Underenhet",
        overordnetEnhet = Organisasjonsnummer("123456789"),
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
            coEvery { brreg.getBrregEnhet(Organisasjonsnummer("123456789")) } returns arrangorHovedenhet.right()
            coEvery { brreg.getBrregEnhet(Organisasjonsnummer("234567891")) } returns arrangorUnderenhet.right()

            val service = createOkonomiService(oebsClient(oebsRespondError()))

            val opprettBestilling = createOpprettBestilling("1")
            service.opprettBestilling(opprettBestilling).shouldBeLeft().should {
                it.message shouldBe "Klarte ikke sende bestilling 1 til oebs"
            }
        }

        test("feiler når kontering mangler for bestilling") {
            coEvery { brreg.getBrregEnhet(Organisasjonsnummer("123456789")) } returns arrangorHovedenhet.right()
            coEvery { brreg.getBrregEnhet(Organisasjonsnummer("234567891")) } returns arrangorUnderenhet.right()

            val service = createOkonomiService(oebsClient(oebsRespondOk()))

            val opprettBestilling = createOpprettBestilling("2").copy(
                periode = Periode.forMonthOf(LocalDate.of(1990, 1, 1)),
            )
            service.opprettBestilling(opprettBestilling).shouldBeLeft().should {
                it.message shouldBe "Kontering for bestilling 2 mangler"
            }
        }

        test("feiler når underenhet er slettet") {
            coEvery { brreg.getBrregEnhet(Organisasjonsnummer("234567891")) } returns SlettetBrregUnderenhetDto(
                organisasjonsnummer = Organisasjonsnummer("234567891"),
                organisasjonsform = "BEDR",
                navn = "Tiltaksarrangør Underenhet",
                slettetDato = LocalDate.of(2025, 1, 1),
            ).right()

            val service = createOkonomiService(oebsClient(oebsRespondOk()))

            val opprettBestilling = createOpprettBestilling("3")

            service.opprettBestilling(opprettBestilling).shouldBeLeft().should {
                it.message shouldBe "Underenhet med orgnr 234567891 er slettet"
            }
        }

        test("feiler når hovedenhet er slettet") {
            coEvery { brreg.getBrregEnhet(Organisasjonsnummer("123456789")) } returns SlettetBrregHovedenhetDto(
                organisasjonsnummer = Organisasjonsnummer("123456789"),
                organisasjonsform = "AS",
                navn = "Tiltaksarrangør AS",
                slettetDato = LocalDate.of(2025, 1, 1),
            ).right()
            coEvery { brreg.getBrregEnhet(Organisasjonsnummer("234567891")) } returns arrangorUnderenhet.right()

            val service = createOkonomiService(oebsClient(oebsRespondOk()))

            val opprettBestilling = createOpprettBestilling("3")

            service.opprettBestilling(opprettBestilling).shouldBeLeft().should {
                it.message shouldBe "Hovedenhet med orgnr 123456789 er slettet"
            }
        }

        test("feiler når leverandør mangler adresse") {
            coEvery { brreg.getBrregEnhet(Organisasjonsnummer("123456789")) } returns BrregHovedenhetDto(
                organisasjonsnummer = Organisasjonsnummer("123456789"),
                organisasjonsform = "AS",
                navn = "Tiltaksarrangør AS",
                overordnetEnhet = null,
                postadresse = null,
                forretningsadresse = null,
            ).right()
            coEvery { brreg.getBrregEnhet(Organisasjonsnummer("234567891")) } returns arrangorUnderenhet.right()

            val service = createOkonomiService(oebsClient(oebsRespondOk()))

            val opprettBestilling = createOpprettBestilling("3")

            service.opprettBestilling(opprettBestilling).shouldBeLeft().should {
                it.message shouldBe "Klarte ikke utlede adresse for leverandør 123456789"
            }
        }

        test("oppretter bestilling med hovedenhet hentet fra brreg") {
            coEvery { brreg.getBrregEnhet(Organisasjonsnummer("123456789")) } returns arrangorHovedenhet.right()
            coEvery { brreg.getBrregEnhet(Organisasjonsnummer("234567891")) } returns arrangorUnderenhet.right()

            val mockEngine = createMockEngine {
                post(OebsPoApClient.BESTILLING_ENDPOINT) {
                    val melding = it.decodeRequestBody<OebsBestillingMelding>()

                    melding.selger.organisasjonsNummer shouldBe "123456789"
                    melding.selger.organisasjonsNavn shouldBe "Tiltaksarrangør AS"
                    melding.selger.adresse shouldContain OebsBestillingMelding.Selger.Adresse(
                        gateNavn = "Gateveien 1",
                        by = "OSLO",
                        postNummer = "0170",
                        landsKode = "NO",
                    )
                    melding.selger.bedriftsNummer shouldBe "234567891"

                    respondOk()
                }
            }

            val service = createOkonomiService(oebsClient(mockEngine))

            val opprettBestilling = createOpprettBestilling("11")

            service.opprettBestilling(opprettBestilling).shouldBeRight().should {
                it.arrangorHovedenhet shouldBe Organisasjonsnummer("123456789")
                it.arrangorUnderenhet shouldBe Organisasjonsnummer("234567891")
            }
        }

        test("oppretter bestilling med øverste organisasjonsledd hentet fra brreg") {
            coEvery { brreg.getBrregEnhet(Organisasjonsnummer("345678912")) } returns arrangorHovedenhet.copy(
                organisasjonsnummer = Organisasjonsnummer("345678912"),
                organisasjonsform = "STAT",
                navn = "Tiltaksarrangør Øverste Organisasjonsledd",
            ).right()
            coEvery { brreg.getBrregEnhet(Organisasjonsnummer("123456789")) } returns BrregHovedenhetDto(
                organisasjonsnummer = Organisasjonsnummer("123456789"),
                organisasjonsform = "ORGL",
                navn = "Tiltaksarrangør Organisasjonsledd",
                overordnetEnhet = Organisasjonsnummer("345678912"),
                postadresse = null,
                forretningsadresse = null,
            ).right()
            coEvery { brreg.getBrregEnhet(Organisasjonsnummer("234567891")) } returns arrangorUnderenhet.right()

            val mockEngine = createMockEngine {
                post(OebsPoApClient.BESTILLING_ENDPOINT) {
                    val melding = it.decodeRequestBody<OebsBestillingMelding>()

                    melding.selger.organisasjonsNummer shouldBe "345678912"
                    melding.selger.organisasjonsNavn shouldBe "Tiltaksarrangør Øverste Organisasjonsledd"

                    respondOk()
                }
            }

            val service = createOkonomiService(oebsClient(mockEngine))

            val opprettBestilling = createOpprettBestilling("3")

            service.opprettBestilling(opprettBestilling).shouldBeRight().should {
                it.arrangorHovedenhet shouldBe Organisasjonsnummer("345678912")
                it.arrangorUnderenhet shouldBe Organisasjonsnummer("234567891")
            }
        }

        test("tillater at postnummer mangler for utenlandske bedrifter") {
            coEvery { brreg.getBrregEnhet(Organisasjonsnummer("920238076")) } returns BrregHovedenhetDto(
                organisasjonsnummer = Organisasjonsnummer("920238076"),
                organisasjonsform = "NUF",
                navn = "BOREALIS DESTINATION MANAGEMENT NUF",
                overordnetEnhet = null,
                postadresse = null,
                forretningsadresse = BrregAdresse(
                    landkode = "DK",
                    postnummer = null,
                    poststed = "Mariehamn",
                    adresse = listOf("Gateveien 1"),
                ),
            ).right()
            coEvery { brreg.getBrregEnhet(Organisasjonsnummer("234567891")) } returns arrangorUnderenhet.copy(
                overordnetEnhet = Organisasjonsnummer("920238076"),
            ).right()

            val mockEngine = createMockEngine {
                post(OebsPoApClient.BESTILLING_ENDPOINT) {
                    val melding = it.decodeRequestBody<OebsBestillingMelding>()

                    melding.selger.adresse shouldContain OebsBestillingMelding.Selger.Adresse(
                        gateNavn = "Gateveien 1",
                        by = "Mariehamn",
                        postNummer = null,
                        landsKode = "DK",
                    )

                    respondOk()
                }
            }

            val service = createOkonomiService(oebsClient(mockEngine))

            val opprettBestilling = createOpprettBestilling("1")
            service.opprettBestilling(opprettBestilling).shouldBeRight().should {
                it.arrangorHovedenhet shouldBe Organisasjonsnummer("920238076")
                it.arrangorUnderenhet shouldBe Organisasjonsnummer("234567891")
            }
        }

        test("skal opprette bestilling hos oebs og lagrer utgående melding om status for bestilling") {
            coEvery { brreg.getBrregEnhet(Organisasjonsnummer("123456789")) } returns arrangorHovedenhet.right()
            coEvery { brreg.getBrregEnhet(Organisasjonsnummer("234567891")) } returns arrangorUnderenhet.right()

            val service = createOkonomiService(oebsClient(oebsRespondOk()))

            val opprettBestilling = createOpprettBestilling("1")
            service.opprettBestilling(opprettBestilling).shouldBeRight().should {
                it.bestillingsnummer shouldBe "1"
                it.status shouldBe BestillingStatusType.SENDT
            }

            db.session { getLatestRecord() }.should {
                it.topic shouldBe "bestilling-status"
                it.key.toString(Charsets.UTF_8) shouldBe "1"
                it.value?.toString(Charsets.UTF_8) shouldBe Json.encodeToString(
                    BestillingStatus(
                        bestillingsnummer = "1",
                        status = BestillingStatusType.SENDT,
                    ),
                )
            }
        }

        test("svarer med eksisterende bestilling og lagrer utgående melding når bestillingsnummer allerede er kjent") {
            val opprettBestilling = createOpprettBestilling("10")
            db.session {
                val bestilling = Bestilling.fromOpprettBestilling(
                    opprettBestilling,
                    arrangorHovedenhet.organisasjonsnummer,
                ).copy(
                    status = BestillingStatusType.OPPGJORT,
                )
                queries.bestilling.insertBestilling(bestilling)
            }

            val service = createOkonomiService(oebsClient(oebsRespondOk()))

            service.opprettBestilling(opprettBestilling).shouldBeRight().should {
                it.bestillingsnummer shouldBe "10"
                it.status shouldBe BestillingStatusType.OPPGJORT
            }

            db.session { getLatestRecord() }.should {
                it.topic shouldBe "bestilling-status"
                it.key.toString(Charsets.UTF_8) shouldBe "10"
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
                val bestilling = createBestilling("4", status = BestillingStatusType.OPPGJORT)
                queries.bestilling.insertBestilling(bestilling)
            }

            val service = createOkonomiService(oebsClient(oebsRespondOk()))

            val annullerBestilling = createAnnullerBestilling("4")
            service.annullerBestilling(annullerBestilling).shouldBeLeft().should {
                it.message shouldBe "Bestilling 4 kan ikke annulleres fordi den har status: OPPGJORT"
            }
        }

        test("annullering feiler når det finnes fakturaer for bestilling") {
            db.session {
                val bestilling = createBestilling("5", status = BestillingStatusType.AKTIV)
                queries.bestilling.insertBestilling(bestilling)

                val faktura = Faktura.fromOpprettFaktura(
                    createOpprettFaktura("5", "5-1"),
                    bestilling.linjer,
                ).copy(status = FakturaStatusType.FULLT_BETALT)
                queries.faktura.insertFaktura(faktura)
            }

            val service = createOkonomiService(oebsClient(oebsRespondOk()))

            val annullerBestilling = createAnnullerBestilling("5")
            service.annullerBestilling(annullerBestilling).shouldBeLeft().should {
                it.message shouldBe "Bestilling 5 kan ikke annulleres fordi det finnes fakturaer for bestillingen"
            }
        }

        val bestilling = createBestilling("6", status = BestillingStatusType.AKTIV)
        db.session {
            queries.bestilling.insertBestilling(bestilling)
        }

        test("annullering feiler når oebs svarer med feilkoder") {
            val service = createOkonomiService(oebsClient(oebsRespondError()))

            val annullerBestilling = createAnnullerBestilling("6")
            service.annullerBestilling(annullerBestilling).shouldBeLeft().should {
                it.message shouldBe "Klarte ikke annullere bestilling 6 hos oebs"
            }
        }

        test("annullering feiler når bestilling ikke er aktiv ennå") {
            val bestilling2 = createBestilling("87")
            db.session {
                queries.bestilling.insertBestilling(bestilling2)
            }
            val service = createOkonomiService(oebsClient(oebsRespondError()))

            val annullerBestilling = createAnnullerBestilling("87")
            service.annullerBestilling(annullerBestilling).shouldBeLeft().should {
                it.message shouldBe "Bestilling ${bestilling2.bestillingsnummer} kan ikke annulleres fordi vi venter på kvittering"
            }
        }

        test("annullering av bestilling lagrer utgående melding om status for bestilling") {
            val service = createOkonomiService(oebsClient(oebsRespondOk()))

            val annullerBestilling = createAnnullerBestilling("6")
            service.annullerBestilling(annullerBestilling).shouldBeRight().should {
                it.bestillingsnummer shouldBe "6"
                it.status shouldBe BestillingStatusType.ANNULLERING_SENDT
            }

            db.session { getLatestRecord() }.should {
                it.topic shouldBe "bestilling-status"
                it.key.toString(Charsets.UTF_8) shouldBe "6"
                it.value?.toString(Charsets.UTF_8) shouldBe Json.encodeToString(
                    BestillingStatus(
                        bestillingsnummer = "6",
                        status = BestillingStatusType.ANNULLERING_SENDT,
                    ),
                )
            }
        }

        test("svarer med eksisterende bestilling og lagrer utgående melding når bestilling allerede er annullert") {
            val service = createOkonomiService(oebsClient(oebsRespondOk()))

            val annullerBestilling = createAnnullerBestilling("6")
            service.annullerBestilling(annullerBestilling).shouldBeRight().should {
                it.bestillingsnummer shouldBe "6"
                it.status shouldBe BestillingStatusType.ANNULLERING_SENDT
            }

            db.session { getLatestRecord() }.should {
                it.topic shouldBe "bestilling-status"
                it.key.toString(Charsets.UTF_8) shouldBe "6"
            }
        }
    }

    context("opprett faktura") {
        val bestillingsnummer = "B1"

        db.session {
            val bestilling = createBestilling(bestillingsnummer, status = BestillingStatusType.AKTIV)
            queries.bestilling.insertBestilling(bestilling)
        }

        test("feiler når bestilling ikke finnes") {
            val service = createOkonomiService(oebsClient(oebsRespondOk()))

            val opprettFaktura = createOpprettFaktura("B2", "B1-F1")
            service.opprettFaktura(opprettFaktura).shouldBeLeft().should {
                it.message shouldBe "Bestilling B2 finnes ikke"
            }
        }

        test("feiler når oebs svarer med feilkoder") {
            val service = createOkonomiService(oebsClient(oebsRespondError()))

            val opprettFaktura = createOpprettFaktura(bestillingsnummer, "B1-F1")
            service.opprettFaktura(opprettFaktura).shouldBeLeft().should {
                it.message shouldBe "Klarte ikke sende faktura B1-F1 til oebs"
            }
        }

        test("feiler når bestilling ikke er aktiv ennå") {
            val bestilling2 = createBestilling("876")
            db.session {
                queries.bestilling.insertBestilling(bestilling2)
            }
            val service = createOkonomiService(oebsClient(oebsRespondError()))

            val opprettFaktura = createOpprettFaktura(bestilling2.bestillingsnummer, "B1-F1")
            service.opprettFaktura(opprettFaktura).shouldBeLeft().should {
                it.message shouldBe "Faktura B1-F1 kan ikke opprettes fordi vi venter på kvittering"
            }
        }

        test("oppretter faktura hos oebs og lagrer utgående melding om status for faktura") {
            val service = createOkonomiService(oebsClient(oebsRespondOk()))

            val opprettFaktura = createOpprettFaktura(bestillingsnummer, "B1-F2")
            service.opprettFaktura(opprettFaktura).shouldBeRight().should {
                it.fakturanummer shouldBe "B1-F2"
                it.status shouldBe FakturaStatusType.SENDT
            }

            db.session { getLatestRecord() }.should {
                it.topic shouldBe "faktura-status"
                it.key.toString(Charsets.UTF_8) shouldBe "B1-F2"
                val fakturaStatus = Json.decodeFromString<FakturaStatus>(it.value?.toString(Charsets.UTF_8) ?: "")
                fakturaStatus.status shouldBe FakturaStatusType.SENDT
                fakturaStatus.fakturanummer shouldBe "B1-F2"
            }
        }

        test("svarer med eksisterende faktura og lagrer utgående melding når fakturanummer allerede er kjent") {
            val opprettFaktura = createOpprettFaktura(bestillingsnummer, "B1-F3")
            db.session {
                val bestilling = checkNotNull(queries.bestilling.getByBestillingsnummer(bestillingsnummer))
                val faktura = Faktura.fromOpprettFaktura(opprettFaktura, bestilling.linjer).copy(
                    status = FakturaStatusType.FULLT_BETALT,
                )
                queries.faktura.insertFaktura(faktura)
            }

            val service = createOkonomiService(oebsClient(oebsRespondOk()))

            service.opprettFaktura(opprettFaktura).shouldBeRight().should {
                it.fakturanummer shouldBe "B1-F3"
                it.status shouldBe FakturaStatusType.FULLT_BETALT
            }

            db.session { getLatestRecord() }.should {
                it.topic shouldBe "faktura-status"
                it.key.toString(Charsets.UTF_8) shouldBe "B1-F3"
            }
        }
    }

    context("gjør opp bestilling") {
        val bestilling1 = createBestilling("B2", status = BestillingStatusType.AKTIV)
        val bestilling2 = createBestilling("B3", status = BestillingStatusType.AKTIV)

        db.session {
            queries.bestilling.insertBestilling(bestilling1)
            queries.bestilling.insertBestilling(bestilling2)
        }

        test("gjorOppBestilling = true setter siste fakturalinje i fakturaen til oebs og oppdaterer status på bestilling") {
            val mockEngine = createMockEngine {
                post(OebsPoApClient.FAKTURA_ENDPOINT) {
                    val melding = it.decodeRequestBody<OebsFakturaMelding>()

                    melding.fakturaLinjer.last().erSisteFaktura shouldBe true

                    respondOk()
                }
            }
            val service = createOkonomiService(oebsClient(mockEngine))

            val opprettFaktura = createOpprettFaktura("B2", "B2-F1")
                .copy(gjorOppBestilling = true)
            service.opprettFaktura(opprettFaktura).shouldBeRight().should {
                it.fakturanummer shouldBe "B2-F1"
                it.status shouldBe FakturaStatusType.SENDT
            }

            db.session { getLatestRecord(topic = "faktura-status") }.should {
                val fakturaStatus = Json.decodeFromString<FakturaStatus>(it.value?.toString(Charsets.UTF_8) ?: "")
                fakturaStatus.status shouldBe FakturaStatusType.SENDT
                fakturaStatus.fakturanummer shouldBe "B2-F1"
            }

            db.session {
                val bestilling = queries.bestilling.getByBestillingsnummer("B2")
                bestilling.shouldNotBeNull().status shouldBe BestillingStatusType.OPPGJORT
            }
        }

        test("lager en faktura med erSisteLinje = true og setter bestillingen til OPPGJORT") {
            val mockEngine = createMockEngine {
                post(OebsPoApClient.FAKTURA_ENDPOINT) {
                    val melding = it.decodeRequestBody<OebsFakturaMelding>()

                    melding.fakturaLinjer.last().erSisteFaktura shouldBe true

                    respondOk()
                }
            }

            val service = createOkonomiService(oebsClient(mockEngine))

            val gjorOppBestilling = createGjorOppBestilling("B3")
            service.gjorOppBestilling(gjorOppBestilling).shouldBeRight().should {
                it.fakturanummer shouldBe "B3-X"
                it.status shouldBe FakturaStatusType.SENDT
            }

            db.session {
                val bestilling = queries.bestilling.getByBestillingsnummer("B3")
                bestilling.shouldNotBeNull().status shouldBe BestillingStatusType.OPPGJORT
            }

            db.session { getLatestRecord(topic = "bestilling-status") }.should {
                it.value?.toString(Charsets.UTF_8) shouldBe Json.encodeToString(
                    BestillingStatus(
                        bestillingsnummer = "B3",
                        status = BestillingStatusType.OPPGJORT,
                    ),
                )
            }
        }

        test("feiler når en faktura venter på kvittering") {
            val service = createOkonomiService(oebsClient(oebsRespondError()))

            val opprettFaktura = createOpprettFaktura(bestilling1.bestillingsnummer, "B1-F55")
            db.session {
                val faktura = Faktura.fromOpprettFaktura(opprettFaktura, bestilling1.linjer)
                queries.faktura.insertFaktura(faktura)
            }

            val gjorOppBestilling = createGjorOppBestilling(bestilling1.bestillingsnummer)
            service.gjorOppBestilling(gjorOppBestilling).shouldBeLeft().should {
                it.message shouldBe "Bestilling ${bestilling1.bestillingsnummer} kan ikke gjøres opp fordi vi venter på kvittering"
            }
        }
    }
})

private fun QueryContext.getLatestRecord(topic: String? = null): StoredProducerRecord {
    @Language("PostgreSQL")
    val sql = """
        select * from kafka_producer_record where coalesce(:topic, topic) = topic order by id desc limit 1
    """.trimIndent()

    val query = queryOf(sql, mapOf("topic" to topic))

    return session.requireSingle(query) { it.toStoredProducerRecord() }
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
    arrangor = Organisasjonsnummer("234567891"),
    avtalenummer = null,
    belop = 1000,
    behandletAv = OkonomiPart.System(OkonomiSystem.TILTAKSADMINISTRASJON),
    behandletTidspunkt = LocalDate.of(2025, 1, 1).atStartOfDay(),
    besluttetAv = OkonomiPart.System(OkonomiSystem.TILTAKSADMINISTRASJON),
    besluttetTidspunkt = LocalDate.of(2025, 1, 1).atStartOfDay(),
    periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
    kostnadssted = NavEnhetNummer("0400"),
)

private fun createBestilling(
    bestillingsnummer: String,
    status: BestillingStatusType = BestillingStatusType.SENDT,
): Bestilling {
    return Bestilling(
        tiltakskode = Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
        arrangorHovedenhet = Organisasjonsnummer("234567891"),
        arrangorUnderenhet = Organisasjonsnummer("123456789"),
        kostnadssted = NavEnhetNummer("0400"),
        bestillingsnummer = bestillingsnummer,
        avtalenummer = null,
        belop = 1000,
        periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
        status = status,
        opprettelse = Bestilling.Totrinnskontroll(
            behandletAv = OkonomiPart.System(OkonomiSystem.TILTAKSADMINISTRASJON),
            behandletTidspunkt = LocalDate.of(2025, 1, 1).atStartOfDay(),
            besluttetAv = OkonomiPart.System(OkonomiSystem.TILTAKSADMINISTRASJON),
            besluttetTidspunkt = LocalDate.of(2025, 1, 1).atStartOfDay(),
        ),
        annullering = null,
        linjer = listOf(
            Bestilling.Linje(
                linjenummer = 1,
                periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                belop = 1000,
            ),
        ),
    )
}

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
    gjorOppBestilling = false,
    beskrivelse = "Beskrivelse",
)

private fun createGjorOppBestilling(bestillingsnummer: String) = GjorOppBestilling(
    bestillingsnummer = bestillingsnummer,
    behandletAv = OkonomiPart.System(OkonomiSystem.TILTAKSADMINISTRASJON),
    behandletTidspunkt = LocalDate.of(2025, 1, 1).atStartOfDay(),
    besluttetAv = OkonomiPart.System(OkonomiSystem.TILTAKSADMINISTRASJON),
    besluttetTidspunkt = LocalDate.of(2025, 1, 1).atStartOfDay(),
)
