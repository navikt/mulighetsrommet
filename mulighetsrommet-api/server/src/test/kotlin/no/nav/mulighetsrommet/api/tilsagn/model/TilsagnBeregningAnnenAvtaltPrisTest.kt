package no.nav.mulighetsrommet.api.tilsagn.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import no.nav.mulighetsrommet.model.NOK
import java.util.UUID

class TilsagnBeregningAnnenAvtaltPrisTest : FunSpec({
    test("overflow kaster exception") {
        shouldThrow<ArithmeticException> {
            val input = TilsagnBeregningAnnenAvtaltPris.Input(
                prisbetingelser = null,
                linjer = listOf(
                    TilsagnBeregningAnnenAvtaltPris.InputLinje(
                        id = UUID.randomUUID(),
                        beskrivelse = "",
                        pris = 999_999_999.NOK,
                        antall = 999_999_999,
                    ),
                ),
            )

            TilsagnBeregningAnnenAvtaltPris.beregn(input)
        }
    }
})
