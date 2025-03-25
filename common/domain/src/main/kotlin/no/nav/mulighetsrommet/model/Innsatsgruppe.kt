package no.nav.mulighetsrommet.model

enum class Innsatsgruppe(val tittel: String, val order: Int) {
    GODE_MULIGHETER("Gode muligheter (standard)", 0),
    TRENGER_VEILEDNING("Trenger veiledning (situasjonsbestemt)", 1),
    TRENGER_VEILEDNING_NEDSATT_ARBEIDSEVNE("Trenger veiledning, nedsatt arbeidsevne (spesielt tilpasset)", 2),
    JOBBE_DELVIS("Jobbe delvis (delvis varig tilpasset, kun ny løsning)", 3),
    LITEN_MULIGHET_TIL_A_JOBBE("Liten mulighet til å jobbe (varig tilpasset)", 4),
}
