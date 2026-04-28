package no.nav.mulighetsrommet.api.tiltakstype.model

enum class TiltakstypeFeature {
    /**
     * Styrer om gjennomføringer vises i Tiltaksadministrasjon
     */
    VISES_I_TILTAKSADMINISTRASJON,

    /**
     * Styrer hvilke tiltak (typer og gjennomføringer) som vises i Modia.
     */
    VISES_I_MODIA,

    /**
     * Administreres i Tiltaksadministrasjon og deles med Arena
     */
    MIGRERT,

    /**
     * Kan fortsatt redigeres, men ikke opprettes nye.
     */
    UTFASET,
}
