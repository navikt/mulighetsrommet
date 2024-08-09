package no.nav.mulighetsrommet.api.okonomi.tilsagn

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.mulighetsrommet.api.okonomi.BestillingDto
import no.nav.mulighetsrommet.api.okonomi.OkonomiClient
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.routes.v1.responses.*
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.domain.dto.NavIdent
import no.nav.mulighetsrommet.domain.dto.Organisasjonsnummer
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

class TilsagnService(
    private val tilsagnRepository: TilsagnRepository,
    private val tiltaksgjennomforingRepository: TiltaksgjennomforingRepository,
    private val validator: TilsagnValidator,
    private val db: Database,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

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
        val gjennomforing = tiltaksgjennomforingRepository.get(dto.tiltaksgjennomforingId)
        requireNotNull(gjennomforing)

        return db.transactionSuspend { tx ->
            // TODO: Setter som annullert uavhengig om den er sendt til okonomi. Man kunne
            // TODO: f. eks sjekket og satt som "avbrutt" i stedet.
            tilsagnRepository.setAnnullertTidspunkt(id, LocalDateTime.now(), tx)
            OkonomiClient.annullerOrder(lagOkonomiId(dto))
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
