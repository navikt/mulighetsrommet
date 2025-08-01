package no.nav.mulighetsrommet.api.tilsagn.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregning
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningFri
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningFri.InputLinje
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningPrisPerManedsverk
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningPrisPerUkesverk
import java.math.RoundingMode

@Serializable
sealed class TilsagnBeregningDto {
    abstract val belop: Int

    @Serializable
    @SerialName("PRIS_PER_MANEDSVERK")
    data class PrisPerManedsverk(
        override val belop: Int,
        val sats: Int,
        val antallPlasser: Int,
        val antallManeder: Double,
    ) : TilsagnBeregningDto()

    @Serializable
    @SerialName("PRIS_PER_UKESVERK")
    data class PrisPerUkesverk(
        override val belop: Int,
        val sats: Int,
        val antallPlasser: Int,
        val antallUker: Double,
    ) : TilsagnBeregningDto()

    @Serializable
    @SerialName("FRI")
    data class Fri(
        override val belop: Int,
        val linjer: List<InputLinje>,
        val prisbetingelser: String?,
    ) : TilsagnBeregningDto()

    companion object {
        fun from(beregning: TilsagnBeregning): TilsagnBeregningDto {
            return when (beregning) {
                is TilsagnBeregningFri -> Fri(
                    belop = beregning.output.belop,
                    linjer = beregning.input.linjer,
                    prisbetingelser = beregning.input.prisbetingelser,
                )
                is TilsagnBeregningPrisPerManedsverk -> PrisPerManedsverk(
                    belop = beregning.output.belop,
                    antallPlasser = beregning.input.antallPlasser,
                    sats = beregning.input.sats,
                    antallManeder = TilsagnBeregningPrisPerManedsverk.manedsverk(beregning.input.periode)
                        .sumOf { it }
                        .setScale(2, RoundingMode.HALF_UP)
                        .toDouble(),
                )
                is TilsagnBeregningPrisPerUkesverk -> PrisPerUkesverk(
                    belop = beregning.output.belop,
                    antallPlasser = beregning.input.antallPlasser,
                    sats = beregning.input.sats,
                    antallUker = TilsagnBeregningPrisPerUkesverk.ukesverk(beregning.input.periode)
                        .setScale(2, RoundingMode.HALF_UP)
                        .toDouble(),
                )
            }
        }
    }
}
