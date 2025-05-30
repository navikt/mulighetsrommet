package no.nav.mulighetsrommet.kafka

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.stringDeserializer
import no.nav.mulighetsrommet.kafka.serialization.JsonElementDeserializer
import java.util.Properties

class TestConsumer(id: String, topic: String, properties: Properties) : KafkaTopicConsumer<String?, String?>(
    Config(id, topic, properties),
    stringDeserializer(),
    stringDeserializer(),
) {
    override suspend fun consume(key: String?, message: String?) {
        if (message != "true") {
            throw RuntimeException("event must be 'true'")
        }
    }
}

class JsonTestConsumer(name: String, properties: Properties) : KafkaTopicConsumer<String?, JsonElement>(
    Config(name, name, properties),
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
