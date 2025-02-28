package no.nav.mulighetsrommet.api.fixtures

import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.tilsagn.db.TilsagnDbo
import no.nav.mulighetsrommet.api.tilsagn.model.Besluttelse
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningFri
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnType
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Periode
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

object TilsagnFixtures {
    val Tilsagn1 = TilsagnDbo(
        id = UUID.randomUUID(),
        gjennomforingId = GjennomforingFixtures.AFT1.id,
        periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
        lopenummer = 1,
        bestillingsnummer = "A-2025/1-1",
        kostnadssted = NavEnhetFixtures.Innlandet.enhetsnummer,
        beregning = TilsagnBeregningFri(
            input = TilsagnBeregningFri.Input(1000),
            output = TilsagnBeregningFri.Output(1000),
        ),
        arrangorId = ArrangorFixtures.underenhet1.id,
        behandletAv = NavAnsattFixture.ansatt1.navIdent,
        behandletTidspunkt = LocalDateTime.now(),
        type = TilsagnType.TILSAGN,
    )

    val Tilsagn2 = TilsagnDbo(
        id = UUID.randomUUID(),
        gjennomforingId = GjennomforingFixtures.AFT1.id,
        periode = Periode.forMonthOf(LocalDate.of(2025, 2, 1)),
        lopenummer = 2,
        bestillingsnummer = "A-2025/1-2",
        kostnadssted = NavEnhetFixtures.Innlandet.enhetsnummer,
        beregning = TilsagnBeregningFri(
            input = TilsagnBeregningFri.Input(1500),
            output = TilsagnBeregningFri.Output(1500),
        ),
        arrangorId = ArrangorFixtures.underenhet1.id,
        behandletAv = NavIdent("Z123456"),
        behandletTidspunkt = LocalDateTime.now(),
        type = TilsagnType.TILSAGN,
    )

    val Tilsagn3 = TilsagnDbo(
        id = UUID.randomUUID(),
        gjennomforingId = GjennomforingFixtures.AFT1.id,
        periode = Periode.forMonthOf(LocalDate.of(2025, 3, 1)),
        lopenummer = 3,
        bestillingsnummer = "A-2025/1-3",
        kostnadssted = NavEnhetFixtures.Innlandet.enhetsnummer,
        beregning = TilsagnBeregningFri(
            input = TilsagnBeregningFri.Input(2500),
            output = TilsagnBeregningFri.Output(2500),
        ),
        arrangorId = ArrangorFixtures.underenhet1.id,
        behandletAv = NavIdent("Z123456"),
        behandletTidspunkt = LocalDateTime.now(),
        type = TilsagnType.TILSAGN,
    )

    fun QueryContext.setTilsagnStatus(tilsagnDbo: TilsagnDbo, status: TilsagnStatus) {
        val dto = queries.tilsagn.get(tilsagnDbo.id)
            ?: throw IllegalStateException("Tilsagnet må være gitt til domain først")
        when (status) {
            TilsagnStatus.TIL_GODKJENNING -> {}
            TilsagnStatus.GODKJENT ->
                queries.totrinnskontroll.upsert(
                    dto.opprettelse.copy(
                        besluttetAv = NavAnsattFixture.ansatt2.navIdent,
                        besluttelse = Besluttelse.GODKJENT,
                        besluttetTidspunkt = LocalDateTime.now(),
                    ),
                )
            TilsagnStatus.RETURNERT ->
                queries.totrinnskontroll.upsert(
                    dto.opprettelse.copy(
                        besluttetAv = NavAnsattFixture.ansatt2.navIdent,
                        besluttelse = Besluttelse.AVVIST,
                        besluttetTidspunkt = LocalDateTime.now(),
                    ),
                )
            TilsagnStatus.TIL_ANNULLERING -> {
                queries.totrinnskontroll.upsert(
                    dto.opprettelse.copy(
                        besluttetAv = NavAnsattFixture.ansatt2.navIdent,
                        besluttelse = Besluttelse.GODKJENT,
                        besluttetTidspunkt = LocalDateTime.now(),
                    ),
                )
                queries.totrinnskontroll.upsert(
                    Totrinnskontroll(
                        id = UUID.randomUUID(),
                        entityId = tilsagnDbo.id,
                        behandletAv = tilsagnDbo.behandletAv,
                        aarsaker = emptyList(),
                        forklaring = null,
                        type = Totrinnskontroll.Type.ANNULLER,
                        behandletTidspunkt = LocalDateTime.now(),
                        besluttelse = null,
                        besluttetAv = null,
                        besluttetTidspunkt = null,
                    ),
                )
            }
            TilsagnStatus.ANNULLERT -> {
                queries.totrinnskontroll.upsert(
                    dto.opprettelse.copy(
                        besluttetAv = NavAnsattFixture.ansatt1.navIdent,
                        besluttelse = Besluttelse.GODKJENT,
                        besluttetTidspunkt = LocalDateTime.now(),
                    ),
                )
                queries.totrinnskontroll.upsert(
                    Totrinnskontroll(
                        id = UUID.randomUUID(),
                        entityId = tilsagnDbo.id,
                        behandletAv = tilsagnDbo.behandletAv,
                        aarsaker = emptyList(),
                        forklaring = null,
                        type = Totrinnskontroll.Type.OPPRETT,
                        behandletTidspunkt = LocalDateTime.now(),
                        besluttelse = Besluttelse.GODKJENT,
                        besluttetAv = NavAnsattFixture.ansatt2.navIdent,
                        besluttetTidspunkt = LocalDateTime.now(),
                    ),
                )
            }
        }
    }
}
