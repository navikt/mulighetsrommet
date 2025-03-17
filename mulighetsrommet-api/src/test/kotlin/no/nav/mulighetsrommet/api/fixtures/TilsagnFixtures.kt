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
        type = TilsagnType.TILSAGN,
    )

    val Tilsagn4 = TilsagnDbo(
        id = UUID.randomUUID(),
        gjennomforingId = GjennomforingFixtures.AFT1.id,
        periode = Periode.forMonthOf(LocalDate.of(2025, 3, 1)),
        lopenummer = 4,
        bestillingsnummer = "A-2025/1-4",
        kostnadssted = NavEnhetFixtures.Innlandet.enhetsnummer,
        beregning = TilsagnBeregningFri(
            input = TilsagnBeregningFri.Input(2500),
            output = TilsagnBeregningFri.Output(2500),
        ),
        arrangorId = ArrangorFixtures.underenhet1.id,
        type = TilsagnType.TILSAGN,
    )

    fun QueryContext.setTilsagnStatus(
        tilsagnDbo: TilsagnDbo,
        status: TilsagnStatus,
        behandletAv: NavIdent = NavAnsattFixture.ansatt1.navIdent,
        besluttetAv: NavIdent = NavAnsattFixture.ansatt2.navIdent,
    ) {
        val dto = queries.tilsagn.get(tilsagnDbo.id)
            ?: throw IllegalStateException("Tilsagnet må være gitt til domain først")

        queries.tilsagn.setStatus(dto.id, status)

        when (status) {
            TilsagnStatus.TIL_GODKJENNING -> {
                queries.totrinnskontroll.upsert(
                    tilGodkjenning(tilsagnDbo.id, Totrinnskontroll.Type.OPPRETT, behandletAv),
                )
            }

            TilsagnStatus.GODKJENT -> {
                queries.totrinnskontroll.upsert(
                    godkjent(tilsagnDbo.id, Totrinnskontroll.Type.OPPRETT, behandletAv, besluttetAv),
                )
            }

            TilsagnStatus.TIL_FRIGJORING -> {
                queries.totrinnskontroll.upsert(
                    godkjent(tilsagnDbo.id, Totrinnskontroll.Type.OPPRETT, behandletAv, besluttetAv),
                )
                queries.totrinnskontroll.upsert(
                    tilGodkjenning(tilsagnDbo.id, Totrinnskontroll.Type.FRIGJOR, behandletAv),
                )
            }

            TilsagnStatus.FRIGJORT -> {
                queries.totrinnskontroll.upsert(
                    godkjent(tilsagnDbo.id, Totrinnskontroll.Type.OPPRETT, behandletAv, besluttetAv),
                )
                queries.totrinnskontroll.upsert(
                    godkjent(tilsagnDbo.id, Totrinnskontroll.Type.FRIGJOR, behandletAv, besluttetAv),
                )
            }

            TilsagnStatus.RETURNERT -> {
                queries.totrinnskontroll.upsert(
                    avvist(tilsagnDbo.id, Totrinnskontroll.Type.OPPRETT, behandletAv, besluttetAv),
                )
            }

            TilsagnStatus.TIL_ANNULLERING -> {
                queries.totrinnskontroll.upsert(
                    godkjent(tilsagnDbo.id, Totrinnskontroll.Type.OPPRETT, behandletAv, besluttetAv),
                )
                queries.totrinnskontroll.upsert(
                    tilGodkjenning(tilsagnDbo.id, Totrinnskontroll.Type.ANNULLER, behandletAv),
                )
            }

            TilsagnStatus.ANNULLERT -> {
                queries.totrinnskontroll.upsert(
                    godkjent(tilsagnDbo.id, Totrinnskontroll.Type.OPPRETT, behandletAv, besluttetAv),
                )
                queries.totrinnskontroll.upsert(
                    godkjent(tilsagnDbo.id, Totrinnskontroll.Type.ANNULLER, behandletAv, besluttetAv),
                )
            }
        }
    }

    private fun tilGodkjenning(
        uuid: UUID,
        type: Totrinnskontroll.Type,
        behandletAv: NavIdent,
    ) = Totrinnskontroll(
        id = UUID.randomUUID(),
        entityId = uuid,
        behandletAv = behandletAv,
        aarsaker = emptyList(),
        forklaring = null,
        type = type,
        behandletTidspunkt = LocalDateTime.now(),
        besluttelse = null,
        besluttetAv = null,
        besluttetTidspunkt = null,
    )

    private fun godkjent(
        uuid: UUID,
        type: Totrinnskontroll.Type,
        behandletAv: NavIdent,
        besluttetAv: NavIdent,
    ) = Totrinnskontroll(
        id = UUID.randomUUID(),
        entityId = uuid,
        behandletAv = behandletAv,
        aarsaker = emptyList(),
        forklaring = null,
        type = type,
        behandletTidspunkt = LocalDateTime.now(),
        besluttelse = Besluttelse.GODKJENT,
        besluttetAv = besluttetAv,
        besluttetTidspunkt = LocalDateTime.now(),
    )

    private fun avvist(
        uuid: UUID,
        type: Totrinnskontroll.Type,
        behandletAv: NavIdent,
        besluttetAv: NavIdent,
    ) = Totrinnskontroll(
        id = UUID.randomUUID(),
        entityId = uuid,
        behandletAv = behandletAv,
        aarsaker = listOf("Årsak 1"),
        forklaring = null,
        type = type,
        behandletTidspunkt = LocalDateTime.now(),
        besluttelse = Besluttelse.AVVIST,
        besluttetAv = besluttetAv,
        besluttetTidspunkt = LocalDateTime.now(),
    )
}
