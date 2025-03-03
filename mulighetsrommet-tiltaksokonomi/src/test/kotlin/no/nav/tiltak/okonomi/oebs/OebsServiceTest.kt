package no.nav.tiltak.okonomi.oebs

import arrow.core.right
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.mulighetsrommet.brreg.BrregAdresse
import no.nav.mulighetsrommet.brreg.BrregClient
import no.nav.mulighetsrommet.brreg.BrregHovedenhetDto
import no.nav.mulighetsrommet.brreg.SlettetBrregHovedenhetDto
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.ktor.createMockEngine
import no.nav.mulighetsrommet.model.*
import no.nav.tiltak.okonomi.*
import no.nav.tiltak.okonomi.db.OkonomiDatabase
import no.nav.tiltak.okonomi.model.*
import java.time.LocalDate

class OebsServiceTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    lateinit var db: OkonomiDatabase

    beforeSpec {
        db = OkonomiDatabase(database.db)

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

    context("opprett bestilling") {
        test("feiler når oebs svarer med feil") {
            coEvery { brreg.getHovedenhet(Organisasjonsnummer("123456789")) } returns leverandor.right()
            val service = OebsService(db, oebsClient(oebsRespondError()), brreg)

            val opprettBestilling = createOpprettBestilling("1")
            service.opprettBestilling(opprettBestilling).shouldBeLeft().should {
                it.message shouldBe "Klarte ikke sende bestilling 1 til oebs"
            }
        }

        test("feiler når kontering mangler for bestilling") {
            coEvery { brreg.getHovedenhet(Organisasjonsnummer("123456789")) } returns leverandor.right()
            val service = OebsService(db, oebsClient(oebsRespondOk()), brreg)

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
            val service = OebsService(db, oebsClient(oebsRespondOk()), brreg)

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
            val service = OebsService(db, oebsClient(oebsRespondOk()), brreg)

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

        test("skal opprette bestilling hos oebs") {
            val bestillingsnummer = "1"

            coEvery { brreg.getHovedenhet(Organisasjonsnummer("123456789")) } returns leverandor.right()
            val service = OebsService(db, oebsClient(oebsRespondOk()), brreg)

            val opprettBestilling = createOpprettBestilling(bestillingsnummer)
            service.opprettBestilling(opprettBestilling).shouldBeRight().should {
                it.bestillingsnummer shouldBe bestillingsnummer
                it.status shouldBe BestillingStatusType.BESTILT
            }
        }

        test("svarer med eksisterende bestilling når bestillingsnummer allerede er kjent") {
            val bestillingsnummer = "10"

            val opprettBestilling = createOpprettBestilling(bestillingsnummer)
            db.session {
                val bestilling = Bestilling.fromOpprettBestilling(opprettBestilling).copy(
                    status = BestillingStatusType.OPPGJORT,
                )
                queries.bestilling.insertBestilling(bestilling)
            }

            val service = OebsService(db, oebsClient(oebsRespondOk()), brreg)

            service.opprettBestilling(opprettBestilling).shouldBeRight().should {
                it.bestillingsnummer shouldBe bestillingsnummer
                it.status shouldBe BestillingStatusType.OPPGJORT
            }
        }
    }

    context("annuller bestilling") {
        test("annullering feiler når bestilling ikke finnes") {
            val service = OebsService(db, oebsClient(oebsRespondOk()), brreg)

            val annullerBestilling = createAnnullerBestilling("4")
            service.annullerBestilling(annullerBestilling).shouldBeLeft().should {
                it.message shouldBe "Bestilling 4 finnes ikke"
            }
        }

        test("annullering feiler når bestilling er oppgjort") {
            val bestillingsnummer = "4"

            db.session {
                val bestilling = Bestilling.fromOpprettBestilling(createOpprettBestilling(bestillingsnummer)).copy(
                    status = BestillingStatusType.OPPGJORT,
                )
                queries.bestilling.insertBestilling(bestilling)
            }

            coEvery { brreg.getHovedenhet(Organisasjonsnummer("123456789")) } returns leverandor.right()
            val service = OebsService(db, oebsClient(oebsRespondOk()), brreg)

            val annullerBestilling = createAnnullerBestilling(bestillingsnummer)
            service.annullerBestilling(annullerBestilling).shouldBeLeft().should {
                it.message shouldBe "Bestilling 4 kan ikke annulleres fordi den er oppgjort"
            }
        }

        test("annullering feiler når det finnes fakturaer for bestilling") {
            val bestillingsnummer = "5"

            db.session {
                val bestilling = Bestilling.fromOpprettBestilling(createOpprettBestilling(bestillingsnummer))
                queries.bestilling.insertBestilling(bestilling)

                val faktura = Faktura.fromOpprettFaktura(
                    createOpprettFaktura(bestillingsnummer, "5-1"),
                    bestilling.linjer,
                )
                queries.faktura.insertFaktura(faktura)
            }

            coEvery { brreg.getHovedenhet(Organisasjonsnummer("123456789")) } returns leverandor.right()
            val service = OebsService(db, oebsClient(oebsRespondOk()), brreg)

            val annullerBestilling = createAnnullerBestilling(bestillingsnummer)
            service.annullerBestilling(annullerBestilling).shouldBeLeft().should {
                it.message shouldBe "Bestilling 5 kan ikke annulleres fordi det finnes fakturaer for bestillingen"
            }
        }

        val bestillingsnummer = "6"

        db.session {
            val bestilling = Bestilling.fromOpprettBestilling(createOpprettBestilling(bestillingsnummer))
            queries.bestilling.insertBestilling(bestilling)
        }

        test("annullering feiler når oebs svarer med feilkoder") {
            coEvery { brreg.getHovedenhet(Organisasjonsnummer("123456789")) } returns leverandor.right()
            val service = OebsService(db, oebsClient(oebsRespondError()), brreg)

            val annullerBestilling = createAnnullerBestilling(bestillingsnummer)
            service.annullerBestilling(annullerBestilling).shouldBeLeft().should {
                it.message shouldBe "Klarte ikke annullere bestilling 6 hos oebs"
            }
        }

        test("annullering av bestilling") {
            coEvery { brreg.getHovedenhet(Organisasjonsnummer("123456789")) } returns leverandor.right()
            val service = OebsService(db, oebsClient(oebsRespondOk()), brreg)

            val annullerBestilling = createAnnullerBestilling(bestillingsnummer)
            service.annullerBestilling(annullerBestilling).shouldBeRight().should {
                it.bestillingsnummer shouldBe bestillingsnummer
                it.status shouldBe BestillingStatusType.ANNULLERT
            }
        }

        test("noop når bestilling allerede er annullert") {
            val service = OebsService(db, oebsClient(oebsRespondOk()), brreg)

            val annullerBestilling = createAnnullerBestilling(bestillingsnummer)
            service.annullerBestilling(annullerBestilling).shouldBeRight().should {
                it.bestillingsnummer shouldBe bestillingsnummer
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
            val service = OebsService(db, oebsClient(oebsRespondOk()), brreg)

            val opprettFaktura = createOpprettFaktura("B-2", "F-1")
            service.opprettFaktura(opprettFaktura).shouldBeLeft().should {
                it.message shouldBe "Bestilling B-2 finnes ikke"
            }
        }

        test("feiler når oebs svarer med feilkoder") {
            val service = OebsService(db, oebsClient(oebsRespondError()), brreg)

            val opprettFaktura = createOpprettFaktura(bestillingsnummer, "F-1")
            service.opprettFaktura(opprettFaktura).shouldBeLeft().should {
                it.message shouldBe "Klarte ikke sende faktura F-1 til oebs"
            }
        }

        test("skal opprette faktura hos oebs") {
            val service = OebsService(db, oebsClient(oebsRespondOk()), brreg)

            val opprettFaktura = createOpprettFaktura(bestillingsnummer, "F-2")
            service.opprettFaktura(opprettFaktura).shouldBeRight().should {
                it.fakturanummer shouldBe "F-2"
                it.status shouldBe FakturaStatusType.UTBETALT
            }
        }
    }
})

private fun oebsRespondError() = createMockEngine {
    post("/api/v1/tilsagn") { respondError(HttpStatusCode.InternalServerError) }

    post("/api/v1/refusjonskrav") { respondError(HttpStatusCode.InternalServerError) }
}

private fun oebsRespondOk() = createMockEngine {
    post("/api/v1/tilsagn") { respondOk() }

    post("/api/v1/refusjonskrav") { respondOk() }
}

private fun oebsClient(mockEngine: MockEngine): OebsTiltakApiClient {
    return OebsTiltakApiClient(mockEngine, "http://localhost") { "token" }
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
)
