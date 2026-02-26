package no.nav.mulighetsrommet.model

enum class Tiltakskode(
    val arenakode: String,
    val egenskaper: Set<TiltakstypeEgenskap>,
    val gruppe: Tiltaksgruppe? = null,
) {
    ARBEIDSFORBEREDENDE_TRENING(
        arenakode = "ARBFORB",
        egenskaper = setOf(
            TiltakstypeEgenskap.KAN_OPPRETTE_AVTALE,
            TiltakstypeEgenskap.KREVER_DIREKTE_VEDTAK,
            TiltakstypeEgenskap.STOTTER_TILSKUDD_FOR_INVESTERINGER,
        ),
    ),
    ARBEIDSRETTET_REHABILITERING(
        arenakode = "ARBRRHDAG",
        egenskaper = setOf(
            TiltakstypeEgenskap.KAN_OPPRETTE_AVTALE,
            TiltakstypeEgenskap.KREVER_DIREKTE_VEDTAK,
        ),
    ),
    AVKLARING(
        arenakode = "AVKLARAG",
        egenskaper = setOf(
            TiltakstypeEgenskap.KAN_OPPRETTE_AVTALE,
            TiltakstypeEgenskap.KREVER_DIREKTE_VEDTAK,
        ),
    ),
    DIGITALT_OPPFOLGINGSTILTAK(
        arenakode = "DIGIOPPARB",
        egenskaper = setOf(
            TiltakstypeEgenskap.KAN_OPPRETTE_AVTALE,
            TiltakstypeEgenskap.KREVER_DIREKTE_VEDTAK,
        ),
    ),
    JOBBKLUBB(
        arenakode = "JOBBK",
        egenskaper = setOf(
            TiltakstypeEgenskap.KAN_OPPRETTE_AVTALE,
            TiltakstypeEgenskap.KREVER_DELTIDSPROSENT,
        ),
    ),
    OPPFOLGING(
        arenakode = "INDOPPFAG",
        egenskaper = setOf(
            TiltakstypeEgenskap.KAN_OPPRETTE_AVTALE,
            TiltakstypeEgenskap.KREVER_DIREKTE_VEDTAK,
        ),
    ),
    VARIG_TILRETTELAGT_ARBEID_SKJERMET(
        arenakode = "VASV",
        egenskaper = setOf(
            TiltakstypeEgenskap.KAN_OPPRETTE_AVTALE,
            TiltakstypeEgenskap.KREVER_DIREKTE_VEDTAK,
            TiltakstypeEgenskap.STOTTER_TILSKUDD_FOR_INVESTERINGER,
        ),
    ),

    ARBEIDSMARKEDSOPPLAERING(
        arenakode = "GRUPPEAMO",
        egenskaper = setOf(
            TiltakstypeEgenskap.KAN_OPPRETTE_AVTALE,
            TiltakstypeEgenskap.KREVER_DELTIDSPROSENT,
        ),
        gruppe = Tiltaksgruppe.OPPLAERING,
    ),
    ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING(
        arenakode = "ENKELAMO",
        egenskaper = setOf(
            TiltakstypeEgenskap.KREVER_DIREKTE_VEDTAK,
        ),
        gruppe = Tiltaksgruppe.OPPLAERING,
    ),
    ENKELTPLASS_FAG_OG_YRKESOPPLAERING(
        arenakode = "ENKFAGYRKE",
        egenskaper = setOf(
            TiltakstypeEgenskap.KREVER_DIREKTE_VEDTAK,
        ),
        gruppe = Tiltaksgruppe.OPPLAERING,
    ),
    FAG_OG_YRKESOPPLAERING(
        arenakode = "GRUFAGYRKE",
        egenskaper = setOf(
            TiltakstypeEgenskap.KAN_OPPRETTE_AVTALE,
            TiltakstypeEgenskap.KREVER_DELTIDSPROSENT,
        ),
        gruppe = Tiltaksgruppe.OPPLAERING,
    ),
    GRUPPE_ARBEIDSMARKEDSOPPLAERING(
        arenakode = "GRUPPEAMO",
        egenskaper = setOf(
            TiltakstypeEgenskap.KAN_OPPRETTE_AVTALE,
            TiltakstypeEgenskap.KREVER_DELTIDSPROSENT,
        ),
        gruppe = Tiltaksgruppe.OPPLAERING,
    ),
    GRUPPE_FAG_OG_YRKESOPPLAERING(
        arenakode = "GRUFAGYRKE",
        egenskaper = setOf(
            TiltakstypeEgenskap.KAN_OPPRETTE_AVTALE,
            TiltakstypeEgenskap.KREVER_DELTIDSPROSENT,
        ),
        gruppe = Tiltaksgruppe.OPPLAERING,
    ),
    HOYERE_UTDANNING(
        arenakode = "HOYEREUTD",
        egenskaper = setOf(
            TiltakstypeEgenskap.KREVER_DELTIDSPROSENT,
        ),
        gruppe = Tiltaksgruppe.OPPLAERING,
    ),
    HOYERE_YRKESFAGLIG_UTDANNING(
        arenakode = "GRUFAGYRKE",
        egenskaper = setOf(
            TiltakstypeEgenskap.KREVER_DELTIDSPROSENT,
        ),
        gruppe = Tiltaksgruppe.OPPLAERING,
    ),
    NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV(
        arenakode = "GRUPPEAMO",
        egenskaper = setOf(
            TiltakstypeEgenskap.KAN_OPPRETTE_AVTALE,
            TiltakstypeEgenskap.KREVER_DELTIDSPROSENT,
        ),
        gruppe = Tiltaksgruppe.OPPLAERING,
    ),
    STUDIESPESIALISERING(
        arenakode = "GRUPPEAMO",
        egenskaper = setOf(
            TiltakstypeEgenskap.KAN_OPPRETTE_AVTALE,
            TiltakstypeEgenskap.KREVER_DELTIDSPROSENT,
        ),
        gruppe = Tiltaksgruppe.OPPLAERING,
    ),
    ;

    fun harEgenskap(vararg egenskap: TiltakstypeEgenskap): Boolean {
        return egenskaper.containsAll(egenskap.toSet())
    }
}

enum class TiltakstypeEgenskap {
    KAN_OPPRETTE_AVTALE,
    KREVER_DELTIDSPROSENT,
    KREVER_DIREKTE_VEDTAK,
    STOTTER_TILSKUDD_FOR_INVESTERINGER,
}

enum class Tiltaksgruppe(val tittel: String) {
    OPPLAERING("Opplæringstiltak"),
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
