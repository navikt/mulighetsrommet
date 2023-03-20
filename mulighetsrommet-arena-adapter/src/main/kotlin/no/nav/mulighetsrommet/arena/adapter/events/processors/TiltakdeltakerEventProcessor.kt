package no.nav.mulighetsrommet.arena.adapter.events.processors

import arrow.core.*
import arrow.core.continuations.either
import io.ktor.http.*
import no.nav.mulighetsrommet.arena.adapter.MulighetsrommetApiClient
import no.nav.mulighetsrommet.arena.adapter.clients.ArenaOrdsProxyClient
import no.nav.mulighetsrommet.arena.adapter.models.ProcessingError
import no.nav.mulighetsrommet.arena.adapter.models.ProcessingResult
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTable
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTiltakdeltaker
import no.nav.mulighetsrommet.arena.adapter.models.db.*
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEntityMapping.Status.Handled
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEntityMapping.Status.Ignored
import no.nav.mulighetsrommet.arena.adapter.services.ArenaEntityService
import no.nav.mulighetsrommet.arena.adapter.utils.AktivitetsplanenLaunchDate
import no.nav.mulighetsrommet.arena.adapter.utils.ArenaUtils
import no.nav.mulighetsrommet.domain.Tiltakskoder.isGruppetiltak
import no.nav.mulighetsrommet.domain.dbo.TiltakshistorikkDbo
import java.util.*

class TiltakdeltakerEventProcessor(
    private val entities: ArenaEntityService,
    private val client: MulighetsrommetApiClient,
    private val ords: ArenaOrdsProxyClient
) : ArenaEventProcessor {
    override val arenaTable: ArenaTable = ArenaTable.Deltaker

    override suspend fun handleEvent(event: ArenaEvent) = either {
        val data = event.decodePayload<ArenaTiltakdeltaker>()

        if (isRegisteredBeforeAktivitetsplanen(data)) {
            return@either ProcessingResult(Ignored, "Deltaker ignorert fordi den registrert før Aktivitetsplanen")
        }

        val tiltaksgjennomforingIsIgnored = entities
            .isIgnored(ArenaTable.Tiltaksgjennomforing, data.TILTAKGJENNOMFORING_ID.toString())
            .bind()
        if (tiltaksgjennomforingIsIgnored) {
            return@either ProcessingResult(Ignored, "Deltaker ignorert fordi tilhørende tiltaksgjennomføring også er ignorert")
        }

        val mapping = entities.getMapping(event.arenaTable, event.arenaId).bind()
        val deltaker = data
            .toDeltaker(mapping.entityId)
            .flatMap { entities.upsertDeltaker(it) }
            .bind()

        val tiltaksgjennomforingMapping = entities
            .getMapping(ArenaTable.Tiltaksgjennomforing, data.TILTAKGJENNOMFORING_ID.toString())
            .bind()
        val tiltaksgjennomforing = entities
            .getTiltaksgjennomforing(tiltaksgjennomforingMapping.entityId)
            .bind()
        val norskIdent = ords.getFnr(deltaker.personId)
            .mapLeft { ProcessingError.fromResponseException(it) }
            .flatMap { it?.right() ?: ProcessingError.InvalidPayload("Fant ikke norsk ident i Arena ORDS").left() }
            .map { it.fnr }
            .bind()
        val tiltakstypeMapping = entities
            .getMapping(ArenaTable.Tiltakstype, tiltaksgjennomforing.tiltakskode)
            .bind()
        val tiltakstype = entities
            .getTiltakstype(tiltakstypeMapping.entityId)
            .bind()
        val tiltakshistorikk = deltaker
            .toTiltakshistorikkDbo(tiltakstype, tiltaksgjennomforing, norskIdent)
            .bind()

        val response = if (event.operation == ArenaEvent.Operation.Delete) {
            client.request<Any>(HttpMethod.Delete, "/api/v1/internal/arena/tiltakshistorikk/${tiltakshistorikk.id}")
        } else {
            client.request(HttpMethod.Put, "/api/v1/internal/arena/tiltakshistorikk", tiltakshistorikk)
        }
        response.mapLeft { ProcessingError.fromResponseException(it) }
            .map { ProcessingResult(Handled) }
            .bind()
    }

    override suspend fun deleteEntity(event: ArenaEvent): Either<ProcessingError, Unit> = either {
        val mapping = entities.getMapping(event.arenaTable, event.arenaId).bind()
        client.request<Any>(HttpMethod.Delete, "/api/v1/internal/arena/tiltakshistorikk/${mapping.entityId}")
            .mapLeft { ProcessingError.fromResponseException(it) }
            .flatMap { entities.deleteDeltaker(mapping.entityId) }
            .bind()
    }

    private fun isRegisteredBeforeAktivitetsplanen(data: ArenaTiltakdeltaker): Boolean {
        return ArenaUtils.parseTimestamp(data.REG_DATO).isBefore(AktivitetsplanenLaunchDate)
    }

    private fun ArenaTiltakdeltaker.toDeltaker(id: UUID) = Either
        .catch {
            Deltaker(
                id = id,
                tiltaksdeltakerId = TILTAKDELTAKER_ID,
                tiltaksgjennomforingId = TILTAKGJENNOMFORING_ID,
                personId = PERSON_ID,
                fraDato = ArenaUtils.parseNullableTimestamp(DATO_FRA),
                tilDato = ArenaUtils.parseNullableTimestamp(DATO_TIL),
                registrertDato = ArenaUtils.parseTimestamp(REG_DATO),
                status = ArenaUtils.toDeltakerstatus(DELTAKERSTATUSKODE)
            )
        }
        .mapLeft { ProcessingError.InvalidPayload(it.localizedMessage) }

    private suspend fun Deltaker.toTiltakshistorikkDbo(
        tiltakstype: Tiltakstype,
        tiltaksgjennomforing: Tiltaksgjennomforing,
        norskIdent: String
    ) = either<ProcessingError, TiltakshistorikkDbo> {
        if (isGruppetiltak(tiltakstype.tiltakskode)) {
            TiltakshistorikkDbo.Gruppetiltak(
                id = id,
                norskIdent = norskIdent,
                status = status,
                fraDato = fraDato,
                tilDato = tilDato,
                tiltaksgjennomforingId = tiltaksgjennomforing.id
            )
        } else {
            val virksomhetsnummer = tiltaksgjennomforing.arrangorId.let { id ->
                ords.getArbeidsgiver(id)
                    .mapLeft { ProcessingError.fromResponseException(it) }
                    .flatMap { it?.right() ?: ProcessingError.InvalidPayload("Fant ikke arrangør i Arena ORDS").left() }
                    .map { it.virksomhetsnummer }
                    .bind()
            }
            TiltakshistorikkDbo.IndividueltTiltak(
                id = id,
                norskIdent = norskIdent,
                status = status,
                fraDato = fraDato,
                tilDato = tilDato,
                beskrivelse = tiltaksgjennomforing.navn,
                tiltakstypeId = tiltakstype.id,
                virksomhetsnummer = virksomhetsnummer
            )
        }
    }
}
