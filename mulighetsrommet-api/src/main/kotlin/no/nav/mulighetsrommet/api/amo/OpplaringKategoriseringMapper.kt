package no.nav.mulighetsrommet.api.amo

import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.model.Tiltakskode
import java.util.UUID

/**
 * Zero-uuid tilfellene kan kanskje fjernes
 */
class OpplaringKategoriseringMapper(val db: ApiDatabase) {
    fun from(tiltakskode: Tiltakskode): OpplaringKategoriseringResponse? = when (tiltakskode) {
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
        -> null

        // TODO: Fiks
        Tiltakskode.ARBEIDSMARKEDSOPPLAERING,
        Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING,
        Tiltakskode.ENKELTPLASS_FAG_OG_YRKESOPPLAERING,
        Tiltakskode.STUDIESPESIALISERING,
        -> ingenValg(tiltakskode)

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
                id = UUID.fromString("00000000-0000-0000-0000-000000000000"),
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
                    id = UUID.fromString("00000000-0000-0000-0000-0000000000"),
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
                                        id = UUID.fromString("00000000-0000-0000-0000-0000000000"),
                                        visningsnavn = "Lærefag",
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
}
