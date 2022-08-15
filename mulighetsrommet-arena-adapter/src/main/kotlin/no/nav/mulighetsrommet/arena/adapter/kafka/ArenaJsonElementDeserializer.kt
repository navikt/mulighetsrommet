package no.nav.mulighetsrommet.arena.adapter.kafka

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import org.apache.kafka.common.serialization.Deserializer
import java.nio.charset.StandardCharsets

class ArenaJsonElementDeserializer : Deserializer<JsonElement> {
    override fun deserialize(topic: String, data: ByteArray?): JsonElement {
        if (data == null) {
            return JsonNull
        }

        val value = String(data, StandardCharsets.UTF_8)
        val stripped = value.replace("\\u0000", "")
        return Json.parseToJsonElement(stripped)
    }
}
