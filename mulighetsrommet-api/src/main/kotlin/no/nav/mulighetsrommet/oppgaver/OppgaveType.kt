package no.nav.mulighetsrommet.oppgaver

import no.nav.mulighetsrommet.api.navansatt.model.Rolle

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
    UTBETALING_TIL_ATTESTERING(
        navn = "Utbetaling til attestering",
        rolle = Rolle.ATTESTANT_UTBETALING,
    ),
    UTBETALING_RETURNERT(
        navn = "Utbetaling returnert av attestant",
        rolle = Rolle.SAKSBEHANDLER_OKONOMI,
    ),
    AVTALE_MANGLER_ADMINISTRATOR(
        navn = "Avtale mangler administrator",
        rolle = Rolle.AVTALER_SKRIV,
    ),
    GJENNOMFORING_MANGLER_ADMINISTRATOR(
        navn = "Gjennomføring mangler administrator",
        rolle = Rolle.TILTAKSGJENNOMFORINGER_SKRIV,
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
            UTBETALING_TIL_ATTESTERING,
        )
        val UtbetalingOppgaver = listOf(
            UTBETALING_TIL_BEHANDLING,
        )
        val AvtaleOppgaver = listOf(
            AVTALE_MANGLER_ADMINISTRATOR,
        )
        val GjennomforingOppgaver = listOf(
            GJENNOMFORING_MANGLER_ADMINISTRATOR,
        )
    }
}
