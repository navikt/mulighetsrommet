package no.nav.mulighetsrommet.domain

object Tiltakskoder {
    val gruppeTiltak = listOf(
        "ARBFORB",
        "ARBRRHDAG",
        "AVKLARAG",
        "DIGIOPPARB",
        "FORSAMOGRU",
        "FORSFAGGRU",
        "GRUFAGYRKE",
        "GRUPPEAMO",
        "INDJOBSTOT",
        "INDOPPFAG",
        "INDOPPRF",
        "IPSUNG",
        "JOBBK",
        "UTVAOONAV",
        "UTVOPPFOPL",
        "VASV"
    )

    fun isGruppetiltak(tiltakstypeArenaKode: String): Boolean {
        return tiltakstypeArenaKode in gruppeTiltak
    }
}
