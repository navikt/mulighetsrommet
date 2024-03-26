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

    companion object {
        fun fromArenaKode(arenaKode: String): Tiltakskode? {
            return when (arenaKode) {
                "ARBFORB" -> ARBEIDSFORBEREDENDE_TRENING
                "ARBRRHDAG" -> ARBEIDSRETTET_REHABILITERING
                "AVKLARAG" -> AVKLARING
                "DIGIOPPARB" -> DIGITALT_OPPFOLGINGSTILTAK
                "GRUFAGYRKE" -> GRUPPE_FAG_OG_YRKESOPPLAERING
                "GRUPPEAMO" -> GRUPPE_ARBEIDSMARKEDSOPPLAERING
                "INDOPPFAG" -> OPPFOLGING
                "JOBBK" -> JOBBKLUBB
                "VASV" -> VARIG_TILRETTELAGT_ARBEID_SKJERMET
                else -> null
            }
        }

        fun toArenaKode(tiltakskode: Tiltakskode): String {
            return when (tiltakskode) {
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

    fun isGruppetiltak(arenaKode: String): Boolean {
        return arenaKode in GruppetiltakArenaKoder
    }

    fun isAFTOrVTA(tiltakskode: String): Boolean {
        return tiltakskode == "VASV" || tiltakskode == "ARBFORB"
    }

    fun isEgenRegiTiltak(arenaKode: String): Boolean {
        return arenaKode in EgenRegiTiltak
    }

    fun isKursTiltak(arenaKode: String): Boolean {
        return arenaKode in TiltakMedFellesOppstart
    }

    fun isAmtTiltak(arenaKode: String): Boolean {
        return arenaKode in AmtTiltak
    }

    fun isTiltakMedAvtalerFraMulighetsrommet(arenaKode: String): Boolean {
        return arenaKode in TiltakMedAvtalerFraMulighetsrommet
    }
}
