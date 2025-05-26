package no.nav.mulighetsrommet.api.tilsagn

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.core.spec.style.FunSpec
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningFri
import java.util.UUID

class TilsagnValidatorTest : FunSpec({

    context("validate Tilsagn") {
        context("TilsagnBeregningFri.Input") {
            test("should return field error if linjer is empty") {
                val input = TilsagnBeregningFri.Input(linjer = emptyList(), prisbetingelser = null)
                TilsagnValidator.validateBeregningInput(input).shouldBeLeft()
            }

            test("should return list of field error for invalid input") {
                val input = TilsagnBeregningFri.Input(
                    linjer = listOf(
                        TilsagnBeregningFri.InputLinje(
                            belop = 0,
                            id = UUID.randomUUID(),
                            beskrivelse = "",
                            antall = 0,
                        ),
                    ),
                    prisbetingelser = null,
                )
                val leftFieldErrors = listOf(
                    FieldError(pointer = "linjer/0/belop", detail = "Beløp må være positivt"),
                    FieldError(pointer = "linjer/0/beskrivelse", detail = "Beskrivelse mangler"),
                    FieldError(pointer = "linjer/0/antall", detail = "Antall må være positivt"),
                )

                TilsagnValidator.validateBeregningInput(input) shouldBeLeft leftFieldErrors
            }
        }
    }
})
