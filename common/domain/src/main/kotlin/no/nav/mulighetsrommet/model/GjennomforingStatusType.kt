package no.nav.mulighetsrommet.model

enum class GjennomforingStatusType(val beskrivelse: String) {
    GJENNOMFORES("Gjennomføres"),
    AVSLUTTET("Avsluttet"),
    AVBRUTT("Avbrutt"),
    AVLYST("Avlyst"),
    ENKELTPLASS_UTKAST_TIL_PAMELDING("Utkast til påmelding"),
    ENKELTPLASS_SOKT_INN("Søkt inn"),
    ENKELTPLASS_VENTER_PA_OPPSTART("Venter på oppstart"),
    ENKELTPLASS_DELTAR("Deltar"),
    ENKELTPLASS_IKKE_AKTUELL("Ikke aktuell"),
    ENKELTPLASS_FULLFORT("Fullført"),
    ENKELTPLASS_AVBRUTT("Avbrutt"),
    ENKELTPLASS_AVBRUTT_UTKAST("Avbrutt utkast"),
    ENKELTPLASS_FEILREGISTRERT("Feilregistrert"),
}
