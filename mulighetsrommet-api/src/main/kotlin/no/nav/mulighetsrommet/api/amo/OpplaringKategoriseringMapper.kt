package no.nav.mulighetsrommet.api.amo

import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.amo.models.Bransje
import no.nav.mulighetsrommet.api.amo.models.ForerkortKlasse
import no.nav.mulighetsrommet.api.amo.models.Kurstype
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

        Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING -> db.session { arenaGruppeAmo() }

        Tiltakskode.ARBEIDSMARKEDSOPPLAERING -> db.session { arenaAmo() }

        Tiltakskode.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV -> db.session { norskOpplaringGrunnleggendeFerdigheterFov() }

        Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING,
        Tiltakskode.FAG_OG_YRKESOPPLAERING,
        ->
            db
                .session { fagOgYrkesOpplaring(tiltakskode) }
    }

    private fun ingenValg(tiltakskode: Tiltakskode): OpplaringKategoriseringResponse = OpplaringKategoriseringResponse(tiltakskode = tiltakskode, alternativer = emptyList())

    private fun QueryContext.norskOpplaringGrunnleggendeFerdigheterFov(): OpplaringKategoriseringResponse {
        val kurstyper = queries.opplaringKategorisering.getKurstyper()
        return OpplaringKategoriseringResponse(
            tiltakskode = Tiltakskode.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV,
            alternativer = listOf(
                OpplaringKategoriseringResponse.Alternativ.Verdigruppe(
                    id = null,
                    representerer = OpplaringKategoriseringRequest::kurstypeId.name,
                    visningsnavn = "Kurstype",
                    pakrevd = true,
                    tooltip = null,
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

    private fun QueryContext.fagOgYrkesOpplaring(tiltakskode: Tiltakskode): OpplaringKategoriseringResponse {
        val utdanningsprogrammer = queries.utdanning.getUtdanningsprogrammer()
        return OpplaringKategoriseringResponse(
            tiltakskode = tiltakskode,
            alternativer = listOf(
                OpplaringKategoriseringResponse.Alternativ.Gruppe(
                    id = null,
                    visningsnavn = "Utdanningsprogram",
                    representerer = "utdanningsprogramId",
                    pakrevd = true,
                    alternativer = utdanningsprogrammer.map { (utdanningsprogram, utdanninger) ->
                        OpplaringKategoriseringResponse.Alternativ.Gruppe(
                            id = utdanningsprogram.id,
                            representerer = null,
                            pakrevd = false,
                            visningsnavn = utdanningsprogram.navn,
                            alternativer = if (utdanninger.isEmpty()) {
                                emptyList()
                            } else {
                                listOf(
                                    OpplaringKategoriseringResponse.Alternativ.Verdigruppe(
                                        id = null,
                                        tooltip = null,
                                        visningsnavn = "Lærefag",
                                        representerer = "larefag",
                                        pakrevd = true,
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

    private fun QueryContext.arenaGruppeAmo(): OpplaringKategoriseringResponse {
        val kurstyper = queries.opplaringKategorisering.getKurstyper(true).toMutableList()
        val yrkesrettetKurs = kurstyper.first {
            it.kode == Kurstype.Kode.BRANSJE_OG_YRKESRETTET
        }.also { kurstyper.remove(it) }

        val bransjer = queries.opplaringKategorisering.getBransjer().toMutableList()
        bransjer.find { it.kode == Bransje.Kode.ANDRE_BRANSJER }?.let {
            bransjer.remove(it)
            bransjer.addLast(it)
        }
        val forerkort = queries.opplaringKategorisering.getForerkortKlasser()

        return OpplaringKategoriseringResponse(
            tiltakskode = Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING,
            alternativer = listOf(
                OpplaringKategoriseringResponse.Alternativ.Gruppe(
                    id = null,
                    representerer = OpplaringKategoriseringRequest::kurstypeId.name,
                    pakrevd = true,
                    visningsnavn = "Kurstype",
                    alternativer = listOf(
                        yrkesrettetKurs.let { kurstype ->
                            OpplaringKategoriseringResponse.Alternativ.Gruppe(
                                id = kurstype.id,
                                visningsnavn = kurstype.navn,
                                representerer = null,
                                pakrevd = false,
                                alternativer = amoBransjeForerkortSertifisering(bransjer, forerkort),
                            )
                        },
                    ) + kurstyper.map { kurstype ->
                        OpplaringKategoriseringResponse.Alternativ.Gruppe(
                            id = kurstype.id,
                            visningsnavn = kurstype.navn,
                            representerer = null,
                            pakrevd = false,
                            alternativer = emptyList(),
                        )
                    },
                ),
            ),
        )
    }

    private fun QueryContext.arenaAmo(): OpplaringKategoriseringResponse {
        val bransjer = queries.opplaringKategorisering.getBransjer().toMutableList()
        bransjer.find { it.kode == Bransje.Kode.ANDRE_BRANSJER }?.let {
            bransjer.remove(it)
            bransjer.addLast(it)
        }
        val forerkort = queries.opplaringKategorisering.getForerkortKlasser()
        return OpplaringKategoriseringResponse(
            tiltakskode = Tiltakskode.ARBEIDSMARKEDSOPPLAERING,
            alternativer = amoBransjeForerkortSertifisering(bransjer, forerkort),
        )
    }

    private fun amoBransjeForerkortSertifisering(
        bransjer: List<Bransje>,
        forerkort: Set<ForerkortKlasse>,
    ): List<OpplaringKategoriseringResponse.Alternativ.Container> = listOf(
        OpplaringKategoriseringResponse.Alternativ.Verdigruppe(
            id = null,
            tooltip = bransjeTooltip(),
            representerer = OpplaringKategoriseringRequest::bransjeId.name,
            pakrevd = true,
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
            tooltip = null,
            representerer = OpplaringKategoriseringRequest::forerkort.name,
            pakrevd = false,
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
            representerer = OpplaringKategoriseringRequest::sertifiseringer.name,
            pakrevd = false,
            visningsnavn = "Sertifiseringer",
            seleksjonstype = OpplaringKategoriseringResponse.Seleksjonstype.FLERVALG,
            kilde = OpplaringKategoriseringResponse.Alternativ.VerdigruppeSok.Kilde.JANZZ_SERTIFISERING,
        ),
    )

    private fun bransjeTooltip(): OpplaringKategoriseringResponse.Tooltip = OpplaringKategoriseringResponse.Tooltip.FlereUtlistinger(
        listOf(
            OpplaringKategoriseringResponse.Tooltip.Utlisting(
                tittel = "Barne- og ungdomsarbeid",
                innhold = listOf(
                    "Skoleassistenter",
                    "Barnehage- og skolefritidsassistenter",
                ),
            ),
            OpplaringKategoriseringResponse.Tooltip.Utlisting(
                tittel = "Butikk- og salgsarbeid",
                innhold = listOf(
                    "Butikkarbeid",
                    "Annet salgsarbeid",
                ),
            ),
            OpplaringKategoriseringResponse.Tooltip.Utlisting(
                tittel = "Bygg og anlegg",
                innhold = listOf(
                    "Rørleggere",
                    "Snekkere og tømrere",
                    "Elektrikere",
                    "Andre bygningsarbeidere",
                    "Anleggsarbeidere",
                    "Hjelpearbeidere innen bygg og anlegg",
                    "Mellomledere innen bygg og anlegg",
                ),
            ),
            OpplaringKategoriseringResponse.Tooltip.Utlisting(
                tittel = "Helse, pleie og omsorg",
                innhold = listOf(
                    "Omsorgs- og pleiearbeidere",
                    "Annet helsepersonell",
                    "Mellomledere innen helse, pleie og omsorg",
                ),
            ),
            OpplaringKategoriseringResponse.Tooltip.Utlisting(
                tittel = "Industriarbeid",
                innhold = listOf(
                    "Mekanikere",
                    "Prosess- og maskinoperatører",
                    "Næringsmiddelarbeid",
                    "Automatikere og elektriske montører",
                    "Andre håndverkere",
                    "Hjelpearbeid innen industrien",
                    "Mellomledere innen industriarbeid",
                ),
            ),
            OpplaringKategoriseringResponse.Tooltip.Utlisting(
                tittel = "Ingeniør- og IKT-fag",
                innhold = listOf(
                    "Andre naturvitenskapelige yrker",
                    "Ikt-yrker",
                    "Ingeniører og teknikere",
                ),
            ),
            OpplaringKategoriseringResponse.Tooltip.Utlisting(
                tittel = "Kontorarbeid",
                innhold = listOf(
                    "Lavere saksbehandlere innen offentlig administrasjon",
                    "Sekretærer",
                    "Økonomi- og kontormedarbeidere",
                    "Lager- og transportmedarbeidere",
                    "Resepsjonister og sentralbordoperatører",
                    "Andre funksjonærer",
                ),
            ),

            OpplaringKategoriseringResponse.Tooltip.Utlisting(
                tittel = "Reiseliv, servering og transport",
                innhold = listOf(
                    "Maritime yrker",
                    "Førere av transportmidler",
                    "Reiseledere, guider og reisebyråmedarbeidere",
                    "Konduktører og kabinpersonale",
                    "Kokker",
                    "Hovmestere, servitører og hjelpepersonell",
                    "Mellomledere innen reiseliv og transport",
                ),
            ),
            OpplaringKategoriseringResponse.Tooltip.Utlisting(
                tittel = "Serviceyrker og annet arbeid",
                innhold = listOf(
                    "Yrker innen politi, brannvesen, toll og forsvar",
                    "Velvære",
                    "Rengjøring",
                    "Vakthold og vaktmestere",
                    "Annet arbeid",
                    "Yrker innen kunst, sport og kultur",
                ),
            ),
        ),
    )
}
