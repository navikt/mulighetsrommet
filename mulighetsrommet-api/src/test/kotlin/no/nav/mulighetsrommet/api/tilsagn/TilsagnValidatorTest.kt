package no.nav.mulighetsrommet.api.tilsagn

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.core.spec.style.FunSpec
import no.nav.mulighetsrommet.api.fixtures.TilsagnFixtures
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.tilsagn.model.*
import no.nav.mulighetsrommet.api.utils.DatoUtils.formaterDatoTilEuropeiskDatoformat
import no.nav.mulighetsrommet.model.Periode
import java.time.LocalDate
import java.util.UUID

class TilsagnValidatorTest : FunSpec({
    context("validate Tilsagn") {
        test("Arrangør slettet") {
            TilsagnValidator.validate(
                TilsagnFixtures.TilsagnRequest1,
                previous = null,
                gyldigTilsagnPeriode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2026, 1, 1)),
                gjennomforingSluttDato = null,
                arrangorSlettet = true,
                tiltakstypeNavn = "AFT",
                avtalteSatser = emptyList(),
            ) shouldBeLeft listOf(
                FieldError.of("Tilsagn kan ikke opprettes fordi arrangøren er slettet i Brreg", TilsagnRequest::id),
            )
        }

        test("null antall plasser samtidig som null periodeStart") {
            TilsagnValidator.validate(
                TilsagnFixtures.TilsagnRequest1.copy(
                    periodeStart = null,
                    beregning = TilsagnBeregningRequest(
                        type = TilsagnBeregningType.PRIS_PER_MANEDSVERK,
                    ),
                ),
                previous = null,
                gyldigTilsagnPeriode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2026, 1, 1)),
                gjennomforingSluttDato = null,
                arrangorSlettet = false,
                tiltakstypeNavn = "AFT",
                avtalteSatser = emptyList(),
            ) shouldBeLeft listOf(
                FieldError.of("Periodestart må være satt", TilsagnRequest::periodeStart),
                FieldError.ofPointer(pointer = "/beregning/antallPlasser", detail = "Antall plasser må være større enn 0"),
            )
        }

        test("should validate gyldig periode") {
            val gyldigStart = LocalDate.of(2025, 1, 8)
            val gyldigSlutt = LocalDate.of(2025, 1, 9)
            TilsagnValidator.validate(
                TilsagnFixtures.TilsagnRequest1,
                previous = null,
                gyldigTilsagnPeriode = Periode.fromInclusiveDates(gyldigStart, gyldigSlutt),
                gjennomforingSluttDato = null,
                arrangorSlettet = false,
                tiltakstypeNavn = "AFT",
                avtalteSatser = emptyList(),
            ) shouldBeLeft listOf(
                FieldError.of("Minimum startdato for tilsagn til AFT er ${gyldigStart.formaterDatoTilEuropeiskDatoformat()}", TilsagnRequest::periodeStart),
                FieldError.of("Maksimum sluttdato for tilsagn til AFT er ${gyldigSlutt.formaterDatoTilEuropeiskDatoformat()}", TilsagnRequest::periodeSlutt),
            )
        }

        test("feil i beregning dukker opp") {
            TilsagnValidator.validate(
                TilsagnFixtures.TilsagnRequest1
                    .copy(
                        beregning = TilsagnBeregningRequest(type = TilsagnBeregningType.FRI),
                    ),
                previous = null,
                gyldigTilsagnPeriode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2026, 1, 1)),
                gjennomforingSluttDato = null,
                arrangorSlettet = false,
                tiltakstypeNavn = "AFT",
                avtalteSatser = emptyList(),
            ) shouldBeLeft listOf(
                FieldError.of("Du må legge til en linje", TilsagnRequest::beregning, TilsagnBeregningRequest::linjer),
            )
        }

        context("TilsagnBeregningFri.Input") {
            test("should return field error if linjer is empty") {
                val input = TilsagnBeregningRequest(
                    type = TilsagnBeregningType.FRI,
                    linjer = emptyList(),
                    prisbetingelser = null,
                )
                TilsagnValidator.validateBeregningFriInput(input).shouldBeLeft()
            }

            test("should return list of field error for invalid input") {
                val input = TilsagnBeregningRequest(
                    type = TilsagnBeregningType.FRI,
                    linjer = listOf(
                        TilsagnInputLinjeRequest(
                            belop = 0,
                            id = UUID.randomUUID(),
                            beskrivelse = "",
                            antall = 0,
                        ),
                    ),
                    prisbetingelser = null,
                )
                val leftFieldErrors = listOf(
                    FieldError(pointer = "/beregning/linjer/0/belop", detail = "Beløp må være positivt"),
                    FieldError(pointer = "/beregning/linjer/0/beskrivelse", detail = "Beskrivelse mangler"),
                    FieldError(pointer = "/beregning/linjer/0/antall", detail = "Antall må være positivt"),
                )

                TilsagnValidator.validateBeregningFriInput(input) shouldBeLeft leftFieldErrors
            }
        }
    }
})
