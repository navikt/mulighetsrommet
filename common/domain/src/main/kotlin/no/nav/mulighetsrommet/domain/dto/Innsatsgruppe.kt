package no.nav.mulighetsrommet.domain.dto

enum class Innsatsgruppe(val tittel: String, val order: Int) {
    STANDARD_INNSATS("Standard innsats", 0),
    SITUASJONSBESTEMT_INNSATS("Situasjonsbestemt innsats", 1),
    SPESIELT_TILPASSET_INNSATS("Spesielt tilpasset innsats", 2),
    GRADERT_VARIG_TILPASSET_INNSATS("Gradert varig tilpasset innsats", 3),
    VARIG_TILPASSET_INNSATS("Varig tilpasset innsats", 4),
}
