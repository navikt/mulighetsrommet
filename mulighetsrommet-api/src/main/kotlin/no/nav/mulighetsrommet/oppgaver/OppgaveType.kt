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
