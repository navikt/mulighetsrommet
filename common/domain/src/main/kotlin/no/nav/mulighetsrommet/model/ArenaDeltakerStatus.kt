package no.nav.mulighetsrommet.model

enum class ArenaDeltakerStatus(val description: String, val variant: DataElement.Status.Variant) {
    AKTUELL("Aktuell", DataElement.Status.Variant.ALT_3),
    AVSLAG("Fått avslag", DataElement.Status.Variant.NEUTRAL),
    DELTAKELSE_AVBRUTT("Deltakelse avbrutt", DataElement.Status.Variant.NEUTRAL),
    FEILREGISTRERT("Feilregistrert", DataElement.Status.Variant.NEUTRAL),
    FULLFORT("Fullført", DataElement.Status.Variant.ALT_1),
    GJENNOMFORES("Gjennomføres", DataElement.Status.Variant.BLANK),
    GJENNOMFORING_AVBRUTT("Gjennomføring avbrutt", DataElement.Status.Variant.NEUTRAL),
    GJENNOMFORING_AVLYST("Gjennomføring avlyst", DataElement.Status.Variant.NEUTRAL),
    IKKE_AKTUELL("Ikke aktuell", DataElement.Status.Variant.NEUTRAL),
    IKKE_MOTT("Ikke møtt", DataElement.Status.Variant.NEUTRAL),
    INFORMASJONSMOTE("Informasjonsmøte", DataElement.Status.Variant.INFO),
    TAKKET_JA_TIL_TILBUD("Takket ja til tilbud", DataElement.Status.Variant.ALT_3),
    TAKKET_NEI_TIL_TILBUD("Takket nei til tilbud", DataElement.Status.Variant.NEUTRAL),
    TILBUD("Godkjent tiltaksplass", DataElement.Status.Variant.INFO),
    VENTELISTE("Venteliste", DataElement.Status.Variant.ALT_1),
    ;

    fun toDataElement() = DataElement.Status(description, variant)
}
