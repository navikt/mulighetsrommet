package no.nav.mulighetsrommet.domain

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.domain.dbo.Avslutningsstatus
import no.nav.mulighetsrommet.domain.dto.Tiltaksgjennomforingsstatus
import java.time.LocalDate

class TiltaksgjennomforingsstatusTest : FunSpec({
    val dagensDato = LocalDate.of(2023, 1, 1)
    val enManedFrem = dagensDato.plusMonths(1)
    val enManedTilbake = dagensDato.minusMonths(1)
    val toManederFrem = dagensDato.plusMonths(2)
    val toManederTiltabke = dagensDato.minusMonths(2)

    test("avlyst skal forbli avlyst uavhengig av datoer") {
        Tiltaksgjennomforingsstatus.fromDbo(dagensDato, enManedTilbake, enManedFrem, Avslutningsstatus.AVLYST) shouldBe Tiltaksgjennomforingsstatus.AVLYST
        Tiltaksgjennomforingsstatus.fromDbo(dagensDato, toManederTiltabke, enManedTilbake, Avslutningsstatus.AVLYST) shouldBe Tiltaksgjennomforingsstatus.AVLYST
        Tiltaksgjennomforingsstatus.fromDbo(dagensDato, enManedFrem, toManederFrem, Avslutningsstatus.AVLYST) shouldBe Tiltaksgjennomforingsstatus.AVLYST
    }

    test("avbrutt skal forbli avbrutt uavhengig av datoer") {
        Tiltaksgjennomforingsstatus.fromDbo(dagensDato, enManedTilbake, enManedFrem, Avslutningsstatus.AVBRUTT) shouldBe Tiltaksgjennomforingsstatus.AVBRUTT
        Tiltaksgjennomforingsstatus.fromDbo(dagensDato, toManederTiltabke, enManedTilbake, Avslutningsstatus.AVBRUTT) shouldBe Tiltaksgjennomforingsstatus.AVBRUTT
        Tiltaksgjennomforingsstatus.fromDbo(dagensDato, enManedFrem, toManederFrem, Avslutningsstatus.AVBRUTT) shouldBe Tiltaksgjennomforingsstatus.AVBRUTT
    }

    test("avsluttet skal forbli avbrutt uavhengig av datoer") {
        Tiltaksgjennomforingsstatus.fromDbo(dagensDato, enManedTilbake, enManedFrem, Avslutningsstatus.AVSLUTTET) shouldBe Tiltaksgjennomforingsstatus.AVSLUTTET
        Tiltaksgjennomforingsstatus.fromDbo(dagensDato, toManederTiltabke, enManedTilbake, Avslutningsstatus.AVSLUTTET) shouldBe Tiltaksgjennomforingsstatus.AVSLUTTET
        Tiltaksgjennomforingsstatus.fromDbo(dagensDato, enManedFrem, toManederFrem, Avslutningsstatus.AVSLUTTET) shouldBe Tiltaksgjennomforingsstatus.AVSLUTTET
    }

    test("bruk datoer hvis status er ikke avsluttet") {
        Tiltaksgjennomforingsstatus.fromDbo(dagensDato, enManedTilbake, enManedFrem, Avslutningsstatus.IKKE_AVSLUTTET) shouldBe Tiltaksgjennomforingsstatus.GJENNOMFORES
        Tiltaksgjennomforingsstatus.fromDbo(dagensDato, toManederTiltabke, enManedTilbake, Avslutningsstatus.IKKE_AVSLUTTET) shouldBe Tiltaksgjennomforingsstatus.AVSLUTTET
        Tiltaksgjennomforingsstatus.fromDbo(dagensDato, enManedFrem, toManederFrem, Avslutningsstatus.IKKE_AVSLUTTET) shouldBe Tiltaksgjennomforingsstatus.PLANLAGT
    }

    test("hvis sluttdato mangler så regnes den som pågående") {
        Tiltaksgjennomforingsstatus.fromDbo(dagensDato, enManedTilbake, null, Avslutningsstatus.IKKE_AVSLUTTET) shouldBe Tiltaksgjennomforingsstatus.GJENNOMFORES
        Tiltaksgjennomforingsstatus.fromDbo(dagensDato, enManedFrem, null, Avslutningsstatus.IKKE_AVSLUTTET) shouldBe Tiltaksgjennomforingsstatus.PLANLAGT
    }
})
