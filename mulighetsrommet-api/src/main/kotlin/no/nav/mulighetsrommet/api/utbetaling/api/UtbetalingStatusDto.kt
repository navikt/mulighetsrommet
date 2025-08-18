package no.nav.mulighetsrommet.api.utbetaling.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatusType

@Serializable
sealed class UtbetalingStatusDto {
    @Serializable
    @SerialName("VENTER_PA_ARRANGOR")
    data object VenterPaArrangor : UtbetalingStatusDto()

    @Serializable
    @SerialName("KLAR_TIL_BEHANDLING")
    data object KlarTilBehandling : UtbetalingStatusDto()

    @Serializable
    @SerialName("TIL_ATTESTERING")
    data object TilAttestering : UtbetalingStatusDto()

    @Serializable
    @SerialName("RETURNERT")
    data object Returnert : UtbetalingStatusDto()

    @Serializable
    @SerialName("OVERFORT_TIL_UTBETALING")
    data object OverfortTilUtbetaling : UtbetalingStatusDto()

    companion object {
        fun fromUtbetaling(utbetaling: Utbetaling): UtbetalingStatusDto {
            return when (utbetaling.status) {
                UtbetalingStatusType.GENERERT -> VenterPaArrangor
                UtbetalingStatusType.INNSENDT -> KlarTilBehandling
                UtbetalingStatusType.TIL_ATTESTERING -> TilAttestering
                UtbetalingStatusType.RETURNERT -> Returnert
                UtbetalingStatusType.FERDIG_BEHANDLET -> OverfortTilUtbetaling
            }
        }
    }
}
