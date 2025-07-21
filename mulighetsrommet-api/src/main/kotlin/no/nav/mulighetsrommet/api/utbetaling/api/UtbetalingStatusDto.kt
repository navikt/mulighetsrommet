package no.nav.mulighetsrommet.api.utbetaling.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import java.time.LocalDateTime

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

    @Serializable
    @SerialName("AVBRUTT")
    data class Avbrutt(
        @Serializable(with = LocalDateTimeSerializer::class)
        val tidspunkt: LocalDateTime,
        val aarsaker: List<String>,
        val forklaring: String?,
    ) : UtbetalingStatusDto()

    companion object {
        fun fromUtbetaling(
            utbetaling: Utbetaling,
        ): UtbetalingStatusDto {
            return when (utbetaling.status) {
                Utbetaling.UtbetalingStatus.OPPRETTET -> VenterPaArrangor
                Utbetaling.UtbetalingStatus.INNSENDT -> KlarTilBehandling
                Utbetaling.UtbetalingStatus.TIL_ATTESTERING -> TilAttestering
                Utbetaling.UtbetalingStatus.RETURNERT -> Returnert
                Utbetaling.UtbetalingStatus.FERDIG_BEHANDLET -> OverfortTilUtbetaling
                Utbetaling.UtbetalingStatus.AVBRUTT -> {
                    requireNotNull(utbetaling.avbruttTidspunkt)
                    Avbrutt(
                        tidspunkt = utbetaling.avbruttTidspunkt,
                        aarsaker = utbetaling.avbruttAarsaker,
                        forklaring = utbetaling.avbruttForklaring,
                    )
                }
            }
        }
    }
}
