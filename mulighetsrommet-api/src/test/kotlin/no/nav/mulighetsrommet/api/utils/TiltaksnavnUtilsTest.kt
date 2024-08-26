package no.nav.mulighetsrommet.api.utils

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.domain.dto.TiltakstypeAdminDto
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.domain.dto.TiltakstypeStatus
import java.time.LocalDate
import java.util.*

class TiltaksnavnUtilsTest : FunSpec({
    context("Casing av konstruerte navn") {
        test("Skal konstruere navn med stor forbokstav i tiltaksnavn og arrangør") {
            val tiltakstype = TiltakstypeAdminDto(
                id = UUID.randomUUID(),
                navn = "Oppfølging",
                tiltakskode = Tiltakskode.OPPFOLGING,
                innsatsgrupper = emptySet(),
                arenaKode = "OPPF",
                startDato = LocalDate.of(2024, 8, 26),
                sluttDato = null,
                status = TiltakstypeStatus.AKTIV,
                sanityId = null,
            )
            val arrangor = "joblearn as"
            val konstruertNavn = TiltaksnavnUtils.tilKonstruertNavn(tiltakstype, arrangor)
            konstruertNavn shouldBe "Oppfølging hos Joblearn AS"
        }
    }
})
