package no.nav.mulighetsrommet.api.arrangorflate.dto

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatusType
import no.nav.mulighetsrommet.model.GjennomforingStatusType

@Serializable
enum class ArrangorflateFilterDirection {
    ASC,
    DESC,
    ;

    companion object {
        fun from(str: String?, fallback: ArrangorflateFilterDirection = ASC): ArrangorflateFilterDirection = str?.let { ArrangorflateFilterDirection.valueOf(it) } ?: fallback
    }
}

@Serializable
enum class ArrangorflateFilterType {
    AKTIVE,
    HISTORISKE,
    ;

    fun toGjennomforingStatuses(): List<GjennomforingStatusType> = when (this) {
        AKTIVE -> listOf(GjennomforingStatusType.GJENNOMFORES)
        HISTORISKE -> GjennomforingStatusType.entries.filter { it != GjennomforingStatusType.GJENNOMFORES }
    }

    fun utbetalingStatuser(): Set<UtbetalingStatusType> = when (this) {
        AKTIVE -> setOf(
            UtbetalingStatusType.GENERERT,
            UtbetalingStatusType.TIL_BEHANDLING,
            UtbetalingStatusType.TIL_ATTESTERING,
            UtbetalingStatusType.RETURNERT,
        )

        HISTORISKE -> setOf(
            UtbetalingStatusType.FERDIG_BEHANDLET,
            UtbetalingStatusType.DELVIS_UTBETALT,
            UtbetalingStatusType.UTBETALT,
            UtbetalingStatusType.AVBRUTT,
        )
    }

    companion object {
        fun from(type: String?): ArrangorflateFilterType = when (type) {
            "AKTIVE" -> AKTIVE
            "HISTORISKE" -> HISTORISKE
            else -> AKTIVE
        }
    }
}
