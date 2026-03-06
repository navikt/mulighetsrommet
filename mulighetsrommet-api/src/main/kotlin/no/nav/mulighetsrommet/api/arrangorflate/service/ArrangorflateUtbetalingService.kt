package no.nav.mulighetsrommet.api.arrangorflate.service

import arrow.core.Either
import arrow.core.left
import arrow.core.nel
import arrow.core.right
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.TransactionalQueryContext
import no.nav.mulighetsrommet.api.arrangor.ArrangorService
import no.nav.mulighetsrommet.api.arrangor.model.Betalingsinformasjon
import no.nav.mulighetsrommet.api.arrangorflate.api.OpprettKravUtbetalingRequest
import no.nav.mulighetsrommet.api.arrangorflate.model.AvtaltPrisPerTimeOppfolgingData
import no.nav.mulighetsrommet.api.arrangorflate.model.OpprettUtbetaling
import no.nav.mulighetsrommet.api.avtale.model.Prismodell
import no.nav.mulighetsrommet.api.endringshistorikk.DocumentClass
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingAvtale
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.utbetaling.db.UtbetalingDbo
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregning
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFri
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningPrisPerTimeOppfolging
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingInputHelper
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatusType
import no.nav.mulighetsrommet.api.utbetaling.service.TidligstTidspunktForUtbetalingCalculator
import no.nav.mulighetsrommet.api.utbetaling.task.JournalforUtbetaling
import no.nav.mulighetsrommet.clamav.Vedlegg
import no.nav.mulighetsrommet.model.Arrangor
import no.nav.mulighetsrommet.model.Kid
import no.nav.mulighetsrommet.model.Periode
import no.nav.tiltak.okonomi.Tilskuddstype
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

