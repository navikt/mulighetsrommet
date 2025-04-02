package no.nav.mulighetsrommet.model

enum class Innsatsgruppe(val tittel: String, val order: Int) {
    GODE_MULIGHETER("Gode muligheter", 0),
    TRENGER_VEILEDNING("Trenger veiledning", 1),
    TRENGER_VEILEDNING_NEDSATT_ARBEIDSEVNE("Trenger veiledning, nedsatt arbeidsevne", 2),
    JOBBE_DELVIS("Jobbe delvis", 3),
    LITEN_MULIGHET_TIL_A_JOBBE("Liten mulighet til å jobbe", 4),
}
