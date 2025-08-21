package no.nav.mulighetsrommet.api.navansatt.model

enum class Rolle {
    /**
     * Gir tilsvarende lestilgang som [TILTAKADMINISTRASJON_GENERELL]. Custom rolle for utviklere i teamet.
     */
    TEAM_MULIGHETSROMMET,

    /**
     * Indikerer Nav-ansatte som kan være kontaktperson (tiltaksansvarlig) for et tiltak.
     *
     * Gir ellers ingen rettigheter.
     */
    KONTAKTPERSON,

    /**
     * Generell tilgang til Tiltaksadministrasjon.
     *
     * Gir generell lesetilgang til en del data, men ikke til modeller som inneholder (eller kan inneholde) persondata,
     * eksempelvis deltakere, tilsagn og utbetalinger.
     */
    TILTAKADMINISTRASJON_GENERELL,

    /**
     * Gir tilgang til alle funksjoner relatert til redigering av gjennomføringer.
     */
    TILTAKSGJENNOMFORINGER_SKRIV,

    /**
     * Gir tilgang til alle funksjoner relatert til redigering av avtaler.
     */
    AVTALER_SKRIV,

    /**
     * Gir tilgang til å behandle og sende tilsagn/utbetalinger til godkjenning/attestering.
     */
    SAKSBEHANDLER_OKONOMI,

    /**
     * Gir tilgang til å godkjenne tilsagn.
     */
    BESLUTTER_TILSAGN,

    /**
     * Gir tilgang til å attestere utbetalinger.
     */
    ATTESTANT_UTBETALING,
}
