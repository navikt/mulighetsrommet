package no.nav.mulighetsrommet.api.arrangorflate.service

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.nel
import arrow.core.right
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.arrangorflate.api.OpprettKravUtbetalingRequest
import no.nav.mulighetsrommet.api.arrangorflate.model.ArrangorflateOpprettUtbetaling
import no.nav.mulighetsrommet.api.arrangorflate.model.AvtaltPrisPerTimeOppfolgingData
import no.nav.mulighetsrommet.api.avtale.model.Prismodell
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingAvtale
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.utbetaling.model.OpprettUtbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregning
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFri
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningPrisPerTimeOppfolging
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingInputHelper
import no.nav.mulighetsrommet.api.utbetaling.service.UtbetalingService
import no.nav.mulighetsrommet.model.Arrangor
import no.nav.mulighetsrommet.model.Periode
import no.nav.tiltak.okonomi.Tilskuddstype
import java.util.UUID

class ArrangorflateUtbetalingService(
    private val db: ApiDatabase,
    private val utbetalingService: UtbetalingService,
) {
    suspend fun opprettUtbetaling(
        opprett: ArrangorflateOpprettUtbetaling,
    ): Either<List<FieldError>, Utbetaling> {
        return beregnUtbetaling(opprett).flatMap { (tilskuddstype, beregning) ->
            val utbetaling = OpprettUtbetaling(
                id = UUID.randomUUID(),
                gjennomforingId = opprett.gjennomforingId,
                periode = opprett.periode,
                beregning = beregning,
                kid = opprett.kidNummer,
                tilskuddstype = tilskuddstype,
                vedlegg = opprett.vedlegg,
                journalpostId = null,
                kommentar = null,
                korreksjonGjelderUtbetalingId = null,
                korreksjonBegrunnelse = null,
            )
            utbetalingService.opprettUtbetaling(utbetaling, Arrangor)
        }
    }

    fun getAvtaltPrisPerTimeOppfolgingData(gjennomforingId: UUID, periode: Periode): AvtaltPrisPerTimeOppfolgingData = db.session {
        val gjennomforing = queries.gjennomforing.getGjennomforingAvtaleOrError(gjennomforingId)
        return getAvtaltPrisPerTimeOppfolgingData(gjennomforing, periode)
    }

    private fun beregnUtbetaling(
        opprett: ArrangorflateOpprettUtbetaling,
    ): Either<List<FieldError>, Pair<Tilskuddstype, UtbetalingBeregning>> = db.session {
        val gjennomforing = queries.gjennomforing.getGjennomforingAvtaleOrError(opprett.gjennomforingId)
        return when (gjennomforing.prismodell) {
            is Prismodell.ForhandsgodkjentPrisPerManedsverk,
            -> Pair(Tilskuddstype.TILTAK_INVESTERINGER, UtbetalingBeregningFri.belop(opprett.pris)).right()

            is Prismodell.AnnenAvtaltPris,
            -> Pair(Tilskuddstype.TILTAK_DRIFTSTILSKUDD, UtbetalingBeregningFri.belop(opprett.pris)).right()

            is Prismodell.AvtaltPrisPerTimeOppfolgingPerDeltaker,
            -> Pair(
                Tilskuddstype.TILTAK_DRIFTSTILSKUDD,
                getBeregningPrisPerTimeOppfolging(opprett, gjennomforing),
            ).right()

            is Prismodell.AvtaltPrisPerManedsverk,
            is Prismodell.AvtaltPrisPerUkesverk,
            is Prismodell.AvtaltPrisPerHeleUkesverk,
            -> FieldError.of(
                "Kan ikke opprette utbetaling for denne tiltaksgjennomføringen",
                OpprettKravUtbetalingRequest::tilsagnId,
            ).nel().left()
        }
    }

    private fun QueryContext.getBeregningPrisPerTimeOppfolging(
        opprett: ArrangorflateOpprettUtbetaling,
        gjennomforing: GjennomforingAvtale,
    ): UtbetalingBeregningPrisPerTimeOppfolging {
        val (satser, stengt, _, deltakelsePerioder) = getAvtaltPrisPerTimeOppfolgingData(gjennomforing, opprett.periode)
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
}
