package no.nav.mulighetsrommet.arena.adapter.events.processors

import arrow.core.Either
import arrow.core.continuations.either
import arrow.core.flatMap
import arrow.core.leftIfNull
import io.ktor.http.*
import no.nav.mulighetsrommet.arena.adapter.ConsumerConfig
import no.nav.mulighetsrommet.arena.adapter.MulighetsrommetApiClient
import no.nav.mulighetsrommet.arena.adapter.clients.ArenaOrdsProxyClient
import no.nav.mulighetsrommet.arena.adapter.models.ProcessingError
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTable
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTiltakdeltaker
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent
import no.nav.mulighetsrommet.arena.adapter.models.db.Deltaker
import no.nav.mulighetsrommet.arena.adapter.models.db.Tiltaksgjennomforing
import no.nav.mulighetsrommet.arena.adapter.models.db.Tiltakstype
import no.nav.mulighetsrommet.arena.adapter.repositories.ArenaEventRepository
import no.nav.mulighetsrommet.arena.adapter.services.ArenaEntityService
import no.nav.mulighetsrommet.arena.adapter.utils.AktivitetsplanenLaunchDate
import no.nav.mulighetsrommet.arena.adapter.utils.ArenaUtils
import no.nav.mulighetsrommet.domain.Tiltakskoder.isGruppetiltak
import no.nav.mulighetsrommet.domain.dbo.TiltakshistorikkDbo
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

class TiltakdeltakerEventProcessor(
    override val config: ConsumerConfig,
    override val events: ArenaEventRepository,
    private val entities: ArenaEntityService,
    private val client: MulighetsrommetApiClient,
    private val ords: ArenaOrdsProxyClient
) : ArenaEventProcessor(
    ArenaTable.Deltaker
) {

    override val logger: Logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handleEvent(event: ArenaEvent) = either<ProcessingError, ArenaEvent.ProcessingStatus> {
        val data = event.decodePayload<ArenaTiltakdeltaker>()

        ensure(isRegisteredAfterAktivitetsplanen(data)) {
            ProcessingError.Ignored("Deltaker ignorert fordi den registrert før Aktivitetsplanen")
        }

        val tiltaksgjennomforingIsIgnored = entities
            .isIgnored(ArenaTable.Tiltaksgjennomforing, data.TILTAKGJENNOMFORING_ID.toString())
            .bind()
        ensure(!tiltaksgjennomforingIsIgnored) {
            ProcessingError.Ignored("Deltaker ignorert fordi tilhørende tiltaksgjennomføring også er ignorert")
        }

        val mapping = entities.getOrCreateMapping(event)
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
            .map { it?.fnr }
            .leftIfNull { ProcessingError.InvalidPayload("Fant ikke norsk ident i Arena ORDS for Arena personId=${deltaker.personId}") }
            .bind()
        val tiltakstypeMapping = entities
            .getMapping(ArenaTable.Tiltakstype, tiltaksgjennomforing.tiltakskode)
            .bind()
        val tiltakstype = entities
            .getTiltakstype(tiltakstypeMapping.entityId)
            .bind()

        val tiltakshistorikk = if (isGruppetiltak(tiltakstype.tiltakskode)) {
            deltaker.toGruppeDbo(tiltaksgjennomforing, norskIdent)
        } else {
            val virksomhetsnummer = tiltaksgjennomforing.arrangorId?.let { id ->
                ords.getArbeidsgiver(id)
                    .mapLeft { ProcessingError.fromResponseException(it) }
                    .leftIfNull { ProcessingError.InvalidPayload("Fant ikke arrangør i Arena ORDS for arrangorId=${tiltaksgjennomforing.arrangorId}") }
                    .map { it.virksomhetsnummer }
                    .bind()
            }
            deltaker.toIndividuellDbo(tiltaksgjennomforing, tiltakstype, virksomhetsnummer, norskIdent)
        }

        val response = if (event.operation == ArenaEvent.Operation.Delete) {
            client.request<Any>(HttpMethod.Delete, "/api/v1/internal/arena/tiltakshistorikk/${tiltakshistorikk.id}")
        } else {
            client.request(HttpMethod.Put, "/api/v1/internal/arena/tiltakshistorikk", tiltakshistorikk)
        }
        response.mapLeft { ProcessingError.fromResponseException(it) }
            .map { ArenaEvent.ProcessingStatus.Processed }
            .bind()
    }

    override suspend fun deleteEntity(event: ArenaEvent): Either<ProcessingError, Unit> = either {
        val mapping = entities.getMapping(event.arenaTable, event.arenaId).bind()
        client.request<Any>(HttpMethod.Delete, "/api/v1/internal/arena/tiltakshistorikk/${mapping.entityId}")
            .mapLeft { ProcessingError.fromResponseException(it) }
            .flatMap { entities.deleteDeltaker(mapping.entityId) }
            .bind()
    }

    private fun isRegisteredAfterAktivitetsplanen(data: ArenaTiltakdeltaker): Boolean {
        return !ArenaUtils.parseTimestamp(data.REG_DATO).isBefore(AktivitetsplanenLaunchDate)
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
                status = ArenaUtils.toDeltakerstatus(DELTAKERSTATUSKODE)
            )
        }
        .mapLeft { ProcessingError.InvalidPayload(it.localizedMessage) }

    private fun Deltaker.toGruppeDbo(
        tiltaksgjennomforing: Tiltaksgjennomforing,
        norskIdent: String
    ): TiltakshistorikkDbo {
        return TiltakshistorikkDbo.Gruppetiltak(
            id = id,
            norskIdent = norskIdent,
            status = status,
            fraDato = fraDato,
            tilDato = tilDato,
            tiltaksgjennomforingId = tiltaksgjennomforing.id
        )
    }

    private fun Deltaker.toIndividuellDbo(
        tiltaksgjennomforing: Tiltaksgjennomforing,
        tiltakstype: Tiltakstype,
        virksomhetsnummer: String?,
        norskIdent: String
    ): TiltakshistorikkDbo {
        return TiltakshistorikkDbo.IndividueltTiltak(
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
