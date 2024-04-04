package no.nav.mulighetsrommet.domain.dbo

enum class Avslutningsstatus {
    AVLYST,
    AVBRUTT,
    AVSLUTTET,
    IKKE_AVSLUTTET,
    ;

    companion object {
        fun fromArenastatus(arenaStatus: String): Avslutningsstatus {
            return when (arenaStatus) {
                "AVLYST" -> AVLYST
                "AVBRUTT" -> AVBRUTT
                "AVSLUTT" -> AVSLUTTET
                else -> IKKE_AVSLUTTET
            }
        }
    }
}
