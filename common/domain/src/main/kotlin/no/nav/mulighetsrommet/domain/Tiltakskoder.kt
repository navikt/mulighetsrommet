package no.nav.mulighetsrommet.domain

enum class Tiltakskode {
    AVKLARING,
    OPPFOLGING,
    GRUPPE_ARBEIDSMARKEDSOPPLAERING,
    JOBBKLUBB,
    DIGITALT_OPPFOLGINGSTILTAK,
    ARBEIDSFORBEREDENDE_TRENING,
    GRUPPE_FAG_OG_YRKESOPPLAERING,
    ARBEIDSRETTET_REHABILITERING,
    VARIG_TILRETTELAGT_ARBEID_SKJERMET,
    ;

    fun toArenaKode(): String {
        return when (this) {
            ARBEIDSFORBEREDENDE_TRENING -> "ARBFORB"
            ARBEIDSRETTET_REHABILITERING -> "ARBRRHDAG"
            AVKLARING -> "AVKLARAG"
            DIGITALT_OPPFOLGINGSTILTAK -> "DIGIOPPARB"
            GRUPPE_FAG_OG_YRKESOPPLAERING -> "GRUFAGYRKE"
            GRUPPE_ARBEIDSMARKEDSOPPLAERING -> "GRUPPEAMO"
            OPPFOLGING -> "INDOPPFAG"
            JOBBKLUBB -> "JOBBK"
            VARIG_TILRETTELAGT_ARBEID_SKJERMET -> "VASV"
        }
    }
}

object Tiltakskoder {
    /**
     * Tiltakskoder for de forhåndsgodkjente og anskaffede tiltakene, kalt "gruppetilak" (av oss i hvert fall), og som
     * skal migreres fra Arena som del av P4.
     */
    val GruppetiltakArenaKoder = listOf(
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
        Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING,
        Tiltakskode.JOBBKLUBB,
        Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING,
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
        Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
        Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET,
    )

    fun isGruppetiltak(arenaKode: String): Boolean {
        return arenaKode in GruppetiltakArenaKoder
    }

    fun isEgenRegiTiltak(arenaKode: String): Boolean {
        return arenaKode in EgenRegiTiltak
    }

    fun isKursTiltak(tiltakskode: Tiltakskode?): Boolean {
        return tiltakskode in TiltakMedFellesOppstart
    }

    fun isAmtTiltak(arenaKode: String): Boolean {
        return arenaKode in AmtTiltak
    }

    fun isTiltakMedAvtalerFraMulighetsrommet(tiltakskode: Tiltakskode?): Boolean {
        return tiltakskode in TiltakMedAvtalerFraMulighetsrommet
    }
}
