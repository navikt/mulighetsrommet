package no.nav.mulighetsrommet.domain.dto

enum class ArbeidsgiverAvtaleStatus(val description: String) {
    /**
     * Tiltaket er påbegynt, men kan fortsatt mangle noe data som er påkrevd for at det skal kunne gjennomføres.
     * Kan anses som en "kladd".
     */
    PAABEGYNT("Påbegynt"),

    /**
     * Bl.a. når man mangler godkjenning av "controller", men kan muligens også være andre godkjenninger som
     * kreves.
     */
    MANGLER_GODKJENNING("Mangler godkjenning"),

    /**
     * Status er basert på startdato for avtale. Kan anta at avtalen er klar, men startdato er i fremtiden.
     */
    KLAR_FOR_OPPSTART("Klar for oppstart"),

    /**
     * Avtale gjennomføres.
     * Bruker deltar på tiltaket.
     */
    GJENNOMFORES("Gjennomføres"),

    /**
     * Avtale har blitt avsluttet.
     * Bruker har deltatt på tiltaket.
     */
    AVSLUTTET("Avsluttet"),

    /**
     * Avtale ble avbrutt.
     */
    AVBRUTT("Avbrutt"),

    /**
     * Tiltaket ble aldri noe av.
     */
    ANNULLERT("Annullert"),
}
