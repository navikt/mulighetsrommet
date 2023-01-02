package no.nav.mulighetsrommet.arena.adapter.consumers

import arrow.core.Either
import arrow.core.getOrHandle
import kotlinx.serialization.json.JsonElement
import no.nav.mulighetsrommet.arena.adapter.kafka.TopicConsumer
import no.nav.mulighetsrommet.arena.adapter.metrics.Metrics
import no.nav.mulighetsrommet.arena.adapter.metrics.recordSuspend
import no.nav.mulighetsrommet.arena.adapter.models.ConsumptionError
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent
import no.nav.mulighetsrommet.arena.adapter.repositories.ArenaEventRepository
import org.slf4j.Logger

abstract class ArenaTopicConsumer(val arenaTable: String) : TopicConsumer() {
    abstract val logger: Logger
    abstract val events: ArenaEventRepository

    override suspend fun run(event: JsonElement) {
        Metrics.processArenaEventTimer(arenaTable).recordSuspend {
            val data = decodeArenaData(event)
            processEvent(data)
        }
    }

    suspend fun processEvent(e: ArenaEvent): ArenaEvent {
        logger.info("Persisting data: table=${e.arenaTable}, arena_id=${e.arenaId}")
        val event = events.upsert(e)

        logger.info("Handling event: table=${e.arenaTable}, key=${event.arenaId}")
        val (status, message) = handleEvent(event)
            .map { Pair(it, null) }
            .getOrHandle {
                logger.info("Event consumption failed: ${it.message}")
                Pair(it.status, it.message)
            }
        return events.upsert(event.copy(status = status, message = message))
    }

    abstract fun decodeArenaData(payload: JsonElement): ArenaEvent

    abstract suspend fun handleEvent(event: ArenaEvent): Either<ConsumptionError, ArenaEvent.ConsumptionStatus>
}
