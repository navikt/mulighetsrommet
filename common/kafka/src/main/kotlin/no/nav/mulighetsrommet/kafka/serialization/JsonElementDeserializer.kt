package no.nav.mulighetsrommet.kafka.serialization

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import org.apache.kafka.common.serialization.Deserializer
import java.nio.charset.StandardCharsets

class JsonElementDeserializer : Deserializer<JsonElement> {
    override fun deserialize(topic: String, data: ByteArray?): JsonElement {
        if (data == null) {
            return JsonNull
        }

        val value = String(data, StandardCharsets.UTF_8)
        return Json.parseToJsonElement(value)
    }
}
