package no.nav.mulighetsrommet.kafka.serialization

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serializer
import java.nio.charset.StandardCharsets

class JsonElementSerde : Serde<JsonElement> {
    private val serializer: Serializer<JsonElement> = JsonElementSerializer()
    private val deserializer: Deserializer<JsonElement> = JsonElementDeserializer()

    override fun serializer(): Serializer<JsonElement> = serializer

    override fun deserializer(): Deserializer<JsonElement> = deserializer
}

class JsonElementSerializer : Serializer<JsonElement> {
    override fun serialize(topic: String, data: JsonElement?): ByteArray? {
        if (data == null) {
            return null
        }

        val jsonString = Json.encodeToString(JsonElement.serializer(), data)
        return jsonString.toByteArray(StandardCharsets.UTF_8)
    }
}

class JsonElementDeserializer : Deserializer<JsonElement> {
    override fun deserialize(topic: String, data: ByteArray?): JsonElement {
        if (data == null) {
            return JsonNull
        }

        val value = String(data, StandardCharsets.UTF_8)
        return Json.parseToJsonElement(value)
    }
}
