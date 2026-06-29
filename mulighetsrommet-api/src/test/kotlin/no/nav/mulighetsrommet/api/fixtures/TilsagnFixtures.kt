package no.nav.mulighetsrommet.api.fixtures

import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.tilsagn.db.TilsagnDbo
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningFri
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningRequest
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningType
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnInputLinjeRequest
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnRequest
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnType
import no.nav.mulighetsrommet.api.totrinnskontroll.model.TotrinnskontrollType
import no.nav.mulighetsrommet.model.NOK
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Valuta
import java.time.LocalDate
import java.util.UUID

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
        belopBrukt = 0.NOK,
        beregning = TilsagnBeregningFri(
            input = TilsagnBeregningFri.Input(
                listOf(
                    TilsagnBeregningFri.InputLinje(
                        id = UUID.randomUUID(),
                        beskrivelse = "1000",
                        pris = 1000.NOK,
                        antall = 1,
                    ),
                ),
                prisbetingelser = null,
            ),
            output = TilsagnBeregningFri.Output(
                pris = 1000.NOK,
            ),
        ),
        kommentar = null,
        beskrivelse = null,
        deltakere = emptyList(),
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
        belopBrukt = 0.NOK,
        beregning = TilsagnBeregningFri(
            input = TilsagnBeregningFri.Input(
                listOf(
                    TilsagnBeregningFri.InputLinje(
                        id = UUID.randomUUID(),
                        beskrivelse = "1500",
                        pris = 1500.NOK,
                        antall = 1,
                    ),
                ),
                prisbetingelser = null,
            ),
            output = TilsagnBeregningFri.Output(
                pris = 1500.NOK,
            ),
        ),
        kommentar = null,
        beskrivelse = null,
        deltakere = emptyList(),
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
        belopBrukt = 0.NOK,
        beregning = TilsagnBeregningFri(
            input = TilsagnBeregningFri.Input(
                listOf(
                    TilsagnBeregningFri.InputLinje(
                        id = UUID.randomUUID(),
                        beskrivelse = "1250",
                        pris = 1250.NOK,
                        antall = 2,
                    ),
                ),
                prisbetingelser = null,
            ),
            output = TilsagnBeregningFri.Output(
                pris = 2500.NOK,
            ),
        ),
        kommentar = null,
        beskrivelse = null,
        deltakere = emptyList(),
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
        belopBrukt = 0.NOK,
        beregning = TilsagnBeregningFri(
            input = TilsagnBeregningFri.Input(
                listOf(
                    TilsagnBeregningFri.InputLinje(
                        id = UUID.randomUUID(),
                        beskrivelse = "Beskrivelse",
                        pris = 1250.NOK,
                        antall = 2,
                    ),
                ),
                prisbetingelser = null,
            ),
            output = TilsagnBeregningFri.Output(
                pris = 2500.NOK,
            ),
        ),
        kommentar = null,
        beskrivelse = null,
        deltakere = emptyList(),
    )

    val TilsagnRequest1 = TilsagnRequest(
        id = UUID.randomUUID(),
        gjennomforingId = GjennomforingFixtures.AFT1.id,
        type = TilsagnType.TILSAGN,
        periodeStart = "2025-01-01",
        periodeSlutt = "2025-01-31",
        kostnadssted = NavEnhetFixtures.Innlandet.enhetsnummer,
        beregning = TilsagnBeregningRequest(
            type = TilsagnBeregningType.FRI,
            linjer = listOf(
                TilsagnInputLinjeRequest(
                    id = UUID.randomUUID(),
                    beskrivelse = "1000",
                    pris = 1000.NOK,
                    antall = 1,
                ),
            ),
            valuta = Valuta.NOK,
            prisbetingelser = null,
        ),
        kommentar = null,
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
            setTilBehandling(tilsagnDbo.id, TotrinnskontrollType.TILSAGN_OPPRETTELSE, behandletAv)
        }

        TilsagnStatus.GODKJENT -> {
            setGodkjent(tilsagnDbo.id, TotrinnskontrollType.TILSAGN_OPPRETTELSE, behandletAv, besluttetAv)
        }

        TilsagnStatus.TIL_OPPGJOR -> {
            setGodkjent(tilsagnDbo.id, TotrinnskontrollType.TILSAGN_OPPRETTELSE, behandletAv, besluttetAv)
            setTilBehandling(tilsagnDbo.id, TotrinnskontrollType.TILSAGN_OPPGJOR, behandletAv)
        }

        TilsagnStatus.OPPGJORT -> {
            setGodkjent(tilsagnDbo.id, TotrinnskontrollType.TILSAGN_OPPRETTELSE, behandletAv, besluttetAv)
            setGodkjent(tilsagnDbo.id, TotrinnskontrollType.TILSAGN_OPPGJOR, behandletAv, besluttetAv)
        }

        TilsagnStatus.RETURNERT -> {
            setAvvist(tilsagnDbo.id, TotrinnskontrollType.TILSAGN_OPPRETTELSE, behandletAv, besluttetAv)
        }

        TilsagnStatus.TIL_ANNULLERING -> {
            setGodkjent(tilsagnDbo.id, TotrinnskontrollType.TILSAGN_OPPRETTELSE, behandletAv, besluttetAv)
            setTilBehandling(tilsagnDbo.id, TotrinnskontrollType.TILSAGN_ANNULLERING, behandletAv)
        }

        TilsagnStatus.ANNULLERT -> {
            setGodkjent(tilsagnDbo.id, TotrinnskontrollType.TILSAGN_OPPRETTELSE, behandletAv, besluttetAv)
            setGodkjent(tilsagnDbo.id, TotrinnskontrollType.TILSAGN_ANNULLERING, behandletAv, besluttetAv)
        }
    }
}
