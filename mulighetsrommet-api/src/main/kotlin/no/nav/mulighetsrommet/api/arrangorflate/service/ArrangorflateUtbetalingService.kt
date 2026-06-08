package no.nav.mulighetsrommet.api.arrangorflate.service

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.nel
import arrow.core.right
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.TransactionalQueryContext
import no.nav.mulighetsrommet.api.arrangorflate.model.ArrangorflateOpprettUtbetaling
import no.nav.mulighetsrommet.api.arrangorflate.model.AvtaltPrisPerTimeOppfolgingData
import no.nav.mulighetsrommet.api.avtale.model.Prismodell
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingAvtale
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.utbetaling.model.AutomatisertUtbetalingResult
import no.nav.mulighetsrommet.api.utbetaling.model.UpsertUtbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregning
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningAvtaltPrisPerBenyttetPlassPerHeleUke
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningAvtaltPrisPerBenyttetPlassPerManed
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningAvtaltPrisPerBenyttetPlassPerUke
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningAvtaltPrisPerTimeOppfolging
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFastSatsPerAvtaltTiltaksplassPerManed
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFastSatsPerBenyttetPlassPerManed
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFri
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingException
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingInputHelper
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatusType
import no.nav.mulighetsrommet.api.utbetaling.service.GenererUtbetalingService
import no.nav.mulighetsrommet.api.utbetaling.service.UtbetalingService
import no.nav.mulighetsrommet.api.utbetaling.task.JournalforUtbetaling
import no.nav.mulighetsrommet.api.validation.validation
import no.nav.mulighetsrommet.clamav.Vedlegg
import no.nav.mulighetsrommet.model.Arrangor
import no.nav.mulighetsrommet.model.Kid
import no.nav.mulighetsrommet.model.Periode
import no.nav.tiltak.okonomi.Tilskuddstype
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.UUID

