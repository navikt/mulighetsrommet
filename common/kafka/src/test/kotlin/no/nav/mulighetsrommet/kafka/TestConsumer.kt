package no.nav.mulighetsrommet.kafka

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.stringDeserializer
import no.nav.mulighetsrommet.kafka.serialization.JsonElementDeserializer

class TestConsumer : KafkaTopicConsumer<String?, String?>(
    stringDeserializer(),
    stringDeserializer(),
) {
    override suspend fun consume(key: String?, message: String?) {
        if (message != "true") {
            throw RuntimeException("event must be 'true'")
        }
    }
}

class JsonTestConsumer : KafkaTopicConsumer<String?, JsonElement>(
    stringDeserializer(),
    JsonElementDeserializer(),
) {
    override suspend fun consume(key: String?, message: JsonElement) {
        val success = message.jsonObject.getValue("success").jsonPrimitive.boolean
        if (!success) {
            throw RuntimeException("success must be 'true'")
        }
    }
}
