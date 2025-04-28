package no.nav.mulighetsrommet.oppgaver

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDateTime
import java.util.*

enum class OppgaveType(val rolle: Rolle) {
    TILSAGN_TIL_GODKJENNING(Rolle.BESLUTTER_TILSAGN),
    TILSAGN_TIL_ANNULLERING(Rolle.BESLUTTER_TILSAGN),
    TILSAGN_TIL_OPPGJOR(Rolle.BESLUTTER_TILSAGN),
    TILSAGN_RETURNERT(Rolle.SAKSBEHANDLER_OKONOMI),
    UTBETALING_TIL_GODKJENNING(Rolle.ATTESTANT_UTBETALING),
    UTBETALING_RETURNERT(Rolle.SAKSBEHANDLER_OKONOMI),
    UTBETALING_TIL_BEHANDLING(Rolle.SAKSBEHANDLER_OKONOMI),
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
    val enhet: NavEnhetNummer?,
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
