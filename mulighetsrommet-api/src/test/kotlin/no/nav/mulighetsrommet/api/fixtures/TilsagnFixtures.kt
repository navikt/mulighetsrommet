package no.nav.mulighetsrommet.api.fixtures

import no.nav.mulighetsrommet.api.tilsagn.db.TilsagnDbo
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningFri
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnType
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
        bestillingsnummer = "2025/1",
        kostnadssted = NavEnhetFixtures.Innlandet.enhetsnummer,
        beregning = TilsagnBeregningFri(
            input = TilsagnBeregningFri.Input(1000),
            output = TilsagnBeregningFri.Output(1000),
        ),
        arrangorId = ArrangorFixtures.underenhet1.id,
        endretAv = NavAnsattFixture.ansatt1.navIdent,
        endretTidspunkt = LocalDateTime.now(),
        type = TilsagnType.TILSAGN,
    )

    val Tilsagn2 = TilsagnDbo(
        id = UUID.randomUUID(),
        gjennomforingId = GjennomforingFixtures.AFT1.id,
        periode = Periode.forMonthOf(LocalDate.of(2025, 2, 1)),
        lopenummer = 2,
        bestillingsnummer = "2025/2",
        kostnadssted = NavEnhetFixtures.Innlandet.enhetsnummer,
        beregning = TilsagnBeregningFri(
            input = TilsagnBeregningFri.Input(1500),
            output = TilsagnBeregningFri.Output(1500),
        ),
        arrangorId = ArrangorFixtures.underenhet1.id,
        endretAv = NavIdent("Z123456"),
        endretTidspunkt = LocalDateTime.now(),
        type = TilsagnType.TILSAGN,
    )

    val Tilsagn3 = TilsagnDbo(
        id = UUID.randomUUID(),
        gjennomforingId = GjennomforingFixtures.AFT1.id,
        periode = Periode.forMonthOf(LocalDate.of(2025, 3, 1)),
        lopenummer = 3,
        bestillingsnummer = "2025/3",
        kostnadssted = NavEnhetFixtures.Innlandet.enhetsnummer,
        beregning = TilsagnBeregningFri(
            input = TilsagnBeregningFri.Input(2500),
            output = TilsagnBeregningFri.Output(2500),
        ),
        arrangorId = ArrangorFixtures.underenhet1.id,
        endretAv = NavIdent("Z123456"),
        endretTidspunkt = LocalDateTime.now(),
        type = TilsagnType.TILSAGN,
    )
}
