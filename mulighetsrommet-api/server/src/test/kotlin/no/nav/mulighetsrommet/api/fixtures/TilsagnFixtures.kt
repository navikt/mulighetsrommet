package no.nav.mulighetsrommet.api.fixtures

import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.domain.testing.fixture.NavAnsattFixture
import no.nav.mulighetsrommet.api.domain.testing.fixture.NavEnhetFixtures
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.TotrinnskontrollType
import no.nav.mulighetsrommet.api.tilsagn.db.TilsagnDbo
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningAnnenAvtaltPris
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningRequest
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningType
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnInputLinjeRequest
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnRequest
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnType
import no.nav.mulighetsrommet.model.NOK
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.model.ValutaBelop
import no.nav.tiltak.okonomi.BestillingStatusType
import java.time.LocalDate
import java.util.UUID

object TilsagnFixtures {
    fun createTilsagn(
        gjennomforingId: UUID = GjennomforingFixtures.AFT1.id,
        lopenummer: Int,
        type: TilsagnType = TilsagnType.TILSAGN,
        periode: Periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
        pris: ValutaBelop = 1000.NOK,
        antall: Int = 1,
        bestillingStatus: BestillingStatusType? = null,
    ): TilsagnDbo = TilsagnDbo(
        id = UUID.randomUUID(),
        gjennomforingId = gjennomforingId,
        type = type,
        periode = periode,
        kostnadssted = NavEnhetFixtures.Innlandet.enhetsnummer,
        lopenummer = lopenummer,
        bestillingsnummer = "A-2025/1-$lopenummer",
        bestillingStatus = bestillingStatus,
        belopBrukt = 0.NOK,
        beregning = TilsagnBeregningAnnenAvtaltPris(
            input = TilsagnBeregningAnnenAvtaltPris.Input(
                listOf(
                    TilsagnBeregningAnnenAvtaltPris.InputLinje(
                        id = UUID.randomUUID(),
                        beskrivelse = pris.belop.toString(),
                        pris = pris,
                        antall = antall,
                    ),
                ),
                prisbetingelser = null,
            ),
            output = TilsagnBeregningAnnenAvtaltPris.Output(
                pris = (pris.belop * antall).NOK,
            ),
        ),
        kommentar = null,
        beskrivelse = null,
        deltakere = emptyList(),
    )

    val Tilsagn1 = createTilsagn(lopenummer = 1)

    val Tilsagn2 = createTilsagn(
        lopenummer = 2,
        periode = Periode.forMonthOf(LocalDate.of(2025, 2, 1)),
        pris = 1500.NOK,
        antall = 1,
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
            setReturnert(tilsagnDbo.id, TotrinnskontrollType.TILSAGN_OPPRETTELSE, behandletAv, besluttetAv)
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
