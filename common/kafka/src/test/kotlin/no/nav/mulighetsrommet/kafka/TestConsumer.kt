package no.nav.mulighetsrommet.kafka

import no.nav.common.kafka.consumer.util.deserializer.Deserializers.stringDeserializer

class TestConsumer(name: String) : KafkaTopicConsumer<String?, String>(
    Config(name, name, true), stringDeserializer(), stringDeserializer()
) {
    override suspend fun consume(key: String?, message: String) {
        if (message != "true") {
            throw RuntimeException("event must be 'true'")
        }
    }
}
