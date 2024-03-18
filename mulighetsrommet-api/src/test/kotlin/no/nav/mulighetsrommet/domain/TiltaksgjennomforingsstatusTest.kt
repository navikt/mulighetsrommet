package no.nav.mulighetsrommet.domain

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.domain.dbo.Avslutningsstatus
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingStatus
import java.time.LocalDate

class TiltaksgjennomforingsstatusTest : FunSpec({
    val dagensDato = LocalDate.of(2023, 1, 1)
    val enManedFrem = dagensDato.plusMonths(1)
    val enManedTilbake = dagensDato.minusMonths(1)
    val toManederFrem = dagensDato.plusMonths(2)
    val toManederTiltabke = dagensDato.minusMonths(2)

    test("avlyst skal forbli avlyst uavhengig av datoer") {
        TiltaksgjennomforingStatus.fromDbo(dagensDato, enManedTilbake, enManedFrem, Avslutningsstatus.AVLYST) shouldBe TiltaksgjennomforingStatus.AVLYST
        TiltaksgjennomforingStatus.fromDbo(dagensDato, toManederTiltabke, enManedTilbake, Avslutningsstatus.AVLYST) shouldBe TiltaksgjennomforingStatus.AVLYST
        TiltaksgjennomforingStatus.fromDbo(dagensDato, enManedFrem, toManederFrem, Avslutningsstatus.AVLYST) shouldBe TiltaksgjennomforingStatus.AVLYST
    }

    test("avbrutt skal forbli avbrutt uavhengig av datoer") {
        TiltaksgjennomforingStatus.fromDbo(dagensDato, enManedTilbake, enManedFrem, Avslutningsstatus.AVBRUTT) shouldBe TiltaksgjennomforingStatus.AVBRUTT
        TiltaksgjennomforingStatus.fromDbo(dagensDato, toManederTiltabke, enManedTilbake, Avslutningsstatus.AVBRUTT) shouldBe TiltaksgjennomforingStatus.AVBRUTT
        TiltaksgjennomforingStatus.fromDbo(dagensDato, enManedFrem, toManederFrem, Avslutningsstatus.AVBRUTT) shouldBe TiltaksgjennomforingStatus.AVBRUTT
    }

    test("avsluttet skal forbli avbrutt uavhengig av datoer") {
        TiltaksgjennomforingStatus.fromDbo(dagensDato, enManedTilbake, enManedFrem, Avslutningsstatus.AVSLUTTET) shouldBe TiltaksgjennomforingStatus.AVSLUTTET
        TiltaksgjennomforingStatus.fromDbo(dagensDato, toManederTiltabke, enManedTilbake, Avslutningsstatus.AVSLUTTET) shouldBe TiltaksgjennomforingStatus.AVSLUTTET
        TiltaksgjennomforingStatus.fromDbo(dagensDato, enManedFrem, toManederFrem, Avslutningsstatus.AVSLUTTET) shouldBe TiltaksgjennomforingStatus.AVSLUTTET
    }

    test("bruk datoer hvis status er ikke avsluttet") {
        TiltaksgjennomforingStatus.fromDbo(dagensDato, enManedTilbake, enManedFrem, Avslutningsstatus.IKKE_AVSLUTTET) shouldBe TiltaksgjennomforingStatus.GJENNOMFORES
        TiltaksgjennomforingStatus.fromDbo(dagensDato, toManederTiltabke, enManedTilbake, Avslutningsstatus.IKKE_AVSLUTTET) shouldBe TiltaksgjennomforingStatus.AVSLUTTET
        TiltaksgjennomforingStatus.fromDbo(dagensDato, enManedFrem, toManederFrem, Avslutningsstatus.IKKE_AVSLUTTET) shouldBe TiltaksgjennomforingStatus.PLANLAGT
    }

    test("hvis sluttdato mangler så regnes den som pågående") {
        TiltaksgjennomforingStatus.fromDbo(dagensDato, enManedTilbake, null, Avslutningsstatus.IKKE_AVSLUTTET) shouldBe TiltaksgjennomforingStatus.GJENNOMFORES
        TiltaksgjennomforingStatus.fromDbo(dagensDato, enManedFrem, null, Avslutningsstatus.IKKE_AVSLUTTET) shouldBe TiltaksgjennomforingStatus.PLANLAGT
    }
})
