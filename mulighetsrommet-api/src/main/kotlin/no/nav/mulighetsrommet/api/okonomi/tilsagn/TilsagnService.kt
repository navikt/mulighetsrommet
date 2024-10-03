package no.nav.mulighetsrommet.api.okonomi.tilsagn

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.mulighetsrommet.api.okonomi.BestillingDto
import no.nav.mulighetsrommet.api.okonomi.OkonomiClient
import no.nav.mulighetsrommet.api.okonomi.prismodell.Prismodell
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.responses.*
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

    fun aftTilsagnBeregning(input: AFTTilsagnBeregningInput): Either<List<ValidationError>, Int> {
        return validator.validateAFTBeregningInput(input)
            .map {
                Prismodell.AFT.beregnTilsagnBelop(
                    sats = input.sats,
                    antallPlasser = input.antallPlasser,
                    periodeStart = input.periodeStart,
                    periodeSlutt = input.periodeSlutt,
                )
            }
    }

    suspend fun beslutt(id: UUID, besluttelse: TilsagnBesluttelse, navIdent: NavIdent): StatusResponse<Unit> {
        val tilsagn = tilsagnRepository.get(id)
            ?: return NotFound("Fant ikke tilsagn").left()

        if (tilsagn.opprettetAv == navIdent) {
            return Forbidden("Kan ikke beslutte eget tilsagn").left()
        }

        if (tilsagn.besluttelse != null) {
            return BadRequest("Tilsagn allerede besluttet").left()
        }

        if (tilsagn.annullertTidspunkt != null) {
            return BadRequest("Tilsagn er annullert").left()
        }

        return db.transactionSuspend { tx ->
            tilsagnRepository.setBesluttelse(tilsagn.id, besluttelse, navIdent, LocalDateTime.now(), tx)
            if (besluttelse == TilsagnBesluttelse.GODKJENT) {
                lagOgSendBestilling(tilsagn)
            }
        }.right()
    }

    suspend fun annuller(id: UUID): StatusResponse<Unit> {
        val dto = tilsagnRepository.get(id)
            ?: return NotFound("Fant ikke tilsagn").left()

        return db.transactionSuspend { tx ->
            tilsagnRepository.setAnnullertTidspunkt(id, LocalDateTime.now(), tx)
            if (dto.besluttelse?.utfall == TilsagnBesluttelse.GODKJENT) {
                OkonomiClient.annullerOrder(lagOkonomiId(dto))
            }
        }.right()
    }

    fun getByGjennomforingId(gjennomforingId: UUID): List<TilsagnDto> =
        tilsagnRepository.getByGjennomforingId(gjennomforingId)

    fun get(id: UUID): TilsagnDto? =
        tilsagnRepository.get(id)

    private fun lagOkonomiId(tilsagn: TilsagnDto): String {
        return "T-${tilsagn.id}"
    }

    private suspend fun lagOgSendBestilling(tilsagn: TilsagnDto) {
        val gjennomforing = tiltaksgjennomforingRepository.get(tilsagn.tiltaksgjennomforingId)
        requireNotNull(gjennomforing) { "Fant ikke gjennomforing til tilsagn" }
        requireNotNull(gjennomforing.avtaleId) { "Fant ikke avtale til gjennomforingen" }

        OkonomiClient.sendBestilling(
            BestillingDto(
                okonomiId = lagOkonomiId(tilsagn),
                periodeStart = tilsagn.periodeStart,
                periodeSlutt = tilsagn.periodeSlutt,
                organisasjonsnummer = Organisasjonsnummer(gjennomforing.arrangor.organisasjonsnummer),
                kostnadSted = tilsagn.kostnadssted,
                belop = tilsagn.belop,
            ),
        )
    }
}
