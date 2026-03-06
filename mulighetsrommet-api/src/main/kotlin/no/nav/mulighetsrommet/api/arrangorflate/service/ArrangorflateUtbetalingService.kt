package no.nav.mulighetsrommet.api.arrangorflate.service

import arrow.core.Either
import arrow.core.left
import arrow.core.nel
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
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFri
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningPrisPerTimeOppfolging
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingInputHelper
import no.nav.mulighetsrommet.api.utbetaling.service.TidligstTidspunktForUtbetalingCalculator
import no.nav.mulighetsrommet.api.utbetaling.service.UtbetalingService
import no.nav.mulighetsrommet.model.Arrangor
import no.nav.mulighetsrommet.model.Periode
import no.nav.tiltak.okonomi.Tilskuddstype
import java.util.UUID

class ArrangorflateUtbetalingService(
    private val db: ApiDatabase,
    private val utbetalingService: UtbetalingService,
) {
    data class Config(
        val tidligstTidspunktForUtbetaling: TidligstTidspunktForUtbetalingCalculator,
    )

    suspend fun opprettUtbetaling(
        opprett: ArrangorflateOpprettUtbetaling,
    ): Either<List<FieldError>, Utbetaling> = db.session {
        val gjennomforing = queries.gjennomforing.getGjennomforingAvtaleOrError(opprett.gjennomforingId)

        val (tilskuddstype, beregning) = when (gjennomforing.prismodell) {
            is Prismodell.ForhandsgodkjentPrisPerManedsverk,
            -> Tilskuddstype.TILTAK_INVESTERINGER to UtbetalingBeregningFri.belop(opprett.pris)

            is Prismodell.AnnenAvtaltPris,
            -> Tilskuddstype.TILTAK_DRIFTSTILSKUDD to UtbetalingBeregningFri.belop(opprett.pris)

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

        return utbetalingService.opprettUtbetaling(
            opprett = OpprettUtbetaling(
                id = UUID.randomUUID(),
                gjennomforingId = gjennomforing.id,
                periodeStart = opprett.periode.start,
                periodeSlutt = opprett.periode.getLastInclusiveDate(),
                beregning = beregning,
                kid = opprett.kidNummer,
                tilskuddstype = tilskuddstype,
                vedlegg = opprett.vedlegg,
                journalpostId = null,
                kommentar = null,
                korreksjonGjelderUtbetalingId = null,
                korreksjonBegrunnelse = null,
            ),
            agent = Arrangor,
        )
    }

    fun getAvtaltPrisPerTimeOppfolgingData(gjennomforingId: UUID, periode: Periode): AvtaltPrisPerTimeOppfolgingData = db.session {
        val gjennomforing = queries.gjennomforing.getGjennomforingAvtaleOrError(gjennomforingId)
        return getAvtaltPrisPerTimeOppfolgingData(gjennomforing, periode)
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
