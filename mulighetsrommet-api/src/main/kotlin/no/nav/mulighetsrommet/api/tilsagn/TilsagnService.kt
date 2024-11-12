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
        val belop = Prismodell.AFT.beregnTilsagnBelop(
            sats = input.sats,
            antallPlasser = input.antallPlasser,
            periodeStart = input.periodeStart,
            periodeSlutt = input.periodeSlutt,
        )

        return TilsagnBeregning.AFT(
            sats = input.sats,
            antallPlasser = input.antallPlasser,
            periodeStart = input.periodeStart,
            periodeSlutt = input.periodeSlutt,
            belop = belop,
        )
    }

    suspend fun beslutt(id: UUID, besluttelse: BesluttTilsagnRequest, navIdent: NavIdent): StatusResponse<Unit> {
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

        when (besluttelse) {
            is BesluttTilsagnRequest.GodkjentTilsagnRequest -> Unit
            is BesluttTilsagnRequest.AvvistTilsagnRequest -> {
                if (besluttelse.aarsaker.contains(AvvistTilsagnAarsak.FEIL_ANNET) && besluttelse.forklaring.isNullOrBlank()) {
                    return BadRequest("Forklaring må oppgis når årsak er 'Annet' ved avvist tilsagn").left()
                }
            }
        }

        return db.transactionSuspend { tx ->
            tilsagnRepository.setBesluttelse(tilsagn.id, besluttelse, navIdent, LocalDateTime.now(), tx)

            when (besluttelse) {
                is BesluttTilsagnRequest.GodkjentTilsagnRequest -> lagOgSendBestilling(tilsagn)
                is BesluttTilsagnRequest.AvvistTilsagnRequest -> Unit
            }
        }.right()
    }

    suspend fun annuller(id: UUID): StatusResponse<Unit> {
        val dto = tilsagnRepository.get(id)
            ?: return NotFound("Fant ikke tilsagn").left()

        return db.transactionSuspend { tx ->
            tilsagnRepository.setAnnullertTidspunkt(id, LocalDateTime.now(), tx)

            if (dto.besluttelse?.status == TilsagnBesluttelseStatus.GODKJENT) {
                OkonomiClient.annullerOrder(lagOkonomiId(dto))
            }
        }.right()
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