class ArrangorflateUtbetalingService(
    private val config: Config,
    private val db: ApiDatabase,
    private val arrangorService: ArrangorService,
    private val journalforUtbetaling: JournalforUtbetaling,
) {
    data class Config(
        val tidligstTidspunktForUtbetaling: TidligstTidspunktForUtbetalingCalculator,
    )

    suspend fun opprettUtbetaling(
        opprett: OpprettUtbetaling,
    ): Either<List<FieldError>, Utbetaling> = db.transaction {
        val gjennomforing = queries.gjennomforing.getGjennomforingAvtaleOrError(opprett.gjennomforingId)

        val (tilskuddstype, beregning) = when (gjennomforing.prismodell) {
            is Prismodell.ForhandsgodkjentPrisPerManedsverk,
            -> Tilskuddstype.TILTAK_INVESTERINGER to getBeregningFri(opprett)

            is Prismodell.AnnenAvtaltPris,
            -> Tilskuddstype.TILTAK_DRIFTSTILSKUDD to getBeregningFri(opprett)

            is Prismodell.AvtaltPrisPerTimeOppfolgingPerDeltaker,
            -> Tilskuddstype.TILTAK_DRIFTSTILSKUDD to getBeregningPrisPerTimeOppfolging(opprett, gjennomforing)

            is Prismodell.AvtaltPrisPerManedsverk,
            is Prismodell.AvtaltPrisPerUkesverk,
            is Prismodell.AvtaltPrisPerHeleUkesverk,
            -> return FieldError.of(
                "Kan ikke opprette utbetaling for denne tiltaksgjennomføringen",
                OpprettKravUtbetalingRequest::tilsagnId,
            ).nel().left()
        }

        return opprettUtbetaling(opprett, gjennomforing, beregning, tilskuddstype)
    }

    fun getAvtaltPrisPerTimeOppfolgingData(gjennomforingId: UUID, periode: Periode): AvtaltPrisPerTimeOppfolgingData = db.session {
        val gjennomforing = queries.gjennomforing.getGjennomforingAvtaleOrError(gjennomforingId)
        return getAvtaltPrisPerTimeOppfolgingData(gjennomforing, periode)
    }

    private fun getBeregningFri(opprett: OpprettUtbetaling): UtbetalingBeregningFri = UtbetalingBeregningFri.beregn(
        input = UtbetalingBeregningFri.Input(opprett.pris),
    )

    private fun QueryContext.getBeregningPrisPerTimeOppfolging(
        opprett: OpprettUtbetaling,
        gjennomforing: GjennomforingAvtale,
    ): UtbetalingBeregningPrisPerTimeOppfolging {
        val periode = Periode(opprett.periodeStart, opprett.periodeSlutt)
        val (satser, stengt, _, deltakelsePerioder) = getAvtaltPrisPerTimeOppfolgingData(gjennomforing, periode)
        return UtbetalingBeregningPrisPerTimeOppfolging.beregn(
            input = UtbetalingBeregningPrisPerTimeOppfolging.Input(
                satser = satser,
                pris = opprett.pris,
                stengt = stengt,
                deltakelser = deltakelsePerioder,
            ),
        )
    }

    private fun QueryContext.getAvtaltPrisPerTimeOppfolgingData(
        gjennomforing: GjennomforingAvtale,
        periode: Periode,
    ): AvtaltPrisPerTimeOppfolgingData {
        val satser = UtbetalingInputHelper.resolveAvtalteSatser(gjennomforing, periode)
        val stengtHosArrangor = UtbetalingInputHelper.resolveStengtHosArrangor(periode, gjennomforing.stengt)
        val deltakere = queries.deltaker.getByGjennomforingId(gjennomforing.id)
        val deltakelsePerioder = UtbetalingInputHelper.resolveDeltakelsePerioder(deltakere, periode)
        return AvtaltPrisPerTimeOppfolgingData(satser, stengtHosArrangor, deltakere, deltakelsePerioder)
    }

    private suspend fun TransactionalQueryContext.opprettUtbetaling(
        opprett: OpprettUtbetaling,
        gjennomforing: GjennomforingAvtale,
        beregning: UtbetalingBeregning,
        tilskuddstype: Tilskuddstype,
    ): Either<List<FieldError>, Utbetaling> {
        val periode = Periode(opprett.periodeStart, opprett.periodeSlutt)
        val utbetalesTidligstTidspunkt = config.tidligstTidspunktForUtbetaling.calculate(
            gjennomforing.tiltakstype.tiltakskode,
            periode,
        )

        val betalingsinformasjon = getUtbetalingsinformasjon(gjennomforing.arrangor.id, opprett.kidNummer)

        val utbetaling = UtbetalingDbo(
            // TODO: id burde kanskje komme fra request for å unngå doble innsendinger
            id = UUID.randomUUID(),
            gjennomforingId = opprett.gjennomforingId,
            status = UtbetalingStatusType.INNSENDT,
            betalingsinformasjon = betalingsinformasjon,
            valuta = gjennomforing.prismodell.valuta,
            beregning = beregning,
            periode = periode,
            innsender = Arrangor,
            kommentar = null,
            korreksjonGjelderUtbetalingId = null,
            korreksjonBegrunnelse = null,
            tilskuddstype = tilskuddstype,
            journalpostId = null,
            godkjentAvArrangorTidspunkt = LocalDateTime.now(),
            utbetalesTidligstTidspunkt = utbetalesTidligstTidspunkt,
            blokkeringer = emptySet(),
        )

        queries.utbetaling.upsert(utbetaling)
        scheduleJournalforUtbetaling(utbetaling.id, opprett.vedlegg)
        return logEndring("Utbetaling sendt inn", utbetaling.id).right()
    }

    private suspend fun getUtbetalingsinformasjon(arrangorId: UUID, kid: Kid?): Betalingsinformasjon {
        return when (val betalingsinformasjon = arrangorService.getBetalingsinformasjon(arrangorId)) {
            is Betalingsinformasjon.BBan -> Betalingsinformasjon.BBan(betalingsinformasjon.kontonummer, kid)
            is Betalingsinformasjon.IBan -> betalingsinformasjon
        }
    }

    private fun TransactionalQueryContext.logEndring(
        operation: String,
        utbetalingId: UUID,
        timestamp: LocalDateTime = LocalDateTime.now(),
    ): Utbetaling {
        val utbetaling = getOrError(utbetalingId)
        queries.endringshistorikk.logEndring(
            DocumentClass.UTBETALING,
            operation,
            Arrangor,
            utbetalingId,
            timestamp,
        ) {
            Json.encodeToJsonElement(utbetaling)
        }
        return utbetaling
    }

    private fun TransactionalQueryContext.scheduleJournalforUtbetaling(utbetalingId: UUID, vedlegg: List<Vedlegg>) {
        journalforUtbetaling.schedule(
            utbetalingId = utbetalingId,
            startTime = Instant.now(),
            tx = session,
            vedlegg = vedlegg,
        )
    }

    private fun QueryContext.getOrError(id: UUID): Utbetaling {
        return queries.utbetaling.getOrError(id)
    }
}
