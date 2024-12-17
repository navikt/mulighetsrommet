package no.nav.mulighetsrommet.api.tilsagn

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.mulighetsrommet.api.gjennomforing.db.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.okonomi.BestillingDto
import no.nav.mulighetsrommet.api.okonomi.OkonomiClient
import no.nav.mulighetsrommet.api.okonomi.Prismodell
import no.nav.mulighetsrommet.api.okonomi.Prismodell.TilsagnBeregning
import no.nav.mulighetsrommet.api.refusjon.model.RefusjonskravPeriode
import no.nav.mulighetsrommet.api.responses.*
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
    private val db: Database,
) {
    suspend fun upsert(request: TilsagnRequest, navIdent: NavIdent): Either<List<ValidationError>, TilsagnDto> {
        val previous = tilsagnRepository.get(request.id)

        return validator.validate(request, previous, navIdent)
            .map {
                db.transactionSuspend { tx ->
                    tilsagnRepository.upsert(it, tx)

                    val dto = tilsagnRepository.get(it.id, tx)
                    requireNotNull(dto) { "Fant ikke tilsagn etter upsert id=${it.id}" }

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
        }.right()
    }

    private fun returnerTilsagn(tilsagn: TilsagnDto, besluttelse: BesluttTilsagnRequest.AvvistTilsagnRequest, navIdent: NavIdent): StatusResponse<Unit> {
        require(tilsagn.status is TilsagnDto.TilsagnStatus.TilGodkjenning)
        if (navIdent == tilsagn.status.endretAv) {
            return Forbidden("Kan ikke beslutte eget tilsagn").left()
        }
        if (besluttelse.aarsaker.isNullOrEmpty()) {
            return BadRequest(message = "Årsaker er påkrevd").left()
        }
        tilsagnRepository.returner(
            tilsagn.id,
            navIdent,
            LocalDateTime.now(),
            besluttelse.aarsaker,
            besluttelse.forklaring,
        )
        return Unit.right()
    }

    private fun annullerTilsagn(tilsagn: TilsagnDto, navIdent: NavIdent): StatusResponse<Unit> {
        require(tilsagn.status is TilsagnDto.TilsagnStatus.TilAnnullering)
        if (navIdent == tilsagn.status.endretAv) {
            return Forbidden("Kan ikke beslutte eget tilsagn").left()
        }

        tilsagnRepository.besluttAnnullering(
            tilsagn.id,
            navIdent,
            LocalDateTime.now(),
        )
        return Unit.right()
    }

    private fun avvisAnnullering(tilsagn: TilsagnDto, navIdent: NavIdent): StatusResponse<Unit> {
        require(tilsagn.status is TilsagnDto.TilsagnStatus.TilAnnullering)
        if (navIdent == tilsagn.status.endretAv) {
            return Forbidden("Kan ikke beslutte eget tilsagn").left()
        }
        tilsagnRepository.avbrytAnnullering(
            tilsagn.id,
            navIdent,
            LocalDateTime.now(),
        )
        return Unit.right()
    }

    fun tilAnnullering(id: UUID, navIdent: NavIdent, request: TilAnnulleringRequest): StatusResponse<Unit> {
        val dto = tilsagnRepository.get(id)
            ?: return NotFound("Fant ikke tilsagn").left()
        if (dto.status !is TilsagnDto.TilsagnStatus.Godkjent) {
            return BadRequest("Kan bare annullere godkjente tilsagn").left()
        }

        tilsagnRepository.tilAnnullering(id, navIdent, LocalDateTime.now(), request.aarsaker, request.forklaring)
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
}
