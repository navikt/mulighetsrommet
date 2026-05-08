package no.nav.mulighetsrommet.api.amo

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.model.Tiltakskode
import java.util.UUID

/**
 * Zero-uuid tilfellene kan kanskje fjernes
 */
class OpplaringKategoriseringMapper(val db: ApiDatabase) {
    companion object {
        enum class Bransje(val id: UUID, val visningsnavn: String) {
            INGENIOR_OG_IKT_FAG(UUID.fromString("d04dff0d-fdca-4839-9bdc-44c722af5d6f"), "Ingeniør- og IKT-fag"),
            HELSE_PLEIE_OG_OMSORG(UUID.fromString("82bd7ce0-70f1-448b-8773-9015dea613e7"), "Helse, pleie og omsorg"),
            BARNE_OG_UNGDOMSARBEID(UUID.fromString("14886bad-a495-420a-9bae-d33e2d88041a"), "Barne- og ungdomsarbeid"),
            KONTORARBEID(UUID.fromString("a86c1f7a-47c3-4f69-b138-89341107e0eb"), "Kontorarbeid"),
            BUTIKK_OG_SALGSARBEID(UUID.fromString("e6749d6c-aacf-452d-baf2-d5fb5021912b"), "Butikk- og salgsarbeid"),
            BYGG_OG_ANLEGG(UUID.fromString("7cc9f791-2980-4c31-8050-e6c53afd5e8d"), "Bygg og anlegg"),
            INDUSTRIARBEID(UUID.fromString("4733d7ef-d106-47a4-b335-bfd132c8ad31"), "Industriarbeid"),
            REISELIV_SERVERING_OG_TRANSPORT(
                UUID.fromString("c8851a31-6362-4ee2-8989-e5da95726076"),
                "Reiseliv, servering og transport",
            ),
            SERVICEYRKER_OG_ANNET_ARBEID(
                UUID.fromString("47c9d5f0-66ea-4e68-949d-86733346ee80"),
                "Serviceyrker og annet arbeid",
            ),
            ANDRE_BRANSJER(UUID.fromString("54ccb278-92ea-4835-8566-659e98602905"), "Andre bransjer"),
        }

        @Serializable
        data class Sertifisering(val konseptId: Long, val label: String)

        enum class ForerkortKlasse(val id: UUID, val visningsnavn: String) {
            A(UUID.fromString("810fe1c6-56b0-4e00-8ae6-00fb574299e5"), "A - Motorsykkel"),
            A1(UUID.fromString("c67006e4-2629-4993-a047-92f31b0db557"), "A1 - Lett motorsykkel"),
            A2(UUID.fromString("ed44bd3a-aedb-4225-a3d8-c8f1b95fec5a"), "A2 - Mellomtung motorsykkel"),
            AM(UUID.fromString("dee7d6b8-02dc-4b7e-bb3a-fa71cc9248e3"), "AM - Moped"),
            AM_147(UUID.fromString("ee66eb0b-d4a8-4527-800a-135dd3c0d422"), "AM 147 - Mopedbil"),
            B(UUID.fromString("79d1a970-e8f0-4ecd-8d5e-e7c8d5f3394c"), "B - Personbil"),
            B_78(UUID.fromString("84a40884-421c-406c-994d-4c4c15ef8bcc"), "B 78 - Personbil med automatgir"),
            BE(UUID.fromString("cdbebefc-2cec-48d0-9c8e-bd464e56cfaa"), "BE - Personbil med tilhenger"),
            C(UUID.fromString("e3fcf1f7-1f20-4fca-bad5-422b7ee0418f"), "C - Lastebil"),
            C1(UUID.fromString("c65936e4-479f-4c84-b106-6c9ec0cf9aee"), "C1 - Lett lastebil"),
            C1E(UUID.fromString("69f88a08-e2de-461f-9258-4f8be546104a"), "C1E - Lett lastebil med tilhenger"),
            CE(UUID.fromString("9a85cdeb-2f6d-44f6-bef2-2add850f7b27"), "CE - Lastebil med tilhenger"),
            D(UUID.fromString("e637320c-a5f0-4f7d-ad44-0a7c4654b4c2"), "D - Buss"),
            D1(UUID.fromString("5d890e23-6800-4574-a05d-24ca81f35a2a"), "D1 - Minibuss"),
            D1E(UUID.fromString("34d00562-f382-4027-953d-2b6f6bb7e0e5"), "D1E - Minibuss med tilhenger"),
            DE(UUID.fromString("a7376d16-b0da-4140-8e67-c589be2c0ea2"), "DE - Buss med tilhenger"),
            S(UUID.fromString("5b1e1732-a5e8-45ca-955f-548c65d11065"), "S - Snøscooter"),
            T(UUID.fromString("53896c05-7650-48ed-bf23-54ae78794eba"), "T - Traktor"),
        }
    }

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

