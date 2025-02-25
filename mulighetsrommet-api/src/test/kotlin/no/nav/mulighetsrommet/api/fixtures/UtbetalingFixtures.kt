package no.nav.mulighetsrommet.api.fixtures

import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.utbetaling.db.UtbetalingDbo
import no.nav.mulighetsrommet.api.utbetaling.model.DelutbetalingDto
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFri
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.Periode
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

object UtbetalingFixtures {
    val utbetaling1 = UtbetalingDbo(
        id = UUID.randomUUID(),
        gjennomforingId = AFT1.id,
        fristForGodkjenning = LocalDate.of(2024, 10, 1).atStartOfDay(),
        periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
        beregning = UtbetalingBeregningFri(
            input = UtbetalingBeregningFri.Input(1000),
            output = UtbetalingBeregningFri.Output(1000),
        ),
        kontonummer = Kontonummer("11111111111"),
        kid = null,
        innsender = null,
    )

    val utbetaling2 = UtbetalingDbo(
        id = UUID.randomUUID(),
        gjennomforingId = AFT1.id,
        fristForGodkjenning = LocalDate.of(2024, 10, 1).atStartOfDay(),
        periode = Periode.forMonthOf(LocalDate.of(2024, 2, 1)),
        beregning = UtbetalingBeregningFri(
            input = UtbetalingBeregningFri.Input(500),
            output = UtbetalingBeregningFri.Output(500),
        ),
        kontonummer = Kontonummer("11111111111"),
        kid = null,
        innsender = null,
    )

    val utbetaling3 = UtbetalingDbo(
        id = UUID.randomUUID(),
        gjennomforingId = GjennomforingFixtures.VTA1.id,
        fristForGodkjenning = LocalDate.of(2024, 10, 1).atStartOfDay(),
        periode = Periode.forMonthOf(LocalDate.of(2024, 2, 1)),
        beregning = UtbetalingBeregningFri(
            input = UtbetalingBeregningFri.Input(500),
            output = UtbetalingBeregningFri.Output(500),
        ),
        kontonummer = Kontonummer("11111111111"),
        kid = null,
        innsender = null,
    )

    val delutbetaling1 = DelutbetalingDto.DelutbetalingTilGodkjenning(
        id = UUID.randomUUID(),
        tilsagnId = TilsagnFixtures.Tilsagn1.id,
        utbetalingId = utbetaling1.id,
        belop = 200,
        periode = utbetaling1.periode,
        lopenummer = 1,
        fakturanummer = "${TilsagnFixtures.Tilsagn1.bestillingsnummer}/1",
        behandletAv = NavAnsattFixture.ansatt1.navIdent,
        behandletTidspunkt = LocalDateTime.of(2025, 1, 1, 0, 0, 0),
    )

    val delutbetaling2 = DelutbetalingDto.DelutbetalingTilGodkjenning(
        id = UUID.randomUUID(),
        tilsagnId = TilsagnFixtures.Tilsagn2.id,
        utbetalingId = utbetaling1.id,
        belop = 150,
        periode = utbetaling1.periode,
        lopenummer = 1,
        fakturanummer = "${TilsagnFixtures.Tilsagn2.bestillingsnummer}/1",
        behandletAv = NavAnsattFixture.ansatt1.navIdent,
        behandletTidspunkt = LocalDateTime.of(2025, 1, 1, 0, 0, 0),
    )

    enum class DelutbetalingStatus {
        DELUTBETALING_TIL_GODKJENNING,
        DELUTBETALING_OVERFORT_TIL_UTBETALING,
        DELUTBETALING_UTBETALT,
        DELUTBETALING_AVVIST,
    }

    fun DelutbetalingDto.medStatus(
        status: DelutbetalingStatus,
        besluttetTidspunkt: LocalDateTime = LocalDateTime.now(),
    ): DelutbetalingDto {
        return when (status) {
            DelutbetalingStatus.DELUTBETALING_TIL_GODKJENNING -> DelutbetalingDto.DelutbetalingTilGodkjenning(
                id,
                tilsagnId,
                utbetalingId,
                belop,
                periode,
                lopenummer,
                fakturanummer,
                behandletAv,
                behandletTidspunkt,
            )
            DelutbetalingStatus.DELUTBETALING_OVERFORT_TIL_UTBETALING -> DelutbetalingDto.DelutbetalingOverfortTilUtbetaling(
                id,
                tilsagnId,
                utbetalingId,
                belop,
                periode,
                lopenummer,
                fakturanummer,
                behandletAv,
                behandletTidspunkt,
                NavAnsattFixture.ansatt2.navIdent,
                besluttetTidspunkt,
            )
            DelutbetalingStatus.DELUTBETALING_UTBETALT -> DelutbetalingDto.DelutbetalingUtbetalt(
                id,
                tilsagnId,
                utbetalingId,
                belop,
                periode,
                lopenummer,
                fakturanummer,
                behandletAv,
                behandletTidspunkt,
                NavAnsattFixture.ansatt2.navIdent,
                besluttetTidspunkt,
            )
            DelutbetalingStatus.DELUTBETALING_AVVIST -> DelutbetalingDto.DelutbetalingAvvist(
                id,
                tilsagnId,
                utbetalingId,
                belop,
                periode,
                lopenummer,
                fakturanummer,
                behandletAv,
                behandletTidspunkt,
                NavAnsattFixture.ansatt2.navIdent,
                besluttetTidspunkt,
                aarsaker = listOf("FEIL_BELOP"),
                forklaring = null,
            )
        }
    }
}
