package no.nav.mulighetsrommet.domain

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.domain.dto.Tiltaksgjennomforingsstatus
import java.time.LocalDate

class TiltaksgjennomforingsstatusTest : FunSpec({
    val dagensDato = LocalDate.of(2023, 1, 1)
    val enManedFrem = dagensDato.plusMonths(1)
    val enManedTilbake = dagensDato.minusMonths(1)
    val toManederFrem = dagensDato.plusMonths(2)
    val toManederTiltabke = dagensDato.minusMonths(2)

    test("avbrutt før start er avlyst") {
        Tiltaksgjennomforingsstatus.fromDbo(dagensDato, enManedTilbake, enManedFrem, enManedTilbake.atStartOfDay().minusDays(1)) shouldBe Tiltaksgjennomforingsstatus.AVLYST
        Tiltaksgjennomforingsstatus.fromDbo(dagensDato, toManederTiltabke, enManedTilbake, toManederTiltabke.atStartOfDay().minusYears(1)) shouldBe Tiltaksgjennomforingsstatus.AVLYST
        Tiltaksgjennomforingsstatus.fromDbo(dagensDato, enManedFrem, toManederFrem, enManedFrem.atStartOfDay().minusMonths(1)) shouldBe Tiltaksgjennomforingsstatus.AVLYST
    }

    test("avbrutt etter start er avbrutt") {
        Tiltaksgjennomforingsstatus.fromDbo(dagensDato, enManedTilbake, enManedFrem, enManedTilbake.atStartOfDay().plusDays(3)) shouldBe Tiltaksgjennomforingsstatus.AVBRUTT
        Tiltaksgjennomforingsstatus.fromDbo(dagensDato, toManederTiltabke, enManedTilbake, toManederFrem.atStartOfDay().plusYears(2)) shouldBe Tiltaksgjennomforingsstatus.AVBRUTT
        Tiltaksgjennomforingsstatus.fromDbo(dagensDato, enManedFrem, toManederFrem, enManedFrem.atStartOfDay().plusMonths(1)) shouldBe Tiltaksgjennomforingsstatus.AVBRUTT
    }

    test("avsluttet hvis ikke avbrutt og etter start") {
        Tiltaksgjennomforingsstatus.fromDbo(dagensDato, enManedTilbake, enManedFrem, null) shouldBe Tiltaksgjennomforingsstatus.GJENNOMFORES
        Tiltaksgjennomforingsstatus.fromDbo(dagensDato, toManederTiltabke, enManedTilbake, null) shouldBe Tiltaksgjennomforingsstatus.AVSLUTTET
        Tiltaksgjennomforingsstatus.fromDbo(dagensDato, enManedFrem, toManederFrem, null) shouldBe Tiltaksgjennomforingsstatus.PLANLAGT
    }

    test("hvis sluttdato mangler så regnes den som pågående") {
        Tiltaksgjennomforingsstatus.fromDbo(dagensDato, enManedTilbake, null, null) shouldBe Tiltaksgjennomforingsstatus.GJENNOMFORES
        Tiltaksgjennomforingsstatus.fromDbo(dagensDato, enManedFrem, null, null) shouldBe Tiltaksgjennomforingsstatus.PLANLAGT
    }
})
