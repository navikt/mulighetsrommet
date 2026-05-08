package no.nav.mulighetsrommet.api.amo

import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.model.Tiltakskode

class OpplaringKategoriseringMapper(val db: ApiDatabase) {
    fun from(tiltakskode: Tiltakskode): OpplaringKategoriseringResponse = when (tiltakskode) {
        Tiltakskode.ARBEIDSTRENING,
        Tiltakskode.ARBEIDSRETTET_REHABILITERING,
        Tiltakskode.AVKLARING,
        Tiltakskode.DIGITALT_OPPFOLGINGSTILTAK,
        Tiltakskode.JOBBKLUBB,
        Tiltakskode.OPPFOLGING,
        Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
        Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET,
        Tiltakskode.TILPASSET_JOBBSTOTTE,
        Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING,
        Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING,
        Tiltakskode.HOYERE_UTDANNING,
        Tiltakskode.HOYERE_YRKESFAGLIG_UTDANNING,
        Tiltakskode.INDIVIDUELL_JOBBSTOTTE,
        Tiltakskode.INDIVIDUELL_JOBBSTOTTE_UNG,
        Tiltakskode.ARBEID_MED_STOTTE,
        Tiltakskode.MIDLERTIDIG_LONNSTLSKUDD,
        Tiltakskode.VARIG_LONNSTILSKUD,
        Tiltakskode.MENTOR,
        Tiltakskode.INKLUDERINGSTILSKUD,
        Tiltakskode.SOMMERJOBB,
        Tiltakskode.VTAO,
        Tiltakskode.FIREARIG_LONNSTILSUDD,
        -> ingenValg(tiltakskode)

        Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING,
        Tiltakskode.ENKELTPLASS_FAG_OG_YRKESOPPLAERING,
        Tiltakskode.STUDIESPESIALISERING,
        -> ingenValg(tiltakskode)

        Tiltakskode.ARBEIDSMARKEDSOPPLAERING -> db.session { arenaAmo() }

        Tiltakskode.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV -> db.session { norskOpplaringGrunnleggendeFerdigheterFov() }

        Tiltakskode.FAG_OG_YRKESOPPLAERING ->
            db
                .session { fagOgYrkesOpplaring() }
    }

    private fun ingenValg(tiltakskode: Tiltakskode): OpplaringKategoriseringResponse = OpplaringKategoriseringResponse(tiltakskode = tiltakskode, alternativer = emptyList())

    private fun QueryContext.norskOpplaringGrunnleggendeFerdigheterFov(): OpplaringKategoriseringResponse {
        val kurstyper = queries.opplaringKategorisering.getKurstyper()
        return OpplaringKategoriseringResponse(
            tiltakskode = Tiltakskode.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV,
            alternativer = listOf(
                OpplaringKategoriseringResponse.Alternativ.Verdigruppe(
                    id = null,
                    representerer = "kurstype",
                    visningsnavn = "Kurstype",
                    seleksjonstype = OpplaringKategoriseringResponse.Seleksjonstype.ENKELTVALG,
                    alternativer = kurstyper.map { kurstype ->
                        OpplaringKategoriseringResponse.Alternativ.Verdi(
                            id = kurstype.id,
                            visningsnavn = kurstype.navn,
                        )
                    },
                ),
            ),
        )
    }

    private fun QueryContext.fagOgYrkesOpplaring(): OpplaringKategoriseringResponse {
        val utdanningsprogrammer = queries.utdanning.getUtdanningsprogrammer()
        return OpplaringKategoriseringResponse(
            tiltakskode = Tiltakskode.FAG_OG_YRKESOPPLAERING,
            alternativer = listOf(
                OpplaringKategoriseringResponse.Alternativ.Gruppe(
                    id = null,
                    visningsnavn = "Utdanningsprogram",
                    alternativer = utdanningsprogrammer.map { (utdanningsprogram, utdanninger) ->
                        OpplaringKategoriseringResponse.Alternativ.Gruppe(
                            id = utdanningsprogram.id,
                            visningsnavn = utdanningsprogram.navn,
                            alternativer = if (utdanninger.isEmpty()) {
                                emptyList()
                            } else {
                                listOf(
                                    OpplaringKategoriseringResponse.Alternativ.Verdigruppe(
                                        id = null,
                                        visningsnavn = "Lærefag",
                                        representerer = "larefag",
                                        seleksjonstype = OpplaringKategoriseringResponse.Seleksjonstype.FLERVALG,
                                        alternativer = utdanninger.map { utdanning ->
                                            OpplaringKategoriseringResponse.Alternativ.Verdi(
                                                id = utdanning.id,
                                                visningsnavn = utdanning.navn,
                                            )
                                        },
                                    ),
                                )
                            },
                        )
                    },
                ),
            ),
        )
    }

    private fun QueryContext.arenaAmo(): OpplaringKategoriseringResponse {
        val bransjer = queries.opplaringKategorisering.getBransjer()
        val forerkort = queries.opplaringKategorisering.getForerkortKlasser()
        return OpplaringKategoriseringResponse(
            tiltakskode = Tiltakskode.ARBEIDSMARKEDSOPPLAERING,
            alternativer = listOf(
                OpplaringKategoriseringResponse.Alternativ.Verdigruppe(
                    id = null,
                    representerer = "bransje",
                    visningsnavn = "Bransje",
                    seleksjonstype = OpplaringKategoriseringResponse.Seleksjonstype.ENKELTVALG,
                    alternativer = bransjer.map { bransje ->
                        OpplaringKategoriseringResponse.Alternativ.Verdi(
                            id = bransje.id,
                            visningsnavn = bransje.navn,
                        )
                    },
                ),
                OpplaringKategoriseringResponse.Alternativ.Verdigruppe(
                    id = null,
                    representerer = "forerkort",
                    visningsnavn = "Førerkort",
                    seleksjonstype = OpplaringKategoriseringResponse.Seleksjonstype.FLERVALG,
                    alternativer = forerkort.map { forerkortKlasse ->
                        OpplaringKategoriseringResponse.Alternativ.Verdi(
                            id = forerkortKlasse.id,
                            visningsnavn = forerkortKlasse.navn,
                        )
                    },
                ),
                OpplaringKategoriseringResponse.Alternativ.VerdigruppeSok(
                    id = null,
                    representerer = "sertifiseringer",
                    visningsnavn = "Sertifiseringer",
                    seleksjonstype = OpplaringKategoriseringResponse.Seleksjonstype.FLERVALG,
                    kilde = OpplaringKategoriseringResponse.Alternativ.VerdigruppeSok.Kilde.JANZZ_SERTIFISERING,
                ),
            ),
        )
    }
}
