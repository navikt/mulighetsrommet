package no.nav.mulighetsrommet.arena.adapter.kafka

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import no.nav.mulighetsrommet.arena.adapter.ConsumerConfig

class TestConsumer(name: String) : TopicConsumer() {
    override val config: ConsumerConfig = ConsumerConfig(name, name, true)

    override suspend fun run(event: JsonElement) {
        val data = Json.decodeFromJsonElement<TestEvent>(event)

        assert(data.success)
    }

    data class TestEvent(
        val success: Boolean
    )
}
