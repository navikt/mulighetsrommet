package no.nav.mulighetsrommet.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

@Serializable(with = PortableTextTypedObjectSerializer::class)
data class PortableTextTypedObject(
    @Suppress("PropertyName")
    val _type: String,
    @Suppress("PropertyName")
    val _key: String?,
    val additionalProperties: Map<String, JsonElement> = emptyMap(),
)

object PortableTextTypedObjectSerializer : KSerializer<PortableTextTypedObject> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("PortableTextTypedObject") {
        element<String>("_type")
        element<String>("_key")
    }

    override fun serialize(encoder: Encoder, value: PortableTextTypedObject) {
        require(encoder is JsonEncoder)
        val jsonObject = buildJsonObject {
            put("_type", JsonPrimitive(value._type))
            put("_key", JsonPrimitive(value._key))
            for ((key, jsonElement) in value.additionalProperties) {
                put(key, jsonElement)
            }
        }
        encoder.encodeJsonElement(jsonObject)
    }

    override fun deserialize(decoder: Decoder): PortableTextTypedObject {
        require(decoder is JsonDecoder)

        val jsonObject = decoder.decodeJsonElement().jsonObject

        val type = jsonObject["_type"]?.jsonPrimitive?.content
            ?: throw IllegalArgumentException("Missing required field: _type")
        val key = jsonObject["_key"]?.jsonPrimitive?.contentOrNull

        val additionalProperties = jsonObject
            .filterKeys { it != "_type" && it != "_key" }

        return PortableTextTypedObject(
            _type = type,
            _key = key,
            additionalProperties = additionalProperties,
        )
    }
}
