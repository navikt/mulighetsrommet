package no.nav.mulighetsrommet.api.fixtures

import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.tilsagn.model.Besluttelse
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
import no.nav.mulighetsrommet.api.utbetaling.db.DelutbetalingDbo
import no.nav.mulighetsrommet.api.utbetaling.db.UtbetalingDbo
import no.nav.mulighetsrommet.api.utbetaling.model.DelutbetalingStatus
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFri
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.NavIdent
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
        beskrivelse = null,
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
        beskrivelse = null,
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
        beskrivelse = null,
    )

    val delutbetaling1 = DelutbetalingDbo(
        id = UUID.randomUUID(),
        tilsagnId = TilsagnFixtures.Tilsagn1.id,
        utbetalingId = utbetaling1.id,
        status = DelutbetalingStatus.TIL_GODKJENNING,
        belop = 200,
        frigjorTilsagn = false,
        periode = utbetaling1.periode,
        lopenummer = 1,
        fakturanummer = "${TilsagnFixtures.Tilsagn1.bestillingsnummer}/1",
    )

    val delutbetaling2 = DelutbetalingDbo(
        id = UUID.randomUUID(),
        tilsagnId = TilsagnFixtures.Tilsagn2.id,
        utbetalingId = utbetaling1.id,
        status = DelutbetalingStatus.TIL_GODKJENNING,
        belop = 150,
        frigjorTilsagn = false,
        periode = utbetaling1.periode,
        lopenummer = 1,
        fakturanummer = "${TilsagnFixtures.Tilsagn2.bestillingsnummer}/1",
    )

    fun QueryContext.setDelutbetalingStatus(
        delutbetalingDbo: DelutbetalingDbo,
        status: DelutbetalingStatus,
        behandletAv: NavIdent = NavAnsattFixture.ansatt1.navIdent,
        besluttetAv: NavIdent = NavAnsattFixture.ansatt2.navIdent,
        besluttetTidspunkt: LocalDateTime = LocalDateTime.now(),
    ) {
        val dto = queries.delutbetaling.get(delutbetalingDbo.id)
            ?: throw IllegalStateException("Dbo må være gitt til domain først")

        queries.delutbetaling.setStatus(dto.id, status)

        when (status) {
            DelutbetalingStatus.TIL_GODKJENNING -> {
                queries.totrinnskontroll.upsert(
                    tilGodkjenning(dto.id, Totrinnskontroll.Type.OPPRETT, behandletAv),
                )
            }

            DelutbetalingStatus.GODKJENT -> {
                queries.totrinnskontroll.upsert(
                    godkjent(
                        dto.id,
                        Totrinnskontroll.Type.OPPRETT,
                        behandletAv,
                        besluttetAv,
                        besluttetTidspunkt = besluttetTidspunkt,
                    ),
                )
            }

            DelutbetalingStatus.RETURNERT -> {
                queries.totrinnskontroll.upsert(
                    avvist(
                        dto.id,
                        Totrinnskontroll.Type.OPPRETT,
                        behandletAv,
                        besluttetAv,
                        besluttetTidspunkt = besluttetTidspunkt,
                    ),
                )
            }

            else -> {}
        }
    }
}

private fun tilGodkjenning(
    uuid: UUID,
    type: Totrinnskontroll.Type,
    behandletAv: NavIdent,
    behandletTidspunkt: LocalDateTime = LocalDateTime.now(),
) = Totrinnskontroll(
    id = UUID.randomUUID(),
    entityId = uuid,
    behandletAv = behandletAv,
    aarsaker = emptyList(),
    forklaring = null,
    type = type,
    behandletTidspunkt = behandletTidspunkt,
    besluttelse = null,
    besluttetAv = null,
    besluttetTidspunkt = null,
)

private fun godkjent(
    uuid: UUID,
    type: Totrinnskontroll.Type,
    behandletAv: NavIdent,
    besluttetAv: NavIdent,
    behandletTidspunkt: LocalDateTime = LocalDateTime.now(),
    besluttetTidspunkt: LocalDateTime = LocalDateTime.now(),
) = Totrinnskontroll(
    id = UUID.randomUUID(),
    entityId = uuid,
    behandletAv = behandletAv,
    aarsaker = emptyList(),
    forklaring = null,
    type = type,
    behandletTidspunkt = behandletTidspunkt,
    besluttelse = Besluttelse.GODKJENT,
    besluttetAv = besluttetAv,
    besluttetTidspunkt = besluttetTidspunkt,
)

private fun avvist(
    uuid: UUID,
    type: Totrinnskontroll.Type,
    behandletAv: NavIdent,
    besluttetAv: NavIdent,
    behandletTidspunkt: LocalDateTime = LocalDateTime.now(),
    besluttetTidspunkt: LocalDateTime = LocalDateTime.now(),
) = Totrinnskontroll(
    id = UUID.randomUUID(),
    entityId = uuid,
    behandletAv = behandletAv,
    aarsaker = listOf("Årsak 1"),
    forklaring = null,
    type = type,
    behandletTidspunkt = behandletTidspunkt,
    besluttelse = Besluttelse.AVVIST,
    besluttetAv = besluttetAv,
    besluttetTidspunkt = besluttetTidspunkt,
)
