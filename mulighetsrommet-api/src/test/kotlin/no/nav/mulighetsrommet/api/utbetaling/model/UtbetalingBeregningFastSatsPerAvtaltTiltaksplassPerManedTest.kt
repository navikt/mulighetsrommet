package no.nav.mulighetsrommet.api.utbetaling.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.utbetaling.model.BeregningTestHelpers.createGjennomforingForForhandsgodkjentSatsPerAvtaltTiltaksplass
import no.nav.mulighetsrommet.api.utbetaling.model.BeregningTestHelpers.createTilsagn
import no.nav.mulighetsrommet.model.NOK
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.model.ValutaBelop
import java.time.LocalDate

class UtbetalingBeregningFastSatsPerAvtaltTiltaksplassPerManedTest : FunSpec({
    val januar = Periode.forMonthOf(LocalDate.of(2025, 1, 1))
    val februar = Periode.forMonthOf(LocalDate.of(2025, 2, 1))
    val mars = Periode.forMonthOf(LocalDate.of(2025, 3, 1))

    test("utbetaling uten tilsagn gir beløp 0") {
        val gjennomforing = createGjennomforingForForhandsgodkjentSatsPerAvtaltTiltaksplass(periode = januar)

        val result = FastSatsPerAvtaltTiltaksplassPerManedBeregning.beregn(gjennomforing, januar, emptyList())

        result.output.pris shouldBe 0.NOK
        result.input.tilsagn.shouldBeEmpty()
    }

    test("tilsagn som ikke overlapper med utbetalingsperioden bidrar ikke til utbetalingen") {
        val gjennomforing = createGjennomforingForForhandsgodkjentSatsPerAvtaltTiltaksplass(periode = januar)
        val tilsagn = createTilsagn(gjennomforing, februar, 5_000.NOK)

        val result = FastSatsPerAvtaltTiltaksplassPerManedBeregning.beregn(gjennomforing, januar, listOf(tilsagn))

        result.output.pris shouldBe 0.NOK
        result.input.tilsagn.shouldBeEmpty()
    }

    test("tilsagn med annen valuta enn gjennomføringens valuta ekskluderes fra beregningen") {
        val gjennomforing = createGjennomforingForForhandsgodkjentSatsPerAvtaltTiltaksplass(periode = januar)
        val tilsagnNok = createTilsagn(gjennomforing, januar, 5_000.NOK)
        val tilsagnSek = createTilsagn(gjennomforing, januar, ValutaBelop(3_000, Valuta.SEK))

        val result = FastSatsPerAvtaltTiltaksplassPerManedBeregning.beregn(
            gjennomforing,
            januar,
            listOf(tilsagnNok, tilsagnSek),
        )

        result.output.pris shouldBe 5_000.NOK
        result.output.tilsagnBidrag.shouldHaveSize(1)
        result.output.tilsagnBidrag[0].tilsagnId shouldBe tilsagnNok.id
    }

    test("tilsagn som dekker hele utbetalingsperioden bidrar med fullt beløp") {
        val gjennomforing = createGjennomforingForForhandsgodkjentSatsPerAvtaltTiltaksplass(periode = januar)
        val tilsagn = createTilsagn(gjennomforing, januar, 10_000.NOK)

        val result = FastSatsPerAvtaltTiltaksplassPerManedBeregning.beregn(gjennomforing, januar, listOf(tilsagn))

        result.output.pris shouldBe 10_000.NOK
        result.output.tilsagnBidrag.shouldHaveSize(1)
        result.output.tilsagnBidrag[0].bidrag shouldBe 10_000.NOK
    }

    test("beregnet beløp er summen av bidrag fra alle tilsagn som overlapper perioden") {
        val gjennomforing = createGjennomforingForForhandsgodkjentSatsPerAvtaltTiltaksplass(periode = januar)
        val t1 = createTilsagn(gjennomforing, januar, 5_000.NOK)
        val t2 = createTilsagn(gjennomforing, januar, 3_000.NOK)

        val result = FastSatsPerAvtaltTiltaksplassPerManedBeregning.beregn(gjennomforing, januar, listOf(t1, t2))

        result.output.pris shouldBe 8_000.NOK
        result.output.tilsagnBidrag.shouldHaveSize(2)
        result.output.tilsagnBidrag[0].bidrag shouldBe 5_000.NOK
        result.output.tilsagnBidrag[1].bidrag shouldBe 3_000.NOK
    }

    test("tilsagn over flere måneder fordeles proporsjonalt, med avrundingsrest i første måned") {
        val gjennomforing = createGjennomforingForForhandsgodkjentSatsPerAvtaltTiltaksplass(periode = januar)
        val q1 = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 4, 1))
        val tilsagn = createTilsagn(gjennomforing, q1, 10_000.NOK)

        // 1 måned (restbeløpet legges til i første periode)
        val r1 = FastSatsPerAvtaltTiltaksplassPerManedBeregning.beregn(gjennomforing, januar, listOf(tilsagn))
        r1.output.pris.belop shouldBe 3_334

        // 1 måned
        val r2 = FastSatsPerAvtaltTiltaksplassPerManedBeregning.beregn(gjennomforing, februar, listOf(tilsagn))
        r2.output.pris.belop shouldBe 3_333

        // 1 måned
        val r3 = FastSatsPerAvtaltTiltaksplassPerManedBeregning.beregn(gjennomforing, mars, listOf(tilsagn))
        r3.output.pris.belop shouldBe 3_333
    }

    test("avrundingsrest tilfaller siste delmåned når den har størst brøkdel av tilsagnets måneder") {
        val gjennomforing = createGjennomforingForForhandsgodkjentSatsPerAvtaltTiltaksplass(periode = januar)
        // Tilsagn for 30/31 dager i januar og 14/28 dager i februar
        val periode = Periode(LocalDate.of(2025, 1, 2), LocalDate.of(2025, 2, 15))
        // En månedspris på 1000 NOK per plass og 2 plasser tilsvarer totalt 2935 (30/31*2*1000 + 14/28*2*1000)
        val belop = UtbetalingBeregningHelpers.calculateManedsverkBelop(periode, 1000.NOK, 2) shouldBe 2935.NOK
        val tilsagn = createTilsagn(gjennomforing, periode, belop)

        // 2935 (beløp) * 0.967.. (brøk i januar) / 1.467.. (totalt antall måneder i tilsagnet) = 1935.164.. -> 1935
        val r1 = FastSatsPerAvtaltTiltaksplassPerManedBeregning.beregn(gjennomforing, januar, listOf(tilsagn))
        r1.output.pris.belop shouldBe 1935

        // 2935 (beløp) * 0.5 (brøk i februar) / 1.467.. (totalt antall måneder i tilsagnet) = 999.835.. -> 1000 (inkludert restbeløp)
        val r2 = FastSatsPerAvtaltTiltaksplassPerManedBeregning.beregn(gjennomforing, februar, listOf(tilsagn))
        r2.output.pris.belop shouldBe 1000
    }

    test("avrundingsrest tilfaller første delmåned når den har størst brøkdel av tilsagnets måneder") {
        val gjennomforing = createGjennomforingForForhandsgodkjentSatsPerAvtaltTiltaksplass(periode = januar)
        val periode = Periode(LocalDate.of(2025, 1, 3), LocalDate.of(2025, 2, 15))
        val belop = UtbetalingBeregningHelpers.calculateManedsverkBelop(periode, 999.NOK, 3) shouldBe 4302.NOK
        val tilsagn = createTilsagn(gjennomforing, periode, belop)

        // 4302 (beløp) * 0.935.. (brøk i januar) / 1.435.. (totalt antall måneder i tilsagnet) = 2803.645.. -> 2804 (inkludert restbeløp)
        val r1 = FastSatsPerAvtaltTiltaksplassPerManedBeregning.beregn(gjennomforing, januar, listOf(tilsagn))
        r1.output.pris.belop shouldBe 2804

        // 4302 (beløp) * 0.5 (brøk i februar) / 1.435.. (totalt antall måneder i tilsagnet) = 1498.449.. -> 1498
        val r2 = FastSatsPerAvtaltTiltaksplassPerManedBeregning.beregn(gjennomforing, februar, listOf(tilsagn))
        r2.output.pris.belop shouldBe 1498
    }

    test("bidrag fra tilsagn begrenses til gjenværende beløp når noe allerede er brukt") {
        val gjennomforing = createGjennomforingForForhandsgodkjentSatsPerAvtaltTiltaksplass(periode = januar)
        val tilsagn = createTilsagn(gjennomforing, januar, 5_000.NOK).copy(belopBrukt = 1000.NOK)

        val result = FastSatsPerAvtaltTiltaksplassPerManedBeregning.beregn(gjennomforing, januar, listOf(tilsagn))

        result.output.pris shouldBe 4_000.NOK
    }

    test("gjenværende beløp på tilsagn begrenser bidraget når beregnet andel overstiger tilgjengelig beløp") {
        val gjennomforing = createGjennomforingForForhandsgodkjentSatsPerAvtaltTiltaksplass(periode = januar)
        val q1 = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 4, 1))
        val tilsagn = createTilsagn(gjennomforing, q1, 10_000.NOK)

        val r1 = FastSatsPerAvtaltTiltaksplassPerManedBeregning.beregn(
            gjennomforing,
            januar,
            listOf(tilsagn.copy(belopBrukt = 0.NOK)),
        )
        r1.output.pris shouldBe 3334.NOK

        val r2 = FastSatsPerAvtaltTiltaksplassPerManedBeregning.beregn(
            gjennomforing,
            februar,
            listOf(tilsagn.copy(belopBrukt = 3334.NOK)),
        )
        r2.output.pris shouldBe 3333.NOK

        val r3 = FastSatsPerAvtaltTiltaksplassPerManedBeregning.beregn(
            gjennomforing,
            mars,
            listOf(tilsagn.copy(belopBrukt = 7000.NOK)),
        )
        r3.output.pris shouldBe 3000.NOK
    }

    test("tar ikke et større bidrag fra tilsagnet enn det som er tilgjengelig ved små tilsagn") {
        val gjennomforing = createGjennomforingForForhandsgodkjentSatsPerAvtaltTiltaksplass(periode = januar)
        val q1 = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 4, 1))
        val tilsagn = createTilsagn(gjennomforing, q1, 2.NOK)

        val r1 = FastSatsPerAvtaltTiltaksplassPerManedBeregning.beregn(gjennomforing, januar, listOf(tilsagn))
        r1.output.pris shouldBe 1.NOK

        val r2 = FastSatsPerAvtaltTiltaksplassPerManedBeregning.beregn(gjennomforing, februar, listOf(tilsagn))
        r2.output.pris shouldBe 1.NOK

        val r3 = FastSatsPerAvtaltTiltaksplassPerManedBeregning.beregn(gjennomforing, mars, listOf(tilsagn))
        r3.output.pris shouldBe 0.NOK
    }
})
