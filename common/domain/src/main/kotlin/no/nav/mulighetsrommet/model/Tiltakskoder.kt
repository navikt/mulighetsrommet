package no.nav.mulighetsrommet.model

enum class Tiltakskode(
    val system: TiltakstypeSystem,
    val arenakode: String?,
    val egenskaper: Set<TiltakstypeEgenskap>,
    val gruppe: Tiltaksgruppe? = null,
) {
    ARBEIDSRETTET_REHABILITERING(
        system = TiltakstypeSystem.TILTAKSADMINISTRASJON,
        arenakode = "ARBRRHDAG",
        egenskaper = setOf(
            TiltakstypeEgenskap.STOTTER_AVTALER,
            TiltakstypeEgenskap.KREVER_DIREKTE_VEDTAK,
        ),
    ),
    AVKLARING(
        system = TiltakstypeSystem.TILTAKSADMINISTRASJON,
        arenakode = "AVKLARAG",
        egenskaper = setOf(
            TiltakstypeEgenskap.STOTTER_AVTALER,
            TiltakstypeEgenskap.KREVER_DIREKTE_VEDTAK,
        ),
    ),
    DIGITALT_OPPFOLGINGSTILTAK(
        system = TiltakstypeSystem.TILTAKSADMINISTRASJON,
        arenakode = "DIGIOPPARB",
        egenskaper = setOf(
            TiltakstypeEgenskap.STOTTER_AVTALER,
            TiltakstypeEgenskap.KREVER_DIREKTE_VEDTAK,
        ),
    ),
    JOBBKLUBB(
        system = TiltakstypeSystem.TILTAKSADMINISTRASJON,
        arenakode = "JOBBK",
        egenskaper = setOf(
            TiltakstypeEgenskap.STOTTER_AVTALER,
            TiltakstypeEgenskap.KREVER_DELTIDSPROSENT,
        ),
    ),
    OPPFOLGING(
        system = TiltakstypeSystem.TILTAKSADMINISTRASJON,
        arenakode = "INDOPPFAG",
        egenskaper = setOf(
            TiltakstypeEgenskap.STOTTER_AVTALER,
            TiltakstypeEgenskap.KREVER_DIREKTE_VEDTAK,
        ),
    ),

    /**
     * Forhåndsgodkjente tiltak
     */
    ARBEIDSFORBEREDENDE_TRENING(
        system = TiltakstypeSystem.TILTAKSADMINISTRASJON,
        arenakode = "ARBFORB",
        egenskaper = setOf(
            TiltakstypeEgenskap.STOTTER_AVTALER,
            TiltakstypeEgenskap.KREVER_DIREKTE_VEDTAK,
            TiltakstypeEgenskap.STOTTER_TILSKUDD_FOR_INVESTERINGER,
        ),
    ),
    VARIG_TILRETTELAGT_ARBEID_SKJERMET(
        system = TiltakstypeSystem.TILTAKSADMINISTRASJON,
        arenakode = "VASV",
        egenskaper = setOf(
            TiltakstypeEgenskap.STOTTER_AVTALER,
            TiltakstypeEgenskap.KREVER_DIREKTE_VEDTAK,
            TiltakstypeEgenskap.STOTTER_TILSKUDD_FOR_INVESTERINGER,
        ),
    ),
    TILPASSET_JOBBSTOTTE(
        system = TiltakstypeSystem.TILTAKSADMINISTRASJON,
        arenakode = null,
        egenskaper = setOf(
            TiltakstypeEgenskap.STOTTER_AVTALER,
        ),
    ),

    /**
     * Opplæringstiltak
     */
    ARBEIDSMARKEDSOPPLAERING(
        system = TiltakstypeSystem.TILTAKSADMINISTRASJON,
        arenakode = "GRUPPEAMO",
        egenskaper = setOf(
            TiltakstypeEgenskap.STOTTER_AVTALER,
            TiltakstypeEgenskap.STOTTER_ENKELTPLASSER,
            TiltakstypeEgenskap.KREVER_DELTIDSPROSENT,
        ),
        gruppe = Tiltaksgruppe.OPPLAERING,
    ),
    ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING(
        system = TiltakstypeSystem.TILTAKSADMINISTRASJON,
        arenakode = "ENKELAMO",
        egenskaper = setOf(
            TiltakstypeEgenskap.STOTTER_ENKELTPLASSER,
            TiltakstypeEgenskap.KREVER_DIREKTE_VEDTAK,
        ),
        gruppe = Tiltaksgruppe.OPPLAERING,
    ),
    ENKELTPLASS_FAG_OG_YRKESOPPLAERING(
        system = TiltakstypeSystem.TILTAKSADMINISTRASJON,
        arenakode = "ENKFAGYRKE",
        egenskaper = setOf(
            TiltakstypeEgenskap.STOTTER_ENKELTPLASSER,
            TiltakstypeEgenskap.KREVER_DIREKTE_VEDTAK,
        ),
        gruppe = Tiltaksgruppe.OPPLAERING,
    ),
    FAG_OG_YRKESOPPLAERING(
        system = TiltakstypeSystem.TILTAKSADMINISTRASJON,
        arenakode = "GRUFAGYRKE",
        egenskaper = setOf(
            TiltakstypeEgenskap.STOTTER_AVTALER,
            TiltakstypeEgenskap.STOTTER_ENKELTPLASSER,
            TiltakstypeEgenskap.KREVER_DELTIDSPROSENT,
        ),
        gruppe = Tiltaksgruppe.OPPLAERING,
    ),
    GRUPPE_ARBEIDSMARKEDSOPPLAERING(
        system = TiltakstypeSystem.TILTAKSADMINISTRASJON,
        arenakode = "GRUPPEAMO",
        egenskaper = setOf(
            TiltakstypeEgenskap.STOTTER_AVTALER,
            TiltakstypeEgenskap.KREVER_DELTIDSPROSENT,
        ),
        gruppe = Tiltaksgruppe.OPPLAERING,
    ),
    GRUPPE_FAG_OG_YRKESOPPLAERING(
        system = TiltakstypeSystem.TILTAKSADMINISTRASJON,
        arenakode = "GRUFAGYRKE",
        egenskaper = setOf(
            TiltakstypeEgenskap.STOTTER_AVTALER,
            TiltakstypeEgenskap.KREVER_DELTIDSPROSENT,
        ),
        gruppe = Tiltaksgruppe.OPPLAERING,
    ),
    HOYERE_UTDANNING(
        system = TiltakstypeSystem.TILTAKSADMINISTRASJON,
        arenakode = "HOYEREUTD",
        egenskaper = setOf(
            TiltakstypeEgenskap.STOTTER_ENKELTPLASSER,
            TiltakstypeEgenskap.KREVER_DELTIDSPROSENT,
        ),
        gruppe = Tiltaksgruppe.OPPLAERING,
    ),
    HOYERE_YRKESFAGLIG_UTDANNING(
        system = TiltakstypeSystem.TILTAKSADMINISTRASJON,
        arenakode = "GRUFAGYRKE",
        egenskaper = setOf(
            TiltakstypeEgenskap.STOTTER_ENKELTPLASSER,
            TiltakstypeEgenskap.KREVER_DELTIDSPROSENT,
        ),
        gruppe = Tiltaksgruppe.OPPLAERING,
    ),
    NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV(
        system = TiltakstypeSystem.TILTAKSADMINISTRASJON,
        arenakode = "GRUPPEAMO",
        egenskaper = setOf(
            TiltakstypeEgenskap.STOTTER_AVTALER,
            TiltakstypeEgenskap.STOTTER_ENKELTPLASSER,
            TiltakstypeEgenskap.KREVER_DELTIDSPROSENT,
        ),
        gruppe = Tiltaksgruppe.OPPLAERING,
    ),
    STUDIESPESIALISERING(
        system = TiltakstypeSystem.TILTAKSADMINISTRASJON,
        arenakode = "GRUPPEAMO",
        egenskaper = setOf(
            TiltakstypeEgenskap.STOTTER_AVTALER,
            TiltakstypeEgenskap.STOTTER_ENKELTPLASSER,
            TiltakstypeEgenskap.KREVER_DELTIDSPROSENT,
        ),
        gruppe = Tiltaksgruppe.OPPLAERING,
    ),

    /**
     * Tiltak i egen regi
     */
    INDIVIDUELL_JOBBSTOTTE(
        system = TiltakstypeSystem.ARENA,
        arenakode = "INDJOBSTOT",
        egenskaper = setOf(),
    ),
    INDIVIDUELL_JOBBSTOTTE_UNG(
        system = TiltakstypeSystem.ARENA,
        arenakode = "IPSUNG",
        egenskaper = setOf(),
    ),
    ARBEID_MED_STOTTE(
        system = TiltakstypeSystem.ARENA,
        arenakode = "UTVAOONAV",
        egenskaper = setOf(),
    ),

    /**
     * Tiltak hos arbeidsgiver
     */
    ARBEIDSTRENING(
        system = TiltakstypeSystem.TILTAK_ARBEIDSGIVER,
        arenakode = "ARBTREN",
        egenskaper = setOf(),
    ),
    MIDLERTIDIG_LONNSTLSKUDD(
        system = TiltakstypeSystem.TILTAK_ARBEIDSGIVER,
        arenakode = "MIDLONTIL",
        egenskaper = setOf(),
    ),
    VARIG_LONNSTILSKUD(
        system = TiltakstypeSystem.TILTAK_ARBEIDSGIVER,
        arenakode = "VARLONTIL",
        egenskaper = setOf(),
    ),
    MENTOR(
        system = TiltakstypeSystem.TILTAK_ARBEIDSGIVER,
        arenakode = "MENTOR",
        egenskaper = setOf(),
    ),
    INKLUDERINGSTILSKUD(
        system = TiltakstypeSystem.TILTAK_ARBEIDSGIVER,
        arenakode = "INKLUTILS",
        egenskaper = setOf(),
    ),
    SOMMERJOBB(
        system = TiltakstypeSystem.TILTAK_ARBEIDSGIVER,
        arenakode = "TILSJOBB",
        egenskaper = setOf(),
    ),
    VTAO(
        system = TiltakstypeSystem.TILTAK_ARBEIDSGIVER,
        arenakode = "VATIAROR",
        egenskaper = setOf(),
    ),
    FIREARIG_LONNSTILSUDD(
        system = TiltakstypeSystem.TILTAK_ARBEIDSGIVER,
        arenakode = null,
        egenskaper = setOf(),
    ),
    ;

    fun harEgenskap(vararg egenskap: TiltakstypeEgenskap): Boolean {
        return egenskaper.containsAll(egenskap.toSet())
    }
}

