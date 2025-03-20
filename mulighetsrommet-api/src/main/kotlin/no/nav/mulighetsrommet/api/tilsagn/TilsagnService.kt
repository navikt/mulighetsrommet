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
import no.nav.mulighetsrommet.api.tilsagn.api.BesluttTilsagnRequest
import no.nav.mulighetsrommet.api.tilsagn.api.TilAnnulleringRequest
import no.nav.mulighetsrommet.api.tilsagn.api.TilsagnRequest
import no.nav.mulighetsrommet.api.tilsagn.db.TilsagnDbo
import no.nav.mulighetsrommet.api.tilsagn.model.*
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Besluttelse
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
    fun upsert(request: TilsagnRequest, navIdent: NavIdent): Either<List<FieldError>, Tilsagn> = db.transaction {
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

        val totrinnskontroll = Totrinnskontroll(
            id = UUID.randomUUID(),
            entityId = request.id,
            behandletAv = navIdent,
            aarsaker = emptyList(),
            forklaring = null,
            type = Totrinnskontroll.Type.OPPRETT,
            behandletTidspunkt = LocalDateTime.now(),
            besluttelse = null,
            besluttetAv = null,
            besluttetTidspunkt = null,
        )

        validateTilsagnBeregningInput(gjennomforing, request.beregning)
            .flatMap { beregnTilsagn(it) }
            .map { beregning ->
                val lopenummer = previous?.lopenummer
                    ?: queries.tilsagn.getNextLopenummeByGjennomforing(gjennomforing.id)

                val bestillingsnummer = previous?.bestilling?.bestillingsnummer
                    ?: "A-${gjennomforing.lopenummer}-$lopenummer"

                TilsagnDbo(
                    id = request.id,
                    gjennomforingId = request.gjennomforingId,
                    type = request.type,
                    periode = Periode.fromInclusiveDates(request.periodeStart, request.periodeSlutt),
                    lopenummer = lopenummer,
                    kostnadssted = request.kostnadssted,
                    bestillingsnummer = bestillingsnummer,
                    bestillingStatus = null,
                    beregning = beregning,
                )
            }
            .flatMap { dbo ->
                TilsagnValidator.validate(dbo, previous)
            }
            .map { dbo ->
                queries.tilsagn.upsert(dbo)
                queries.totrinnskontroll.upsert(totrinnskontroll)

                val dto = getOrError(dbo.id)

                logEndring("Sendt til godkjenning", dto, navIdent)
                dto
            }
    }

    fun tilAnnulleringRequest(id: UUID, navIdent: NavIdent, request: TilAnnulleringRequest) = db.transaction {
        val tilsagn = queries.tilsagn.get(id) ?: throw StatusException(HttpStatusCode.NotFound, "Fant ikke tilsagn")

        setTilAnnullering(tilsagn, navIdent, request.aarsaker.map { it.name }, request.forklaring)
    }

    fun tilGjorOppRequest(id: UUID, navIdent: NavIdent, request: TilAnnulleringRequest) = db.transaction {
        val tilsagn = queries.tilsagn.get(id) ?: throw StatusException(HttpStatusCode.NotFound, "Fant ikke tilsagn")

        setTilOppgjort(tilsagn, navIdent, request.aarsaker.map { it.name }, request.forklaring)
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

    fun beslutt(id: UUID, besluttelse: BesluttTilsagnRequest, navIdent: NavIdent): StatusResponse<Tilsagn> = db.transaction {
        val tilsagn = queries.tilsagn.get(id) ?: return NotFound("Fant ikke tilsagn").left()

        return when (tilsagn.status) {
            TilsagnStatus.OPPGJORT, TilsagnStatus.ANNULLERT, TilsagnStatus.GODKJENT, TilsagnStatus.RETURNERT ->
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

            TilsagnStatus.TIL_OPPGJOR -> {
                when (besluttelse.besluttelse) {
                    Besluttelse.GODKJENT ->
                        gjorOppTilsagn(tilsagn, navIdent)
                            .right()
                            .also {
                                // Ved manuell oppgjør må vi sende melding til OeBS, det trenger vi ikke
                                // når vi gjør opp på en delutbetaling.
                                okonomi.scheduleBehandleOppgjortTilsagn(tilsagn.id, session)
                            }

                    Besluttelse.AVVIST -> avvisOppgjor(tilsagn, navIdent).right()
                }
            }
        }
    }

    private fun godkjennTilsagn(tilsagn: Tilsagn, besluttetAv: NavIdent): StatusResponse<Tilsagn> = db.transaction {
        require(tilsagn.status == TilsagnStatus.TIL_GODKJENNING)

        val opprettelse = queries.totrinnskontroll.getOrError(tilsagn.id, Totrinnskontroll.Type.OPPRETT)
        if (besluttetAv == opprettelse.behandletAv) {
            return Forbidden("Kan ikke beslutte eget tilsagn").left()
        }

        queries.totrinnskontroll.upsert(
            opprettelse.copy(
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
        tilsagn: Tilsagn,
        besluttelse: BesluttTilsagnRequest.AvvistTilsagnRequest,
        besluttetAv: NavIdent,
    ): StatusResponse<Tilsagn> = db.transaction {
        require(tilsagn.status == TilsagnStatus.TIL_GODKJENNING)

        val opprettelse = queries.totrinnskontroll.getOrError(tilsagn.id, Totrinnskontroll.Type.OPPRETT)
        if (besluttetAv == opprettelse.behandletAv) {
            return Forbidden("Kan ikke beslutte eget tilsagn").left()
        }
        if (besluttelse.aarsaker.isEmpty()) {
            return BadRequest(detail = "Årsaker er påkrevd").left()
        }

        queries.totrinnskontroll.upsert(
            opprettelse.copy(
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

    private fun QueryContext.annullerTilsagn(tilsagn: Tilsagn, besluttetAv: NavIdent): Tilsagn {
        require(tilsagn.status == TilsagnStatus.TIL_ANNULLERING)

        val annullering = queries.totrinnskontroll.getOrError(tilsagn.id, Totrinnskontroll.Type.ANNULLER)
        require(besluttetAv != annullering.behandletAv) {
            "Kan ikke beslutte eget tilsagn"
        }

        queries.totrinnskontroll.upsert(
            annullering.copy(
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

    private fun QueryContext.avvisAnnullering(tilsagn: Tilsagn, besluttetAv: Agent): Tilsagn {
        require(tilsagn.status == TilsagnStatus.TIL_ANNULLERING)

        val annullering = queries.totrinnskontroll.getOrError(tilsagn.id, Totrinnskontroll.Type.ANNULLER)
        require(besluttetAv != annullering.behandletAv) {
            "Kan ikke beslutte eget tilsagn"
        }

        queries.totrinnskontroll.upsert(
            annullering.copy(
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

    private fun QueryContext.setTilOppgjort(
        tilsagn: Tilsagn,
        agent: Agent,
        aarsaker: List<String>,
        forklaring: String?,
    ): Tilsagn {
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
                type = Totrinnskontroll.Type.GJOR_OPP,
                behandletTidspunkt = LocalDateTime.now(),
                besluttelse = null,
                besluttetAv = null,
                besluttetTidspunkt = null,
            ),
        )
        queries.tilsagn.setStatus(tilsagn.id, TilsagnStatus.TIL_OPPGJOR)

        val dto = getOrError(tilsagn.id)
        logEndring("Sendt til oppgjør", dto, agent)
        return dto
    }

    private fun QueryContext.gjorOppTilsagn(
        tilsagn: Tilsagn,
        besluttetAv: Agent,
    ): Tilsagn {
        require(tilsagn.status == TilsagnStatus.TIL_OPPGJOR)

        val oppgjor = queries.totrinnskontroll.getOrError(tilsagn.id, Totrinnskontroll.Type.GJOR_OPP)
        require(besluttetAv !is NavIdent || besluttetAv != oppgjor.behandletAv) {
            "Kan ikke beslutte eget tilsagn"
        }

        queries.totrinnskontroll.upsert(
            oppgjor.copy(
                besluttetAv = besluttetAv,
                besluttetTidspunkt = LocalDateTime.now(),
                besluttelse = Besluttelse.GODKJENT,
            ),
        )
        queries.tilsagn.setStatus(tilsagn.id, TilsagnStatus.OPPGJORT)

        val dto = getOrError(tilsagn.id)
        logEndring("Tilsagn oppgjort", dto, besluttetAv)
        return dto
    }

    private fun avvisOppgjor(tilsagn: Tilsagn, besluttetAv: Agent): Tilsagn = db.transaction {
        require(tilsagn.status == TilsagnStatus.TIL_OPPGJOR)

        val oppgjor = queries.totrinnskontroll.getOrError(tilsagn.id, Totrinnskontroll.Type.GJOR_OPP)
        require(besluttetAv != oppgjor.behandletAv) {
            "Kan ikke beslutte eget tilsagn"
        }

        queries.totrinnskontroll.upsert(
            oppgjor.copy(
                besluttetAv = besluttetAv,
                besluttetTidspunkt = LocalDateTime.now(),
                besluttelse = Besluttelse.AVVIST,
            ),
        )
        queries.tilsagn.setStatus(tilsagn.id, TilsagnStatus.GODKJENT)

        val dto = getOrError(tilsagn.id)
        logEndring("Oppgjør avvist", dto, besluttetAv)
        return dto
    }

    fun gjorOppAutomatisk(id: UUID, queryContext: QueryContext): Tilsagn {
        var tilsagn = requireNotNull(queryContext.queries.tilsagn.get(id))

        tilsagn = queryContext.setTilOppgjort(tilsagn, Tiltaksadministrasjon, emptyList(), null)

        return queryContext.gjorOppTilsagn(tilsagn, Tiltaksadministrasjon)
    }

    private fun QueryContext.setTilAnnullering(
        tilsagn: Tilsagn,
        behandletAv: Agent,
        aarsaker: List<String>,
        forklaring: String?,
    ): Tilsagn {
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
        dto: Tilsagn,
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

    private fun QueryContext.getOrError(id: UUID): Tilsagn {
        return requireNotNull(queries.tilsagn.get(id)) { "Tilsagn med id=$id finnes ikke" }
    }
}
