package no.nav.mulighetsrommet.api.tilsagn

import arrow.core.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.endringshistorikk.DocumentClass
import no.nav.mulighetsrommet.api.endringshistorikk.EndretAv
import no.nav.mulighetsrommet.api.endringshistorikk.EndringshistorikkDto
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingDto
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.responses.StatusResponse
import no.nav.mulighetsrommet.api.tilsagn.db.TilsagnDbo
import no.nav.mulighetsrommet.api.tilsagn.model.*
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
import no.nav.mulighetsrommet.ktor.exception.BadRequest
import no.nav.mulighetsrommet.ktor.exception.Forbidden
import no.nav.mulighetsrommet.ktor.exception.NotFound
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Periode
import java.time.LocalDateTime
import java.util.*

class TilsagnService(
    private val db: ApiDatabase,
    private val okonomi: OkonomiBestillingService,
) {
    fun upsert(request: TilsagnRequest, navIdent: NavIdent): Either<List<FieldError>, TilsagnDto> = db.transaction {
        val gjennomforing = queries.gjennomforing.get(request.gjennomforingId)
            ?: return FieldError
                .of(TilsagnRequest::gjennomforingId, "Tiltaksgjennomforingen finnes ikke")
                .nel()
                .left()

        val previous = queries.tilsagn.get(request.id)

        val beregningInput = request.beregning

        validateGjennomforingBeregningInput(gjennomforing, beregningInput)
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
                    endretAv = navIdent,
                    // TODO: flytt til db
                    endretTidspunkt = LocalDateTime.now(),
                    arrangorId = gjennomforing.arrangor.id,
                )
            }
            .flatMap { dbo ->
                TilsagnValidator.validate(dbo, previous)
            }
            .map { dbo ->
                queries.tilsagn.upsert(dbo)

                val dto = getOrError(dbo.id)

                logEndring("Sendt til godkjenning", dto, EndretAv.NavAnsatt(navIdent))
                dto
            }
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

    fun beslutt(id: UUID, besluttelse: BesluttTilsagnRequest, navIdent: NavIdent): StatusResponse<TilsagnDto> = db.session {
        val tilsagn = queries.tilsagn.get(id) ?: return NotFound("Fant ikke tilsagn").left()

        return when (tilsagn.status) {
            TilsagnStatus.ANNULLERT, TilsagnStatus.GODKJENT, TilsagnStatus.RETURNERT ->
                BadRequest("Tilsagnet kan ikke besluttes fordi det har status ${tilsagn.status}").left()

            TilsagnStatus.TIL_GODKJENNING -> {
                when (besluttelse) {
                    BesluttTilsagnRequest.GodkjentTilsagnRequest -> godkjennTilsagn(tilsagn, navIdent)
                    is BesluttTilsagnRequest.AvvistTilsagnRequest -> returnerTilsagn(tilsagn, besluttelse, navIdent)
                }
            }

            TilsagnStatus.TIL_ANNULLERING -> {
                when (besluttelse.besluttelse) {
                    Besluttelse.GODKJENT -> annullerTilsagn(tilsagn, navIdent)
                    Besluttelse.AVVIST -> avvisAnnullering(tilsagn, navIdent)
                }
            }
        }
    }

    private fun godkjennTilsagn(tilsagn: TilsagnDto, godkjentAv: NavIdent): StatusResponse<TilsagnDto> = db.transaction {
        require(tilsagn.status == TilsagnStatus.TIL_GODKJENNING)

        if (godkjentAv == tilsagn.opprettelse.behandletAv) {
            return Forbidden("Kan ikke beslutte eget tilsagn").left()
        }

        queries.totrinnskontroll.upsert(
            tilsagn.opprettelse.copy(
                besluttetAv = godkjentAv,
                besluttetTidspunkt = LocalDateTime.now(),
                besluttelse = Besluttelse.GODKJENT,
            ),
        )

        okonomi.scheduleBehandleGodkjentTilsagn(tilsagn.id, session)

        val dto = getOrError(tilsagn.id)
        logEndring("Tilsagn godkjent", dto, EndretAv.NavAnsatt(godkjentAv))
        dto.right()
    }

    private fun returnerTilsagn(
        tilsagn: TilsagnDto,
        besluttelse: BesluttTilsagnRequest.AvvistTilsagnRequest,
        navIdent: NavIdent,
    ): StatusResponse<TilsagnDto> = db.transaction {
        require(tilsagn.status == TilsagnStatus.TIL_GODKJENNING)

        if (navIdent == tilsagn.opprettelse.behandletAv) {
            return Forbidden("Kan ikke beslutte eget tilsagn").left()
        } else if (besluttelse.aarsaker.isEmpty()) {
            return BadRequest(detail = "Årsaker er påkrevd").left()
        }

        queries.totrinnskontroll.upsert(
            tilsagn.opprettelse.copy(
                besluttetAv = navIdent,
                besluttetTidspunkt = LocalDateTime.now(),
                besluttelse = Besluttelse.AVVIST,
                aarsaker = besluttelse.aarsaker.map { it.name },
                forklaring = besluttelse.forklaring,
            ),
        )

        val dto = getOrError(tilsagn.id)
        logEndring("Tilsagn returnert", dto, EndretAv.NavAnsatt(navIdent))
        dto.right()
    }

    private fun annullerTilsagn(tilsagn: TilsagnDto, navIdent: NavIdent): StatusResponse<TilsagnDto> = db.transaction {
        require(tilsagn.status == TilsagnStatus.TIL_ANNULLERING)
        requireNotNull(tilsagn.annullering)

        if (navIdent == tilsagn.annullering.behandletAv) {
            return Forbidden("Kan ikke beslutte eget tilsagn").left()
        }

        queries.totrinnskontroll.upsert(
            tilsagn.opprettelse.copy(
                besluttetAv = navIdent,
                besluttetTidspunkt = LocalDateTime.now(),
                besluttelse = Besluttelse.GODKJENT,
            ),
        )

        okonomi.scheduleBehandleAnnullertTilsagn(tilsagn.id, session)

        val dto = getOrError(tilsagn.id)
        logEndring("Tilsagn annullert", dto, EndretAv.NavAnsatt(navIdent))
        dto.right()
    }

    private fun avvisAnnullering(tilsagn: TilsagnDto, navIdent: NavIdent): StatusResponse<TilsagnDto> = db.transaction {
        require(tilsagn.status == TilsagnStatus.TIL_ANNULLERING)
        requireNotNull(tilsagn.annullering)

        if (navIdent == tilsagn.annullering.behandletAv) {
            return Forbidden("Kan ikke beslutte eget tilsagn").left()
        }

        queries.totrinnskontroll.upsert(
            tilsagn.opprettelse.copy(
                besluttetAv = navIdent,
                besluttetTidspunkt = LocalDateTime.now(),
                besluttelse = Besluttelse.AVVIST,
            ),
        )

        val dto = getOrError(tilsagn.id)
        logEndring("Annullering avvist", dto, EndretAv.NavAnsatt(navIdent))
        dto.right()
    }

    fun tilAnnullering(
        id: UUID,
        navIdent: NavIdent,
        annullering: TilAnnulleringRequest,
    ): StatusResponse<TilsagnDto> = db.transaction {
        val tilsagn = queries.tilsagn.get(id) ?: return NotFound("Fant ikke tilsagn").left()

        if (tilsagn.status != TilsagnStatus.GODKJENT) {
            return BadRequest("Kan bare annullere godkjente tilsagn").left()
        }

        queries.totrinnskontroll.upsert(
            Totrinnskontroll(
                id = UUID.randomUUID(),
                entityId = tilsagn.id,
                behandletAv = navIdent,
                aarsaker = annullering.aarsaker.map { it.name },
                forklaring = annullering.forklaring,
                type = Totrinnskontroll.Type.ANNULLER,
                behandletTidspunkt = LocalDateTime.now(),
                besluttelse = null,
                besluttetAv = null,
                besluttetTidspunkt = null,
            ),
        )

        val dto = getOrError(tilsagn.id)
        logEndring("Sendt til annullering", dto, EndretAv.NavAnsatt(navIdent))
        dto.right()
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

    private fun validateGjennomforingBeregningInput(
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
        endretAv: EndretAv,
    ) {
        queries.endringshistorikk.logEndring(
            DocumentClass.TILSAGN,
            operation,
            endretAv,
            dto.id,
        ) {
            Json.encodeToJsonElement(dto)
        }
    }

    private fun QueryContext.getOrError(id: UUID): TilsagnDto {
        return requireNotNull(queries.tilsagn.get(id)) { "Tilsagn med id=$id finnes ikke" }
    }
}
