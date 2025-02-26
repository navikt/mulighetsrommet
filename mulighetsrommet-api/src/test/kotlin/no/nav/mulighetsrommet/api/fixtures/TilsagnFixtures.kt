package no.nav.mulighetsrommet.api.fixtures

import no.nav.mulighetsrommet.api.tilsagn.model.*
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

object TilsagnFixtures {
    val Tilsagn1 = TilsagnDto(
        id = UUID.randomUUID(),
        gjennomforing = TilsagnDto.Gjennomforing(
            id = GjennomforingFixtures.AFT1.id,
            navn = GjennomforingFixtures.AFT1.navn,
            tiltakskode = TiltakstypeFixtures.AFT.tiltakskode!!,
        ),
        periodeStart = LocalDate.of(2025, 1, 1),
        periodeSlutt = LocalDate.of(2025, 1, 31),
        lopenummer = 1,
        bestillingsnummer = "A-2025/1-1",
        kostnadssted = NavEnhetFixtures.Innlandet,
        beregning = TilsagnBeregningFri(
            input = TilsagnBeregningFri.Input(1000),
            output = TilsagnBeregningFri.Output(1000),
        ),
        arrangor = TilsagnDto.Arrangor(
            id = ArrangorFixtures.underenhet1.id,
            navn = ArrangorFixtures.underenhet1.navn,
            organisasjonsnummer = ArrangorFixtures.underenhet1.organisasjonsnummer,
            slettet = false,
        ),
        status = TilsagnStatus.TIL_GODKJENNING,
        opprettelse = Totrinnskontroll.Ubesluttet(
            behandletAv = NavAnsattFixture.ansatt1.navIdent,
            behandletTidspunkt = LocalDateTime.of(2025, 1, 1, 0, 0, 0),
            aarsaker = emptyList(),
            forklaring = null,
        ),
        annullering = null,
        type = TilsagnType.TILSAGN,
    )

    val Tilsagn2 = TilsagnDto(
        id = UUID.randomUUID(),
        gjennomforing = TilsagnDto.Gjennomforing(
            id = GjennomforingFixtures.AFT1.id,
            navn = GjennomforingFixtures.AFT1.navn,
            tiltakskode = TiltakstypeFixtures.AFT.tiltakskode!!,
        ),
        periodeStart = LocalDate.of(2025, 2, 1),
        periodeSlutt = LocalDate.of(2025, 2, 28),
        lopenummer = 2,
        bestillingsnummer = "A-2025/1-2",
        kostnadssted = NavEnhetFixtures.Innlandet,
        beregning = TilsagnBeregningFri(
            input = TilsagnBeregningFri.Input(1500),
            output = TilsagnBeregningFri.Output(1500),
        ),
        arrangor = TilsagnDto.Arrangor(
            id = ArrangorFixtures.underenhet1.id,
            navn = ArrangorFixtures.underenhet1.navn,
            organisasjonsnummer = ArrangorFixtures.underenhet1.organisasjonsnummer,
            slettet = false,
        ),
        status = TilsagnStatus.TIL_GODKJENNING,
        opprettelse = Totrinnskontroll.Ubesluttet(
            behandletAv = NavAnsattFixture.ansatt1.navIdent,
            behandletTidspunkt = LocalDateTime.of(2025, 1, 1, 0, 0, 0),
            aarsaker = emptyList(),
            forklaring = null,
        ),
        annullering = null,
        type = TilsagnType.TILSAGN,
    )

    val Tilsagn3 = TilsagnDto(
        id = UUID.randomUUID(),
        gjennomforing = TilsagnDto.Gjennomforing(
            id = GjennomforingFixtures.AFT1.id,
            navn = GjennomforingFixtures.AFT1.navn,
            tiltakskode = TiltakstypeFixtures.AFT.tiltakskode!!,
        ),
        periodeStart = LocalDate.of(2025, 3, 1),
        periodeSlutt = LocalDate.of(2025, 3, 31),
        lopenummer = 3,
        bestillingsnummer = "A-2025/1-3",
        kostnadssted = NavEnhetFixtures.Innlandet,
        beregning = TilsagnBeregningFri(
            input = TilsagnBeregningFri.Input(2500),
            output = TilsagnBeregningFri.Output(2500),
        ),
        arrangor = TilsagnDto.Arrangor(
            id = ArrangorFixtures.underenhet1.id,
            navn = ArrangorFixtures.underenhet1.navn,
            organisasjonsnummer = ArrangorFixtures.underenhet1.organisasjonsnummer,
            slettet = false,
        ),
        status = TilsagnStatus.TIL_GODKJENNING,
        opprettelse = Totrinnskontroll.Ubesluttet(
            behandletAv = NavAnsattFixture.ansatt1.navIdent,
            behandletTidspunkt = LocalDateTime.of(2025, 1, 1, 0, 0, 0),
            aarsaker = emptyList(),
            forklaring = null,
        ),
        annullering = null,
        type = TilsagnType.TILSAGN,
    )

