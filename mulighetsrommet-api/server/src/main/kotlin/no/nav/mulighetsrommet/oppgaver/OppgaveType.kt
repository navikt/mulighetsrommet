package no.nav.mulighetsrommet.oppgaver

import no.nav.mulighetsrommet.api.domain.navansatt.Rolle

enum class OppgaveType(
    val navn: String,
    val roller: Set<Rolle>,
    val kategori: Kategori,
) {
    TILSAGN_TIL_GODKJENNING(
        navn = "Tilsagn til godkjenning",
        roller = setOf(Rolle.OKONOMI_BESLUTTER_GRUPPETILTAK, Rolle.OKONOMI_BESLUTTER_ENKELTPLASS),
        kategori = Kategori.TILSAGN,
    ),
    TILSAGN_TIL_ANNULLERING(
        navn = "Tilsagn til annullering",
        roller = setOf(Rolle.OKONOMI_BESLUTTER_GRUPPETILTAK, Rolle.OKONOMI_BESLUTTER_ENKELTPLASS),
        kategori = Kategori.TILSAGN,
    ),
    TILSAGN_TIL_OPPGJOR(
        navn = "Tilsagn til oppgjør",
        roller = setOf(Rolle.OKONOMI_BESLUTTER_GRUPPETILTAK, Rolle.OKONOMI_BESLUTTER_ENKELTPLASS),
        kategori = Kategori.TILSAGN,
    ),
    TILSAGN_RETURNERT(
        navn = "Tilsagn returnert",
        roller = setOf(Rolle.OKONOMI_SAKSBEHANDLER_GRUPPETILTAK, Rolle.OKONOMI_SAKSBEHANDLER_ENKELTPLASS),
        kategori = Kategori.TILSAGN,
    ),
    UTBETALING_TIL_BEHANDLING(
        navn = "Utbetaling til behandling",
        roller = setOf(Rolle.OKONOMI_SAKSBEHANDLER_GRUPPETILTAK, Rolle.OKONOMI_SAKSBEHANDLER_ENKELTPLASS),
        kategori = Kategori.UTBETALING,
    ),
    UTBETALING_TIL_GODKJENNING(
        navn = "Utbetaling til godkjenning",
        roller = setOf(Rolle.OKONOMI_ATTESTANT_GRUPPETILTAK, Rolle.OKONOMI_BESLUTTER_ENKELTPLASS),
        kategori = Kategori.UTBETALING_LINJE,
    ),
    UTBETALING_RETURNERT(
        navn = "Utbetaling returnert",
        roller = setOf(Rolle.OKONOMI_SAKSBEHANDLER_GRUPPETILTAK, Rolle.OKONOMI_SAKSBEHANDLER_ENKELTPLASS),
        kategori = Kategori.UTBETALING_LINJE,
    ),
    UTBETALING_TIL_AVBRYTELSE(
        navn = "Utbetaling til avbrytelse",
        roller = setOf(Rolle.OKONOMI_SAKSBEHANDLER_GRUPPETILTAK, Rolle.OKONOMI_SAKSBEHANDLER_ENKELTPLASS),
        kategori = Kategori.UTBETALING,
    ),
    UTBETALING_MANGLER_TILSAGN(
        navn = "Utbetaling mangler tilsagn",
        roller = setOf(Rolle.OKONOMI_SAKSBEHANDLER_GRUPPETILTAK),
        kategori = Kategori.UTBETALING,
    ),
    AVTALE_MANGLER_ADMINISTRATOR(
        navn = "Avtale mangler administrator",
        roller = setOf(Rolle.AVTALER_SKRIV),
        kategori = Kategori.AVTALE,
    ),
    GJENNOMFORING_MANGLER_ADMINISTRATOR(
        navn = "Gjennomføring mangler administrator",
        roller = setOf(Rolle.TILTAKSGJENNOMFORINGER_SKRIV),
        kategori = Kategori.GJENNOMFORING,
    ),
    ENKELTPLASS_TIL_GODKJENNING(
        navn = "Enkeltplass til godkjenning",
        roller = setOf(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS),
        kategori = Kategori.ENKELTPLASS,
    ),
    ENKELTPLASS_SATT_PA_VENT(
        navn = "Enkeltplass satt på vent",
        roller = setOf(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS),
        kategori = Kategori.ENKELTPLASS,
    ),
    TILSKUDDBEHANDLING_TIL_GODKJENNING(
        navn = "Tilskuddsbehandling til godkjenning",
        roller = setOf(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS),
        kategori = Kategori.TILSKUDDBEHANDLING,
    ),
    TILSKUDDBEHANDLING_RETURNERT(
        navn = "Tilskuddsbehandling returnert",
        roller = setOf(Rolle.OKONOMI_SAKSBEHANDLER_ENKELTPLASS),
        kategori = Kategori.TILSKUDDBEHANDLING,
    ),
}

enum class Kategori {
    TILSAGN,
    UTBETALING_LINJE,
    UTBETALING,
    AVTALE,
    GJENNOMFORING,
    ENKELTPLASS,
    TILSKUDDBEHANDLING,
}
