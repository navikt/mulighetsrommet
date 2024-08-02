package no.nav.mulighetsrommet.api.okonomi.tilsagn

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import kotliquery.Session
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
        return validator.validate(request, navIdent)
            .map {
                db.transactionSuspend { tx ->
                    tilsagnRepository.upsert(it, tx)

                    val dto = tilsagnRepository.get(it.id, tx)
                    requireNotNull(dto) { "Fant ikke tilsagn etter upsert id=${it.id}" }

                    // TODO: Sender med én gang, men her skal vi nok ha noe steg med beslutter
                    // TODO: i mellom, og sende i et annet endepunkt senere.
                    lagOgSendBestilling(dto, tx)
                    dto
                }
            }
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
            OkonomiClient.annullerOrder(lagOkonomiId(dto.lopenummer))
        }.right()
    }

    fun getByGjennomforingId(gjennomforingId: UUID): List<TilsagnDto> =
        tilsagnRepository.getByGjennomforingId(gjennomforingId)

    fun get(id: UUID): TilsagnDto? =
        tilsagnRepository.get(id)

    private fun lagOkonomiId(lopenummer: Int): String {
        return "T-$lopenummer" // TODO: Inkluder mer ting, som f. eks tiltaksnummer
    }

    private suspend fun lagOgSendBestilling(tilsagn: TilsagnDto, tx: Session) {
        val gjennomforing = tiltaksgjennomforingRepository.get(tilsagn.tiltaksgjennomforingId)
        requireNotNull(gjennomforing) { "Fant ikke gjennomforing til tilsagn" }

        OkonomiClient.sendBestilling(
            BestillingDto(
                okonomiId = lagOkonomiId(tilsagn.lopenummer),
                periodeStart = tilsagn.periodeStart,
                periodeSlutt = tilsagn.periodeSlutt,
                organisasjonsnummer = Organisasjonsnummer(gjennomforing.arrangor.organisasjonsnummer),
                kostnadSted = tilsagn.kostnadssted,
                belop = tilsagn.belop,
            ),
        )
        tilsagnRepository.setSendtTidspunkt(tilsagn.id, LocalDateTime.now(), tx)
    }
}
