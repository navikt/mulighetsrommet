package no.nav.tiltak.okonomi.oebs

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.tiltak.okonomi.Tilskuddstype
import no.nav.tiltak.okonomi.model.OebsKontering
import no.nav.tiltak.okonomi.test.Fixtures
import java.time.LocalDate

class OebsMeldingMapperTest : FunSpec({

    val bestilling = Fixtures.bestilling

    val faktura = Fixtures.faktura

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

        test("melding til leverandør blir bare satt når kid mangler") {
            val meldingMedKid = OebsMeldingMapper.toOebsFakturaMelding(
                bestilling = bestilling,
                faktura = faktura,
                erSisteFaktura = false,
            )

            meldingMedKid.kidNummer shouldBe "123123123123123"
            meldingMedKid.meldingTilLeverandor.shouldBeNull()

            val meldingUtenKid = OebsMeldingMapper.toOebsFakturaMelding(
                bestilling = bestilling,
                faktura = faktura.copy(kid = null),
                erSisteFaktura = false,
            )

            meldingUtenKid.kidNummer.shouldBeNull()
            meldingUtenKid.meldingTilLeverandor shouldBe """Utbetaling fra Nav
Tiltak: ARBEIDSFORBEREDENDE_TRENING
Periode: 01.01.2025 - 31.01.2025"""
        }
    }
})
