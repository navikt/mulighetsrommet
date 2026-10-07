package no.nav.mulighetsrommet.api.domain.navansatt

enum class Rolle(val visningsnavn: String) {
    /**
     * Gir tilsvarende lestilgang som [TILTAKADMINISTRASJON_GENERELL]. Custom rolle for utviklere i teamet.
     */
    TEAM_MULIGHETSROMMET("Team Mulighetsrommet"),

    /**
     * Generell tilgang til Tiltaksadministrasjon.
     *
     * Gir generell lesetilgang til en del data, men ikke til modeller som inneholder (eller kan inneholde) persondata,
     * eksempelvis deltakere, tilsagn og utbetalinger.
     */
    TILTAKADMINISTRASJON_GENERELL("Tiltaksadministrasjon generell"),

    /**
     * Gir tilgang til å redigere redaksjonelt innhold på tiltakstyper.
     */
    TILTAKSTYPER_SKRIV("Skrivetilgang - Tiltakstyper"),

    /**
     * Gir tilgang til å redigere tiltakstypens "innhold for deltakere".
     */
    TILTAKSTYPER_REDIGER_DELTAKERINFO("Rediger innhold for deltakere - Tiltakstyper"),

    /**
     * Gir tilgang til alle funksjoner relatert til redigering av gjennomføringer.
     */
    TILTAKSGJENNOMFORINGER_SKRIV("Skrivetilgang - Gjennomføring"),

    /**
     * Gir tilgang til enkelte funksjoner som er knyttet til oppfølging av gjennomføringer, men gir ikke full tilgang
     * til å opprette eller redigere innhold, økonomi etc.
     */
    OPPFOLGER_GJENNOMFORING("Oppfølger - Gjennomføring"),

    /**
     * Gir tilgang til alle funksjoner relatert til redigering av avtaler.
     */
    AVTALER_SKRIV("Skrivetilgang - Avtale"),

    /**
     * Gir lesetilgang til detaljer på tilsagn og utbetalinger for både gruppetiltak og enkeltplasser.
     */
    OKONOMI_LES("Lesetilgang - Økonomi"),

    /**
     * Gir tilgang til å behandle og sende økonomi (tilsagn og utbetalinger) for gruppetiltak
     * til godkjenning.
     */
    OKONOMI_SAKSBEHANDLER_GRUPPETILTAK("Saksbehandler - Gruppetiltak"),

    /**
     * Gir tilgang til å godkjenne tilsagn for gruppetiltak.
     */
    OKONOMI_BESLUTTER_GRUPPETILTAK("Beslutter - Gruppetiltak"),

    /**
     * Gir tilgang til å godkjenne utbetalinger for gruppetiltak.
     */
    OKONOMI_ATTESTANT_GRUPPETILTAK("Attestant - Gruppetiltak"),

    /**
     * Gir tilgang til å behandle og sende økonomi (tilsagn, utbetalinger og tilskuddsbehandlinger)
     * for enkeltplasser til godkjenning.
     */
    OKONOMI_SAKSBEHANDLER_ENKELTPLASS("Saksbehandler - Enkeltplass"),

    /**
     * Gir tilgang til å godkjenne enkeltplasser og tilhørende økonomi (tilsagn, utbetalinger og
     * tilskuddsbehandlinger).
     */
    OKONOMI_BESLUTTER_ENKELTPLASS("Beslutter - Enkeltplass"),

    /**
     * Indikerer Nav-ansatte som kan være kontaktperson (tiltaksansvarlig) for et tiltak.
     *
     * Gir ellers ingen rettigheter.
     */
    KONTAKTPERSON("Kontaktperson"),
}
