package no.nav.mulighetsrommet.oppgaver

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.navansatt.db.NavAnsattRolle
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDateTime
import java.util.*

enum class OppgaveType(val rolle: NavAnsattRolle) {
    TILSAGN_TIL_GODKJENNING(NavAnsattRolle.OKONOMI_BESLUTTER),
    TILSAGN_TIL_ANNULLERING(NavAnsattRolle.OKONOMI_BESLUTTER),
    TILSAGN_TIL_FRIGJORING(NavAnsattRolle.OKONOMI_BESLUTTER),
    TILSAGN_RETURNERT(NavAnsattRolle.TILTAKSGJENNOMFORINGER_SKRIV),
    UTBETALING_TIL_GODKJENNING(NavAnsattRolle.OKONOMI_BESLUTTER),
    UTBETALING_RETURNERT(NavAnsattRolle.TILTAKSGJENNOMFORINGER_SKRIV),
    UTBETALING_TIL_BEHANDLING(NavAnsattRolle.TILTAKSGJENNOMFORINGER_SKRIV),
    ;

    companion object {
        val TilsagnOppgaver = listOf(
            TILSAGN_TIL_GODKJENNING,
            TILSAGN_TIL_ANNULLERING,
            TILSAGN_RETURNERT,
        )
        val DelutbetalingOppgaver = listOf(
            UTBETALING_RETURNERT,
            UTBETALING_TIL_GODKJENNING,
        )
        val UtbetalingOppgaver = listOf(
            UTBETALING_TIL_BEHANDLING,
        )
    }
}

@Serializable
data class Oppgave(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val type: OppgaveType,
    val title: String,
    val description: String? = null,
    val tiltakstype: Tiltakskode,
    val link: OppgaveLink,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime,
    val oppgaveIcon: OppgaveIcon,
)

enum class OppgaveIcon {
    TILSAGN,
    UTBETALING,
}

@Serializable
data class OppgaveLink(
    val linkText: String,
    val link: String,
)
