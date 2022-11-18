package no.nav.mulighetsrommet.arena.adapter.kafka

import kotlinx.serialization.json.JsonElement
import no.nav.mulighetsrommet.arena.adapter.ConsumerConfig

abstract class TopicConsumer {
    abstract val config: ConsumerConfig

    abstract suspend fun run(event: JsonElement)
}
