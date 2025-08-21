package no.nav.mulighetsrommet.api.navansatt.model

enum class Rolle(val visningsnavn: String) {
    /**
     * Gir tilsvarende lestilgang som [TILTAKADMINISTRASJON_GENERELL]. Custom rolle for utviklere i teamet.
     */
    TEAM_MULIGHETSROMMET("Team Mulighetsrommet"),

    /**
     * Indikerer Nav-ansatte som kan være kontaktperson (tiltaksansvarlig) for et tiltak.
     *
     * Gir ellers ingen rettigheter.
     */
    KONTAKTPERSON("Kontaktperson"),

    /**
     * Generell tilgang til Tiltaksadministrasjon.
     *
     * Gir generell lesetilgang til en del data, men ikke til modeller som inneholder (eller kan inneholde) persondata,
     * eksempelvis deltakere, tilsagn og utbetalinger.
     */
    TILTAKADMINISTRASJON_GENERELL("Tiltaksadministrasjon generell"),

    /**
     * Gir tilgang til alle funksjoner relatert til redigering av gjennomføringer.
     */
    TILTAKSGJENNOMFORINGER_SKRIV("Skrivetilgang - Gjennomføring"),

    /**
     * Gir tilgang til alle funksjoner relatert til redigering av avtaler.
     */
    AVTALER_SKRIV("Skrivetilgang - Avtale"),

    /**
     * Gir tilgang til å behandle og sende tilsagn/utbetalinger til godkjenning/attestering.
     */
    SAKSBEHANDLER_OKONOMI("Saksbehandler - Økonomi"),

    /**
     * Gir lesetilgang til detaljer på tilsagn og utbetalinger.
     */
    OKONOMI_LES("Lesetilgang - Økonomi"),

    /**
     * Gir tilgang til å godkjenne tilsagn.
     */
    BESLUTTER_TILSAGN("Beslutter - Tilsagn"),

    /**
     * Gir tilgang til å attestere utbetalinger.
     */
    ATTESTANT_UTBETALING("Attestant - Utbetaling"),
}
