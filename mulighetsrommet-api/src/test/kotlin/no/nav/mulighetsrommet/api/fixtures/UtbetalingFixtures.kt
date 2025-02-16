package no.nav.mulighetsrommet.api.fixtures

import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.utbetaling.db.UtbetalingDbo
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFri
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.Periode
import java.time.LocalDate
import java.util.*

object UtbetalingFixtures {
    val utbetaling1 = UtbetalingDbo(
        id = UUID.randomUUID(),
        gjennomforingId = AFT1.id,
        fristForGodkjenning = LocalDate.of(2024, 10, 1).atStartOfDay(),
        periode = Periode.forMonthOf(LocalDate.of(2024, 2, 1)),
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
}
