package no.nav.mulighetsrommet.api.fixtures

import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
import no.nav.mulighetsrommet.api.utbetaling.db.DelutbetalingDbo
import no.nav.mulighetsrommet.api.utbetaling.db.UtbetalingDbo
import no.nav.mulighetsrommet.api.utbetaling.model.DelutbetalingStatus
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFri
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Periode
import no.nav.tiltak.okonomi.Tilskuddstype
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

object UtbetalingFixtures {
    val utbetaling1 = UtbetalingDbo(
        id = UUID.randomUUID(),
        gjennomforingId = AFT1.id,
        periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
        beregning = UtbetalingBeregningFri(
            input = UtbetalingBeregningFri.Input(1000),
            output = UtbetalingBeregningFri.Output(1000),
        ),
        kontonummer = Kontonummer("11111111111"),
        kid = null,
        innsender = null,
        beskrivelse = null,
        tilskuddstype = Tilskuddstype.TILTAK_DRIFTSTILSKUDD,
        godkjentAvArrangorTidspunkt = null,
    )

    val utbetaling2 = UtbetalingDbo(
        id = UUID.randomUUID(),
        gjennomforingId = AFT1.id,
        periode = Periode.forMonthOf(LocalDate.of(2024, 2, 1)),
        beregning = UtbetalingBeregningFri(
            input = UtbetalingBeregningFri.Input(500),
            output = UtbetalingBeregningFri.Output(500),
        ),
        kontonummer = Kontonummer("11111111111"),
        kid = null,
        innsender = null,
        beskrivelse = null,
        tilskuddstype = Tilskuddstype.TILTAK_DRIFTSTILSKUDD,
        godkjentAvArrangorTidspunkt = null,
    )

    val utbetaling3 = UtbetalingDbo(
        id = UUID.randomUUID(),
        gjennomforingId = GjennomforingFixtures.VTA1.id,
        periode = Periode.forMonthOf(LocalDate.of(2024, 2, 1)),
        beregning = UtbetalingBeregningFri(
            input = UtbetalingBeregningFri.Input(500),
            output = UtbetalingBeregningFri.Output(500),
        ),
        kontonummer = Kontonummer("11111111111"),
        kid = null,
        innsender = null,
        beskrivelse = null,
        tilskuddstype = Tilskuddstype.TILTAK_DRIFTSTILSKUDD,
        godkjentAvArrangorTidspunkt = null,
    )

    val delutbetaling1 = DelutbetalingDbo(
        id = UUID.randomUUID(),
        tilsagnId = TilsagnFixtures.Tilsagn1.id,
        utbetalingId = utbetaling1.id,
        status = DelutbetalingStatus.TIL_ATTESTERING,
        fakturaStatusSistOppdatert = LocalDateTime.of(2025, 1, 1, 12, 0),
        belop = 200,
        gjorOppTilsagn = false,
        periode = utbetaling1.periode,
        lopenummer = 1,
        fakturanummer = "${TilsagnFixtures.Tilsagn1.bestillingsnummer}/1",
        fakturaStatus = null,
    )

    val delutbetaling2 = DelutbetalingDbo(
        id = UUID.randomUUID(),
        tilsagnId = TilsagnFixtures.Tilsagn2.id,
        utbetalingId = utbetaling1.id,
        status = DelutbetalingStatus.TIL_ATTESTERING,
        fakturaStatusSistOppdatert = LocalDateTime.of(2025, 1, 1, 12, 0),
        belop = 150,
        gjorOppTilsagn = false,
        periode = utbetaling1.periode,
        lopenummer = 1,
        fakturanummer = "${TilsagnFixtures.Tilsagn2.bestillingsnummer}/1",
        fakturaStatus = null,
    )
}

fun QueryContext.setDelutbetalingStatus(
    delutbetalingDbo: DelutbetalingDbo,
    status: DelutbetalingStatus,
    behandletAv: NavIdent = NavAnsattFixture.DonaldDuck.navIdent,
    besluttetAv: NavIdent = NavAnsattFixture.MikkeMus.navIdent,
    besluttetTidspunkt: LocalDateTime = LocalDateTime.now(),
) {
    val dto = queries.delutbetaling.get(delutbetalingDbo.id)
        ?: throw IllegalStateException("Dbo må være gitt til domain først")

    queries.delutbetaling.setStatus(dto.id, status)

    when (status) {
        DelutbetalingStatus.TIL_ATTESTERING, DelutbetalingStatus.BEHANDLES_AV_NAV -> {
            setTilGodkjenning(dto.id, Totrinnskontroll.Type.OPPRETT, behandletAv)
        }

        DelutbetalingStatus.GODKJENT, DelutbetalingStatus.UTBETALT, DelutbetalingStatus.OVERFORT_TIL_UTBETALING -> {
            setGodkjent(
                dto.id,
                Totrinnskontroll.Type.OPPRETT,
                behandletAv,
                besluttetAv,
                besluttetTidspunkt = besluttetTidspunkt,
            )
        }

        DelutbetalingStatus.RETURNERT -> {
            setAvvist(
                dto.id,
                Totrinnskontroll.Type.OPPRETT,
                behandletAv,
                besluttetAv,
                besluttetTidspunkt = besluttetTidspunkt,
            )
        }
    }
}
