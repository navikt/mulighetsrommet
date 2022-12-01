package no.nav.mulighetsrommet.arena.adapter.consumers

import arrow.core.continuations.either
import arrow.core.continuations.ensureNotNull
import arrow.core.leftIfNull
import io.ktor.http.*
import kotlinx.serialization.json.JsonElement
import no.nav.mulighetsrommet.arena.adapter.ConsumerConfig
import no.nav.mulighetsrommet.arena.adapter.MulighetsrommetApiClient
import no.nav.mulighetsrommet.arena.adapter.clients.ArenaOrdsProxyClient
import no.nav.mulighetsrommet.arena.adapter.models.ArenaEventData
import no.nav.mulighetsrommet.arena.adapter.models.ConsumptionError
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTables
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTiltakdeltaker
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEntityMapping
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent
import no.nav.mulighetsrommet.arena.adapter.models.db.Deltaker
import no.nav.mulighetsrommet.arena.adapter.repositories.ArenaEntityMappingRepository
import no.nav.mulighetsrommet.arena.adapter.repositories.ArenaEventRepository
import no.nav.mulighetsrommet.arena.adapter.repositories.DeltakerRepository
import no.nav.mulighetsrommet.arena.adapter.utils.ProcessingUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import no.nav.mulighetsrommet.domain.models.Deltaker as MrDeltaker

class TiltakdeltakerEndretConsumer(
    override val config: ConsumerConfig,
    override val events: ArenaEventRepository,
    private val deltakere: DeltakerRepository,
    private val arenaEntityMappings: ArenaEntityMappingRepository,
    private val client: MulighetsrommetApiClient,
    private val ords: ArenaOrdsProxyClient
) : ArenaTopicConsumer(
    ArenaTables.Deltaker
) {

    override val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun decodeArenaData(payload: JsonElement): ArenaEvent {
        val decoded = ArenaEventData.decode<ArenaTiltakdeltaker>(payload)

        return ArenaEvent(
            arenaTable = decoded.table,
            arenaId = decoded.data.TILTAKDELTAKER_ID.toString(),
            payload = payload,
            status = ArenaEvent.ConsumptionStatus.Pending,
        )
    }

    override suspend fun handleEvent(event: ArenaEvent) = either<ConsumptionError, ArenaEvent.ConsumptionStatus> {
        val decoded = ArenaEventData.decode<ArenaTiltakdeltaker>(event.payload)

        // TODO: burde status Ignored settes på ArenaEntityMapping i stedet?
        val tiltaksgjennomforing = events.get(
            ArenaTables.Tiltaksgjennomforing,
            decoded.data.TILTAKGJENNOMFORING_ID.toString()
        )
        ensureNotNull(tiltaksgjennomforing) {
            ConsumptionError.MissingDependency("Tiltaksgjennomføring har enda ikke blitt prosessert")
        }
        ensure(tiltaksgjennomforingHasNotBeenIgnored(tiltaksgjennomforing)) {
            ConsumptionError.Ignored("Deltaker ignorert fordi tilhørende tiltaksgjennomføring også er ignorert")
        }

        val mapping = arenaEntityMappings.get(event.arenaTable, event.arenaId) ?: arenaEntityMappings.insert(
            ArenaEntityMapping.Deltaker(event.arenaTable, event.arenaId, UUID.randomUUID())
        )
        val deltaker = decoded.data
            .toDeltaker(mapping.entityId)
            .let { deltakere.upsert(it) }
            .mapLeft { ConsumptionError.fromDatabaseOperationError(it) }
            .bind()

        val tiltaksgjennomforingMapping = arenaEntityMappings.get(
            ArenaTables.Tiltaksgjennomforing,
            decoded.data.TILTAKGJENNOMFORING_ID.toString()
        )
        ensureNotNull(tiltaksgjennomforingMapping) {
            ConsumptionError.MissingDependency("Tiltaksgjennomføring har enda ikke blitt prosessert")
        }

        val norskIdent = ords.getFnr(deltaker.personId)
            .mapLeft { ConsumptionError.fromResponseException(it) }
            .leftIfNull { ConsumptionError.InvalidPayload("Fant ikke norsk ident i Arena ORDS for Arena personId=${deltaker.personId}") }
            .bind()

        val mrDeltaker = deltaker.toDomain(tiltaksgjennomforingMapping.entityId, norskIdent)

        val method = if (decoded.operation == ArenaEventData.Operation.Delete) HttpMethod.Delete else HttpMethod.Put
        client.request(method, "/api/v1/arena/deltaker", mrDeltaker)
            .mapLeft { ConsumptionError.fromResponseException(it) }
            .map { ArenaEvent.ConsumptionStatus.Processed }
            .bind()
    }

    private fun tiltaksgjennomforingHasNotBeenIgnored(tiltaksgjennomforing: ArenaEvent): Boolean {
        return tiltaksgjennomforing.status != ArenaEvent.ConsumptionStatus.Ignored
    }

    private fun ArenaTiltakdeltaker.toDeltaker(id: UUID) = Deltaker(
        id = id,
        tiltaksdeltakerId = TILTAKDELTAKER_ID,
        tiltaksgjennomforingId = TILTAKGJENNOMFORING_ID,
        personId = PERSON_ID,
        fraDato = ProcessingUtils.getArenaDateFromTo(DATO_FRA),
        tilDato = ProcessingUtils.getArenaDateFromTo(DATO_TIL),
        status = ProcessingUtils.toDeltakerstatus(DELTAKERSTATUSKODE)
    )

    private fun Deltaker.toDomain(tiltaksgjennomforingId: UUID, norskIdent: String) = MrDeltaker(
        id = id,
        tiltaksgjennomforingId = tiltaksgjennomforingId,
        norskIdent = norskIdent,
        status = status,
        fraDato = fraDato,
        tilDato = tilDato,
    )
}