enum class TiltakstypeEgenskap {
    /**
     * Gjør at tiltaket har systemstøtte for avtaler (inkludert gjennomføringer for avtaler).
     * Dette inkluderer bl.a.
     *   - Vises i filter for avtaler og gjennomføringer
     *   - Avtaler, samt gjennomføringer for avtaler, kan opprettes for tiltaket
     */
    STOTTER_AVTALER,

    /**
     * Gjør at tiltaket har systemstøtte for enkeltplass-gjennomføringer.
     * Dette inkluderer bl.a.
     *   - Vises i filter for gjennomføringer
     *   - Enkeltplasser kan opprettes
     */
    STOTTER_ENKELTPLASSER,

    /**
     * Gjør deltidsprosent påkrevd i gjennomføringer
     */
    KREVER_DELTIDSPROSENT,

    /**
     * Gjør deltakers innsøksform til "direkte vedtak" - altså at det er Nav-veileder som gjør vedtak om tiltaksplass.
     * Hvis [KREVER_DIREKTE_VEDTAK] ikke er satt så kan innsøksform bestemmes av administrator for tiltaket.
     */
    KREVER_DIREKTE_VEDTAK,

    /**
     * Indikerer at tiltakstypen støtter tilskudd for investeringer.
     */
    STOTTER_TILSKUDD_FOR_INVESTERINGER,
}

