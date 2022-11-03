package no.nav.mulighetsrommet.arena.adapter.kafka

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import no.nav.mulighetsrommet.arena.adapter.ConsumerConfig
import no.nav.mulighetsrommet.arena.adapter.repositories.EventRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class TestConsumer(name: String, override val events: EventRepository) : TopicConsumer<TestConsumer.TestEvent>() {
    override val logger: Logger = LoggerFactory.getLogger(javaClass)

    override val consumerConfig: ConsumerConfig = ConsumerConfig(name, name, true)

    override fun decodeEvent(payload: JsonElement): TestEvent = Json.decodeFromJsonElement(payload)

    override fun resolveKey(event: TestEvent): String = consumerConfig.id

    override suspend fun handleEvent(event: TestEvent) = assert(event.success)

    data class TestEvent(
        val success: Boolean
    )
}
