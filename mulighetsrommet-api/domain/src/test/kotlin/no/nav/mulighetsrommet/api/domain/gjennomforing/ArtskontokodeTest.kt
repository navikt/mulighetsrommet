package no.nav.mulighetsrommet.api.domain.gjennomforing

import io.kotest.core.spec.style.FunSpec
import io.kotest.data.blocking.forAll
import io.kotest.data.row
import io.kotest.matchers.nulls.shouldBeNull
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

    test("returnerer null for tiltakskoder som foreløpig mangler artskontokode") {
        forAll(
            row(Tiltakskode.ARBEIDSFORBEREDENDE_TRENING),
            row(Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING),
            row(Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING),
            row(Tiltakskode.ENKELTPLASS_FAG_OG_YRKESOPPLAERING),
            row(Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING),
            row(Tiltakskode.HOYERE_UTDANNING),
            row(Tiltakskode.EKSPERTBISTAND),
        ) { tiltakskode ->
            forAll(
                row(GjennomforingOppstartstype.FELLES),
                row(GjennomforingOppstartstype.LOPENDE),
                row(GjennomforingOppstartstype.ENKELTPLASS),
            ) { oppstartstype ->
                utledArtskontokode(tiltakskode, oppstartstype).shouldBeNull()
            }
        }
    }
})
