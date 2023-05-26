package no.nav.mulighetsrommet.domain

object Tiltakskoder {
    /**
     * Tiltakskoder for de forhåndsgodkjente og anskaffede tiltakene, kalt "gruppetilak" (av oss i hvert fall).
     */
    val Gruppetiltak = listOf(
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
        "VASV",
    )

    /**
     * Tiltakskoder som, enn så lenge, blir antatt å ha en felles oppstartsdato for alle deltakere.
     * Disse har blitt referert til som "kurs" av komet.
     */
    val TiltakMedFellesOppstart = listOf(
        "GRUPPEAMO",
        "JOBBK",
        "GRUFAGYRKE",
    )

    /**
     * Tiltakskoder der Komet har tatt eierskap til deltakelsene.
     */
    val AmtTiltak = listOf(
        "ARBFORB",
        "ARBRRHDAG",
        "AVKLARAG",
        "DIGIOPPARB",
        "INDOPPFAG",
        "VASV",
    )

    fun isGruppetiltak(tiltakskode: String): Boolean {
        return tiltakskode in Gruppetiltak
    }

    fun hasFellesOppstart(tiltakskode: String): Boolean {
        return tiltakskode in TiltakMedFellesOppstart
    }

    fun isAmtTiltak(tiltakskode: String): Boolean {
        return tiltakskode in AmtTiltak
    }
}
