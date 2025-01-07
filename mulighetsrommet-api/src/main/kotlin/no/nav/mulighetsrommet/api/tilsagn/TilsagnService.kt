package no.nav.mulighetsrommet.api.tilsagn

import arrow.core.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.domain.dto.EndringshistorikkDto
import no.nav.mulighetsrommet.api.endringshistorikk.DocumentClass
import no.nav.mulighetsrommet.api.endringshistorikk.EndretAv
import no.nav.mulighetsrommet.api.gjennomforing.model.TiltaksgjennomforingDto
import no.nav.mulighetsrommet.api.okonomi.BestillingDto
import no.nav.mulighetsrommet.api.okonomi.OkonomiClient
import no.nav.mulighetsrommet.api.refusjon.model.RefusjonskravPeriode
import no.nav.mulighetsrommet.api.responses.*
import no.nav.mulighetsrommet.api.tilsagn.db.TilsagnDbo
import no.nav.mulighetsrommet.api.tilsagn.model.*
import no.nav.mulighetsrommet.domain.dto.NavIdent
import java.time.LocalDateTime
import java.util.*

class TilsagnService(
    private val db: ApiDatabase,
) {
    fun upsert(request: TilsagnRequest, navIdent: NavIdent): Either<List<ValidationError>, TilsagnDto> = db.tx {
        val gjennomforing = Queries.gjennomforing.get(request.gjennomforingId)
            ?: return ValidationError
                .of(TilsagnRequest::gjennomforingId, "Tiltaksgjennomforingen finnes ikke")
                .nel()
                .left()

        val previous = Queries.tilsagn.get(request.id)

        val beregningInput = request.beregning

        validateGjennomforingBeregningInput(gjennomforing, beregningInput)
            .flatMap { beregnTilsagn(beregningInput) }
            .map { beregning ->
                TilsagnDbo(
                    id = request.id,
                    tiltaksgjennomforingId = request.gjennomforingId,
                    type = request.type,
                    periodeStart = request.periodeStart,
                    periodeSlutt = request.periodeSlutt,
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
                Queries.tilsagn.upsert(dbo)

                val dto = getOrError(dbo.id)

                logEndring("Sendt til godkjenning", dto, EndretAv.NavAnsatt(navIdent))
                dto
            }
    }

    fun beregnTilsagn(input: TilsagnBeregningInput): Either<List<ValidationError>, TilsagnBeregning> {
        return TilsagnValidator.validateBeregningInput(input)
            .map {
                when (input) {
                    is TilsagnBeregningForhandsgodkjent.Input -> TilsagnBeregningForhandsgodkjent.beregn(input)
                    is TilsagnBeregningFri.Input -> TilsagnBeregningFri.beregn(input)
                }
            }
    }

    suspend fun beslutt(id: UUID, besluttelse: BesluttTilsagnRequest, navIdent: NavIdent): StatusResponse<TilsagnDto> = db.session {
        val tilsagn = Queries.tilsagn.get(id) ?: return NotFound("Fant ikke tilsagn").left()

        return when (tilsagn.status) {
            is TilsagnDto.TilsagnStatus.Annullert, is TilsagnDto.TilsagnStatus.Godkjent, is TilsagnDto.TilsagnStatus.Returnert ->
                BadRequest("Tilsagnet kan ikke besluttes fordi det har status ${tilsagn.status.javaClass.simpleName}").left()

            is TilsagnDto.TilsagnStatus.TilGodkjenning -> {
                when (besluttelse) {
                    BesluttTilsagnRequest.GodkjentTilsagnRequest -> godkjennTilsagn(tilsagn, navIdent)
                    is BesluttTilsagnRequest.AvvistTilsagnRequest -> returnerTilsagn(tilsagn, besluttelse, navIdent)
                }
            }

            is TilsagnDto.TilsagnStatus.TilAnnullering -> {
                when (besluttelse.besluttelse) {
                    TilsagnBesluttelseStatus.GODKJENT -> annullerTilsagn(tilsagn, navIdent)
                    TilsagnBesluttelseStatus.AVVIST -> avvisAnnullering(tilsagn, navIdent)
                }
            }
        }
    }

    private suspend fun godkjennTilsagn(tilsagn: TilsagnDto, navIdent: NavIdent): StatusResponse<TilsagnDto> = db.tx {
        require(tilsagn.status is TilsagnDto.TilsagnStatus.TilGodkjenning)

        if (navIdent == tilsagn.status.endretAv) {
            return Forbidden("Kan ikke beslutte eget tilsagn").left()
        }

        Queries.tilsagn.besluttGodkjennelse(tilsagn.id, navIdent, LocalDateTime.now())
        lagOgSendBestilling(tilsagn)

        val dto = getOrError(tilsagn.id)
        logEndring("Tilsagn godkjent", dto, EndretAv.NavAnsatt(navIdent))
        dto.right()
    }

    private fun returnerTilsagn(
        tilsagn: TilsagnDto,
        besluttelse: BesluttTilsagnRequest.AvvistTilsagnRequest,
        navIdent: NavIdent,
    ): StatusResponse<TilsagnDto> = db.tx {
        require(tilsagn.status is TilsagnDto.TilsagnStatus.TilGodkjenning)

        if (navIdent == tilsagn.status.endretAv) {
            return Forbidden("Kan ikke beslutte eget tilsagn").left()
        } else if (besluttelse.aarsaker.isEmpty()) {
            return BadRequest(message = "Årsaker er påkrevd").left()
        }

        Queries.tilsagn.returner(
            tilsagn.id,
            navIdent,
            LocalDateTime.now(),
            besluttelse.aarsaker,
            besluttelse.forklaring,
        )

        val dto = getOrError(tilsagn.id)
        logEndring("Tilsagn returnert", dto, EndretAv.NavAnsatt(navIdent))
        dto.right()
    }

    private fun annullerTilsagn(tilsagn: TilsagnDto, navIdent: NavIdent): StatusResponse<TilsagnDto> = db.tx {
        require(tilsagn.status is TilsagnDto.TilsagnStatus.TilAnnullering)

        if (navIdent == tilsagn.status.endretAv) {
            return Forbidden("Kan ikke beslutte eget tilsagn").left()
        }

        Queries.tilsagn.besluttAnnullering(tilsagn.id, navIdent, LocalDateTime.now())

        val dto = getOrError(tilsagn.id)
        logEndring("Tilsagn annullert", dto, EndretAv.NavAnsatt(navIdent))
        dto.right()
    }

    private fun avvisAnnullering(tilsagn: TilsagnDto, navIdent: NavIdent): StatusResponse<TilsagnDto> = db.tx {
        require(tilsagn.status is TilsagnDto.TilsagnStatus.TilAnnullering)

        if (navIdent == tilsagn.status.endretAv) {
            return Forbidden("Kan ikke beslutte eget tilsagn").left()
        }

        Queries.tilsagn.avbrytAnnullering(tilsagn.id, navIdent, LocalDateTime.now())

        val dto = getOrError(tilsagn.id)
        logEndring("Annullering avvist", dto, EndretAv.NavAnsatt(navIdent))
        dto.right()
    }

    fun tilAnnullering(
        id: UUID,
        navIdent: NavIdent,
        request: TilAnnulleringRequest,
    ): StatusResponse<TilsagnDto> = db.tx {
        val tilsagn = Queries.tilsagn.get(id) ?: return NotFound("Fant ikke tilsagn").left()

        if (tilsagn.status !is TilsagnDto.TilsagnStatus.Godkjent) {
            return BadRequest("Kan bare annullere godkjente tilsagn").left()
        }

        Queries.tilsagn.tilAnnullering(
            id,
            navIdent,
            LocalDateTime.now(),
            request.aarsaker,
            request.forklaring,
        )

        val dto = getOrError(tilsagn.id)
        logEndring("Sendt til annullering", dto, EndretAv.NavAnsatt(navIdent))
        dto.right()
    }

    fun slettTilsagn(id: UUID): StatusResponse<Unit> = db.tx {
        val tilsagn = Queries.tilsagn.get(id) ?: return NotFound("Fant ikke tilsagn").left()

        if (tilsagn.status !is TilsagnDto.TilsagnStatus.Returnert) {
            return BadRequest("Kan ikke slette tilsagn som er godkjent").left()
        }

        Queries.tilsagn.delete(id).right()
    }

    fun getAll() = db.session {
        Queries.tilsagn.getAll()
    }

    fun getTilsagnTilRefusjon(
        gjennomforingId: UUID,
        periode: RefusjonskravPeriode,
    ): List<TilsagnDto> = db.session {
        return Queries.tilsagn.getTilsagnTilRefusjon(gjennomforingId, periode)
    }

    fun getArrangorflateTilsagnTilRefusjon(
        gjennomforingId: UUID,
        periode: RefusjonskravPeriode,
    ): List<ArrangorflateTilsagn> = db.session {
        return Queries.tilsagn.getArrangorflateTilsagnTilRefusjon(gjennomforingId, periode)
    }

    private fun lagOkonomiId(tilsagn: TilsagnDto): String {
        return "T-${tilsagn.id}"
    }

    private suspend fun QueryContext.lagOgSendBestilling(tilsagn: TilsagnDto) {
        val gjennomforing = requireNotNull(Queries.gjennomforing.get(tilsagn.tiltaksgjennomforing.id)) {
            "Fant ikke gjennomforing til tilsagn"
        }

        OkonomiClient.sendBestilling(
            BestillingDto(
                okonomiId = lagOkonomiId(tilsagn),
                periodeStart = tilsagn.periodeStart,
                periodeSlutt = tilsagn.periodeSlutt,
                organisasjonsnummer = gjennomforing.arrangor.organisasjonsnummer,
                kostnadSted = tilsagn.kostnadssted,
                belop = tilsagn.beregning.output.belop,
            ),
        )
    }

    fun getEndringshistorikk(id: UUID): EndringshistorikkDto = db.session {
        Queries.endringshistorikk.getEndringshistorikk(DocumentClass.TILSAGN, id)
    }

    private fun validateGjennomforingBeregningInput(
        gjennomforing: TiltaksgjennomforingDto,
        input: TilsagnBeregningInput,
    ): Either<List<ValidationError>, TilsagnBeregningInput> {
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
        Queries.endringshistorikk.logEndring(
            DocumentClass.TILSAGN,
            operation,
            endretAv,
            dto.id,
        ) {
            Json.encodeToJsonElement(dto)
        }
    }

    private fun QueryContext.getOrError(id: UUID): TilsagnDto {
        return requireNotNull(Queries.tilsagn.get(id)) { "Tilsagn med id=$id finnes ikke" }
    }
}
