package no.nav.mulighetsrommet.api.domain.gjennomforing

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.blocking.forAll
import io.kotest.data.row
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.model.GjennomforingOppstartstype
import no.nav.mulighetsrommet.model.Tiltakskode

class ArtskontokodeTest : FunSpec({
    test("utleder artskontokode ut fra tiltakskode og oppstartstype") {
        forAll(
            row(
                Tiltakskode.ARBEIDSMARKEDSOPPLAERING,
                Artskontokode.ARBEIDSMARKEDSOPPLAERING,
                Artskontokode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING,
            ),
            row(
                Tiltakskode.FAG_OG_YRKESOPPLAERING,
                Artskontokode.FAG_OG_YRKESOPPLAERING,
                Artskontokode.ENKELTPLASS_FAG_OG_YRKESOPPLAERING,
            ),
            row(
                Tiltakskode.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV,
                Artskontokode.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV,
                Artskontokode.ENKELTPLASS_NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV,
            ),
            row(
                Tiltakskode.STUDIESPESIALISERING,
                Artskontokode.STUDIESPESIALISERING,
                Artskontokode.ENKELTPLASS_STUDIESPESIALISERING,
            ),
        ) { tiltakskode, gruppekode, enkeltplasskode ->
            utledArtskontokode(tiltakskode, GjennomforingOppstartstype.FELLES) shouldBe gruppekode
            utledArtskontokode(tiltakskode, GjennomforingOppstartstype.LOPENDE) shouldBe gruppekode
            utledArtskontokode(tiltakskode, GjennomforingOppstartstype.ENKELTPLASS) shouldBe enkeltplasskode
        }
    }

    test("utleder artskontokode for øvrige støttede tiltakskoder uavhengig av oppstartstype") {
        forAll(
            row(Tiltakskode.ARBEIDSRETTET_REHABILITERING, Artskontokode.ARBEIDSRETTET_REHABILITERING),
            row(Tiltakskode.AVKLARING, Artskontokode.AVKLARING),
            row(Tiltakskode.DIGITALT_OPPFOLGINGSTILTAK, Artskontokode.DIGITALT_OPPFOLGINGSTILTAK),
            row(Tiltakskode.JOBBKLUBB, Artskontokode.JOBBKLUBB),
            row(Tiltakskode.OPPFOLGING, Artskontokode.OPPFOLGING),
            row(Tiltakskode.ARBEIDSFORBEREDENDE_TRENING, Artskontokode.ARBEIDSFORBEREDENDE_TRENING),
            row(Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET, Artskontokode.VARIG_TILRETTELAGT_ARBEID_SKJERMET),
            row(Tiltakskode.TILRETTELAGT_ARBEID_ORDINAER, Artskontokode.TILRETTELAGT_ARBEID_ORDINAER),
            row(Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING, Artskontokode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING),
            row(Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING, Artskontokode.GRUPPE_ARBEIDSMARKEDSOPPLAERING),
            row(Tiltakskode.ENKELTPLASS_FAG_OG_YRKESOPPLAERING, Artskontokode.ENKELTPLASS_FAG_OG_YRKESOPPLAERING),
            row(Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING, Artskontokode.GRUPPE_FAG_OG_YRKESOPPLAERING),
            row(Tiltakskode.HOYERE_UTDANNING, Artskontokode.HOYERE_UTDANNING),
            row(Tiltakskode.HOYERE_YRKESFAGLIG_UTDANNING, Artskontokode.HOYERE_YRKESFAGLIG_UTDANNING),
        ) { tiltakskode, artskontokode ->
            forAll(
                row(GjennomforingOppstartstype.FELLES),
                row(GjennomforingOppstartstype.LOPENDE),
                row(GjennomforingOppstartstype.ENKELTPLASS),
            ) { oppstartstype ->
                utledArtskontokode(tiltakskode, oppstartstype) shouldBe artskontokode
            }
        }
    }

    test("avviser tiltakskoder uten artskontokode i domenet") {
        forAll(
            row(Tiltakskode.INDIVIDUELL_JOBBSTOTTE),
            row(Tiltakskode.INDIVIDUELL_JOBBSTOTTE_UNG),
            row(Tiltakskode.ARBEID_MED_STOTTE),
            row(Tiltakskode.ARBEIDSTRENING),
            row(Tiltakskode.MIDLERTIDIG_LONNSTILSKUDD),
            row(Tiltakskode.VARIG_LONNSTILSKUDD),
            row(Tiltakskode.MENTOR),
            row(Tiltakskode.INKLUDERINGSTILSKUDD),
            row(Tiltakskode.SOMMERJOBB),
            row(Tiltakskode.VTAO),
            row(Tiltakskode.FIREARIG_LONNSTILSKUDD),
            row(Tiltakskode.EKSPERTBISTAND),
        ) { tiltakskode ->
            shouldThrow<IllegalStateException> {
                utledArtskontokode(tiltakskode, GjennomforingOppstartstype.ENKELTPLASS)
            }
        }
    }
})
