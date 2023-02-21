package no.nav.mulighetsrommet.arena.adapter.events

import kotlinx.serialization.json.JsonElement
import no.nav.mulighetsrommet.arena.adapter.ConsumerConfig
import no.nav.mulighetsrommet.arena.adapter.kafka.TopicConsumer
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent
import no.nav.mulighetsrommet.arena.adapter.services.ArenaEventService

class ArenaEventConsumer(
    override val config: ConsumerConfig,
    private val arenaEventService: ArenaEventService,
) : TopicConsumer() {
    override suspend fun run(event: JsonElement) {
        val data = ArenaEvent.decodeFromJson(event)
        arenaEventService.processEvent(data)
    }
}
