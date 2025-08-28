package no.nav.mulighetsrommet.api.tilsagn

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.core.spec.style.FunSpec
import no.nav.mulighetsrommet.api.fixtures.TilsagnFixtures
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingDto.*
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.tilsagn.model.*
import no.nav.mulighetsrommet.model.Periode
import java.time.LocalDate
import java.util.UUID

class TilsagnValidatorTest : FunSpec({
    context("validate Tilsagn") {
        test("Arrangør slettet") {
            TilsagnValidator.validate(
                TilsagnFixtures.TilsagnRequest1,
                previous = null,
                minimumTilsagnPeriodeStart = LocalDate.of(2025, 1, 1),
                gjennomforingSluttDato = null,
                arrangorSlettet = true,
                tiltakstypeNavn = "AFT",
                avtalteSatser = emptyList(),
            ) shouldBeLeft listOf(
                FieldError.of(TilsagnRequest::id, "Tilsagn kan ikke opprettes fordi arrangøren er slettet i Brreg"),
            )
        }

        context("TilsagnBeregningFri.Input") {
            test("should return field error if linjer is empty") {
                val input = TilsagnBeregningRequest(
                    type = TilsagnBeregningType.FRI,
                    linjer = emptyList(),
                    prisbetingelser = null,
                )
                TilsagnValidator.validateBeregning(input, Periode.forMonthOf(LocalDate.of(2024, 1, 1)), null, emptyList()).shouldBeLeft()
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
                    FieldError(pointer = "beregning/linjer/0/belop", detail = "Beløp må være positivt"),
                    FieldError(pointer = "beregning/linjer/0/beskrivelse", detail = "Beskrivelse mangler"),
                    FieldError(pointer = "beregning/linjer/0/antall", detail = "Antall må være positivt"),
                )

                TilsagnValidator.validateBeregning(
                    input,
                    Periode.forMonthOf(LocalDate.of(2024, 1, 1)),
                    sats = null,
                    emptyList(),
                ) shouldBeLeft leftFieldErrors
            }
        }
    }
})
