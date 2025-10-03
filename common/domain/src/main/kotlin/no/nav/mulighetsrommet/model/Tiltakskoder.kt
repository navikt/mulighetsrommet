package no.nav.mulighetsrommet.model

enum class Tiltakskode(val arenakode: String) {
    ARBEIDSFORBEREDENDE_TRENING("ARBFORB"),
    ARBEIDSRETTET_REHABILITERING("ARBRRHDAG"),
    AVKLARING("AVKLARAG"),
    DIGITALT_OPPFOLGINGSTILTAK("DIGIOPPARB"),
    ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING("ENKELAMO"),
    ENKELTPLASS_FAG_OG_YRKESOPPLAERING("ENKFAGYRKE"),
    GRUPPE_ARBEIDSMARKEDSOPPLAERING("GRUPPEAMO"),
    GRUPPE_FAG_OG_YRKESOPPLAERING("GRUFAGYRKE"),
    HOYERE_UTDANNING("HOYEREUTD"),
    JOBBKLUBB("JOBBK"),
    OPPFOLGING("INDOPPFAG"),
    VARIG_TILRETTELAGT_ARBEID_SKJERMET("VASV"),
}

object Tiltakskoder {
    /**
     * Tiltakskoder for de forhåndsgodkjente og anskaffede tiltakene, kalt "gruppetilak" (av oss i hvert fall), og som
     * skal migreres fra Arena som del av P4.
     */
    private val GruppetiltakArenaKoder = listOf(
        Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
        Tiltakskode.ARBEIDSRETTET_REHABILITERING,
        Tiltakskode.AVKLARING,
        Tiltakskode.DIGITALT_OPPFOLGINGSTILTAK,
        Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING,
        Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING,
        Tiltakskode.OPPFOLGING,
        Tiltakskode.JOBBKLUBB,
        Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET,
    )

    /**
     * Tiltakskoder for tiltak i egen regi (regi av Nav), og som foreløpig administreres i Sanity ikke i admin-flate.
     */
    private val EgenRegiTiltak = listOf(
        "INDJOBSTOT",
        "IPSUNG",
        "UTVAOONAV",
    )

    /**
     * Tiltakskoder som, enn så lenge, blir antatt å ha en felles oppstartsdato for alle deltakere.
     * Disse har blitt referert til som "kurs" av komet.
     */
    private val TiltakMedFellesOppstart = listOf(
        Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING,
        Tiltakskode.JOBBKLUBB,
        Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING,
    )

    private val TiltakskodeArenaEnkeltplass = listOf(
        Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING,
        Tiltakskode.ENKELTPLASS_FAG_OG_YRKESOPPLAERING,
        Tiltakskode.HOYERE_UTDANNING,
    )

    fun isGruppetiltak(arenaKode: String): Boolean {
        return arenaKode in GruppetiltakArenaKoder.map { it.arenakode }
    }

    fun isEgenRegiTiltak(arenaKode: String): Boolean {
        return arenaKode in EgenRegiTiltak
    }

    fun isKursTiltak(tiltakskode: Tiltakskode): Boolean {
        return tiltakskode in TiltakMedFellesOppstart
    }

    fun isEnkeltplassTiltak(arenakode: String): Boolean {
        return arenakode in TiltakskodeArenaEnkeltplass.map { it.arenakode }
    }
}
