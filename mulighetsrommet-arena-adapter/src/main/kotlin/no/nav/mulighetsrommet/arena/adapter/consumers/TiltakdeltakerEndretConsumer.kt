package no.nav.mulighetsrommet.arena.adapter.consumers

import arrow.core.continuations.either
import io.ktor.http.*
import kotlinx.serialization.json.JsonElement
import no.nav.mulighetsrommet.arena.adapter.ConsumerConfig
import no.nav.mulighetsrommet.arena.adapter.MulighetsrommetApiClient
import no.nav.mulighetsrommet.arena.adapter.models.ArenaEventData
import no.nav.mulighetsrommet.arena.adapter.models.ConsumptionError
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

class TiltakdeltakerEndretConsumer(
    override val config: ConsumerConfig,
    override val events: ArenaEventRepository,
    private val deltakere: DeltakerRepository,
    private val arenaEntityMappings: ArenaEntityMappingRepository,
    private val client: MulighetsrommetApiClient
) : ArenaTopicConsumer(
    "SIAMO.TILTAKDELTAKER"
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

    override suspend fun handleEvent(event: ArenaEvent) = either<ConsumptionError, Unit> {
        val decoded = ArenaEventData.decode<ArenaTiltakdeltaker>(event.payload)

        val mapping = arenaEntityMappings.get(event.arenaTable, event.arenaId) ?: arenaEntityMappings.insert(
            ArenaEntityMapping.Deltaker(event.arenaTable, event.arenaId, UUID.randomUUID())
        )

        val deltaker = decoded.data
            .toDeltaker(mapping.entityId)
            .let {
                if (decoded.operation == ArenaEventData.Operation.Delete) {
                    deltakere.delete(it)
                } else {
                    deltakere.upsert(it)
                }
            }
            .mapLeft { ConsumptionError.fromDatabaseOperationError(it) }
            .bind()

        // TODO: oppdater til ny api-modell
        val method = if (decoded.operation == ArenaEventData.Operation.Delete) HttpMethod.Delete else HttpMethod.Put
        client.sendRequest(method, "/api/v1/arena/deltaker", deltaker)
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
}
