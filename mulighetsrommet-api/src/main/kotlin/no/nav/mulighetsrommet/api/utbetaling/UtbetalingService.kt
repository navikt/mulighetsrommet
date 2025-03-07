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
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingDto
import no.nav.mulighetsrommet.api.responses.StatusResponse
import no.nav.mulighetsrommet.api.responses.ValidationError
import no.nav.mulighetsrommet.api.tilsagn.OkonomiBestillingService
import no.nav.mulighetsrommet.api.tilsagn.model.*
import no.nav.mulighetsrommet.api.utbetaling.db.DelutbetalingDbo
import no.nav.mulighetsrommet.api.utbetaling.db.UtbetalingDbo
import no.nav.mulighetsrommet.api.utbetaling.model.*
import no.nav.mulighetsrommet.api.utbetaling.task.JournalforUtbetaling
import no.nav.mulighetsrommet.ktor.exception.BadRequest
import no.nav.mulighetsrommet.ktor.exception.NotFound
import no.nav.mulighetsrommet.model.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class UtbetalingService(
    private val db: ApiDatabase,
    private val okonomi: OkonomiBestillingService,
    private val journalforUtbetaling: JournalforUtbetaling,
) {
    private val log: Logger = LoggerFactory.getLogger(javaClass.simpleName)

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
                logEndring("Utbetaling opprettet", dto, Tiltaksadministrasjon)
                dto
            }
    }

    fun recalculateUtbetalingForGjennomforing(id: UUID): Unit = db.transaction {
        queries.utbetaling
            .getByGjennomforing(id)
            .filter { it.innsender == null }
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
                logEndring("Utbetaling beregning oppdatert", dto, Tiltaksadministrasjon)
            }
    }

    fun createUtbetalingAft(
        utbetalingId: UUID,
        gjennomforingId: UUID,
        periode: Periode,
    ): UtbetalingDbo = db.session {
        val frist = periode.slutt.plusMonths(2)

        // TODO: burde også verifisere at start og slutt har samme pris
        val sats = ForhandsgodkjenteSatser.findSats(Tiltakskode.ARBEIDSFORBEREDENDE_TRENING, periode.start)
            ?: throw IllegalStateException("Sats mangler for periode $periode")

        val gjennomforing = requireNotNull(queries.gjennomforing.get(gjennomforingId))
        val stengtHosArrangor = resolveStengtHosArrangor(periode, gjennomforing.stengt)

        val deltakelser = resolveDeltakelser(gjennomforingId, periode)

        val input = UtbetalingBeregningAft.Input(
            periode = periode,
            sats = sats,
            stengt = stengtHosArrangor,
            deltakelser = deltakelser,
        )

        val beregning = UtbetalingBeregningAft.beregn(input)

        val forrigeKrav = queries.utbetaling.getSisteGodkjenteUtbetaling(gjennomforingId)

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
        queries.utbetaling.setBetalingsinformasjon(
            utbetalingId,
            request.betalingsinformasjon.kontonummer,
            request.betalingsinformasjon.kid,
        )
        val dto = getOrError(utbetalingId)
        logEndring("Utbetaling sendt inn", dto, Arrangor)
        journalforUtbetaling.schedule(utbetalingId, Instant.now(), session as TransactionalSession)
        automatiskUtbetaling(utbetalingId)
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
            logEndring("Utbetaling sendt inn", dto, navIdent)
        }
    }

    fun opprettDelutbetalinger(
        request: OpprettDelutbetalingerRequest,
        navIdent: NavIdent,
    ): StatusResponse<Unit> = db.transaction {
        val utbetaling = queries.utbetaling.get(request.utbetalingId)
            ?: return NotFound("Utbetaling med id=$request.utbetalingId finnes ikke").left()

        request.delutbetalinger.map { req ->
            validateAndUpsertDelutbetaling(req, utbetaling, navIdent).onLeft { return it.left() }
        }
        return Unit.right()
    }

    fun besluttDelutbetaling(
        id: UUID,
        request: BesluttDelutbetalingRequest,
        navIdent: NavIdent,
    ) = db.transaction {
        val delutbetaling = queries.delutbetaling.get(id)
            ?: throw IllegalArgumentException("Delutbetaling finnes ikke")
        when (request) {
            is BesluttDelutbetalingRequest.AvvistDelutbetalingRequest -> {
                avvisDelutbetaling(delutbetaling, request.aarsaker, request.forklaring, navIdent)
            }

            is BesluttDelutbetalingRequest.GodkjentDelutbetalingRequest -> {
                godkjennDelutbetaling(delutbetaling, navIdent)
            }
        }
    }

    fun getUtbetalingKompakt(id: UUID): AdminUtbetalingKompakt = db.session {
        val utbetaling = queries.utbetaling.get(id) ?: throw NoSuchElementException("Utbetaling id=$id finnes ikke")
        toAdminUtbetalingKompakt(utbetaling)
    }

    fun getUtbetalingKompaktByGjennomforing(id: UUID): List<AdminUtbetalingKompakt> = db.session {
        queries.utbetaling.getByGjennomforing(id).map { utbetaling -> toAdminUtbetalingKompakt(utbetaling) }
    }

    private fun QueryContext.validateAndUpsertDelutbetaling(
        request: DelutbetalingRequest,
        utbetaling: UtbetalingDto,
        navIdent: NavIdent,
    ): StatusResponse<Unit> {
        val tilsagn = queries.tilsagn.get(request.tilsagnId)
            ?: return NotFound("Tilsagn med id=${request.tilsagnId} finnes ikke").left()

        val previous = queries.delutbetaling.get(request.id)
        when (previous) {
            is DelutbetalingDto.DelutbetalingOverfortTilUtbetaling,
            is DelutbetalingDto.DelutbetalingTilGodkjenning,
            is DelutbetalingDto.DelutbetalingUtbetalt,
            -> return BadRequest("Utbetaling kan ikke endres").left()

            is DelutbetalingDto.DelutbetalingAvvist, null -> {}
        }

        val utbetaltBelop = queries.delutbetaling.getByUtbetalingId(utbetaling.id)
            .filter { it.tilsagnId != tilsagn.id }
            .sumOf { it.belop }
        val gjenstaendeBelop = utbetaling.beregning.output.belop - utbetaltBelop

        UtbetalingValidator.validate(belop = request.belop, tilsagn = tilsagn, maxBelop = gjenstaendeBelop)
            .onLeft { return ValidationError(errors = it).left() }

        upsertDelutbetaling(utbetaling, tilsagn, request.id, request.belop, request.frigjorTilsagn, navIdent)
        return Unit.right()
    }

    private fun QueryContext.toAdminUtbetalingKompakt(utbetaling: UtbetalingDto): AdminUtbetalingKompakt {
        val delutbetalinger = queries.delutbetaling.getByUtbetalingId(utbetaling.id)
        val status = AdminUtbetalingStatus.fromUtbetaling(utbetaling, delutbetalinger)
        return AdminUtbetalingKompakt.fromUtbetalingDto(utbetaling, status)
    }

    // TODO: returner årsak til hvorfor utbetaling ikke ble utført slik at dette kan assertes i tester
    private fun QueryContext.automatiskUtbetaling(utbetalingId: UUID): Boolean {
        val utbetaling = requireNotNull(queries.utbetaling.get(utbetalingId)) {
            "Fant ikke utbetaling med id=$utbetalingId"
        }
        if (utbetaling.tiltakstype.tiltakskode !in listOf(Tiltakskode.ARBEIDSFORBEREDENDE_TRENING)) {
            log.debug("Avbryter automatisk utbetaling. Feil tiltakskode. UtbetalingId: {}", utbetalingId)
            return false
        }
        val relevanteTilsagn = queries.tilsagn.getAll(
            gjennomforingId = utbetaling.gjennomforing.id,
            statuser = listOf(TilsagnStatus.GODKJENT),
            typer = listOf(TilsagnType.TILSAGN, TilsagnType.EKSTRATILSAGN),
            periode = utbetaling.periode,
        )
        if (relevanteTilsagn.size != 1) {
            log.debug(
                "Avbryter automatisk utbetaling. Feil antall tilsagn: {}. UtbetalingId: {}",
                relevanteTilsagn.size,
                utbetalingId,
            )
            return false
        }
        val tilsagn = relevanteTilsagn[0]
        // TODO: Bruk gjenstående beløp
        if (tilsagn.beregning.output.belop < utbetaling.beregning.output.belop) {
            log.debug("Avbryter automatisk utbetaling. Ikke nok penger. UtbetalingId: {}", utbetalingId)
            return false
        }
        val frigjorTilsagn = tilsagn.periodeSlutt in utbetaling.periode
        val delutbetalingId = UUID.randomUUID()
        upsertDelutbetaling(
            utbetaling,
            tilsagn,
            delutbetalingId,
            belop = utbetaling.beregning.output.belop,
            frigjorTilsagn = frigjorTilsagn,
            Tiltaksadministrasjon,
        )
        val delutbetaling = requireNotNull(queries.delutbetaling.get(delutbetalingId))
        godkjennDelutbetaling(
            delutbetaling,
            Tiltaksadministrasjon,
        )
        log.debug("Automatisk behandling av utbetaling gjennomført. DelutbetalingId: {}", delutbetalingId)
        return true
    }

    private fun QueryContext.upsertDelutbetaling(
        utbetaling: UtbetalingDto,
        tilsagn: TilsagnDto,
        id: UUID,
        belop: Int,
        frigjorTilsagn: Boolean,
        behandletAv: Agent,
    ) {
        val tilsagnPeriode = Periode.fromInclusiveDates(tilsagn.periodeStart, tilsagn.periodeSlutt)
        val periode = requireNotNull(utbetaling.periode.intersect(tilsagnPeriode)) {
            "Utbetalingsperiode og tilsagnsperiode overlapper ikke"
        }

        val lopenummer = queries.delutbetaling.getNextLopenummerByTilsagn(tilsagn.id)
        val dbo = DelutbetalingDbo(
            id = id,
            utbetalingId = utbetaling.id,
            tilsagnId = tilsagn.id,
            periode = periode,
            belop = belop,
            behandletAv = behandletAv,
            frigjorTilsagn = frigjorTilsagn,
            lopenummer = lopenummer,
            fakturanummer = "${tilsagn.bestillingsnummer}-$lopenummer",
        )

        queries.delutbetaling.upsert(dbo)
        logEndring(
            "Utbetaling sendt til godkjenning",
            getOrError(utbetaling.id),
            behandletAv,
        )
    }

    private fun QueryContext.godkjennDelutbetaling(
        delutbetaling: DelutbetalingDto,
        besluttetAv: Agent,
    ) {
        require(besluttetAv !is NavIdent || besluttetAv != delutbetaling.opprettelse.behandletAv) {
            "Kan ikke beslutte egen utbetaling"
        }
        require(delutbetaling.opprettelse.besluttetAv == null) {
            "Utbetaling er allerede besluttet"
        }

        queries.totrinnskontroll.upsert(
            delutbetaling.opprettelse.copy(
                besluttetAv = besluttetAv,
                besluttelse = Besluttelse.GODKJENT,
                besluttetTidspunkt = LocalDateTime.now(),
                aarsaker = emptyList(),
                forklaring = null,
            ),
        )
        okonomi.scheduleBehandleGodkjenteUtbetalinger(delutbetaling.tilsagnId, session)
        logEndring(
            "Utbetaling godkjent",
            getOrError(delutbetaling.utbetalingId),
            besluttetAv,
        )
    }

    private fun QueryContext.avvisDelutbetaling(
        delutbetaling: DelutbetalingDto,
        aarsaker: List<String>,
        forklaring: String?,
        besluttetAv: Agent,
    ) {
        require(besluttetAv !is NavIdent || besluttetAv != delutbetaling.opprettelse.behandletAv) {
            "Kan ikke beslutte egen utbetaling"
        }
        require(delutbetaling is DelutbetalingDto.DelutbetalingTilGodkjenning) {
            "Utbetaling er allerede besluttet"
        }

        queries.totrinnskontroll.upsert(
            delutbetaling.opprettelse.copy(
                besluttetAv = besluttetAv,
                besluttelse = Besluttelse.AVVIST,
                aarsaker = aarsaker,
                forklaring = forklaring,
                besluttetTidspunkt = LocalDateTime.now(),
            ),
        )
        logEndring(
            "Utbetaling returnert",
            getOrError(delutbetaling.utbetalingId),
            besluttetAv,
        )
    }

    private fun resolveStengtHosArrangor(
        periode: Periode,
        stengt: List<GjennomforingDto.StengtPeriode>,
    ): Set<StengtPeriode> {
        return stengt
            .mapNotNull { stengt ->
                Periode(stengt.start, stengt.slutt.plusDays(1)).intersect(periode)?.let {
                    StengtPeriode(it.start, it.slutt, stengt.beskrivelse)
                }
            }
            .toSet()
    }

    private fun resolveDeltakelser(
        gjennomforingId: UUID,
        periode: Periode,
    ): Set<DeltakelsePerioder> = db.session {
        queries.deltaker.getAll(gjennomforingId = gjennomforingId)
            .asSequence()
            .filter { deltaker ->
                isRelevantForUtbetalingsperide(deltaker, periode)
            }
            .map { deltaker ->
                val deltakelsesmengder = queries.deltaker.getDeltakelsesmengder(deltaker.id)

                val sluttDatoInPeriode = getSluttDatoInPeriode(deltaker, periode)

                val perioder = deltakelsesmengder.mapIndexedNotNull { index, mengde ->
                    val gyldigTil = deltakelsesmengder.getOrNull(index + 1)?.gyldigFra ?: sluttDatoInPeriode

                    Periode.of(mengde.gyldigFra, gyldigTil)?.intersect(periode)?.let { overlappingPeriode ->
                        DeltakelsePeriode(
                            start = overlappingPeriode.start,
                            slutt = overlappingPeriode.slutt,
                            deltakelsesprosent = mengde.deltakelsesprosent,
                        )
                    }
                }

                check(perioder.isNotEmpty()) {
                    "Deltaker id=${deltaker.id} er relevant for utbetaling, men mangler deltakelsesmengder innenfor perioden=$periode"
                }

                DeltakelsePerioder(deltaker.id, perioder)
            }
            .toSet()
    }

    private fun QueryContext.logEndring(
        operation: String,
        dto: UtbetalingDto,
        endretAv: Agent,
    ) {
        queries.endringshistorikk.logEndring(
            DocumentClass.UTBETALING,
            operation,
            endretAv,
            dto.id,
            LocalDateTime.now(),
        ) {
            Json.encodeToJsonElement(dto)
        }
    }

    private fun QueryContext.getOrError(id: UUID): UtbetalingDto {
        return requireNotNull(queries.utbetaling.get(id)) { "Utbetaling med id=$id finnes ikke" }
    }
}

private fun isRelevantForUtbetalingsperide(
    deltaker: DeltakerDto,
    periode: Periode,
): Boolean {
    val relevantDeltakerStatusForUtbetaling = listOf(
        DeltakerStatus.Type.AVBRUTT,
        DeltakerStatus.Type.DELTAR,
        DeltakerStatus.Type.FULLFORT,
        DeltakerStatus.Type.HAR_SLUTTET,
    )
    if (deltaker.status.type !in relevantDeltakerStatusForUtbetaling) {
        return false
    }

    val startDato = requireNotNull(deltaker.startDato) {
        "Deltaker må ha en startdato når status er ${deltaker.status.type} og den er relevant for utbetaling"
    }
    val sluttDatoInPeriode = getSluttDatoInPeriode(deltaker, periode)
    return Periode.of(startDato, sluttDatoInPeriode)?.overlaps(periode) ?: false
}

private fun getSluttDatoInPeriode(deltaker: DeltakerDto, periode: Periode): LocalDate {
    return deltaker.sluttDato?.plusDays(1)?.coerceAtMost(periode.slutt) ?: periode.slutt
}
