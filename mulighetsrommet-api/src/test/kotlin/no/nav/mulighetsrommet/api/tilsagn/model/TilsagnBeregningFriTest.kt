package no.nav.mulighetsrommet.api.tilsagn.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import no.nav.mulighetsrommet.model.Currency
import java.util.UUID

class TilsagnBeregningFriTest : FunSpec({
    test("overflow kaster exception") {
        shouldThrow<ArithmeticException> {
            val input = TilsagnBeregningFri.Input(
                prisbetingelser = null,
                linjer = listOf(
                    TilsagnBeregningFri.InputLinje(
                        id = UUID.randomUUID(),
                        beskrivelse = "",
                        belop = 999_999_999,
                        valuta = Currency.NOK,
                        antall = 999_999_999,
                    ),
                ),
            )

            TilsagnBeregningFri.beregn(input)
        }
    }
})
