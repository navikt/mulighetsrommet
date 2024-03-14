package no.nav.mulighetsrommet.domain

object Tiltakskoder {
    /**
     * Tiltakskoder for de forhåndsgodkjente og anskaffede tiltakene, kalt "gruppetilak" (av oss i hvert fall), og som
     * skal migreres fra Arena som del av P4.
     */
    val Gruppetiltak = listOf(
        "ARBFORB",
        "ARBRRHDAG",
        "AVKLARAG",
        "DIGIOPPARB",
        "GRUFAGYRKE",
        "GRUPPEAMO",
        "INDJOBSTOT",
        "INDOPPFAG",
        "IPSUNG",
        "JOBBK",
        "UTVAOONAV",
        "VASV",
    )

    /**
     * Tiltakskoder for de gruppetiltak som er i egen regi, og som administreres i Sanity ikke i admin-flate
     */
    val EgenRegiTiltak = listOf(
        "INDJOBSTOT",
        "IPSUNG",
        "UTVAOONAV",
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
        "GRUFAGYRKE",
        "GRUPPEAMO",
        "INDOPPFAG",
        "JOBBK",
        "VASV",
    )

    val TiltakMedAvtalerFraMulighetsrommet = listOf(
        "ARBFORB",
        "VASV",
    )

    fun isAFTOrVTA(tiltakskode: String): Boolean {
        return TiltakMedAvtalerFraMulighetsrommet.contains(tiltakskode)
    }

    fun isGruppetiltak(tiltakskode: String): Boolean {
        return tiltakskode in Gruppetiltak
    }

    fun isEgenRegiTiltak(tiltakskode: String): Boolean {
        return tiltakskode in EgenRegiTiltak
    }

    fun isKursTiltak(tiltakskode: String): Boolean {
        return tiltakskode in TiltakMedFellesOppstart
    }

    fun isAmtTiltak(tiltakskode: String): Boolean {
        return tiltakskode in AmtTiltak
    }

    fun isTiltakMedAvtalerFraMulighetsrommet(tiltakskode: String): Boolean {
        return tiltakskode in TiltakMedAvtalerFraMulighetsrommet
    }
}
