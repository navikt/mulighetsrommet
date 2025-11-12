package no.nav.mulighetsrommet.model

enum class ArbeidsgiverAvtaleStatus(val description: String, val variant: DataElement.Status.Variant) {
    /**
     * Tiltaket er påbegynt, men kan fortsatt mangle noe data som er påkrevd for at det skal kunne gjennomføres.
     * Kan anses som en "kladd".
     */
    PAABEGYNT("Påbegynt", DataElement.Status.Variant.WARNING),

    /**
     * Bl.a. når man mangler godkjenning av "controller", men kan muligens også være andre godkjenninger som
     * kreves.
     */
    MANGLER_GODKJENNING("Mangler godkjenning", DataElement.Status.Variant.INFO),

    /**
     * Status er basert på startdato for avtale. Kan anta at avtalen er klar, men startdato er i fremtiden.
     */
    KLAR_FOR_OPPSTART("Klar for oppstart", DataElement.Status.Variant.INFO),

    /**
     * Avtale gjennomføres.
     * Bruker deltar på tiltaket.
     */
    GJENNOMFORES("Gjennomføres", DataElement.Status.Variant.BLANK),

    /**
     * Avtale har blitt avsluttet.
     * Bruker har deltatt på tiltaket.
     */
    AVSLUTTET("Avsluttet", DataElement.Status.Variant.ALT_1),

    /**
     * Avtale ble avbrutt.
     */
    AVBRUTT("Avbrutt", DataElement.Status.Variant.NEUTRAL),

    /**
     * Tiltaket ble aldri noe av.
     */
    ANNULLERT("Annullert", DataElement.Status.Variant.NEUTRAL),
    ;

    fun toDataElement() = DataElement.Status(description, variant)
}
