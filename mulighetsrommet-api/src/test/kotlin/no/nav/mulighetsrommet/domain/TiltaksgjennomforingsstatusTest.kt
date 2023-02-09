package no.nav.mulighetsrommet.domain

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.domain.dto.Avslutningsstatus
import no.nav.mulighetsrommet.domain.dto.Tiltaksgjennomforingsstatus
import java.time.LocalDate

class TiltaksgjennomforingsstatusTest : FunSpec({
    val enManedFrem = LocalDate.now().plusMonths(1)
    val enManedTilbake = LocalDate.now().minusMonths(1)
    val toManederFrem = LocalDate.now().plusMonths(2)
    val toManederTiltabke = LocalDate.now().minusMonths(2)

    test("avlyst skal forbli avlyst uavhengig av datoer") {
        Tiltaksgjennomforingsstatus.fromDbo(enManedTilbake, enManedFrem, Avslutningsstatus.AVLYST) shouldBe Tiltaksgjennomforingsstatus.AVLYST
        Tiltaksgjennomforingsstatus.fromDbo(toManederTiltabke, enManedTilbake, Avslutningsstatus.AVLYST) shouldBe Tiltaksgjennomforingsstatus.AVLYST
        Tiltaksgjennomforingsstatus.fromDbo(enManedFrem, toManederFrem, Avslutningsstatus.AVLYST) shouldBe Tiltaksgjennomforingsstatus.AVLYST
     }

    test("avbrutt skal forbli avbrutt uavhengig av datoer") {
        Tiltaksgjennomforingsstatus.fromDbo(enManedTilbake, enManedFrem, Avslutningsstatus.AVBRUTT) shouldBe Tiltaksgjennomforingsstatus.AVBRUTT
        Tiltaksgjennomforingsstatus.fromDbo(toManederTiltabke, enManedTilbake, Avslutningsstatus.AVBRUTT) shouldBe Tiltaksgjennomforingsstatus.AVBRUTT
        Tiltaksgjennomforingsstatus.fromDbo(enManedFrem, toManederFrem, Avslutningsstatus.AVBRUTT) shouldBe Tiltaksgjennomforingsstatus.AVBRUTT
    }

    test("avsluttet skal forbli avbrutt uavhengig av datoer") {
        Tiltaksgjennomforingsstatus.fromDbo(enManedTilbake, enManedFrem, Avslutningsstatus.AVSLUTTET) shouldBe Tiltaksgjennomforingsstatus.AVSLUTTET
        Tiltaksgjennomforingsstatus.fromDbo(toManederTiltabke, enManedTilbake, Avslutningsstatus.AVSLUTTET) shouldBe Tiltaksgjennomforingsstatus.AVSLUTTET
        Tiltaksgjennomforingsstatus.fromDbo(enManedFrem, toManederFrem, Avslutningsstatus.AVSLUTTET) shouldBe Tiltaksgjennomforingsstatus.AVSLUTTET
    }

    test("bruk datoer hvis status er ikke avsluttet") {
        Tiltaksgjennomforingsstatus.fromDbo(enManedTilbake, enManedFrem, Avslutningsstatus.IKKE_AVSLUTTET) shouldBe Tiltaksgjennomforingsstatus.GJENNOMFORES
        Tiltaksgjennomforingsstatus.fromDbo(toManederTiltabke, enManedTilbake, Avslutningsstatus.IKKE_AVSLUTTET) shouldBe Tiltaksgjennomforingsstatus.AVSLUTTET
        Tiltaksgjennomforingsstatus.fromDbo(enManedFrem, toManederFrem, Avslutningsstatus.IKKE_AVSLUTTET) shouldBe Tiltaksgjennomforingsstatus.APENT_FOR_INNSOK
    }

    test("hvis sluttdato mangler så regnes den som pågående") {
        Tiltaksgjennomforingsstatus.fromDbo(enManedTilbake, null, Avslutningsstatus.IKKE_AVSLUTTET) shouldBe Tiltaksgjennomforingsstatus.GJENNOMFORES
        Tiltaksgjennomforingsstatus.fromDbo(enManedFrem, null, Avslutningsstatus.IKKE_AVSLUTTET) shouldBe Tiltaksgjennomforingsstatus.APENT_FOR_INNSOK
    }
})
