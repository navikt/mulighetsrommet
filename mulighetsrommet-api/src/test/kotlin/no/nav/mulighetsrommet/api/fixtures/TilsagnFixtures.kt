package no.nav.mulighetsrommet.api.fixtures

import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.tilsagn.db.TilsagnDbo
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningFri
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnType
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Periode
import java.time.LocalDate
import java.util.*

object TilsagnFixtures {
    val Tilsagn1 = TilsagnDbo(
        id = UUID.randomUUID(),
        gjennomforingId = GjennomforingFixtures.AFT1.id,
        type = TilsagnType.TILSAGN,
        periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
        kostnadssted = NavEnhetFixtures.Innlandet.enhetsnummer,
        lopenummer = 1,
        bestillingsnummer = "A-2025/1-1",
        bestillingStatus = null,
        belopBrukt = 0,
        beregning = TilsagnBeregningFri(
            input = TilsagnBeregningFri.Input(1000),
            output = TilsagnBeregningFri.Output(1000),
        ),
    )

    val Tilsagn2 = TilsagnDbo(
        id = UUID.randomUUID(),
        gjennomforingId = GjennomforingFixtures.AFT1.id,
        type = TilsagnType.TILSAGN,
        periode = Periode.forMonthOf(LocalDate.of(2025, 2, 1)),
        kostnadssted = NavEnhetFixtures.Innlandet.enhetsnummer,
        lopenummer = 2,
        bestillingsnummer = "A-2025/1-2",
        bestillingStatus = null,
        belopBrukt = 0,
        beregning = TilsagnBeregningFri(
            input = TilsagnBeregningFri.Input(1500),
            output = TilsagnBeregningFri.Output(1500),
        ),
    )

    val Tilsagn3 = TilsagnDbo(
        id = UUID.randomUUID(),
        gjennomforingId = GjennomforingFixtures.AFT1.id,
        type = TilsagnType.TILSAGN,
        periode = Periode.forMonthOf(LocalDate.of(2025, 3, 1)),
        kostnadssted = NavEnhetFixtures.Innlandet.enhetsnummer,
        lopenummer = 3,
        bestillingsnummer = "A-2025/1-3",
        bestillingStatus = null,
        belopBrukt = 0,
        beregning = TilsagnBeregningFri(
            input = TilsagnBeregningFri.Input(2500),
            output = TilsagnBeregningFri.Output(2500),
        ),
    )

    val Tilsagn4 = TilsagnDbo(
        id = UUID.randomUUID(),
        gjennomforingId = GjennomforingFixtures.AFT1.id,
        type = TilsagnType.TILSAGN,
        periode = Periode.forMonthOf(LocalDate.of(2025, 3, 1)),
        kostnadssted = NavEnhetFixtures.Innlandet.enhetsnummer,
        lopenummer = 4,
        bestillingsnummer = "A-2025/1-4",
        bestillingStatus = null,
        belopBrukt = 0,
        beregning = TilsagnBeregningFri(
            input = TilsagnBeregningFri.Input(2500),
            output = TilsagnBeregningFri.Output(2500),
        ),
    )
}

fun QueryContext.setTilsagnStatus(
    tilsagnDbo: TilsagnDbo,
    status: TilsagnStatus,
    behandletAv: NavIdent = NavAnsattFixture.DonaldDuck.navIdent,
    besluttetAv: NavIdent = NavAnsattFixture.FetterAnton.navIdent,
) {
    val dto = queries.tilsagn.get(tilsagnDbo.id)
        ?: throw IllegalStateException("Tilsagnet må være gitt til domain først")

    queries.tilsagn.setStatus(dto.id, status)

    when (status) {
        TilsagnStatus.TIL_GODKJENNING -> {
            setTilGodkjenning(tilsagnDbo.id, Totrinnskontroll.Type.OPPRETT, behandletAv)
        }

        TilsagnStatus.GODKJENT -> {
            setGodkjent(tilsagnDbo.id, Totrinnskontroll.Type.OPPRETT, behandletAv, besluttetAv)
        }

        TilsagnStatus.TIL_OPPGJOR -> {
            setGodkjent(tilsagnDbo.id, Totrinnskontroll.Type.OPPRETT, behandletAv, besluttetAv)
            setTilGodkjenning(tilsagnDbo.id, Totrinnskontroll.Type.GJOR_OPP, behandletAv)
        }

        TilsagnStatus.OPPGJORT -> {
            setGodkjent(tilsagnDbo.id, Totrinnskontroll.Type.OPPRETT, behandletAv, besluttetAv)
            setGodkjent(tilsagnDbo.id, Totrinnskontroll.Type.GJOR_OPP, behandletAv, besluttetAv)
        }

        TilsagnStatus.RETURNERT -> {
            setAvvist(tilsagnDbo.id, Totrinnskontroll.Type.OPPRETT, behandletAv, besluttetAv)
        }

        TilsagnStatus.TIL_ANNULLERING -> {
            setGodkjent(tilsagnDbo.id, Totrinnskontroll.Type.OPPRETT, behandletAv, besluttetAv)
            setTilGodkjenning(tilsagnDbo.id, Totrinnskontroll.Type.ANNULLER, behandletAv)
        }

        TilsagnStatus.ANNULLERT -> {
            setGodkjent(tilsagnDbo.id, Totrinnskontroll.Type.OPPRETT, behandletAv, besluttetAv)
            setGodkjent(tilsagnDbo.id, Totrinnskontroll.Type.ANNULLER, behandletAv, besluttetAv)
        }
    }
}
