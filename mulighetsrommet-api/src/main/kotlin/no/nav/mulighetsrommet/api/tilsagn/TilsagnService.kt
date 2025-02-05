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
import no.nav.mulighetsrommet.api.tilsagn.kafka.OkonomiBestillingProducer
import no.nav.mulighetsrommet.api.tilsagn.model.*
import no.nav.mulighetsrommet.ktor.exception.BadRequest
import no.nav.mulighetsrommet.ktor.exception.Forbidden
import no.nav.mulighetsrommet.ktor.exception.NotFound
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Periode
import no.nav.tiltak.okonomi.OkonomiPart
import no.nav.tiltak.okonomi.OpprettBestilling
import java.time.LocalDateTime
import java.util.*

class TilsagnService(
    private val db: ApiDatabase,
    private val okonomi: OkonomiBestillingProducer,
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
                    bestillingsnummer = previous?.bestillingsnummer ?: "${gjennomforing.lopenummer}/$lopenummer",
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

    private fun godkjennTilsagn(tilsagn: TilsagnDto, navIdent: NavIdent): StatusResponse<TilsagnDto> = db.transaction {
        require(tilsagn.status is TilsagnDto.TilsagnStatus.TilGodkjenning)

        if (navIdent == tilsagn.status.endretAv) {
            return Forbidden("Kan ikke beslutte eget tilsagn").left()
        }

        queries.tilsagn.besluttGodkjennelse(tilsagn.id, navIdent, LocalDateTime.now())
        opprettBestilling(tilsagn)

        val dto = getOrError(tilsagn.id)
        logEndring("Tilsagn godkjent", dto, EndretAv.NavAnsatt(navIdent))
        dto.right()
    }

    private fun returnerTilsagn(
        tilsagn: TilsagnDto,
        besluttelse: BesluttTilsagnRequest.AvvistTilsagnRequest,
        navIdent: NavIdent,
    ): StatusResponse<TilsagnDto> = db.transaction {
        require(tilsagn.status is TilsagnDto.TilsagnStatus.TilGodkjenning)

        if (navIdent == tilsagn.status.endretAv) {
            return Forbidden("Kan ikke beslutte eget tilsagn").left()
        } else if (besluttelse.aarsaker.isEmpty()) {
            return BadRequest(detail = "Årsaker er påkrevd").left()
        }

        queries.tilsagn.returner(
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

    private fun annullerTilsagn(tilsagn: TilsagnDto, navIdent: NavIdent): StatusResponse<TilsagnDto> = db.transaction {
        require(tilsagn.status is TilsagnDto.TilsagnStatus.TilAnnullering)

        if (navIdent == tilsagn.status.endretAv) {
            return Forbidden("Kan ikke beslutte eget tilsagn").left()
        }

        queries.tilsagn.besluttAnnullering(tilsagn.id, navIdent, LocalDateTime.now())

        val dto = getOrError(tilsagn.id)
        logEndring("Tilsagn annullert", dto, EndretAv.NavAnsatt(navIdent))
        dto.right()
    }

    private fun avvisAnnullering(tilsagn: TilsagnDto, navIdent: NavIdent): StatusResponse<TilsagnDto> = db.transaction {
        require(tilsagn.status is TilsagnDto.TilsagnStatus.TilAnnullering)

        if (navIdent == tilsagn.status.endretAv) {
            return Forbidden("Kan ikke beslutte eget tilsagn").left()
        }

        queries.tilsagn.avbrytAnnullering(tilsagn.id, navIdent, LocalDateTime.now())

        val dto = getOrError(tilsagn.id)
        logEndring("Annullering avvist", dto, EndretAv.NavAnsatt(navIdent))
        dto.right()
    }

    fun tilAnnullering(
        id: UUID,
        navIdent: NavIdent,
        request: TilAnnulleringRequest,
    ): StatusResponse<TilsagnDto> = db.transaction {
        val tilsagn = queries.tilsagn.get(id) ?: return NotFound("Fant ikke tilsagn").left()

        if (tilsagn.status !is TilsagnDto.TilsagnStatus.Godkjent) {
            return BadRequest("Kan bare annullere godkjente tilsagn").left()
        }

        queries.tilsagn.tilAnnullering(
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

    fun slettTilsagn(id: UUID): StatusResponse<Unit> = db.transaction {
        val tilsagn = queries.tilsagn.get(id) ?: return NotFound("Fant ikke tilsagn").left()

        if (tilsagn.status !is TilsagnDto.TilsagnStatus.Returnert) {
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

    private fun QueryContext.opprettBestilling(tilsagn: TilsagnDto) {
        val gjennomforing = requireNotNull(queries.gjennomforing.get(tilsagn.gjennomforing.id)) {
            "Fant ikke gjennomforing til tilsagn"
        }

        val avtale = requireNotNull(gjennomforing.avtaleId?.let { queries.avtale.get(it) }) {
            "Gjennomføring ${gjennomforing.id} mangler avtale"
        }

        val bestilling = OpprettBestilling(
            tiltakskode = gjennomforing.tiltakstype.tiltakskode,
            arrangor = OpprettBestilling.Arrangor(
                hovedenhet = avtale.arrangor.organisasjonsnummer,
                underenhet = gjennomforing.arrangor.organisasjonsnummer,
            ),
            kostnadssted = NavEnhetNummer(tilsagn.kostnadssted.enhetsnummer),
            bestillingsnummer = tilsagn.bestillingsnummer,
            // TODO: hvilket avtalenummer?
            avtalenummer = avtale.avtalenummer,
            belop = tilsagn.beregning.output.belop,
            periode = Periode(tilsagn.periodeStart, tilsagn.periodeSlutt),
            // TODO: hvem har opprettet/besluttet?
            opprettetAv = OkonomiPart.NavAnsatt(NavIdent("Z123456")),
            opprettetTidspunkt = LocalDateTime.now(),
            besluttetAv = OkonomiPart.NavAnsatt(NavIdent("Z123456")),
            besluttetTidspunkt = LocalDateTime.now(),
        )

        okonomi.publishBestilling(bestilling)
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
