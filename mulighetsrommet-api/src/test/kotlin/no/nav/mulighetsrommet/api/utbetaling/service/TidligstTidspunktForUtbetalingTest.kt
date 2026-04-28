package no.nav.mulighetsrommet.api.utbetaling.service

import io.kotest.core.spec.style.FunSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode
import java.time.LocalDate
import java.time.ZoneId

class TidligstTidspunktForUtbetalingTest : FunSpec({
    test("tidligste tidspunkt for utbetaling av Avklaring, Oppfølging og ARR er 30 dager etter den 7. i måneden etter utbetalingsperioden") {
        tidligstTidspunktForUtbetalingProd.calculate(
            Tiltakskode.OPPFOLGING,
            Periode.forMonthOf(LocalDate.of(2025, 10, 1)),
        ) shouldBe LocalDate.of(2025, 12, 7).atStartOfDay(ZoneId.of("Europe/Oslo")).toInstant()

        tidligstTidspunktForUtbetalingProd.calculate(
            Tiltakskode.OPPFOLGING,
            Periode.forMonthOf(LocalDate.of(2025, 11, 1)),
        ) shouldBe LocalDate.of(2026, 1, 6).atStartOfDay(ZoneId.of("Europe/Oslo")).toInstant()

        tidligstTidspunktForUtbetalingProd.calculate(
            Tiltakskode.OPPFOLGING,
            Periode.forMonthOf(LocalDate.of(2025, 12, 1)),
        ) shouldBe LocalDate.of(2026, 2, 6).atStartOfDay(ZoneId.of("Europe/Oslo")).toInstant()

        tidligstTidspunktForUtbetalingProd.calculate(
            Tiltakskode.AVKLARING,
            Periode.forMonthOf(LocalDate.of(2026, 1, 1)),
        ) shouldBe LocalDate.of(2026, 3, 9).atStartOfDay(ZoneId.of("Europe/Oslo")).toInstant()

        tidligstTidspunktForUtbetalingProd.calculate(
            Tiltakskode.ARBEIDSRETTET_REHABILITERING,
            Periode.forMonthOf(LocalDate.of(2026, 2, 1)),
        ) shouldBe LocalDate.of(2026, 4, 6).atStartOfDay(ZoneId.of("Europe/Oslo")).toInstant()

        tidligstTidspunktForUtbetalingProd.calculate(
            Tiltakskode.ARBEIDSRETTET_REHABILITERING,
            Periode(LocalDate.of(2025, 10, 1), LocalDate.of(2025, 11, 15)),
        ) shouldBe LocalDate.of(2025, 12, 21).atStartOfDay(ZoneId.of("Europe/Oslo")).toInstant()

        tidligstTidspunktForUtbetalingProd.calculate(
            Tiltakskode.DIGITALT_OPPFOLGINGSTILTAK,
            Periode.forMonthOf(LocalDate.of(2025, 11, 1)),
        ) shouldBe LocalDate.of(2026, 1, 6).atStartOfDay(ZoneId.of("Europe/Oslo")).toInstant()

        tidligstTidspunktForUtbetalingProd.calculate(
            Tiltakskode.DIGITALT_OPPFOLGINGSTILTAK,
            Periode(LocalDate.of(2025, 11, 1), LocalDate.of(2025, 12, 3)),
        ) shouldBe LocalDate.of(2026, 1, 8).atStartOfDay(ZoneId.of("Europe/Oslo")).toInstant()
    }

    test("tidligste tidspunkt for utbetaling av andre tiltak er med en gang") {
        val periode = Periode.forMonthOf(LocalDate.of(2025, 10, 1))
        forAll(
            row(Tiltakskode.ARBEIDSFORBEREDENDE_TRENING),
            row(Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING),
            row(Tiltakskode.ENKELTPLASS_FAG_OG_YRKESOPPLAERING),
            row(Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING),
            row(Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING),
            row(Tiltakskode.HOYERE_UTDANNING),
            row(Tiltakskode.JOBBKLUBB),
            row(Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET),
        ) { tiltakskode ->
            tidligstTidspunktForUtbetalingProd.calculate(tiltakskode, periode) shouldBe null
        }
    }
})
