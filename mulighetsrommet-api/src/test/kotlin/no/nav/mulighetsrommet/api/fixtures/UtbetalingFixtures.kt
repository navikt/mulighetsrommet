package no.nav.mulighetsrommet.api.fixtures

import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.arrangor.model.Betalingsinformasjon
import no.nav.mulighetsrommet.api.arrangorflate.model.ArrangorflateUtbetaling
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.totrinnskontroll.model.TotrinnskontrollType
import no.nav.mulighetsrommet.api.utbetaling.db.UtbetalingDbo
import no.nav.mulighetsrommet.api.utbetaling.db.UtbetalingLinjeDbo
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling.Arrangor
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling.Gjennomforing
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling.Tiltakstype
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFri
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingLinjeStatus
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatusType
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.NOK
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltaksnummer
import no.nav.mulighetsrommet.model.Valuta
import no.nav.tiltak.okonomi.Tilskuddstype
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

object UtbetalingFixtures {
    val utbetaling1 = UtbetalingDbo(
        id = UUID.randomUUID(),
        gjennomforingId = AFT1.id,
        status = UtbetalingStatusType.GENERERT,
        periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
        valuta = Valuta.NOK,
        beregning = UtbetalingBeregningFri(
            input = UtbetalingBeregningFri.Input(1000.NOK),
            output = UtbetalingBeregningFri.Output(1000.NOK),
        ),
        betalingsinformasjon = Betalingsinformasjon.BBan(Kontonummer("11111111111"), null),
        kommentar = null,
        korreksjonGjelderUtbetalingId = null,
        korreksjonBegrunnelse = null,
        tilskuddstype = Tilskuddstype.TILTAK_DRIFTSTILSKUDD,
        journalpostId = null,
        innsendtAvArrangorTidspunkt = null,
        utbetalesTidligstTidspunkt = null,
    )

    val utbetalingDto1 = Utbetaling(
        id = UUID.randomUUID(),
        gjennomforing = Gjennomforing(
            id = AFT1.id,
            lopenummer = Tiltaksnummer("2025/10000"),
        ),
        korreksjon = null,
        innsending = null,
        periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
        valuta = Valuta.NOK,
        beregning = UtbetalingBeregningFri(
            input = UtbetalingBeregningFri.Input(1000.NOK),
            output = UtbetalingBeregningFri.Output(1000.NOK),
        ),
        kommentar = null,
        tilskuddstype = Tilskuddstype.TILTAK_DRIFTSTILSKUDD,
        utbetalesTidligstTidspunkt = null,
        status = UtbetalingStatusType.GENERERT,
        tiltakstype = Tiltakstype(
            navn = TiltakstypeFixtures.AFT.navn,
            tiltakskode = TiltakstypeFixtures.AFT.tiltakskode,
        ),
        arrangor = Arrangor(
            id = ArrangorFixtures.underenhet1.id,
            organisasjonsnummer = ArrangorFixtures.underenhet1.organisasjonsnummer,
            navn = ArrangorFixtures.underenhet1.navn,
            slettet = false,
        ),
        betalingsinformasjon = Betalingsinformasjon.BBan(Kontonummer("11111111111"), null),
        journalpostId = null,
        begrunnelseMindreBetalt = null,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
        avbruttBegrunnelse = null,
        avbruttTidspunkt = null,
        blokkeringer = emptySet(),
    )

    val arrangorflateUtbetalingDto1 = ArrangorflateUtbetaling(
        id = utbetalingDto1.id,
        gjennomforing = ArrangorflateUtbetaling.Gjennomforing(
            id = utbetalingDto1.gjennomforing.id,
            navn = "AFT #1",
            lopenummer = utbetalingDto1.gjennomforing.lopenummer,
            startDato = LocalDate.of(2025, 1, 1),
            sluttDato = LocalDate.of(2025, 1, 31),
        ),
        korreksjon = null,
        innsending = null,
        periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
        valuta = Valuta.NOK,
        beregning = UtbetalingBeregningFri(
            input = UtbetalingBeregningFri.Input(1000.NOK),
            output = UtbetalingBeregningFri.Output(1000.NOK),
        ),
        tilskuddstype = Tilskuddstype.TILTAK_DRIFTSTILSKUDD,
        utbetalesTidligstTidspunkt = null,
        status = UtbetalingStatusType.GENERERT,
        tiltakstype = ArrangorflateUtbetaling.Tiltakstype(
            navn = TiltakstypeFixtures.AFT.navn,
            tiltakskode = TiltakstypeFixtures.AFT.tiltakskode,
        ),
        arrangor = ArrangorflateUtbetaling.Arrangor(
            id = ArrangorFixtures.underenhet1.id,
            organisasjonsnummer = ArrangorFixtures.underenhet1.organisasjonsnummer,
            navn = ArrangorFixtures.underenhet1.navn,
        ),
        betalingsinformasjon = Betalingsinformasjon.BBan(Kontonummer("11111111111"), null),
        createdAt = utbetalingDto1.createdAt,
        updatedAt = utbetalingDto1.updatedAt,
        avbruttTidspunkt = null,
        blokkeringer = emptySet(),
    )

