package no.nav.tiltak.okonomi.oebs

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.model.*
import no.nav.tiltak.okonomi.*
import no.nav.tiltak.okonomi.model.Bestilling
import no.nav.tiltak.okonomi.model.Faktura
import no.nav.tiltak.okonomi.model.OebsKontering
import java.time.LocalDate

class OebsMeldingMapperTest : FunSpec({

    val bestilling = Bestilling(
        tiltakskode = Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
        arrangorHovedenhet = Organisasjonsnummer("123456789"),
        arrangorUnderenhet = Organisasjonsnummer("234567890"),
        kostnadssted = NavEnhetNummer("0400"),
        bestillingsnummer = "A-1",
        avtalenummer = null,
        belop = 1000,
        periode = Periode(LocalDate.of(2025, 2, 1), LocalDate.of(2025, 4, 1)),
        status = BestillingStatusType.AKTIV,
        opprettelse = Bestilling.Totrinnskontroll(
            behandletAv = OkonomiPart.System(OkonomiSystem.TILTAKSADMINISTRASJON),
            behandletTidspunkt = LocalDate.of(2025, 1, 1).atStartOfDay(),
            besluttetAv = OkonomiPart.NavAnsatt(NavIdent("Z123456")),
            besluttetTidspunkt = LocalDate.of(2025, 1, 2).atStartOfDay(),
        ),
        annullering = null,
        linjer = listOf(
            Bestilling.Linje(
                linjenummer = 1,
                periode = Periode(
                    LocalDate.of(2025, 2, 1),
                    LocalDate.of(2025, 3, 1),
                ),
                belop = 500,
            ),
            Bestilling.Linje(
                linjenummer = 2,
                periode = Periode(
                    LocalDate.of(2025, 3, 1),
                    LocalDate.of(2025, 4, 1),
                ),
                belop = 500,
            ),
        ),
    )

    val faktura = Faktura(
        fakturanummer = "4567",
        bestillingsnummer = "A-1",
        kontonummer = Kontonummer("12345678901"),
        kid = Kid("123123123123123"),
        belop = 500,
        periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
        status = FakturaStatusType.SENDT,
        behandletAv = OkonomiPart.System(OkonomiSystem.TILTAKSADMINISTRASJON),
        behandletTidspunkt = LocalDate.of(2025, 2, 1).atStartOfDay(),
        besluttetAv = OkonomiPart.NavAnsatt(NavIdent("Z123456")),
        besluttetTidspunkt = LocalDate.of(2025, 2, 2).atStartOfDay(),
        linjer = listOf(
            Faktura.Linje(
                linjenummer = 1,
                periode = Periode(
                    LocalDate.of(2025, 1, 1),
                    LocalDate.of(2025, 2, 1),
                ),
                belop = 500,
            ),
            Faktura.Linje(
                linjenummer = 2,
                periode = Periode(
                    LocalDate.of(2025, 2, 1),
                    LocalDate.of(2025, 3, 1),
                ),
                belop = 500,
            ),
        ),
    )

    context("toOebsBestillingMelding") {
        val kontering = OebsKontering(
            tilskuddstype = Tilskuddstype.TILTAK_DRIFTSTILSKUDD,
            tiltakskode = Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
            periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2026, 1, 1)),
            statligRegnskapskonto = "12345678901",
            statligArtskonto = "23456789012",
        )

        val selger = OebsBestillingMelding.Selger(
            organisasjonsNummer = "123456789",
            bedriftsNummer = "234567890",
            organisasjonsNavn = "Bedrift AS",
            adresse = listOf(),
        )

        test("mapper linjer med antall utledet fra beløp og riktig periode") {
            val melding = OebsMeldingMapper.toOebsBestillingMelding(
                bestilling = bestilling,
                kontering = kontering,
                selger = selger,
            )

            melding.startDato shouldBe LocalDate.of(2025, 2, 1)
            melding.sluttDato shouldBe LocalDate.of(2025, 3, 31)
            melding.bestillingsLinjer shouldBe listOf(
                OebsBestillingMelding.Linje(
                    linjeNummer = 1,
                    antall = 500,
                    pris = 1,
                    periode = "02",
                    startDato = LocalDate.of(2025, 2, 1),
                    sluttDato = LocalDate.of(2025, 2, 28),
                ),
                OebsBestillingMelding.Linje(
                    linjeNummer = 2,
                    antall = 500,
                    pris = 1,
                    periode = "03",
                    startDato = LocalDate.of(2025, 3, 1),
                    sluttDato = LocalDate.of(2025, 3, 31),
                ),
            )
        }
    }

    context("toOebsFakturaMelding") {
        test("mapper linjer utledet fra bestillingens linjer") {
            val melding = OebsMeldingMapper.toOebsFakturaMelding(
                bestilling = bestilling,
                faktura = faktura,
                erSisteFaktura = false,
            )

            melding.fakturaLinjer shouldBe listOf(
                OebsFakturaMelding.Linje(
                    bestillingsNummer = "A-1",
                    bestillingsLinjeNummer = 1,
                    antall = 500,
                    pris = 1,
                    erSisteFaktura = false,
                ),
                OebsFakturaMelding.Linje(
                    bestillingsNummer = "A-1",
                    bestillingsLinjeNummer = 2,
                    antall = 500,
                    pris = 1,
                    erSisteFaktura = false,
                ),
            )
        }

        test("siste linje blir satt til erSisteFaktura når erSisteFaktura = true") {
            val melding = OebsMeldingMapper.toOebsFakturaMelding(
                bestilling = bestilling,
                faktura = faktura,
                erSisteFaktura = true,
            )

            melding.fakturaLinjer shouldBe listOf(
                OebsFakturaMelding.Linje(
                    bestillingsNummer = "A-1",
                    bestillingsLinjeNummer = 1,
                    antall = 500,
                    pris = 1,
                    erSisteFaktura = false,
                ),
                OebsFakturaMelding.Linje(
                    bestillingsNummer = "A-1",
                    bestillingsLinjeNummer = 2,
                    antall = 500,
                    pris = 1,
                    erSisteFaktura = true,
                ),
            )
        }
    }
})
