package no.nav.mulighetsrommet.arena

enum class Avslutningsstatus {
    AVLYST,
    AVBRUTT,
    AVSLUTTET,
    UTKAST,
    IKKE_AVSLUTTET,
    ;

    companion object {
        fun fromArenastatus(arenaStatus: String): Avslutningsstatus {
            return when (arenaStatus) {
                "AVLYST" -> AVLYST
                "AVBRUTT" -> AVBRUTT
                "AVSLUTT" -> AVSLUTTET
                "PLANLAGT" -> UTKAST
                else -> IKKE_AVSLUTTET
            }
        }
    }
}