    fun TilsagnDto.medStatus(
        status: TilsagnStatus,
    ): TilsagnDto {
        return when (status) {
            TilsagnStatus.TIL_GODKJENNING -> this.copy(
                status = TilsagnStatus.TIL_GODKJENNING,
                opprettelse = Totrinnskontroll.Ubesluttet(
                    behandletAv = NavAnsattFixture.ansatt1.navIdent,
                    behandletTidspunkt = LocalDateTime.now(),
                    aarsaker = emptyList(),
                    forklaring = null,
                ),
            )
            TilsagnStatus.GODKJENT -> this.copy(
                status = TilsagnStatus.GODKJENT,
                opprettelse = Totrinnskontroll.Besluttet(
                    behandletAv = NavAnsattFixture.ansatt1.navIdent,
                    besluttetAv = NavAnsattFixture.ansatt2.navIdent,
                    besluttetTidspunkt = LocalDateTime.now(),
                    behandletTidspunkt = LocalDateTime.now(),
                    besluttelse = Besluttelse.GODKJENT,
                    aarsaker = emptyList(),
                    forklaring = null,
                ),
            )
            TilsagnStatus.RETURNERT -> this.copy(
                status = TilsagnStatus.RETURNERT,
                opprettelse = Totrinnskontroll.Besluttet(
                    behandletAv = NavAnsattFixture.ansatt1.navIdent,
                    besluttetAv = NavAnsattFixture.ansatt2.navIdent,
                    besluttetTidspunkt = LocalDateTime.now(),
                    behandletTidspunkt = LocalDateTime.now(),
                    besluttelse = Besluttelse.AVVIST,
                    aarsaker = listOf(TilsagnStatusAarsak.FEIL_BELOP.name),
                    forklaring = null,
                ),
            )
            TilsagnStatus.TIL_ANNULLERING -> this.copy(
                status = TilsagnStatus.TIL_ANNULLERING,
                opprettelse = Totrinnskontroll.Besluttet(
                    behandletAv = NavAnsattFixture.ansatt1.navIdent,
                    besluttetAv = NavAnsattFixture.ansatt2.navIdent,
                    besluttetTidspunkt = LocalDateTime.now(),
                    behandletTidspunkt = LocalDateTime.now(),
                    besluttelse = Besluttelse.GODKJENT,
                    aarsaker = emptyList(),
                    forklaring = null,
                ),
                annullering = Totrinnskontroll.Ubesluttet(
                    behandletAv = NavAnsattFixture.ansatt1.navIdent,
                    behandletTidspunkt = LocalDateTime.now(),
                    aarsaker = emptyList(),
                    forklaring = null,
                ),
            )
            TilsagnStatus.ANNULLERT -> this.copy(
                status = TilsagnStatus.ANNULLERT,
                opprettelse = Totrinnskontroll.Besluttet(
                    behandletAv = NavAnsattFixture.ansatt1.navIdent,
                    besluttetAv = NavAnsattFixture.ansatt2.navIdent,
                    besluttetTidspunkt = LocalDateTime.now(),
                    behandletTidspunkt = LocalDateTime.now(),
                    besluttelse = Besluttelse.GODKJENT,
                    aarsaker = emptyList(),
                    forklaring = null,
                ),
                annullering = Totrinnskontroll.Besluttet(
                    behandletAv = NavAnsattFixture.ansatt1.navIdent,
                    behandletTidspunkt = LocalDateTime.now(),
                    besluttetAv = NavAnsattFixture.ansatt2.navIdent,
                    besluttelse = Besluttelse.GODKJENT,
                    besluttetTidspunkt = LocalDateTime.now(),
                    aarsaker = emptyList(),
                    forklaring = null,
                ),
            )
        }
    }
}