class ArrangorflateUtbetalingService(
    private val db: ApiDatabase,
    private val utbetalingService: UtbetalingService,
    private val genererUtbetalingService: GenererUtbetalingService,
    private val journalforUtbetaling: JournalforUtbetaling,
) {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    fun getUtbetaling(id: UUID): Utbetaling? = db.session {
        return queries.utbetaling.get(id)
    }

    fun getAvtaltPrisPerTimeOppfolgingData(gjennomforingId: UUID, periode: Periode): AvtaltPrisPerTimeOppfolgingData = db.session {
        val gjennomforing = queries.gjennomforing.getGjennomforingAvtaleOrError(gjennomforingId)
        return getAvtaltPrisPerTimeOppfolgingData(gjennomforing, periode)
    }

    suspend fun opprettUtbetaling(
        opprett: ArrangorflateOpprettUtbetaling,
    ): Either<List<FieldError>, Utbetaling> {
        return beregnUtbetaling(opprett).flatMap { (tilskuddstype, beregning) ->
            val utbetaling = UpsertUtbetaling.Innsending(
                id = UUID.randomUUID(),
                gjennomforingId = opprett.gjennomforingId,
                periode = opprett.periode,
                beregning = beregning,
                kid = opprett.kidNummer,
                tilskuddstype = tilskuddstype,
            )
            db.transaction {
                scheduleJournalforUtbetaling(utbetaling.id, opprett.vedlegg)
                utbetalingService.opprettUtbetaling(utbetaling, Arrangor)
            }
        }
    }

    fun godkjentAvArrangor(
        utbetalingId: UUID,
        kid: Kid?,
        today: LocalDate = LocalDate.now(),
    ): Either<List<FieldError>, AutomatisertUtbetalingResult> {
        val result = db.transaction {
            val utbetaling = getOrError(utbetalingId)
            if (utbetaling.periode.slutt > today) {
                return FieldError.of("Utbetalingen kan ikke godkjennes før perioden er passert").nel().left()
            }

            if (utbetaling.betalingsinformasjon == null) {
                return FieldError.of("Utbetalingen kan ikke godkjennes fordi kontonummer mangler.").nel().left()
            }

            val advarsler = utbetalingService.getAdvarsler(utbetaling)
            if (utbetaling.blokkeringer.isNotEmpty() || advarsler.isNotEmpty()) {
                return FieldError.of("Det finnes advarsler på deltakere som påvirker utbetalingen. Disse må fikses før utbetalingen kan sendes inn.")
                    .nel()
                    .left()
            }
            scheduleJournalforUtbetaling(utbetalingId, listOf())
            utbetalingService.godkjentAvArrangor(utbetaling.id, kid)
        }

        return result.map { tryAutomatisertUtbetaling(it) }
    }

    fun avbrytUtbetaling(
        utbetalingId: UUID,
        begrunnelse: String,
    ): Either<List<FieldError>, Utbetaling> = db.transaction {
        val utbetaling = getOrError(utbetalingId)
        if (arrangorAvbrytStatus(utbetaling) != ArrangorAvbrytStatus.ACTIVATED) {
            return FieldError.of("Utbetalingen kan ikke avbrytes").nel().left()
        }

        utbetalingService.avbrytUtbetaling(utbetaling.id, begrunnelse, Arrangor)
    }

    suspend fun regenererUtbetaling(
        utbetaling: Utbetaling,
    ): Either<List<FieldError>, Utbetaling> = validation {
        validate(utbetaling.status == UtbetalingStatusType.AVBRUTT) {
            FieldError.of("Utbetalingen kan bare regenereres når den er avbrutt")
        }
        validateNotNull(utbetaling.innsending) {
            FieldError.of("Utbetalingen kan bare regenereres når den er innsendt")
        }
        when (utbetaling.beregning) {
            is UtbetalingBeregningFri,
            is UtbetalingBeregningFastSatsPerAvtaltTiltaksplassPerManed,
            is UtbetalingBeregningAvtaltPrisPerTimeOppfolging,
            -> error { FieldError.of("Utbetalingen kan ikke regenereres") }

            is UtbetalingBeregningFastSatsPerBenyttetPlassPerManed,
            is UtbetalingBeregningAvtaltPrisPerBenyttetPlassPerHeleUke,
            is UtbetalingBeregningAvtaltPrisPerBenyttetPlassPerManed,
            is UtbetalingBeregningAvtaltPrisPerBenyttetPlassPerUke,
            -> Unit
        }
    }.map {
        genererUtbetalingService.regenererUtbetaling(utbetaling)
    }

    private fun QueryContext.getOrError(id: UUID): Utbetaling {
        return queries.utbetaling.getOrError(id)
    }

    private fun beregnUtbetaling(
        opprett: ArrangorflateOpprettUtbetaling,
    ): Either<List<FieldError>, Pair<Tilskuddstype, UtbetalingBeregning>> = db.session {
        val gjennomforing = queries.gjennomforing.getGjennomforingAvtaleOrError(opprett.gjennomforingId)
        return when (gjennomforing.prismodell) {
            is Prismodell.FastSatsPerBenyttetPlassPerManed,
            -> Pair(Tilskuddstype.TILTAK_INVESTERINGER, UtbetalingBeregningFri.from(opprett.pris)).right()

            is Prismodell.AnnenAvtaltPris,
            -> Pair(Tilskuddstype.TILTAK_DRIFTSTILSKUDD, UtbetalingBeregningFri.from(opprett.pris)).right()

            is Prismodell.AvtaltPrisPerTimeOppfolgingPerDeltaker,
            -> Pair(
                Tilskuddstype.TILTAK_DRIFTSTILSKUDD,
                getBeregningPrisPerTimeOppfolging(opprett, gjennomforing),
            ).right()

            is Prismodell.AvtaltPrisPerBenyttetPlassPerManed,
            is Prismodell.AvtaltPrisPerBenyttetPlassPerUke,
            is Prismodell.AvtaltPrisPerBenyttetPlassPerHeleUke,
            is Prismodell.FastSatsPerAvtaltPlassPerManed,
            is Prismodell.TilskuddTilOpplaering,
            is Prismodell.IngenKostnader,
            -> FieldError.of("Kan ikke opprette utbetaling for denne tiltaksgjennomføringen").nel().left()
        }
    }

    private fun QueryContext.getBeregningPrisPerTimeOppfolging(
        opprett: ArrangorflateOpprettUtbetaling,
        gjennomforing: GjennomforingAvtale,
    ): UtbetalingBeregningAvtaltPrisPerTimeOppfolging {
        val (satser, stengt, _, deltakelser) = getAvtaltPrisPerTimeOppfolgingData(gjennomforing, opprett.periode)
        return UtbetalingBeregningAvtaltPrisPerTimeOppfolging.from(satser, stengt, deltakelser, opprett.pris)
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

    private fun TransactionalQueryContext.scheduleJournalforUtbetaling(utbetalingId: UUID, vedlegg: List<Vedlegg>) {
        journalforUtbetaling.schedule(JournalforUtbetaling.TaskData(utbetalingId, vedlegg), session)
    }

    private fun tryAutomatisertUtbetaling(utbetaling: Utbetaling): AutomatisertUtbetalingResult {
        return try {
            when (utbetaling.beregning) {
                is UtbetalingBeregningFri,
                is UtbetalingBeregningAvtaltPrisPerBenyttetPlassPerManed,
                is UtbetalingBeregningAvtaltPrisPerBenyttetPlassPerUke,
                is UtbetalingBeregningAvtaltPrisPerBenyttetPlassPerHeleUke,
                is UtbetalingBeregningAvtaltPrisPerTimeOppfolging,
                is UtbetalingBeregningFastSatsPerAvtaltTiltaksplassPerManed,
                -> AutomatisertUtbetalingResult.FEIL_PRISMODELL

                is UtbetalingBeregningFastSatsPerBenyttetPlassPerManed -> db.transaction {
                    utbetalingService.automatisertUtbetalingVedEttRelevantTilsagn(utbetaling.id)
                }
            }.also { result ->
                log.info("Automatisert utbetaling for utbetaling=${utbetaling.id} resulterte i: $result")
            }
        } catch (error: UtbetalingException) {
            log.error("Uventet valideringsfeil oppsto under utbetaling: ${error.errors}")
            AutomatisertUtbetalingResult.VALIDERINGSFEIL
        }
    }
}
