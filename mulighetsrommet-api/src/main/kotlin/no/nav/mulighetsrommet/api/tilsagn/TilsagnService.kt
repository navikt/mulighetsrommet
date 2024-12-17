package no.nav.mulighetsrommet.api.tilsagn

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotliquery.TransactionalSession
import no.nav.mulighetsrommet.api.domain.dto.EndringshistorikkDto
import no.nav.mulighetsrommet.api.gjennomforing.db.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.okonomi.BestillingDto
import no.nav.mulighetsrommet.api.okonomi.OkonomiClient
import no.nav.mulighetsrommet.api.okonomi.Prismodell
import no.nav.mulighetsrommet.api.okonomi.Prismodell.TilsagnBeregning
import no.nav.mulighetsrommet.api.refusjon.model.RefusjonskravPeriode
import no.nav.mulighetsrommet.api.responses.*
import no.nav.mulighetsrommet.api.services.DocumentClass
import no.nav.mulighetsrommet.api.services.EndretAv
import no.nav.mulighetsrommet.api.services.EndringshistorikkService
import no.nav.mulighetsrommet.api.tilsagn.db.TilsagnRepository
import no.nav.mulighetsrommet.api.tilsagn.model.*
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.domain.dto.NavIdent
import no.nav.mulighetsrommet.domain.dto.Organisasjonsnummer
import java.time.LocalDateTime
import java.util.*

