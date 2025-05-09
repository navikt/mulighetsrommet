package no.nav.mulighetsrommet.oppgaver

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDateTime
import java.util.*

enum class OppgaveType(val navn: String, val rolle: Rolle) {
    TILSAGN_TIL_GODKJENNING(
        navn = "Tilsagn til godkjenning",
        rolle = Rolle.BESLUTTER_TILSAGN,
    ),
    TILSAGN_TIL_ANNULLERING(
        navn = "Tilsagn til annullering",
        rolle = Rolle.BESLUTTER_TILSAGN,
    ),
    TILSAGN_TIL_OPPGJOR(
        navn = "Tilsagn til oppgjør",
        rolle = Rolle.BESLUTTER_TILSAGN,
    ),
    TILSAGN_RETURNERT(
        navn = "Tilsagn returnert av beslutter",
        rolle = Rolle.SAKSBEHANDLER_OKONOMI,
    ),
    UTBETALING_TIL_BEHANDLING(
        navn = "Utbetaling til behandling",
        rolle = Rolle.SAKSBEHANDLER_OKONOMI,
    ),
    UTBETALING_TIL_GODKJENNING(
        navn = "Utbetaling til godkjenning",
        rolle = Rolle.ATTESTANT_UTBETALING,
    ),
    UTBETALING_RETURNERT(
        navn = "Utbetaling returnert av attestant",
        rolle = Rolle.SAKSBEHANDLER_OKONOMI,
    ),
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
    val enhet: OppgaveEnhet?,
    val title: String,
    val description: String? = null,
    val tiltakstype: OppgaveTiltakstype,
    val link: OppgaveLink,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime,
    val oppgaveIcon: OppgaveIcon,
)

@Serializable
data class OppgaveEnhet(
    val nummer: NavEnhetNummer,
    val navn: String,
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
