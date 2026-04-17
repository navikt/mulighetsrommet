package no.nav.mulighetsrommet.api.tiltakstype.model

enum class TiltakstypeFeature {
    /**
     * Styrer om gjennomføringer vises i Tiltaksadministrasjon
     */
    VISES_I_TILTAKSADMINISTRASJON,

    /**
     * Administreres i Tiltaksadministrasjon og deles med Arena
     */
    MIGRERT,

    /**
     * Kan fortsatt redigeres, men ikke opprettes nye.
     */
    UTFASET,

    /**
     * Redaksjonelt innhold hentes fra databasen i stedet for Sanity.
     */
    MIGRERT_REDAKSJONELT_INNHOLD,
}
