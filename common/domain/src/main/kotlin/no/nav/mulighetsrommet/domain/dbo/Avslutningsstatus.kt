package no.nav.mulighetsrommet.domain.dbo

enum class Avslutningsstatus {
    AVLYST,
    AVBRUTT,
    AVSLUTTET,
    IKKE_AVSLUTTET,
    ;

    fun toArenastatus() =
        when (this) {
            AVLYST -> "AVLYST"
            AVBRUTT -> "AVBRUTT"
            AVSLUTTET -> "AVSLUTT"
            IKKE_AVSLUTTET -> "IKKE_AVSLUTTET" // #TODO: Sjekk hva arena kaller denne
        }

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
