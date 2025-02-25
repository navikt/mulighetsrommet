package no.nav.mulighetsrommet.api.utbetaling

import arrow.core.left
import arrow.core.right
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotliquery.TransactionalSession
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.arrangorflate.GodkjennUtbetaling
import no.nav.mulighetsrommet.api.endringshistorikk.DocumentClass
import no.nav.mulighetsrommet.api.endringshistorikk.EndretAv
import no.nav.mulighetsrommet.api.responses.StatusResponse
import no.nav.mulighetsrommet.api.responses.ValidationError
import no.nav.mulighetsrommet.api.tilsagn.OkonomiBestillingService
import no.nav.mulighetsrommet.api.tilsagn.model.Besluttelse
import no.nav.mulighetsrommet.api.tilsagn.model.ForhandsgodkjenteSatser
import no.nav.mulighetsrommet.api.totrinnskontroll.db.TotrinnskontrollType
import no.nav.mulighetsrommet.api.utbetaling.db.DelutbetalingDbo
import no.nav.mulighetsrommet.api.utbetaling.db.UtbetalingDbo
import no.nav.mulighetsrommet.api.utbetaling.model.*
import no.nav.mulighetsrommet.api.utbetaling.task.JournalforUtbetaling
import no.nav.mulighetsrommet.ktor.exception.BadRequest
import no.nav.mulighetsrommet.ktor.exception.Forbidden
import no.nav.mulighetsrommet.ktor.exception.InternalServerError
import no.nav.mulighetsrommet.ktor.exception.NotFound
import no.nav.mulighetsrommet.model.DeltakerStatus
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class UtbetalingService(
    private val db: ApiDatabase,
    private val okonomi: OkonomiBestillingService,
    private val journalforUtbetaling: JournalforUtbetaling,
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
                val dto = getOrError(utbetaling.id)
                logEndring("Utbetaling opprettet", dto, EndretAv.System)
                dto
            }
    }

    fun recalculateUtbetalingForGjennomforing(id: UUID): Unit = db.transaction {
        queries.utbetaling
            .getByGjennomforing(id)
            .filter { it.status == UtbetalingStatus.KLAR_FOR_GODKJENNING }
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
                val dto = getOrError(utbetaling.id)
                logEndring("Utbetaling beregning oppdatert", dto, EndretAv.System)
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
            innsender = null,
        )
    }

    fun godkjentAvArrangor(
        utbetalingId: UUID,
        request: GodkjennUtbetaling,
    ) = db.transaction {
        queries.utbetaling.setGodkjentAvArrangor(utbetalingId, LocalDateTime.now())
        queries.utbetaling.setBetalingsInformasjon(
            utbetalingId,
            request.betalingsinformasjon.kontonummer,
            request.betalingsinformasjon.kid,
        )
        val dto = getOrError(utbetalingId)
        logEndring("Utbetaling sendt inn", dto, EndretAv.Arrangor)
        journalforUtbetaling.schedule(utbetalingId, Instant.now(), session as TransactionalSession)
    }

    fun opprettManuellUtbetaling(
        utbetalingId: UUID,
        request: OpprettManuellUtbetalingRequest,
        navIdent: NavIdent,
    ) {
        db.transaction {
            queries.utbetaling.upsert(
                UtbetalingDbo(
                    id = utbetalingId,
                    gjennomforingId = request.gjennomforingId,
                    fristForGodkjenning = request.periode.slutt.plusMonths(2).atStartOfDay(),
                    kontonummer = request.kontonummer,
                    kid = request.kidNummer,
                    beregning = UtbetalingBeregningFri.beregn(
                        input = UtbetalingBeregningFri.Input(
                            belop = request.belop,
                        ),
                    ),
                    periode = Periode.fromInclusiveDates(
                        request.periode.start,
                        request.periode.slutt,
                    ),
                    innsender = UtbetalingDto.Innsender.NavAnsatt(navIdent),
                ),
            )
            val dto = getOrError(utbetalingId)
            logEndring("Utbetaling sendt inn", dto, EndretAv.NavAnsatt(navIdent))
        }
    }

    fun upsertDelutbetaling(
        utbetalingId: UUID,
        request: DelutbetalingRequest,
        opprettetAv: NavIdent,
    ): StatusResponse<Unit> = db.transaction {
        val utbetaling = queries.utbetaling.get(utbetalingId)
            ?: return NotFound("Utbetaling med id=$utbetalingId finnes ikke").left()
        val tilsagn = queries.tilsagn.get(request.tilsagnId)
            ?: return NotFound("Tilsagn med id=${request.tilsagnId} finnes ikke").left()

        val previous = queries.delutbetaling.get(utbetalingId, request.tilsagnId)
        when (previous) {
            is DelutbetalingDto.DelutbetalingOverfortTilUtbetaling,
            is DelutbetalingDto.DelutbetalingTilGodkjenning,
            is DelutbetalingDto.DelutbetalingUtbetalt,
            -> return BadRequest("Utbetaling kan ikke endres").left()

            is DelutbetalingDto.DelutbetalingAvvist, null -> {}
        }

        val utbetaltBelop = queries.delutbetaling.getByUtbetalingId(utbetalingId)
            .filter { it.tilsagnId != tilsagn.id }
            .sumOf { it.belop }
        val gjenstaendeBelop = utbetaling.beregning.output.belop - utbetaltBelop

        UtbetalingValidator.validate(belop = request.belop, tilsagn = tilsagn, maxBelop = gjenstaendeBelop)
            .onLeft { return ValidationError(errors = it).left() }

        val tilsagnPeriode = Periode.fromInclusiveDates(tilsagn.periodeStart, tilsagn.periodeSlutt)
        val periode = utbetaling.periode.intersect(tilsagnPeriode)
            ?: return InternalServerError("Utbetalingsperiode og tilsagnsperiode overlapper ikke").left()

        val lopenummer = queries.delutbetaling.getNextLopenummerByTilsagn(tilsagn.id)
        val dbo = DelutbetalingDbo(
            id = request.id,
            utbetalingId = utbetaling.id,
            tilsagnId = tilsagn.id,
            periode = periode,
            belop = request.belop,
            opprettetAv = opprettetAv,
            lopenummer = lopenummer,
            fakturanummer = "${tilsagn.bestillingsnummer}-$lopenummer",
        )

        queries.delutbetaling.upsert(dbo)
        val dto = getOrError(utbetalingId)
        logEndring("Utbetaling sendt til godkjenning", dto, EndretAv.NavAnsatt(opprettetAv))

        return Unit.right()
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

        if (delutbetaling is DelutbetalingDto.DelutbetalingOverfortTilUtbetaling) {
            return BadRequest("Utbetaling er allerede besluttet").left()
        }

        when (request) {
            is BesluttDelutbetalingRequest.AvvistDelutbetalingRequest ->
                queries.totrinnskontroll.beslutter(
                    entityId = delutbetaling.id,
                    navIdent = navIdent,
                    besluttelse = Besluttelse.AVVIST,
                    type = TotrinnskontrollType.OPPRETT,
                    aarsaker = request.aarsaker,
                    forklaring = request.forklaring,
                    tidspunkt = LocalDateTime.now(),
                )
            is BesluttDelutbetalingRequest.GodkjentDelutbetalingRequest -> {
                queries.totrinnskontroll.beslutter(
                    entityId = delutbetaling.id,
                    navIdent = navIdent,
                    besluttelse = Besluttelse.GODKJENT,
                    type = TotrinnskontrollType.OPPRETT,
                    aarsaker = null,
                    forklaring = null,
                    tidspunkt = LocalDateTime.now(),
                )
                okonomi.scheduleBehandleGodkjenteUtbetalinger(request.tilsagnId, session)
            }
        }
        val dto = getOrError(utbetalingId)
        logEndring(
            "Utbetaling ${if (request.besluttelse == Besluttelse.GODKJENT) "godkjent" else "returnert"}",
            dto,
            EndretAv.NavAnsatt(navIdent),
        )

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

    private fun QueryContext.logEndring(
        operation: String,
        dto: UtbetalingDto,
        endretAv: EndretAv,
    ) {
        queries.endringshistorikk.logEndring(
            DocumentClass.UTBETALING,
            operation,
            endretAv,
            dto.id,
        ) {
            Json.encodeToJsonElement(dto)
        }
    }

    private fun QueryContext.getOrError(id: UUID): UtbetalingDto {
        return requireNotNull(queries.utbetaling.get(id)) { "Utbetaling med id=$id finnes ikke" }
    }
}
