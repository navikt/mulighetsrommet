package no.nav.mulighetsrommet.api.tilsagn

import arrow.core.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.OkonomiConfig
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.endringshistorikk.DocumentClass
import no.nav.mulighetsrommet.api.endringshistorikk.EndringshistorikkDto
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingDto
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.responses.StatusResponse
import no.nav.mulighetsrommet.api.tilsagn.db.TilsagnDbo
import no.nav.mulighetsrommet.api.tilsagn.model.*
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
import no.nav.mulighetsrommet.api.utils.DatoUtils.formaterDatoTilEuropeiskDatoformat
import no.nav.mulighetsrommet.ktor.exception.BadRequest
import no.nav.mulighetsrommet.ktor.exception.Forbidden
import no.nav.mulighetsrommet.ktor.exception.NotFound
import no.nav.mulighetsrommet.ktor.exception.StatusException
import no.nav.mulighetsrommet.model.Agent
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltaksadministrasjon
import java.time.LocalDateTime
import java.util.*

class TilsagnService(
    val config: OkonomiConfig,
    private val db: ApiDatabase,
    private val okonomi: OkonomiBestillingService,
) {
    fun upsert(request: TilsagnRequest, navIdent: NavIdent): Either<List<FieldError>, TilsagnDto> = db.transaction {
        val gjennomforing = queries.gjennomforing.get(request.gjennomforingId)
            ?: return FieldError
                .of("Tiltaksgjennomforingen finnes ikke", TilsagnRequest::gjennomforingId)
                .nel()
                .left()

        val minTilsagnCreationDate = config.minimumTilsagnPeriodeStart[gjennomforing.tiltakstype.tiltakskode]
        if (minTilsagnCreationDate == null) {
            return FieldError
                .of(
                    "Tilsagn for tiltakstype ${gjennomforing.tiltakstype.navn} er ikke støttet enda",
                    TilsagnRequest::periodeStart,
                )
                .nel()
                .left()
        } else if (request.periodeStart < minTilsagnCreationDate) {
            return FieldError
                .of(
                    "Minimum startdato for tilsagn til ${gjennomforing.tiltakstype.navn} er ${minTilsagnCreationDate.formaterDatoTilEuropeiskDatoformat()}",
                    TilsagnRequest::periodeStart,
                )
                .nel()
                .left()
        }

        val previous = queries.tilsagn.get(request.id)

        val beregningInput = request.beregning

        validateTilsagnBeregningInput(gjennomforing, beregningInput)
            .flatMap { beregnTilsagn(beregningInput) }
            .map { beregning ->
                val lopenummer = previous?.lopenummer
                    ?: queries.tilsagn.getNextLopenummeByGjennomforing(gjennomforing.id)

                TilsagnDbo(
                    id = request.id,
                    gjennomforingId = request.gjennomforingId,
                    type = request.type,
                    periode = Periode.fromInclusiveDates(request.periodeStart, request.periodeSlutt),
                    lopenummer = lopenummer,
                    bestillingsnummer = previous?.bestillingsnummer ?: "A-${gjennomforing.lopenummer}-$lopenummer",
                    kostnadssted = request.kostnadssted,
                    beregning = beregning,
                    behandletAv = navIdent,
                    behandletTidspunkt = LocalDateTime.now(),
                    arrangorId = gjennomforing.arrangor.id,
                )
            }
            .flatMap { dbo ->
                TilsagnValidator.validate(dbo, previous)
            }
            .map { dbo ->
                queries.tilsagn.upsert(dbo)

                val dto = getOrError(dbo.id)

                logEndring("Sendt til godkjenning", dto, navIdent)
                dto
            }
    }

    fun tilAnnulleringRequest(id: UUID, navIdent: NavIdent, request: TilAnnulleringRequest) = db.transaction {
        val tilsagn = db.session { queries.tilsagn.get(id) }
            ?: throw StatusException(HttpStatusCode.NotFound, "Fant ikke tilsagn")

        setTilAnnullering(tilsagn, navIdent, request.aarsaker.map { it.name }, request.forklaring)
    }

    fun tilFrigjoringRequest(id: UUID, navIdent: NavIdent, request: TilAnnulleringRequest) = db.transaction {
        val tilsagn = db.session { queries.tilsagn.get(id) }
            ?: throw StatusException(HttpStatusCode.NotFound, "Fant ikke tilsagn")

        setTilFrigjoring(tilsagn, navIdent, request.aarsaker.map { it.name }, request.forklaring)
    }

    fun beregnTilsagn(input: TilsagnBeregningInput): Either<List<FieldError>, TilsagnBeregning> {
        return TilsagnValidator.validateBeregningInput(input)
            .map {
                when (input) {
                    is TilsagnBeregningForhandsgodkjent.Input -> TilsagnBeregningForhandsgodkjent.beregn(input)
                    is TilsagnBeregningFri.Input -> TilsagnBeregningFri.beregn(input)
                }
            }
    }

    fun beslutt(id: UUID, besluttelse: BesluttTilsagnRequest, navIdent: NavIdent): StatusResponse<TilsagnDto> = db.transaction {
        val tilsagn = queries.tilsagn.get(id) ?: return NotFound("Fant ikke tilsagn").left()

        return when (tilsagn.status) {
            TilsagnStatus.FRIGJORT, TilsagnStatus.ANNULLERT, TilsagnStatus.GODKJENT, TilsagnStatus.RETURNERT ->
                BadRequest("Tilsagnet kan ikke besluttes fordi det har status ${tilsagn.status}").left()

            TilsagnStatus.TIL_GODKJENNING -> {
                when (besluttelse) {
                    BesluttTilsagnRequest.GodkjentTilsagnRequest -> godkjennTilsagn(tilsagn, navIdent)
                    is BesluttTilsagnRequest.AvvistTilsagnRequest -> returnerTilsagn(tilsagn, besluttelse, navIdent)
                }
            }

            TilsagnStatus.TIL_ANNULLERING -> {
                when (besluttelse.besluttelse) {
                    Besluttelse.GODKJENT -> annullerTilsagn(tilsagn, navIdent).right()
                    Besluttelse.AVVIST -> avvisAnnullering(tilsagn, navIdent).right()
                }
            }

            TilsagnStatus.TIL_FRIGJORING -> {
                when (besluttelse.besluttelse) {
                    Besluttelse.GODKJENT -> frigjorTilsagn(tilsagn, navIdent).right()
                    Besluttelse.AVVIST -> avvisFrigjoring(tilsagn, navIdent).right()
                }
            }
        }
    }

    private fun godkjennTilsagn(tilsagn: TilsagnDto, besluttetAv: NavIdent): StatusResponse<TilsagnDto> = db.transaction {
        require(tilsagn.status == TilsagnStatus.TIL_GODKJENNING)

        if (besluttetAv == tilsagn.opprettelse.behandletAv) {
            return Forbidden("Kan ikke beslutte eget tilsagn").left()
        }

        queries.totrinnskontroll.upsert(
            tilsagn.opprettelse.copy(
                besluttetAv = besluttetAv,
                besluttetTidspunkt = LocalDateTime.now(),
                besluttelse = Besluttelse.GODKJENT,
            ),
        )
        queries.tilsagn.setStatus(tilsagn.id, TilsagnStatus.GODKJENT)

        okonomi.scheduleBehandleGodkjentTilsagn(tilsagn.id, session)

        val dto = getOrError(tilsagn.id)
        logEndring("Tilsagn godkjent", dto, besluttetAv)
        dto.right()
    }

    private fun returnerTilsagn(
        tilsagn: TilsagnDto,
        besluttelse: BesluttTilsagnRequest.AvvistTilsagnRequest,
        besluttetAv: NavIdent,
    ): StatusResponse<TilsagnDto> = db.transaction {
        require(tilsagn.status == TilsagnStatus.TIL_GODKJENNING)

        if (besluttetAv == tilsagn.opprettelse.behandletAv) {
            return Forbidden("Kan ikke beslutte eget tilsagn").left()
        }
        if (besluttelse.aarsaker.isEmpty()) {
            return BadRequest(detail = "Årsaker er påkrevd").left()
        }

        queries.totrinnskontroll.upsert(
            tilsagn.opprettelse.copy(
                besluttetAv = besluttetAv,
                besluttetTidspunkt = LocalDateTime.now(),
                besluttelse = Besluttelse.AVVIST,
                aarsaker = besluttelse.aarsaker.map { it.name },
                forklaring = besluttelse.forklaring,
            ),
        )
        queries.tilsagn.setStatus(tilsagn.id, TilsagnStatus.RETURNERT)

        val dto = getOrError(tilsagn.id)
        logEndring("Tilsagn returnert", dto, besluttetAv)
        dto.right()
    }

    private fun QueryContext.annullerTilsagn(tilsagn: TilsagnDto, besluttetAv: NavIdent): TilsagnDto {
        require(tilsagn.status == TilsagnStatus.TIL_ANNULLERING)
        requireNotNull(tilsagn.annullering)
        require(besluttetAv != tilsagn.annullering.behandletAv) {
            "Kan ikke beslutte eget tilsagn"
        }

        queries.totrinnskontroll.upsert(
            tilsagn.annullering.copy(
                besluttetAv = besluttetAv,
                besluttetTidspunkt = LocalDateTime.now(),
                besluttelse = Besluttelse.GODKJENT,
            ),
        )
        queries.tilsagn.setStatus(tilsagn.id, TilsagnStatus.ANNULLERT)

        okonomi.scheduleBehandleAnnullertTilsagn(tilsagn.id, session)

        val dto = getOrError(tilsagn.id)
        logEndring("Tilsagn annullert", dto, besluttetAv)
        return dto
    }

    private fun QueryContext.avvisAnnullering(tilsagn: TilsagnDto, besluttetAv: Agent): TilsagnDto {
        require(tilsagn.status == TilsagnStatus.TIL_ANNULLERING)
        requireNotNull(tilsagn.annullering)
        require(besluttetAv != tilsagn.annullering.behandletAv) {
            "Kan ikke beslutte eget tilsagn"
        }

        queries.totrinnskontroll.upsert(
            tilsagn.annullering.copy(
                besluttetAv = besluttetAv,
                besluttetTidspunkt = LocalDateTime.now(),
                besluttelse = Besluttelse.AVVIST,
            ),
        )
        queries.tilsagn.setStatus(tilsagn.id, TilsagnStatus.GODKJENT)

        val dto = getOrError(tilsagn.id)
        logEndring("Annullering avvist", dto, besluttetAv)
        return dto
    }

    private fun QueryContext.setTilFrigjoring(
        tilsagn: TilsagnDto,
        agent: Agent,
        aarsaker: List<String>,
        forklaring: String?,
    ): TilsagnDto {
        require(tilsagn.status == TilsagnStatus.GODKJENT) {
            "Kan bare annullere godkjente tilsagn"
        }

        queries.totrinnskontroll.upsert(
            Totrinnskontroll(
                id = UUID.randomUUID(),
                entityId = tilsagn.id,
                behandletAv = agent,
                aarsaker = aarsaker,
                forklaring = forklaring,
                type = Totrinnskontroll.Type.FRIGJOR,
                behandletTidspunkt = LocalDateTime.now(),
                besluttelse = null,
                besluttetAv = null,
                besluttetTidspunkt = null,
            ),
        )
        queries.tilsagn.setStatus(tilsagn.id, TilsagnStatus.TIL_FRIGJORING)

        val dto = getOrError(tilsagn.id)
        logEndring("Sendt til frigjøring", dto, agent)
        return dto
    }

    private fun QueryContext.frigjorTilsagn(tilsagn: TilsagnDto, besluttetAv: Agent): TilsagnDto {
        require(tilsagn.status == TilsagnStatus.TIL_FRIGJORING)
        requireNotNull(tilsagn.frigjoring)

        require(besluttetAv !is NavIdent || besluttetAv != tilsagn.frigjoring.behandletAv) {
            "Kan ikke beslutte eget tilsagn"
        }

        queries.totrinnskontroll.upsert(
            tilsagn.frigjoring.copy(
                besluttetAv = besluttetAv,
                besluttetTidspunkt = LocalDateTime.now(),
                besluttelse = Besluttelse.GODKJENT,
            ),
        )
        queries.tilsagn.setStatus(tilsagn.id, TilsagnStatus.FRIGJORT)

        okonomi.scheduleBehandleFrigjortTilsagn(tilsagn.id, session)

        val dto = getOrError(tilsagn.id)
        logEndring("Tilsagn frigjort", dto, besluttetAv)
        return dto
    }

    private fun avvisFrigjoring(tilsagn: TilsagnDto, besluttetAv: Agent): TilsagnDto = db.transaction {
        require(tilsagn.status == TilsagnStatus.TIL_FRIGJORING)
        requireNotNull(tilsagn.frigjoring)

        require(besluttetAv != tilsagn.frigjoring.behandletAv) {
            "Kan ikke beslutte eget tilsagn"
        }

        queries.totrinnskontroll.upsert(
            tilsagn.frigjoring.copy(
                besluttetAv = besluttetAv,
                besluttetTidspunkt = LocalDateTime.now(),
                besluttelse = Besluttelse.AVVIST,
            ),
        )
        queries.tilsagn.setStatus(tilsagn.id, TilsagnStatus.GODKJENT)

        val dto = getOrError(tilsagn.id)
        logEndring("Frigjoring avvist", dto, besluttetAv)
        return dto
    }

    fun frigjorAutomatisk(id: UUID) {
        println("Start frigjorAutomatisk") // Debugging log
        try {
            println("Start frigjorAutomatisk") // Debugging log
            db.transaction {
                try {
                    var tilsagn = requireNotNull(queries.tilsagn.get(id))
                    println("After get(id)") // If this doesn't print, the get() is failing

                    tilsagn = setTilFrigjoring(tilsagn, Tiltaksadministrasjon, emptyList(), null)
                    println("After setTilFrigjoring")

                    frigjorTilsagn(tilsagn, Tiltaksadministrasjon)
                    println("After frigjorTilsagn")
                } catch (e: Exception) {
                    println("Exception in frigjorAutomatisk: ${e.message}") // Log the error
                    throw e // Rethrow to see if an outer handler catches it
                }
            }
        } catch (e: Exception) {
            println("Exception in frigjorAutomatisk: ${e.message}") // Log the error
            throw e // Rethrow to see if an outer handler catches it
        }
    }

    private fun QueryContext.setTilAnnullering(
        tilsagn: TilsagnDto,
        behandletAv: Agent,
        aarsaker: List<String>,
        forklaring: String?,
    ): TilsagnDto {
        require(tilsagn.status == TilsagnStatus.GODKJENT) {
            "Kan bare annullere godkjente tilsagn"
        }

        queries.totrinnskontroll.upsert(
            Totrinnskontroll(
                id = UUID.randomUUID(),
                entityId = tilsagn.id,
                behandletAv = behandletAv,
                aarsaker = aarsaker,
                forklaring = forklaring,
                type = Totrinnskontroll.Type.ANNULLER,
                behandletTidspunkt = LocalDateTime.now(),
                besluttelse = null,
                besluttetAv = null,
                besluttetTidspunkt = null,
            ),
        )
        queries.tilsagn.setStatus(tilsagn.id, TilsagnStatus.TIL_ANNULLERING)

        val dto = getOrError(tilsagn.id)
        logEndring("Sendt til annullering", dto, behandletAv)
        return dto
    }

    fun slettTilsagn(id: UUID): StatusResponse<Unit> = db.transaction {
        val tilsagn = queries.tilsagn.get(id) ?: return NotFound("Fant ikke tilsagn").left()

        if (tilsagn.status != TilsagnStatus.RETURNERT) {
            return BadRequest("Kan ikke slette tilsagn som er godkjent").left()
        }

        queries.tilsagn.delete(id).right()
    }

    fun getAll() = db.session {
        queries.tilsagn.getAll()
    }

    fun getArrangorflateTilsagnTilUtbetaling(
        gjennomforingId: UUID,
        periode: Periode,
    ): List<ArrangorflateTilsagn> = db.session {
        return queries.tilsagn.getArrangorflateTilsagnTilUtbetaling(gjennomforingId, periode)
    }

    fun getEndringshistorikk(id: UUID): EndringshistorikkDto = db.session {
        queries.endringshistorikk.getEndringshistorikk(DocumentClass.TILSAGN, id)
    }

    private fun validateTilsagnBeregningInput(
        gjennomforing: GjennomforingDto,
        input: TilsagnBeregningInput,
    ): Either<List<FieldError>, TilsagnBeregningInput> {
        return when (input) {
            is TilsagnBeregningForhandsgodkjent.Input -> TilsagnValidator.validateForhandsgodkjentSats(
                gjennomforing.tiltakstype.tiltakskode,
                input,
            )

            else -> input.right()
        }
    }

    private fun QueryContext.logEndring(
        operation: String,
        dto: TilsagnDto,
        endretAv: Agent,
    ) {
        queries.endringshistorikk.logEndring(
            DocumentClass.TILSAGN,
            operation,
            endretAv,
            dto.id,
            LocalDateTime.now(),
        ) {
            Json.encodeToJsonElement(dto)
        }
    }

    private fun QueryContext.getOrError(id: UUID): TilsagnDto {
        return requireNotNull(queries.tilsagn.get(id)) { "Tilsagn med id=$id finnes ikke" }
    }
}
