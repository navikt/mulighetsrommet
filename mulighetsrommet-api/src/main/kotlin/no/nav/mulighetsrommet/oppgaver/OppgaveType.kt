package no.nav.mulighetsrommet.oppgaver

import no.nav.mulighetsrommet.api.navansatt.model.Rolle

enum class OppgaveType(val navn: String, val rolle: Rolle, val kategori: Kategori) {
    TILSAGN_TIL_GODKJENNING(
        navn = "Tilsagn til godkjenning",
        rolle = Rolle.BESLUTTER_TILSAGN,
        kategori = Kategori.TILSAGN,
    ),
    TILSAGN_TIL_ANNULLERING(
        navn = "Tilsagn til annullering",
        rolle = Rolle.BESLUTTER_TILSAGN,
        kategori = Kategori.TILSAGN,
    ),
    TILSAGN_TIL_OPPGJOR(
        navn = "Tilsagn til oppgjør",
        rolle = Rolle.BESLUTTER_TILSAGN,
        kategori = Kategori.TILSAGN,
    ),
    TILSAGN_RETURNERT(
        navn = "Tilsagn returnert av beslutter",
        rolle = Rolle.SAKSBEHANDLER_OKONOMI,
        kategori = Kategori.TILSAGN,
    ),
    UTBETALING_TIL_BEHANDLING(
        navn = "Utbetaling til behandling",
        rolle = Rolle.SAKSBEHANDLER_OKONOMI,
        kategori = Kategori.UTBETALING,
    ),
    UTBETALING_TIL_AVBRYTELSE(
        navn = "Utbetaling til avbrytelse",
        rolle = Rolle.ATTESTANT_UTBETALING,
        kategori = Kategori.UTBETALING,
    ),
    UTBETALING_TIL_ATTESTERING(
        navn = "Utbetaling til attestering",
        rolle = Rolle.ATTESTANT_UTBETALING,
        kategori = Kategori.DELUTBETALING,
    ),
    UTBETALING_RETURNERT(
        navn = "Utbetaling returnert av attestant",
        rolle = Rolle.SAKSBEHANDLER_OKONOMI,
        kategori = Kategori.DELUTBETALING,
    ),
    AVTALE_MANGLER_ADMINISTRATOR(
        navn = "Avtale mangler administrator",
        rolle = Rolle.AVTALER_SKRIV,
        kategori = Kategori.AVTALE,
    ),
    GJENNOMFORING_MANGLER_ADMINISTRATOR(
        navn = "Gjennomføring mangler administrator",
        rolle = Rolle.TILTAKSGJENNOMFORINGER_SKRIV,
        kategori = Kategori.GJENNOMFORING,
    ),
}

enum class Kategori {
    TILSAGN,
    DELUTBETALING,
    UTBETALING,
    AVTALE,
    GJENNOMFORING,
}
