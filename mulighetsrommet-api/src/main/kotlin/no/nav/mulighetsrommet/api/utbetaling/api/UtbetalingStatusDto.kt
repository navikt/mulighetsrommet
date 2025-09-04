package no.nav.mulighetsrommet.api.utbetaling.api

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatusType
import no.nav.mulighetsrommet.model.DataElement

@Serializable
data class UtbetalingStatusDto(
    val type: Type,
    val status: DataElement.Status,
) {
    companion object {
        fun fromUtbetaling(utbetaling: Utbetaling): UtbetalingStatusDto {
            val type = when (utbetaling.status) {
                UtbetalingStatusType.GENERERT -> Type.VENTER_PA_ARRANGOR
                UtbetalingStatusType.INNSENDT -> Type.KLAR_TIL_BEHANDLING
                UtbetalingStatusType.TIL_ATTESTERING -> Type.TIL_ATTESTERING
                UtbetalingStatusType.RETURNERT -> Type.RETURNERT
                UtbetalingStatusType.FERDIG_BEHANDLET -> Type.OVERFORT_TIL_UTBETALING
            }
            val status = DataElement.Status(type.beskrivelse, type.variant)
            return UtbetalingStatusDto(type, status)
        }
    }

    enum class Type(val beskrivelse: String, val variant: DataElement.Status.Variant) {
        VENTER_PA_ARRANGOR("Venter på arrangør", DataElement.Status.Variant.ALT),
        KLAR_TIL_BEHANDLING("Klar til behandling", DataElement.Status.Variant.SUCCESS),
        TIL_ATTESTERING("Til attestering", DataElement.Status.Variant.WARNING),
        RETURNERT("Returnert", DataElement.Status.Variant.ERROR),
        OVERFORT_TIL_UTBETALING("Overført til utbetaling", DataElement.Status.Variant.SUCCESS),
    }
}
