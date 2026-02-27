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
        fun fromUtbetalingStatus(utbetalingStatus: UtbetalingStatusType, blokkeringer: Set<Utbetaling.Blokkering>): UtbetalingStatusDto {
            val type = when (utbetalingStatus) {
                UtbetalingStatusType.GENERERT -> if (blokkeringer.isEmpty()) {
                    Type.VENTER_PA_ARRANGOR
                } else {
                    Type.UBEHANDLET_FORSLAG
                }

                UtbetalingStatusType.INNSENDT -> Type.KLAR_TIL_BEHANDLING

                UtbetalingStatusType.TIL_ATTESTERING -> Type.TIL_ATTESTERING

                UtbetalingStatusType.RETURNERT -> Type.RETURNERT

                UtbetalingStatusType.FERDIG_BEHANDLET -> Type.OVERFORT_TIL_UTBETALING

                UtbetalingStatusType.DELVIS_UTBETALT -> Type.DELVIS_UTBETALT

                UtbetalingStatusType.UTBETALT -> Type.UTBETALT

                UtbetalingStatusType.AVBRUTT -> Type.AVBRUTT
            }
            val status = DataElement.Status(type.beskrivelse, type.variant)
            return UtbetalingStatusDto(type, status)
        }
    }

    enum class Type(val beskrivelse: String, val variant: DataElement.Status.Variant) {
        VENTER_PA_ARRANGOR("Venter på arrangør", DataElement.Status.Variant.ALT_1),
        UBEHANDLET_FORSLAG("Ubehandlede forslag", DataElement.Status.Variant.WARNING),
        KLAR_TIL_BEHANDLING("Klar til behandling", DataElement.Status.Variant.SUCCESS),
        TIL_ATTESTERING("Til attestering", DataElement.Status.Variant.INFO),
        RETURNERT("Returnert", DataElement.Status.Variant.ERROR),
        OVERFORT_TIL_UTBETALING("Overført til utbetaling", DataElement.Status.Variant.SUCCESS),
        DELVIS_UTBETALT("Delvis utbetalt", DataElement.Status.Variant.SUCCESS),
        UTBETALT("Utbetalt", DataElement.Status.Variant.SUCCESS),
        AVBRUTT("Avbrutt av arrangør", DataElement.Status.Variant.ERROR),
    }
}
