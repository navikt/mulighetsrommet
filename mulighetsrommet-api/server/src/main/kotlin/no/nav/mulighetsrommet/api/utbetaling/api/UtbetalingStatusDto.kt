package no.nav.mulighetsrommet.api.utbetaling.api

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.admin.totrinnskontroll.TotrinnskontrollDto
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.Totrinnskontroll
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.TotrinnskontrollStatus
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatusType
import no.nav.mulighetsrommet.model.DataElement

@Serializable
data class UtbetalingStatusDto(
    val type: Type,
    val status: DataElement.Status,
) {
    companion object {
        fun fromUtbetalingStatus(utbetalingStatus: UtbetalingStatusType, blokkeringer: Set<Utbetaling.Blokkering>, avbrytelse: TotrinnskontrollDto?): UtbetalingStatusDto {
            return fromUtbetalingStatus(utbetalingStatus, blokkeringer) { utledAvbruttStatus(avbrytelse) }
        }

        fun fromUtbetalingStatus(utbetalingStatus: UtbetalingStatusType, blokkeringer: Set<Utbetaling.Blokkering>, avbrytelse: Totrinnskontroll?): UtbetalingStatusDto {
            return fromUtbetalingStatus(utbetalingStatus, blokkeringer) { utledAvbruttStatus(avbrytelse) }
        }

        private fun fromUtbetalingStatus(utbetalingStatus: UtbetalingStatusType, blokkeringer: Set<Utbetaling.Blokkering>, utledAvbrutt: () -> Type): UtbetalingStatusDto {
            val type = when (utbetalingStatus) {
                UtbetalingStatusType.GENERERT -> if (blokkeringer.isNotEmpty()) {
                    Type.BLOKKERT_FOR_INNSENDING
                } else {
                    Type.VENTER_PA_ARRANGOR
                }

                UtbetalingStatusType.TIL_BEHANDLING -> Type.KLAR_TIL_BEHANDLING

                UtbetalingStatusType.TIL_ATTESTERING -> Type.TIL_ATTESTERING

                UtbetalingStatusType.RETURNERT -> Type.RETURNERT

                UtbetalingStatusType.FERDIG_BEHANDLET -> Type.OVERFORT_TIL_UTBETALING

                UtbetalingStatusType.DELVIS_UTBETALT -> Type.DELVIS_UTBETALT

                UtbetalingStatusType.UTBETALT -> Type.UTBETALT

                UtbetalingStatusType.TIL_AVBRYTELSE -> Type.TIL_AVBRYTELSE

                UtbetalingStatusType.AVBRUTT -> utledAvbrutt()
            }
            val status = DataElement.Status(type.beskrivelse, type.variant)
            return UtbetalingStatusDto(type, status)
        }

        private fun utledAvbruttStatus(avbrytelse: TotrinnskontrollDto?): Type {
            if (avbrytelse == null) {
                return Type.AVBRUTT_AV_ARRANGOR
            }
            return if (avbrytelse is TotrinnskontrollDto.Besluttet && avbrytelse.beslutning == TotrinnskontrollDto.Beslutning.GODKJENT) {
                Type.AVBRUTT_AV_NAV
            } else {
                Type.AVBRUTT_AV_ARRANGOR
            }
        }

        private fun utledAvbruttStatus(avbrytelse: Totrinnskontroll?): Type {
            if (avbrytelse == null) {
                return Type.AVBRUTT_AV_ARRANGOR
            }
            return if (avbrytelse.status == TotrinnskontrollStatus.GODKJENT) {
                Type.AVBRUTT_AV_NAV
            } else {
                Type.AVBRUTT_AV_ARRANGOR
            }
        }
    }

    enum class Type(val beskrivelse: String, val variant: DataElement.Status.Variant) {
        VENTER_PA_ARRANGOR("Venter på arrangør", DataElement.Status.Variant.ALT_1),
        BLOKKERT_FOR_INNSENDING("Blokkert for innsending", DataElement.Status.Variant.WARNING),
        KLAR_TIL_BEHANDLING("Klar til behandling", DataElement.Status.Variant.SUCCESS),
        TIL_ATTESTERING("Til attestering", DataElement.Status.Variant.INFO),
        RETURNERT("Returnert", DataElement.Status.Variant.ERROR),
        OVERFORT_TIL_UTBETALING("Overført til utbetaling", DataElement.Status.Variant.SUCCESS),
        DELVIS_UTBETALT("Delvis utbetalt", DataElement.Status.Variant.SUCCESS),
        UTBETALT("Utbetalt", DataElement.Status.Variant.SUCCESS),
        AVBRUTT_AV_ARRANGOR("Avbrutt av arrangør", DataElement.Status.Variant.NEUTRAL),
        AVBRUTT_AV_NAV("Avbrutt av Nav", DataElement.Status.Variant.NEUTRAL),
        TIL_AVBRYTELSE("Til avbrytelse", DataElement.Status.Variant.NEUTRAL),
    }
}
