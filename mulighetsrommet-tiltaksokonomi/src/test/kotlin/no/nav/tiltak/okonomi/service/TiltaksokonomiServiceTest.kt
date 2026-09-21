package no.nav.tiltak.okonomi.service

import arrow.core.right
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondError
import io.ktor.client.engine.mock.respondOk
import io.ktor.http.HttpStatusCode
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.serialization.json.Json
import kotliquery.queryOf
import no.nav.common.kafka.producer.feilhandtering.StoredProducerRecord
import no.nav.common.kafka.util.KafkaUtils
import no.nav.mulighetsrommet.brreg.BrregAdresse
import no.nav.mulighetsrommet.brreg.BrregClient
import no.nav.mulighetsrommet.brreg.BrregHovedenhetDto
import no.nav.mulighetsrommet.brreg.BrregUnderenhetDto
import no.nav.mulighetsrommet.brreg.SlettetBrregHovedenhetDto
import no.nav.mulighetsrommet.brreg.SlettetBrregUnderenhetDto
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.requireSingle
import no.nav.mulighetsrommet.kafka.toStoredProducerRecord
import no.nav.mulighetsrommet.ktor.createMockEngine
import no.nav.mulighetsrommet.ktor.decodeRequestBody
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Valuta
import no.nav.tiltak.okonomi.AnnullerBestilling
import no.nav.tiltak.okonomi.BestillingStatus
import no.nav.tiltak.okonomi.BestillingStatusType
import no.nav.tiltak.okonomi.Bestillingsnummer
import no.nav.tiltak.okonomi.FAGSYSTEM_HEADER_NAME
import no.nav.tiltak.okonomi.FakturaStatus
import no.nav.tiltak.okonomi.FakturaStatusType
import no.nav.tiltak.okonomi.Fakturanummer
import no.nav.tiltak.okonomi.GjorOppBestilling
import no.nav.tiltak.okonomi.KafkaTopics
import no.nav.tiltak.okonomi.OkonomiFagsystem
import no.nav.tiltak.okonomi.OkonomiPart
import no.nav.tiltak.okonomi.OpprettBestilling
import no.nav.tiltak.okonomi.OpprettFaktura
import no.nav.tiltak.okonomi.Tilskuddstype
import no.nav.tiltak.okonomi.databaseConfig
import no.nav.tiltak.okonomi.db.OkonomiDatabase
import no.nav.tiltak.okonomi.db.QueryContext
import no.nav.tiltak.okonomi.model.Bestilling
import no.nav.tiltak.okonomi.model.Faktura
import no.nav.tiltak.okonomi.oebs.OebsBestillingMelding
import no.nav.tiltak.okonomi.oebs.OebsFakturaKvittering
import no.nav.tiltak.okonomi.oebs.OebsFakturaMelding
import no.nav.tiltak.okonomi.oebs.OebsKontering
import no.nav.tiltak.okonomi.oebs.OebsPoApClient
import org.intellij.lang.annotations.Language
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

