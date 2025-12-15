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

    // Nye tiltakskoder 2025 §7-2 a-f:
    ARBEIDSMARKEDSOPPLAERING("GRUPPEAMO"), // a
    NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV("GRUPPEAMO"), // b
    STUDIESPESIALISERING("GRUPPEAMO"), // c
    FAG_OG_YRKESOPPLAERING("GRUFAGYRKE"), // d
    HOYERE_YRKESFAGLIG_UTDANNING("GRUFAGYRKE"), // e
    // HOYERE_UTDANNING("HOYEREUTD") // f, eksisterer fra før
}

object Tiltakskoder {
    /**
     * Tiltakskoder for de forhåndsgodkjente og anskaffede tiltakene, kalt "gruppetilak" (av oss i hvert fall), og som
     * skal migreres fra Arena som del av P4.
     */
    private val TiltakskoderGruppe = listOf(
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

    fun tilArenaStottetType(tiltakskode: Tiltakskode): Tiltakskode = when (tiltakskode) {
        Tiltakskode.ARBEIDSFORBEREDENDE_TRENING -> Tiltakskode.ARBEIDSFORBEREDENDE_TRENING

        Tiltakskode.ARBEIDSRETTET_REHABILITERING -> Tiltakskode.ARBEIDSRETTET_REHABILITERING

        Tiltakskode.AVKLARING -> Tiltakskode.AVKLARING

        Tiltakskode.DIGITALT_OPPFOLGINGSTILTAK -> Tiltakskode.DIGITALT_OPPFOLGINGSTILTAK

        Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING -> Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING

        Tiltakskode.ENKELTPLASS_FAG_OG_YRKESOPPLAERING -> Tiltakskode.ENKELTPLASS_FAG_OG_YRKESOPPLAERING

        Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING -> Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING

        Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING -> Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING

        Tiltakskode.HOYERE_UTDANNING -> Tiltakskode.HOYERE_UTDANNING

        Tiltakskode.JOBBKLUBB -> Tiltakskode.JOBBKLUBB

        Tiltakskode.OPPFOLGING -> Tiltakskode.OPPFOLGING

        Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET -> Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET

        Tiltakskode.ARBEIDSMARKEDSOPPLAERING,
        Tiltakskode.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV,
        Tiltakskode.STUDIESPESIALISERING,
        ->
            Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING

        Tiltakskode.FAG_OG_YRKESOPPLAERING,
        Tiltakskode.HOYERE_YRKESFAGLIG_UTDANNING,
        ->
            Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING
    }

    fun erStottetIArena(tiltakskode: Tiltakskode): Boolean = tiltakskode == tilArenaStottetType(tiltakskode)


    /**
     * Nye tiltakskoder 2025 §7-2 a-f:
     */
    val OpplaeringsTiltak2025 = setOf(
        Tiltakskode.ARBEIDSMARKEDSOPPLAERING, // a
        Tiltakskode.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV, // b
        Tiltakskode.STUDIESPESIALISERING, // c
        Tiltakskode.FAG_OG_YRKESOPPLAERING, // d
        Tiltakskode.HOYERE_YRKESFAGLIG_UTDANNING, // e
        Tiltakskode.HOYERE_UTDANNING, // f
    )

    /**
     * Tiltakskoder for tiltak i egen regi (regi av Nav), og som foreløpig administreres i Sanity ikke i admin-flate.
     */
    private val TiltakskoderEgenRegi = listOf(
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

    private val TiltakskoderEnkeltplasser = listOf(
        Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING,
        Tiltakskode.ENKELTPLASS_FAG_OG_YRKESOPPLAERING,
        Tiltakskode.HOYERE_UTDANNING,
    )

    fun isGruppetiltak(tiltakskode: Tiltakskode): Boolean {
        return tiltakskode in TiltakskoderGruppe
    }

    fun isGruppetiltak(arenaKode: String): Boolean {
        return arenaKode in TiltakskoderGruppe.map { it.arenakode }
    }

    fun isEgenRegiTiltak(arenaKode: String): Boolean {
        return arenaKode in TiltakskoderEgenRegi
    }

    fun isKursTiltak(tiltakskode: Tiltakskode): Boolean {
        return tiltakskode in TiltakMedFellesOppstart
    }

    fun isEnkeltplassTiltak(arenakode: String): Boolean {
        return arenakode in TiltakskoderEnkeltplasser.map { it.arenakode }
    }

    fun isEnkeltplassTiltak(tiltakskode: Tiltakskode): Boolean {
        return tiltakskode in TiltakskoderEnkeltplasser
    }
}
