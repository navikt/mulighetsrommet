package no.nav.mulighetsrommet.domain.dto

enum class ArenaDeltakerStatus(val description: String) {
    AKTUELL("Aktuell"),
    AVSLAG("Fått avslag"),
    DELTAKELSE_AVBRUTT("Deltakelse avbrutt"),
    FEILREGISTRERT("Feilregistrert"),
    FULLFORT("Fullført"),
    GJENNOMFORES("Gjennomføres"),
    GJENNOMFORING_AVBRUTT("Gjennomføring avbrutt"),
    GJENNOMFORING_AVLYST("Gjennomføring avlyst"),
    IKKE_AKTUELL("Ikke aktuell"),
    IKKE_MOTT("Ikke møtt"),
    INFORMASJONSMOTE("Informasjonsmøte"),
    TAKKET_JA_TIL_TILBUD("Takket ja til tilbud"),
    TAKKET_NEI_TIL_TILBUD("Takket nei til tilbud"),
    TILBUD("Godkjent tiltaksplass"),
    VENTELISTE("Venteliste"),
}