        Tiltakskode.ARBEIDSMARKEDSOPPLAERING -> arenaAmo()

        Tiltakskode.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV -> norskOpplaringGrunnleggendeFerdigheterFov()

        Tiltakskode.FAG_OG_YRKESOPPLAERING ->
            db
                .session { fagOgYrkesOpplaring() }
    }

    private fun ingenValg(tiltakskode: Tiltakskode): OpplaringKategoriseringResponse = OpplaringKategoriseringResponse(tiltakskode = tiltakskode, alternativer = emptyList())

    private fun norskOpplaringGrunnleggendeFerdigheterFov(): OpplaringKategoriseringResponse = OpplaringKategoriseringResponse(
        tiltakskode = Tiltakskode.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV,
        alternativer = listOf(
            OpplaringKategoriseringResponse.Alternativ.Verdigruppe(
                id = null,
                representerer = "kurstype",
                visningsnavn = "Kurstype",
                seleksjonstype = OpplaringKategoriseringResponse.Seleksjonstype.ENKELTVALG,
                alternativer = listOf(
                    OpplaringKategoriseringResponse.Alternativ.Verdi(
                        id = UUID.fromString("8e294221-bf60-466a-96bd-7c59c338ee5e"),
                        visningsnavn = "Norskopplæring",
                    ),
                    OpplaringKategoriseringResponse.Alternativ.Verdi(
                        id = UUID.fromString("19544ff4-25e5-4925-b942-6109b2a98552"),
                        visningsnavn = "Grunnleggende ferdigheter",
                    ),
                    OpplaringKategoriseringResponse.Alternativ.Verdi(
                        id = UUID.fromString("19544ff4-25e5-4925-b942-6109b2a98552"),
                        visningsnavn = "FOV (Forberedende opplæring for voksne)",
                    ),
                ),
            ),
        ),
    )

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

    private fun arenaAmo(): OpplaringKategoriseringResponse {
        return OpplaringKategoriseringResponse(
            tiltakskode = Tiltakskode.ARBEIDSMARKEDSOPPLAERING,
            alternativer = listOf(
                OpplaringKategoriseringResponse.Alternativ.Verdigruppe(
                    id = null,
                    representerer = "bransje",
                    visningsnavn = "Bransje",
                    seleksjonstype = OpplaringKategoriseringResponse.Seleksjonstype.ENKELTVALG,
                    alternativer = Bransje.entries.map { bransje ->
                        OpplaringKategoriseringResponse.Alternativ.Verdi(
                            id = bransje.id,
                            visningsnavn = bransje.name,
                        )
                    },
                ),
                OpplaringKategoriseringResponse.Alternativ.Verdigruppe(
                    id = null,
                    representerer = "forerkort",
                    visningsnavn = "Førerkort",
                    seleksjonstype = OpplaringKategoriseringResponse.Seleksjonstype.FLERVALG,
                    alternativer = ForerkortKlasse.entries.map { forerkortKlasse ->
                        OpplaringKategoriseringResponse.Alternativ.Verdi(
                            id = forerkortKlasse.id,
                            visningsnavn = forerkortKlasse.name,
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
