package no.nav.mulighetsrommet.arena.adapter.kafka

import no.nav.common.kafka.consumer.util.deserializer.Deserializers.stringDeserializer
import no.nav.mulighetsrommet.arena.adapter.ConsumerConfig

class TestConsumer(name: String) : KafkaTopicConsumer<String, String>(
    ConsumerConfig(name, name, true), stringDeserializer(), stringDeserializer()
) {
    override suspend fun run(event: String) {
        if (event != "true") {
            throw RuntimeException("event must be 'true'")
        }
    }
}
