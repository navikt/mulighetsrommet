package no.nav.mulighetsrommet.api.utbetaling

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.right
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.responses.StatusResponse
import no.nav.mulighetsrommet.api.tilsagn.OkonomiBestillingService
import no.nav.mulighetsrommet.api.tilsagn.model.ForhandsgodkjenteSatser
import no.nav.mulighetsrommet.api.utbetaling.db.DelutbetalingDbo
import no.nav.mulighetsrommet.api.utbetaling.db.UtbetalingDbo
import no.nav.mulighetsrommet.api.utbetaling.model.*
import no.nav.mulighetsrommet.ktor.exception.Forbidden
import no.nav.mulighetsrommet.ktor.exception.NotFound
import no.nav.mulighetsrommet.model.DeltakerStatus
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode
import java.time.LocalDate
import java.util.*

class UtbetalingService(
    private val db: ApiDatabase,
    private val okonomi: OkonomiBestillingService,
) {
    fun genererUtbetalingForMonth(date: LocalDate): List<UtbetalingDto> = db.transaction {
        val periode = Periode.forMonthOf(date)

        queries.gjennomforing
            .getGjennomforesInPeriodeUtenUtbetaling(periode)
            .mapNotNull { gjennomforing ->
                val utbetaling = when (gjennomforing.tiltakstype.tiltakskode) {
                    Tiltakskode.ARBEIDSFORBEREDENDE_TRENING -> createUtbetalingAft(
                        utbetalingId = UUID.randomUUID(),
                        gjennomforingId = gjennomforing.id,
                        periode = periode,
                    )

                    else -> null
                }

                utbetaling?.takeIf { it.beregning.output.belop > 0 }
            }
            .map { utbetaling ->
                queries.utbetaling.upsert(utbetaling)
                requireNotNull(queries.utbetaling.get(utbetaling.id)) { "Utbetaling forventet siden det nettopp ble opprettet" }
            }
    }

    fun recalculateUtbetalingForGjennomforing(id: UUID): Unit = db.transaction {
        queries.utbetaling
            .getByGjennomforing(id, statuser = listOf(UtbetalingStatus.KLAR_FOR_GODKJENNING))
            .mapNotNull { gjeldendeKrav ->
                val nyttKrav = when (gjeldendeKrav.beregning) {
                    is UtbetalingBeregningAft -> createUtbetalingAft(
                        utbetalingId = gjeldendeKrav.id,
                        gjennomforingId = gjeldendeKrav.gjennomforing.id,
                        periode = gjeldendeKrav.beregning.input.periode,
                    )

                    is UtbetalingBeregningFri -> null
                }

                nyttKrav?.takeIf { it.beregning != gjeldendeKrav.beregning }
            }
            .forEach { utbetaling ->
                queries.utbetaling.upsert(utbetaling)
            }
    }

    fun createUtbetalingAft(
        utbetalingId: UUID,
        gjennomforingId: UUID,
        periode: Periode,
    ): UtbetalingDbo {
        val frist = periode.slutt.plusMonths(2)

        val deltakere = getDeltakelser(gjennomforingId, periode)

        // TODO: burde også verifisere at start og slutt har samme pris
        val sats = ForhandsgodkjenteSatser.findSats(Tiltakskode.ARBEIDSFORBEREDENDE_TRENING, periode.start)
            ?: throw IllegalStateException("Sats mangler for periode $periode")

        val input = UtbetalingBeregningAft.Input(
            periode = periode,
            sats = sats,
            deltakelser = deltakere,
        )

        val beregning = UtbetalingBeregningAft.beregn(input)

        val forrigeKrav = db.session {
            queries.utbetaling.getSisteGodkjenteUtbetaling(gjennomforingId)
        }

        return UtbetalingDbo(
            id = utbetalingId,
            fristForGodkjenning = frist.atStartOfDay(),
            gjennomforingId = gjennomforingId,
            beregning = beregning,
            kontonummer = forrigeKrav?.betalingsinformasjon?.kontonummer,
            kid = forrigeKrav?.betalingsinformasjon?.kid,
            periode = periode,
        )
    }

    fun upsertDelutbetaling(
        utbetalingId: UUID,
        request: DelutbetalingRequest,
        opprettetAv: NavIdent,
    ): Either<List<FieldError>, Unit> = either {
        val utbetaling = db.session { queries.utbetaling.get(utbetalingId) }
            ?: return listOf(FieldError.root("Utbetaling med id=$utbetalingId finnes ikke")).left()
        val tilsagn = db.session { queries.tilsagn.get(request.tilsagnId) }
            ?: return listOf(FieldError.root("Tilsagn med id=${request.tilsagnId} finnes ikke")).left()

        val previous = db.session { queries.delutbetaling.get(utbetalingId, request.tilsagnId) }
        when (previous) {
            is DelutbetalingDto.DelutbetalingGodkjent, is DelutbetalingDto.DelutbetalingTilGodkjenning ->
                return listOf(FieldError.root("Utbetaling kan ikke endres")).left()
            is DelutbetalingDto.DelutbetalingAvvist, null -> {}
        }

        val maxBelop = utbetaling.beregning.output.belop -
            db.session { queries.delutbetaling.getByUtbetalingId(utbetalingId) }
                .filter { it.tilsagnId != tilsagn.id }
                .sumOf { it.belop }

        UtbetalingValidator.validate(belop = request.belop, tilsagn = tilsagn, maxBelop = maxBelop).bind()

        val periode = utbetaling.periode.intersect(Periode.fromInclusiveDates(tilsagn.periodeStart, tilsagn.periodeSlutt))
            ?: return listOf(FieldError.root("Utbetalingsperiode og tilsagnsperiode overlapper ikke")).left()

        val lopenummer = db.session { queries.delutbetaling.getNextLopenummerByTilsagn(tilsagn.id) }
        val dbo = DelutbetalingDbo(
            utbetalingId = utbetaling.id,
            tilsagnId = tilsagn.id,
            periode = periode,
            belop = request.belop,
            opprettetAv = opprettetAv,
            lopenummer = lopenummer,
            fakturanummer = "${tilsagn.bestillingsnummer}/$lopenummer",
        )

        db.session {
            queries.delutbetaling.upsert(dbo)
        }
    }

    fun besluttDelutbetaling(
        request: BesluttDelutbetalingRequest,
        utbetalingId: UUID,
        navIdent: NavIdent,
    ): StatusResponse<Unit> = db.transaction {
        val delutbetaling = queries.delutbetaling.get(utbetalingId, request.tilsagnId)
            ?: return NotFound("Delutbetaling finnes ikke").left()

        if (delutbetaling.opprettetAv == navIdent) {
            return Forbidden("Kan ikke beslutte egen utbetaling").left()
        }

        when (request) {
            is BesluttDelutbetalingRequest.AvvistDelutbetalingRequest ->
                queries.delutbetaling.avvis(
                    utbetalingId = utbetalingId,
                    navIdent = navIdent,
                    request = request,
                )
            is BesluttDelutbetalingRequest.GodkjentDelutbetalingRequest -> {
                queries.delutbetaling.godkjenn(
                    utbetalingId = utbetalingId,
                    tilsagnId = request.tilsagnId,
                    navIdent = navIdent,
                )
                okonomi.scheduleBehandleGodkjentUtbetaling(utbetalingId, request.tilsagnId, session)
            }
        }

        return Unit.right()
    }

    private fun getDeltakelser(
        gjennomforingId: UUID,
        periode: Periode,
    ): Set<DeltakelsePerioder> {
        val deltakelser = db.session {
            queries.deltaker.getAll(gjennomforingId = gjennomforingId)
        }

        return deltakelser
            .asSequence()
            .filter {
                it.status.type in listOf(
                    DeltakerStatus.Type.AVBRUTT,
                    DeltakerStatus.Type.DELTAR,
                    DeltakerStatus.Type.HAR_SLUTTET,
                    DeltakerStatus.Type.FULLFORT,
                )
            }
            .filter { it.deltakelsesprosent != null }
            .filter {
                it.startDato != null && it.startDato.isBefore(periode.slutt)
            }
            .filter {
                it.sluttDato == null || it.sluttDato.plusDays(1).isAfter(periode.start)
            }
            .map { deltakelse ->
                val start = maxOf(requireNotNull(deltakelse.startDato), periode.start)
                val slutt = minOf(deltakelse.sluttDato?.plusDays(1) ?: periode.slutt, periode.slutt)
                val deltakelsesprosent = requireNotNull(deltakelse.deltakelsesprosent) {
                    "deltakelsesprosent mangler for deltakelse id=${deltakelse.id}"
                }

                // TODO: periodisering av prosent - fra Komet
                val perioder = listOf(DeltakelsePeriode(start, slutt, deltakelsesprosent))

                DeltakelsePerioder(
                    deltakelseId = deltakelse.id,
                    perioder = perioder,
                )
            }
            .toSet()
    }
}