enum class Tiltaksgruppe(val tittel: String) {
    OPPLAERING("Opplæringstiltak"),
}

enum class TiltakstypeSystem {
    TILTAKSADMINISTRASJON,
    ARENA,
    TILTAK_ARBEIDSGIVER,
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
        // TODO: disse tiltakskodene er egentlig ikke bare for "gruppetiltak", men foreløpig er det OK.
        //  Vi burde komme oss vekk fra disse tiltaskode-listene og evt. erstatte med egenskaper direkte på Tiltalkskode-enumen
        Tiltakskode.ARBEIDSMARKEDSOPPLAERING,
        Tiltakskode.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV,
        Tiltakskode.STUDIESPESIALISERING,
        Tiltakskode.FAG_OG_YRKESOPPLAERING,
    )

    /**
     * Tiltakskoder for tiltak i egen regi (regi av Nav), og som foreløpig administreres i Sanity ikke i admin-flate.
     */
    private val TiltakskoderEgenRegi = listOf(
        "INDJOBSTOT",
        "IPSUNG",
        "UTVAOONAV",
    )

    private val TiltakskoderEnkeltplasser = listOf(
        Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING,
        Tiltakskode.ENKELTPLASS_FAG_OG_YRKESOPPLAERING,
        Tiltakskode.HOYERE_UTDANNING,
        Tiltakskode.HOYERE_YRKESFAGLIG_UTDANNING,
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

    fun isEnkeltplassTiltak(arenakode: String): Boolean {
        return arenakode in TiltakskoderEnkeltplasser.map { it.arenakode }
    }

    fun isEnkeltplassTiltak(tiltakskode: Tiltakskode): Boolean {
        return tiltakskode in TiltakskoderEnkeltplasser
    }
}