class TiltaksokonomiServiceTest : FunSpec({
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

    fun createOkonomiService(
        oebsTiltakApiClient: OebsPoApClient,
    ) = TiltaksokonomiService(
        config = TiltaksokonomiService.Config(
            topics = KafkaTopics("bestilling-status", "faktura-status"),
        ),
        db = db,
        oebs = oebsTiltakApiClient,
        brreg = brreg,
    )

    context("opprett bestilling") {
        val fagsystem = OkonomiFagsystem.TILTAKSADMINISTRASJON

        test("feiler når oebs svarer med feil") {
            coEvery { brreg.getBrregEnhet(Organisasjonsnummer("123456789")) } returns arrangorHovedenhet.right()
            coEvery { brreg.getBrregEnhet(Organisasjonsnummer("234567891")) } returns arrangorUnderenhet.right()

            val service = createOkonomiService(oebsClient(oebsRespondError()))

            val opprettBestilling = createOpprettBestilling(Bestillingsnummer("A-1-1"))
            service.opprettBestilling(fagsystem, opprettBestilling).shouldBeLeft().should {
                it.message shouldBe "Klarte ikke sende bestilling A-1-1 til oebs"
            }
        }

        test("feiler når kontering mangler for bestilling") {
            coEvery { brreg.getBrregEnhet(Organisasjonsnummer("123456789")) } returns arrangorHovedenhet.right()
            coEvery { brreg.getBrregEnhet(Organisasjonsnummer("234567891")) } returns arrangorUnderenhet.right()

            val service = createOkonomiService(oebsClient(oebsRespondOk()))

            val opprettBestilling = createOpprettBestilling(Bestillingsnummer("A-2-1")).copy(
                periode = Periode.forMonthOf(LocalDate.of(1990, 1, 1)),
            )
            service.opprettBestilling(fagsystem, opprettBestilling).shouldBeLeft().should {
                it.message shouldBe "Kontering for bestilling A-2-1 mangler"
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

            val opprettBestilling = createOpprettBestilling(Bestillingsnummer("A-3-1"))

            service.opprettBestilling(fagsystem, opprettBestilling).shouldBeLeft().should {
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

            val opprettBestilling = createOpprettBestilling(Bestillingsnummer("A-3-1"))

            service.opprettBestilling(fagsystem, opprettBestilling).shouldBeLeft().should {
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

            val opprettBestilling = createOpprettBestilling(Bestillingsnummer("A-3-1"))

            service.opprettBestilling(fagsystem, opprettBestilling).shouldBeLeft().should {
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

            val opprettBestilling = createOpprettBestilling(Bestillingsnummer("A-11-1"))

            service.opprettBestilling(fagsystem, opprettBestilling).shouldBeRight().should {
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

            val opprettBestilling = createOpprettBestilling(Bestillingsnummer("A-3-1"))

            service.opprettBestilling(fagsystem, opprettBestilling).shouldBeRight().should {
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

            val opprettBestilling = createOpprettBestilling(Bestillingsnummer("A-1-1"))
            service.opprettBestilling(fagsystem, opprettBestilling).shouldBeRight().should {
                it.arrangorHovedenhet shouldBe Organisasjonsnummer("920238076")
                it.arrangorUnderenhet shouldBe Organisasjonsnummer("234567891")
            }
        }

        test("skal opprette bestilling hos oebs og lagrer utgående melding om status for bestilling") {
            coEvery { brreg.getBrregEnhet(Organisasjonsnummer("123456789")) } returns arrangorHovedenhet.right()
            coEvery { brreg.getBrregEnhet(Organisasjonsnummer("234567891")) } returns arrangorUnderenhet.right()

            val service = createOkonomiService(oebsClient(oebsRespondOk()))

            val bestillingsnummer = Bestillingsnummer("A-1-1")
            val opprettBestilling = createOpprettBestilling(bestillingsnummer)

            service.opprettBestilling(fagsystem, opprettBestilling).shouldBeRight().should {
                it.bestillingsnummer shouldBe bestillingsnummer
                it.status shouldBe BestillingStatusType.SENDT
            }

            db.session { getLatestRecord() }.should {
                it.topic shouldBe "bestilling-status"
                it.key.toString(Charsets.UTF_8) shouldBe "A-1-1"

                val bestillingStatus = Json.decodeFromString<BestillingStatus>(it.value.toString(Charsets.UTF_8))
                bestillingStatus.bestillingsnummer shouldBe bestillingsnummer
                bestillingStatus.status shouldBe BestillingStatusType.SENDT

                val header = KafkaUtils.jsonToHeaders(it.headersJson).shouldHaveSize(1).first()
                header.key() shouldBe FAGSYSTEM_HEADER_NAME
                String(header.value()) shouldBe "TILTAKSADMINISTRASJON"
            }
        }

        test("svarer med eksisterende bestilling og lagrer utgående melding når bestillingsnummer allerede er kjent") {
            val bestillingsnummer = Bestillingsnummer("A-10-1")
            val opprettBestilling = createOpprettBestilling(bestillingsnummer)
            db.session {
                val bestilling = Bestilling.fromOpprettBestilling(
                    fagsystem = fagsystem,
                    bestilling = opprettBestilling,
                    arrangorHovedenhet = arrangorHovedenhet.organisasjonsnummer,
                ).copy(
                    status = BestillingStatusType.OPPGJORT,
                )
                queries.bestilling.insertBestilling(bestilling)
            }

            val service = createOkonomiService(oebsClient(oebsRespondOk()))

            service.opprettBestilling(fagsystem, opprettBestilling).shouldBeRight().should {
                it.bestillingsnummer shouldBe bestillingsnummer
                it.status shouldBe BestillingStatusType.OPPGJORT
            }

            db.session { getLatestRecord() }.should {
                it.topic shouldBe "bestilling-status"
                it.key.toString(Charsets.UTF_8) shouldBe bestillingsnummer.value

                val bestillingStatus = Json.decodeFromString<BestillingStatus>(it.value.toString(Charsets.UTF_8))
                bestillingStatus.bestillingsnummer shouldBe bestillingsnummer
                bestillingStatus.status shouldBe BestillingStatusType.OPPGJORT
            }
        }
    }

    context("annuller bestilling") {
        test("annullering feiler når bestilling ikke finnes") {
            val service = createOkonomiService(oebsClient(oebsRespondOk()))

            val annullerBestilling = createAnnullerBestilling(Bestillingsnummer("A-4-1"))
            service.annullerBestilling(annullerBestilling).shouldBeLeft().should {
                it.message shouldBe "Bestilling A-4-1 finnes ikke"
            }
        }

        test("annullering feiler når bestilling er oppgjort") {
            db.session {
                val bestilling = createBestilling(Bestillingsnummer("A-4-1"), status = BestillingStatusType.OPPGJORT)
                queries.bestilling.insertBestilling(bestilling)
            }

            val service = createOkonomiService(oebsClient(oebsRespondOk()))

            val annullerBestilling = createAnnullerBestilling(Bestillingsnummer("A-4-1"))
            service.annullerBestilling(annullerBestilling).shouldBeLeft().should {
                it.message shouldBe "Bestilling A-4-1 kan ikke annulleres fordi den har status OPPGJORT"
            }
        }

        test("annullering feiler når det finnes fakturaer for bestilling") {
            db.session {
                val bestilling = createBestilling(Bestillingsnummer("A-5-1"), status = BestillingStatusType.AKTIV)
                queries.bestilling.insertBestilling(bestilling)

                val faktura = Faktura.fromOpprettFaktura(
                    createOpprettFaktura(Bestillingsnummer("A-5-1"), Fakturanummer("A-5-1-1")),
                    bestilling.linjer,
                ).copy(status = FakturaStatusType.FULLT_BETALT)
                queries.faktura.insertFaktura(faktura)
            }

            val service = createOkonomiService(oebsClient(oebsRespondOk()))

            val annullerBestilling = createAnnullerBestilling(Bestillingsnummer("A-5-1"))
            service.annullerBestilling(annullerBestilling).shouldBeLeft().should {
                it.message shouldBe "Bestilling A-5-1 kan ikke annulleres fordi det finnes fakturaer for bestillingen"
            }
        }

        val bestilling = createBestilling(Bestillingsnummer("A-6-1"), status = BestillingStatusType.AKTIV)
        db.session {
            queries.bestilling.insertBestilling(bestilling)
        }

        test("annullering feiler når oebs svarer med feilkoder") {
            val service = createOkonomiService(oebsClient(oebsRespondError()))

            val annullerBestilling = createAnnullerBestilling(Bestillingsnummer("A-6-1"))
            service.annullerBestilling(annullerBestilling).shouldBeLeft().should {
                it.message shouldBe "Klarte ikke annullere bestilling A-6-1 hos oebs"
            }
        }

        test("annullering feiler når bestilling ikke er aktiv ennå") {
            val bestilling2 = createBestilling(Bestillingsnummer("A-87-1"))
            db.session {
                queries.bestilling.insertBestilling(bestilling2)
            }
            val service = createOkonomiService(oebsClient(oebsRespondError()))

            val annullerBestilling = createAnnullerBestilling(Bestillingsnummer("A-87-1"))
            service.annullerBestilling(annullerBestilling).shouldBeLeft().should {
                it.message shouldBe "Bestilling A-87-1 kan ikke annulleres fordi den har status SENDT"
            }
        }

        test("annullering av bestilling lagrer utgående melding om status for bestilling") {
            val service = createOkonomiService(oebsClient(oebsRespondOk()))

            val bestillingsnummer = Bestillingsnummer("A-6-1")
            val annullerBestilling = createAnnullerBestilling(bestillingsnummer)
            service.annullerBestilling(annullerBestilling).shouldBeRight().should {
                it.bestillingsnummer shouldBe bestillingsnummer
                it.status shouldBe BestillingStatusType.ANNULLERING_SENDT
            }

            db.session { getLatestRecord() }.should {
                it.topic shouldBe "bestilling-status"
                it.key.toString(Charsets.UTF_8) shouldBe bestillingsnummer.value

                val bestillingStatus = Json.decodeFromString<BestillingStatus>(it.value.toString(Charsets.UTF_8))
                bestillingStatus.bestillingsnummer shouldBe bestillingsnummer
                bestillingStatus.status shouldBe BestillingStatusType.ANNULLERING_SENDT
            }
        }

        test("svarer med eksisterende bestilling og lagrer utgående melding når bestilling allerede er annullert") {
            val service = createOkonomiService(oebsClient(oebsRespondOk()))

            val bestillingsnummer = Bestillingsnummer("A-6-1")
            val annullerBestilling = createAnnullerBestilling(bestillingsnummer)
            service.annullerBestilling(annullerBestilling).shouldBeRight().should {
                it.bestillingsnummer shouldBe bestillingsnummer
                it.status shouldBe BestillingStatusType.ANNULLERING_SENDT
            }

            db.session { getLatestRecord() }.should {
                it.topic shouldBe "bestilling-status"
                it.key.toString(Charsets.UTF_8) shouldBe bestillingsnummer.value
            }
        }
    }

    context("opprett faktura") {
        val bestillingsnummer = Bestillingsnummer("B-1-1")

        db.session {
            val bestilling = createBestilling(bestillingsnummer, status = BestillingStatusType.AKTIV)
            queries.bestilling.insertBestilling(bestilling)
        }

        test("feiler når bestilling ikke finnes") {
            val service = createOkonomiService(oebsClient(oebsRespondOk()))

            val opprettFaktura = createOpprettFaktura(Bestillingsnummer("B-2-1"), Fakturanummer("B-1-1-F1"))
            service.opprettFaktura(opprettFaktura).shouldBeLeft().should {
                it.message shouldBe "Bestilling B-2-1 finnes ikke"
            }
        }

        test("feiler når oebs svarer med feilkoder") {
            val service = createOkonomiService(oebsClient(oebsRespondError()))

            val opprettFaktura = createOpprettFaktura(bestillingsnummer, Fakturanummer("B-1-1-F1"))
            service.opprettFaktura(opprettFaktura).shouldBeLeft().should {
                it.message shouldBe "Klarte ikke sende faktura B-1-1-F1 til oebs"
            }
        }

        test("feiler når bestilling ikke er aktiv ennå") {
            val bestilling = createBestilling(Bestillingsnummer("A-876-1"))
            db.session {
                queries.bestilling.insertBestilling(bestilling)
            }

            val service = createOkonomiService(oebsClient(oebsRespondError()))

            val opprettFaktura = createOpprettFaktura(bestilling.bestillingsnummer, Fakturanummer("A-876-1-F1"))
            service.opprettFaktura(opprettFaktura).shouldBeLeft().should {
                it.message shouldBe "Faktura A-876-1-F1 kan ikke opprettes fordi bestilling A-876-1 har status SENDT"
            }
        }

        test("oppretter faktura hos oebs og lagrer utgående melding om status for faktura") {
            val service = createOkonomiService(oebsClient(oebsRespondOk()))

            val opprettFaktura = createOpprettFaktura(bestillingsnummer, Fakturanummer("B-1-1-F2"))
            service.opprettFaktura(opprettFaktura).shouldBeRight().should {
                it.fakturanummer shouldBe Fakturanummer("B-1-1-F2")
                it.status shouldBe FakturaStatusType.SENDT
            }

            db.session { getLatestRecord() }.should {
                it.topic shouldBe "faktura-status"
                it.key.toString(Charsets.UTF_8) shouldBe "B-1-1-F2"

                val fakturaStatus = Json.decodeFromString<FakturaStatus>(it.value.toString(Charsets.UTF_8))
                fakturaStatus.status shouldBe FakturaStatusType.SENDT
                fakturaStatus.fakturanummer shouldBe Fakturanummer("B-1-1-F2")

                val header = KafkaUtils.jsonToHeaders(it.headersJson).shouldHaveSize(1).first()
                header.key() shouldBe FAGSYSTEM_HEADER_NAME
                String(header.value()) shouldBe "TILTAKSADMINISTRASJON"
            }
        }

        test("sender ikke faktura til oebs før det finnes kvitteringer for tidligere fakturaer") {
            val bestilling2 = createBestilling(Bestillingsnummer("A-678-1"), status = BestillingStatusType.AKTIV)
            db.session {
                queries.bestilling.insertBestilling(bestilling2)
            }

            val service = createOkonomiService(oebsClient(oebsRespondOk()))

            val opprettFaktura1 = createOpprettFaktura(bestilling2.bestillingsnummer, Fakturanummer("A-678-1-F1"))
            service.opprettFaktura(opprettFaktura1).shouldBeRight().should {
                it.status shouldBe FakturaStatusType.SENDT
            }

            val opprettFaktura2 = createOpprettFaktura(bestilling2.bestillingsnummer, Fakturanummer("A-678-1-F2"))
            service.opprettFaktura(opprettFaktura2).shouldBeLeft().should {
                it.message shouldBe "Faktura A-678-1-F2 kan ikke opprettes fordi vi venter på kvittering"
            }
        }

        test("svarer med eksisterende faktura og lagrer utgående melding når fakturanummer allerede er kjent") {
            val opprettFaktura = createOpprettFaktura(bestillingsnummer, Fakturanummer("B-1-1-F3"))
            db.session {
                val bestilling = checkNotNull(queries.bestilling.getByBestillingsnummer(bestillingsnummer))
                val faktura = Faktura.fromOpprettFaktura(opprettFaktura, bestilling.linjer).copy(
                    status = FakturaStatusType.FULLT_BETALT,
                )
                queries.faktura.insertFaktura(faktura)
            }

            val service = createOkonomiService(oebsClient(oebsRespondOk()))

            service.opprettFaktura(opprettFaktura).shouldBeRight().should {
                it.fakturanummer shouldBe Fakturanummer("B-1-1-F3")
                it.status shouldBe FakturaStatusType.FULLT_BETALT
            }

            db.session { getLatestRecord() }.should {
                it.topic shouldBe "faktura-status"
                it.key.toString(Charsets.UTF_8) shouldBe "B-1-1-F3"
            }
        }
    }

    context("gjør opp bestilling") {
        test("faktura med gjorOppBestilling = true setter siste fakturalinje i fakturaen til oebs og oppdaterer status på bestilling") {
            val b2 = Bestillingsnummer("B-2-1")
            val bestilling = createBestilling(b2, status = BestillingStatusType.AKTIV)
            db.session {
                queries.bestilling.insertBestilling(bestilling)
            }

            val mockEngine = createMockEngine {
                post(OebsPoApClient.FAKTURA_ENDPOINT) {
                    val melding = it.decodeRequestBody<OebsFakturaMelding>()

                    melding.fakturaLinjer.last().erSisteFaktura shouldBe true

                    respondOk()
                }
            }
            val service = createOkonomiService(oebsClient(mockEngine))

            val opprettFaktura = createOpprettFaktura(b2, Fakturanummer("B-2-1-F1"))
                .copy(gjorOppBestilling = true)
            service.opprettFaktura(opprettFaktura).shouldBeRight().should {
                it.fakturanummer shouldBe Fakturanummer("B-2-1-F1")
                it.status shouldBe FakturaStatusType.SENDT
            }

            db.session { getLatestRecord(topic = "faktura-status") }.should {
                val fakturaStatus = Json.decodeFromString<FakturaStatus>(it.value.toString(Charsets.UTF_8))
                fakturaStatus.fakturanummer shouldBe Fakturanummer("B-2-1-F1")
                fakturaStatus.status shouldBe FakturaStatusType.SENDT
            }

            db.session {
                val bestilling = queries.bestilling.getByBestillingsnummer(b2)
                bestilling.shouldNotBeNull().status shouldBe BestillingStatusType.OPPGJORT
            }
        }

        test("operasjon for gjorOppBestilling lager en faktura med erSisteLinje = true og setter bestillingen til OPPGJORT") {
            val b3 = Bestillingsnummer("B-3-1")
            val bestilling = createBestilling(b3, status = BestillingStatusType.AKTIV)
            db.session {
                queries.bestilling.insertBestilling(bestilling)
            }

            val mockEngine = createMockEngine {
                post(OebsPoApClient.FAKTURA_ENDPOINT) {
                    val melding = it.decodeRequestBody<OebsFakturaMelding>()

                    melding.fakturaLinjer.last().erSisteFaktura shouldBe true

                    respondOk()
                }
            }

            val service = createOkonomiService(oebsClient(mockEngine))

            val gjorOppBestilling = createGjorOppBestilling(b3)
            service.gjorOppBestilling(gjorOppBestilling).shouldBeRight().should {
                it.fakturanummer shouldBe Fakturanummer("B-3-1-X")
                it.status shouldBe FakturaStatusType.SENDT
            }

            db.session {
                val bestilling = queries.bestilling.getByBestillingsnummer(b3)
                bestilling.shouldNotBeNull().status shouldBe BestillingStatusType.OPPGJORT
            }

            db.session { getLatestRecord(topic = "bestilling-status") }.should {
                val bestillingStatus = Json.decodeFromString<BestillingStatus>(it.value.toString(Charsets.UTF_8))
                bestillingStatus.bestillingsnummer shouldBe b3
                bestillingStatus.status shouldBe BestillingStatusType.OPPGJORT
            }
        }

        test("feiler hvis bestillingen ikke er aktiv") {
            val b4 = Bestillingsnummer("B-4-1")
            val bestilling = createBestilling(b4, status = BestillingStatusType.SENDT)
            db.session {
                queries.bestilling.insertBestilling(bestilling)
            }

            val service = createOkonomiService(oebsClient(oebsRespondError()))

            val gjorOppBestilling = createGjorOppBestilling(b4)
            service.gjorOppBestilling(gjorOppBestilling).shouldBeLeft().should {
                it.message shouldBe "Bestilling B-4-1 kan ikke gjøres opp fordi den har status SENDT"
            }
        }

        test("feiler når en faktura venter på kvittering") {
            val b5 = Bestillingsnummer("B-5-1")
            val bestilling = createBestilling(b5, status = BestillingStatusType.AKTIV)
            db.session {
                queries.bestilling.insertBestilling(bestilling)
            }

            val service = createOkonomiService(oebsClient(oebsRespondError()))

            val opprettFaktura = createOpprettFaktura(b5, Fakturanummer("B-5-1-F55"))
            db.session {
                val faktura = Faktura.fromOpprettFaktura(opprettFaktura, bestilling.linjer)
                queries.faktura.insertFaktura(faktura)
            }

            val gjorOppBestilling = createGjorOppBestilling(b5)
            service.gjorOppBestilling(gjorOppBestilling).shouldBeLeft().should {
                it.message shouldBe "Bestilling B-5-1 kan ikke gjøres opp fordi vi venter på kvittering"
            }
        }

        test("kvittering for gjorOppBestilling faktura publiseres ikke på kafka") {
            val bestillingsnummer = Bestillingsnummer("Z-7-1")
            val bestilling = createBestilling(bestillingsnummer, status = BestillingStatusType.AKTIV)
            db.session { queries.bestilling.insertBestilling(bestilling) }

            val mockEngine = createMockEngine {
                post(OebsPoApClient.FAKTURA_ENDPOINT) {
                    val melding = it.decodeRequestBody<OebsFakturaMelding>()

                    melding.fakturaLinjer.last().erSisteFaktura shouldBe true

                    respondOk()
                }
            }

            val service = createOkonomiService(oebsClient(mockEngine))

            val gjorOppBestilling = createGjorOppBestilling(bestillingsnummer)
            service.gjorOppBestilling(gjorOppBestilling).shouldBeRight()

            val faktura = db.session { queries.faktura.getByBestillingsnummer(bestillingsnummer) }.first()
            service.mottaFakturaKvittering(
                faktura = faktura,
                kvittering = OebsFakturaKvittering(
                    fakturaNummer = faktura.fakturanummer.value,
                    opprettelsesTidspunkt = LocalDateTime.now(),
                    statusBetalt = OebsFakturaKvittering.StatusBetalt.IkkeBetalt,
                ),
            )
            db.session { queries.kafkaProducerRecord.getRecords(10) }
                .filter { it.topic == "faktura-status" }
                .map {
                    Json.decodeFromString<FakturaStatus>(it.value.toString(Charsets.UTF_8))
                }
                .filter { it.fakturanummer == faktura.fakturanummer }
                .shouldHaveSize(0)
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

private fun createOpprettBestilling(
    bestillingsnummer: Bestillingsnummer,
    organisasjonsnummer: Organisasjonsnummer? = null,
) = OpprettBestilling(
    bestillingsnummer = bestillingsnummer,
    tilskuddstype = Tilskuddstype.TILTAK_DRIFTSTILSKUDD,
    tiltakskode = Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
    arrangor = OpprettBestilling.Arrangor.Norsk(organisasjonsnummer ?: Organisasjonsnummer("234567891")),
    avtalenummer = null,
    belop = 1000,
    behandletAv = OkonomiPart.Fagsystem(OkonomiFagsystem.TILTAKSADMINISTRASJON),
    behandletTidspunkt = Instant.parse("2025-01-01T00:00:00Z"),
    besluttetAv = OkonomiPart.Fagsystem(OkonomiFagsystem.TILTAKSADMINISTRASJON),
    besluttetTidspunkt = Instant.parse("2025-01-01T00:00:00Z"),
    periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
    kostnadssted = NavEnhetNummer("0400"),
    valuta = Valuta.NOK,
)

private fun createBestilling(
    bestillingsnummer: Bestillingsnummer,
    status: BestillingStatusType = BestillingStatusType.SENDT,
): Bestilling {
    return Bestilling(
        tiltakskode = Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
        arrangorHovedenhet = Organisasjonsnummer("234567891"),
        arrangorUnderenhet = Organisasjonsnummer("123456789"),
        kostnadssted = NavEnhetNummer("0400"),
        bestillingsnummer = bestillingsnummer,
        fagsystem = OkonomiFagsystem.TILTAKSADMINISTRASJON,
        avtalenummer = null,
        belop = 1000,
        periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
        status = status,
        statusSistOppdatert = Instant.parse("2025-01-01T00:00:00Z"),
        opprettelse = Bestilling.Totrinnskontroll(
            behandletAv = OkonomiPart.Fagsystem(OkonomiFagsystem.TILTAKSADMINISTRASJON),
            behandletTidspunkt = Instant.parse("2025-01-01T00:00:00Z"),
            besluttetAv = OkonomiPart.Fagsystem(OkonomiFagsystem.TILTAKSADMINISTRASJON),
            besluttetTidspunkt = Instant.parse("2025-01-01T00:00:00Z"),
        ),
        annullering = null,
        linjer = listOf(
            Bestilling.Linje(
                linjenummer = 1,
                periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                belop = 1000,
            ),
        ),
        valuta = Valuta.NOK,
    )
}

private fun createAnnullerBestilling(bestillingsnummer: Bestillingsnummer) = AnnullerBestilling(
    bestillingsnummer = bestillingsnummer,
    behandletAv = OkonomiPart.Fagsystem(OkonomiFagsystem.TILTAKSADMINISTRASJON),
    behandletTidspunkt = Instant.parse("2025-01-01T00:00:00Z"),
    besluttetAv = OkonomiPart.Fagsystem(OkonomiFagsystem.TILTAKSADMINISTRASJON),
    besluttetTidspunkt = Instant.parse("2025-01-01T00:00:00Z"),
)

private fun createOpprettFaktura(bestillingsnummer: Bestillingsnummer, fakturanummer: Fakturanummer) = OpprettFaktura(
    fakturanummer = fakturanummer,
    bestillingsnummer = bestillingsnummer,
    betalingsinformasjon = OpprettFaktura.Betalingsinformasjon.BBan(
        kontonummer = Kontonummer("12345678901"),
        kid = null,
    ),
    belop = 1000,
    periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
    behandletAv = OkonomiPart.Fagsystem(OkonomiFagsystem.TILTAKSADMINISTRASJON),
    behandletTidspunkt = Instant.parse("2025-01-01T00:00:00Z"),
    besluttetAv = OkonomiPart.Fagsystem(OkonomiFagsystem.TILTAKSADMINISTRASJON),
    besluttetTidspunkt = Instant.parse("2025-01-01T00:00:00Z"),
    gjorOppBestilling = false,
    beskrivelse = "Beskrivelse",
    valuta = Valuta.NOK,
)

private fun createGjorOppBestilling(bestillingsnummer: Bestillingsnummer) = GjorOppBestilling(
    bestillingsnummer = bestillingsnummer,
    behandletAv = OkonomiPart.Fagsystem(OkonomiFagsystem.TILTAKSADMINISTRASJON),
    behandletTidspunkt = Instant.parse("2025-01-01T00:00:00Z"),
    besluttetAv = OkonomiPart.Fagsystem(OkonomiFagsystem.TILTAKSADMINISTRASJON),
    besluttetTidspunkt = Instant.parse("2025-01-01T00:00:00Z"),
)