class TilsagnService(
    private val tilsagnRepository: TilsagnRepository,
    private val tiltaksgjennomforingRepository: TiltaksgjennomforingRepository,
    private val validator: TilsagnValidator,
    private val endringshistorikkService: EndringshistorikkService,
    private val db: Database,
) {
    suspend fun upsert(request: TilsagnRequest, navIdent: NavIdent): Either<List<ValidationError>, TilsagnDto> {
        val previous = tilsagnRepository.get(request.id)

        return validator.validate(request, previous, navIdent)
            .map {
                db.transactionSuspend { tx ->
                    tilsagnRepository.upsert(it, tx)

                    val dto = getOrError(it.id, tx)

                    logEndring(
                        if (previous == null) {
                            "Opprettet tilsagn"
                        } else {
                            "Redigerte tilsagn"
                        },
                        dto,
                        EndretAv.NavAnsatt(navIdent),
                        tx,
                    )

                    dto
                }
            }
    }

    fun tilsagnBeregning(input: TilsagnBeregningInput): Either<List<ValidationError>, TilsagnBeregning> {
        return validator.validateBeregningInput(input)
            .map {
                when (input) {
                    is TilsagnBeregningInput.AFT -> aftTilsagnBeregning(input)
                    is TilsagnBeregningInput.Fri -> TilsagnBeregning.Fri(input.belop)
                }
            }
    }

    private fun aftTilsagnBeregning(input: TilsagnBeregningInput.AFT): TilsagnBeregning.AFT {
        val sats = Prismodell.AFT.findSats(input.periodeStart)
        val belop = Prismodell.AFT.beregnTilsagnBelop(
            sats = sats,
            antallPlasser = input.antallPlasser,
            periodeStart = input.periodeStart,
            periodeSlutt = input.periodeSlutt,
        )

        return TilsagnBeregning.AFT(
            sats = sats,
            antallPlasser = input.antallPlasser,
            periodeStart = input.periodeStart,
            periodeSlutt = input.periodeSlutt,
            belop = belop,
        )
    }

    suspend fun beslutt(id: UUID, besluttelse: BesluttTilsagnRequest, navIdent: NavIdent): StatusResponse<Unit> {
        val tilsagn = tilsagnRepository.get(id)
            ?: return NotFound("Fant ikke tilsagn").left()

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

    private suspend fun godkjennTilsagn(tilsagn: TilsagnDto, navIdent: NavIdent): StatusResponse<Unit> {
        require(tilsagn.status is TilsagnDto.TilsagnStatus.TilGodkjenning)
        if (navIdent == tilsagn.status.endretAv) {
            return Forbidden("Kan ikke beslutte eget tilsagn").left()
        }

        return db.transactionSuspend { tx ->
            tilsagnRepository.besluttGodkjennelse(
                tilsagn.id,
                navIdent,
                LocalDateTime.now(),
                tx,
            )
            lagOgSendBestilling(tilsagn)
            logEndring("Tilsagn godkjent", getOrError(tilsagn.id, tx), EndretAv.NavAnsatt(navIdent), tx)
        }.right()
    }

    private fun returnerTilsagn(tilsagn: TilsagnDto, besluttelse: BesluttTilsagnRequest.AvvistTilsagnRequest, navIdent: NavIdent): StatusResponse<Unit> {
        require(tilsagn.status is TilsagnDto.TilsagnStatus.TilGodkjenning)
        if (navIdent == tilsagn.status.endretAv) {
            return Forbidden("Kan ikke beslutte eget tilsagn").left()
        }
        if (besluttelse.aarsaker.isEmpty()) {
            return BadRequest(message = "Årsaker er påkrevd").left()
        }

        db.transaction { tx ->
            tilsagnRepository.returner(
                tilsagn.id,
                navIdent,
                LocalDateTime.now(),
                besluttelse.aarsaker,
                besluttelse.forklaring,
                tx,
            )
            logEndring("Tilsagn returnert", getOrError(tilsagn.id, tx), EndretAv.NavAnsatt(navIdent), tx)
        }

        return Unit.right()
    }

    private fun annullerTilsagn(tilsagn: TilsagnDto, navIdent: NavIdent): StatusResponse<Unit> {
        require(tilsagn.status is TilsagnDto.TilsagnStatus.TilAnnullering)
        if (navIdent == tilsagn.status.endretAv) {
            return Forbidden("Kan ikke beslutte eget tilsagn").left()
        }

        db.transaction { tx ->
            tilsagnRepository.besluttAnnullering(
                tilsagn.id,
                navIdent,
                LocalDateTime.now(),
                tx,
            )
            logEndring("Tilsagn annullert", getOrError(tilsagn.id, tx), EndretAv.NavAnsatt(navIdent), tx)
        }

        return Unit.right()
    }

    private fun avvisAnnullering(tilsagn: TilsagnDto, navIdent: NavIdent): StatusResponse<Unit> {
        require(tilsagn.status is TilsagnDto.TilsagnStatus.TilAnnullering)
        if (navIdent == tilsagn.status.endretAv) {
            return Forbidden("Kan ikke beslutte eget tilsagn").left()
        }

        db.transaction { tx ->
            tilsagnRepository.avbrytAnnullering(
                tilsagn.id,
                navIdent,
                LocalDateTime.now(),
                tx,
            )
            logEndring("Annullering avvist", getOrError(tilsagn.id, tx), EndretAv.NavAnsatt(navIdent), tx)
        }

        return Unit.right()
    }

    fun tilAnnullering(id: UUID, navIdent: NavIdent, request: TilAnnulleringRequest): StatusResponse<Unit> {
        val dto = tilsagnRepository.get(id)
            ?: return NotFound("Fant ikke tilsagn").left()
        if (dto.status !is TilsagnDto.TilsagnStatus.Godkjent) {
            return BadRequest("Kan bare annullere godkjente tilsagn").left()
        }

        db.transaction { tx ->
            tilsagnRepository.tilAnnullering(id, navIdent, LocalDateTime.now(), request.aarsaker, request.forklaring, tx)
            logEndring("Sendt til annullering", getOrError(id, tx), EndretAv.NavAnsatt(navIdent), tx)
        }

        return Unit.right()
    }

    fun slettTilsagn(id: UUID): Either<StatusResponseError, Unit> {
        val dto = tilsagnRepository.get(id)
            ?: return NotFound("Fant ikke tilsagn").left()

        if (dto.status !is TilsagnDto.TilsagnStatus.Returnert) {
            return BadRequest("Kan ikke slette tilsagn som er godkjent").left()
        }

        return tilsagnRepository.delete(id).right()
    }

    fun getAllArrangorflateTilsagn(organisasjonsnummer: Organisasjonsnummer): List<ArrangorflateTilsagn> {
        return tilsagnRepository.getAllArrangorflateTilsagn(organisasjonsnummer)
    }

    fun getArrangorflateTilsagnTilRefusjon(
        gjennomforingId: UUID,
        periode: RefusjonskravPeriode,
    ): List<ArrangorflateTilsagn> {
        return tilsagnRepository.getArrangorflateTilsagnTilRefusjon(gjennomforingId, periode)
    }

    fun getArrangorflateTilsagn(id: UUID): ArrangorflateTilsagn? = tilsagnRepository.getArrangorflateTilsagn(id)

    fun getByGjennomforingId(gjennomforingId: UUID): List<TilsagnDto> = tilsagnRepository.getByGjennomforingId(gjennomforingId)

    fun get(id: UUID): TilsagnDto? = tilsagnRepository.get(id)

    private fun lagOkonomiId(tilsagn: TilsagnDto): String {
        return "T-${tilsagn.id}"
    }

    private suspend fun lagOgSendBestilling(tilsagn: TilsagnDto) {
        val gjennomforing = tiltaksgjennomforingRepository.get(tilsagn.tiltaksgjennomforing.id)
        requireNotNull(gjennomforing) { "Fant ikke gjennomforing til tilsagn" }
        requireNotNull(gjennomforing.avtaleId) { "Fant ikke avtale til gjennomforingen" }

        OkonomiClient.sendBestilling(
            BestillingDto(
                okonomiId = lagOkonomiId(tilsagn),
                periodeStart = tilsagn.periodeStart,
                periodeSlutt = tilsagn.periodeSlutt,
                organisasjonsnummer = gjennomforing.arrangor.organisasjonsnummer,
                kostnadSted = tilsagn.kostnadssted,
                belop = tilsagn.beregning.belop,
            ),
        )
    }

    fun getEndringshistorikk(id: UUID): EndringshistorikkDto = endringshistorikkService.getEndringshistorikk(DocumentClass.TILSAGN, id)

    private fun logEndring(
        operation: String,
        dto: TilsagnDto,
        endretAv: EndretAv,
        tx: TransactionalSession,
    ) {
        endringshistorikkService.logEndring(
            tx,
            DocumentClass.TILSAGN,
            operation,
            endretAv,
            dto.id,
        ) {
            Json.encodeToJsonElement(dto)
        }
    }

    private fun getOrError(id: UUID, tx: TransactionalSession): TilsagnDto {
        val dto = tilsagnRepository.get(id, tx)
        return requireNotNull(dto) { "Tilsagn med id=$id finnes ikke" }
    }
}
