package no.nav.mulighetsrommet.oppgaver

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsattRolle
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDateTime
import java.util.*

enum class OppgaveType(val rolle: NavAnsattRolle) {
    TILSAGN_TIL_GODKJENNING(NavAnsattRolle.BESLUTTER_TILSAGN),
    TILSAGN_TIL_ANNULLERING(NavAnsattRolle.BESLUTTER_TILSAGN),
    TILSAGN_TIL_OPPGJOR(NavAnsattRolle.BESLUTTER_TILSAGN),
    TILSAGN_RETURNERT(NavAnsattRolle.TILTAKSGJENNOMFORINGER_SKRIV),
    UTBETALING_TIL_GODKJENNING(NavAnsattRolle.ATTESTANT_UTBETALING),
    UTBETALING_RETURNERT(NavAnsattRolle.TILTAKSGJENNOMFORINGER_SKRIV),
    UTBETALING_TIL_BEHANDLING(NavAnsattRolle.TILTAKSGJENNOMFORINGER_SKRIV),
    ;

    companion object {
        val TilsagnOppgaver = listOf(
            TILSAGN_TIL_GODKJENNING,
            TILSAGN_TIL_ANNULLERING,
            TILSAGN_TIL_OPPGJOR,
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
    val tiltakstype: OppgaveTiltakstype,
    val link: OppgaveLink,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime,
    val oppgaveIcon: OppgaveIcon,
)

@Serializable
data class OppgaveTiltakstype(
    val tiltakskode: Tiltakskode,
    val navn: String,
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
