package no.nav.mulighetsrommet.api.tilsagn.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.model.ValutaBelop
import no.nav.mulighetsrommet.model.withValuta
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.util.UUID

@Serializable
@SerialName("FRI")
data class TilsagnBeregningFri(
    override val input: Input,
    override val output: Output,
) : TilsagnBeregning() {

    @Serializable
    @SerialName("FRI")
    data class Input(
        val linjer: List<InputLinje>,
        val prisbetingelser: String?,
    ) : TilsagnBeregningInput()

    @Serializable
    data class InputLinje(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val beskrivelse: String,
        val pris: ValutaBelop,
        val antall: Int,
    )

    @Serializable
    @SerialName("FRI")
    data class Output(
        override val pris: ValutaBelop,
    ) : TilsagnBeregningOutput()

    companion object {
        fun beregn(input: Input): TilsagnBeregningFri {
            return TilsagnBeregningFri(
                input,
                Output(
                    input.linjer.sumOf {
                        it.pris.belop.toBigInteger().multiply(it.antall.toBigInteger())
                    }.intValueExact().withValuta(input.linjer.firstOrNull()?.pris?.valuta ?: Valuta.NOK),
                ),
            )
        }
    }
}
