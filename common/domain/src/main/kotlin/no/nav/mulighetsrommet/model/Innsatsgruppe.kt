package no.nav.mulighetsrommet.model

enum class Innsatsgruppe(val tittel: String, val order: Int) {
    STANDARD_INNSATS("Gode muligheter (standard)", 0),
    SITUASJONSBESTEMT_INNSATS("Trenger veiledning (situasjonsbestemt)", 1),
    SPESIELT_TILPASSET_INNSATS("Trenger veiledning, nedsatt arbeidsevne (spesielt tilpasset)", 2),
    GRADERT_VARIG_TILPASSET_INNSATS("Jobbe delvis (delvis varig tilpasset, kun ny løsning)", 3),
    VARIG_TILPASSET_INNSATS("Liten mulighet til å jobbe (varig tilpasset)", 4),
}