    val utbetaling2 = UtbetalingDbo(
        id = UUID.randomUUID(),
        gjennomforingId = AFT1.id,
        status = UtbetalingStatusType.GENERERT,
        periode = Periode.forMonthOf(LocalDate.of(2024, 2, 1)),
        valuta = Valuta.NOK,
        beregning = UtbetalingBeregningFri(
            input = UtbetalingBeregningFri.Input(500.NOK),
            output = UtbetalingBeregningFri.Output(500.NOK),
        ),
        betalingsinformasjon = Betalingsinformasjon.BBan(Kontonummer("11111111111"), null),
        kommentar = null,
        korreksjonGjelderUtbetalingId = null,
        korreksjonBegrunnelse = null,
        tilskuddstype = Tilskuddstype.TILTAK_DRIFTSTILSKUDD,
        journalpostId = null,
        innsendtAvArrangorTidspunkt = null,
        utbetalesTidligstTidspunkt = null,
    )

    val utbetaling3 = UtbetalingDbo(
        id = UUID.randomUUID(),
        gjennomforingId = GjennomforingFixtures.VTA1.id,
        status = UtbetalingStatusType.GENERERT,
        periode = Periode.forMonthOf(LocalDate.of(2024, 2, 1)),
        valuta = Valuta.NOK,
        beregning = UtbetalingBeregningFri(
            input = UtbetalingBeregningFri.Input(500.NOK),
            output = UtbetalingBeregningFri.Output(500.NOK),
        ),
        betalingsinformasjon = Betalingsinformasjon.BBan(Kontonummer("11111111111"), null),
        kommentar = null,
        korreksjonGjelderUtbetalingId = null,
        korreksjonBegrunnelse = null,
        tilskuddstype = Tilskuddstype.TILTAK_DRIFTSTILSKUDD,
        journalpostId = null,
        innsendtAvArrangorTidspunkt = null,
        utbetalesTidligstTidspunkt = null,
    )

    val utbetalingLinje1 = UtbetalingLinjeDbo(
        id = UUID.randomUUID(),
        tilsagnId = TilsagnFixtures.Tilsagn1.id,
        utbetalingId = utbetaling1.id,
        status = UtbetalingLinjeStatus.TIL_ATTESTERING,
        pris = 200.NOK,
        gjorOppTilsagn = false,
        periode = utbetaling1.periode,
        lopenummer = 1,
        fakturanummer = "${TilsagnFixtures.Tilsagn1.bestillingsnummer}/1",
        fakturaStatusEndretTidspunkt = null,
        fakturaStatus = null,
    )

    val utbetalingLinje2 = UtbetalingLinjeDbo(
        id = UUID.randomUUID(),
        tilsagnId = TilsagnFixtures.Tilsagn2.id,
        utbetalingId = utbetaling1.id,
        status = UtbetalingLinjeStatus.TIL_ATTESTERING,
        pris = 150.NOK,
        gjorOppTilsagn = false,
        periode = utbetaling1.periode,
        lopenummer = 1,
        fakturanummer = "${TilsagnFixtures.Tilsagn2.bestillingsnummer}/1",
        fakturaStatusEndretTidspunkt = null,
        fakturaStatus = null,
    )
}

fun QueryContext.setUtbetalingLinjeStatus(
    utbetalingLinjeDbo: UtbetalingLinjeDbo,
    status: UtbetalingLinjeStatus,
    behandletAv: NavIdent = NavAnsattFixture.DonaldDuck.navIdent,
    besluttetAv: NavIdent = NavAnsattFixture.MikkeMus.navIdent,
    besluttetTidspunkt: Instant = Instant.now(),
) {
    val dto = queries.utbetalingLinje.get(utbetalingLinjeDbo.id)
        ?: throw IllegalStateException("Dbo må være gitt til domain først")

    queries.utbetalingLinje.setStatus(dto.id, status)

    when (status) {
        UtbetalingLinjeStatus.TIL_ATTESTERING -> {
            setTilGodkjenning(dto.id, TotrinnskontrollType.UTBETALING_LINJE_OPPRETTELSE, behandletAv)
        }

        UtbetalingLinjeStatus.GODKJENT, UtbetalingLinjeStatus.UTBETALT, UtbetalingLinjeStatus.OVERFORT_TIL_UTBETALING -> {
            setGodkjent(
                dto.id,
                TotrinnskontrollType.UTBETALING_LINJE_OPPRETTELSE,
                behandletAv,
                besluttetAv,
                besluttetTidspunkt = besluttetTidspunkt,
            )
        }

        UtbetalingLinjeStatus.RETURNERT -> {
            setAvvist(
                dto.id,
                TotrinnskontrollType.UTBETALING_LINJE_OPPRETTELSE,
                behandletAv,
                besluttetAv,
                besluttetTidspunkt = besluttetTidspunkt,
            )
        }
    }
}
