package no.nav.mulighetsrommet.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import java.util.UUID

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

fun List<PortableTextTypedObject>.fromSlateFormat(): List<PortableTextTypedObject> {
    return this.map {
        // skip mapping if key already exists
        if (it._key != null) {
            return@map it
        }
        return@map it.fromSlateFormat()
    }
}

fun PortableTextTypedObject.fromSlateFormat(): PortableTextTypedObject {
    if (this._type != "block") {
        return this
    }
    val additionalProperties = this.additionalProperties.toMutableMap()
    additionalProperties["style"] = JsonPrimitive("normal")

    if ("listItem" in additionalProperties && "level" !in additionalProperties) {
        additionalProperties["level"] = JsonPrimitive(1)
    }
    val children = additionalProperties["children"]?.let { it as? JsonArray }?.map { child ->
        if (child !is JsonObject) {
            return@map child
        }
        val mutChild = child.toMutableMap()
        mutChild["_key"] = JsonPrimitive(getOrGenerateKey(child))
        return@map JsonObject(mutChild)
    }

    if (children != null) {
        additionalProperties["children"] = JsonArray(children)
    }

    val markDefs = additionalProperties["markDefs"]?.let {
        if (it is JsonArray && it.isNotEmpty()) {
            return@let it.updateMarkDefsLink(additionalProperties)
        }
        return@let null
    }
    if (markDefs != null) {
        additionalProperties["markDefs"] = markDefs
    }
    return this.copy(
        _key = this._key ?: getOrGenerateKey(),
        additionalProperties = additionalProperties,
    )
}

private fun JsonArray.updateMarkDefsLink(additionalProperties: MutableMap<String, JsonElement>): JsonArray {
    val markDefsArray = this.toMutableList()
    val linkMarkDefIndex = this.indexOfFirst { obj ->
        if (obj is JsonObject && "_type" in obj) {
            val type = obj["_type"]
            return@indexOfFirst type == JsonPrimitive("link")
        }
        return@indexOfFirst false
    }
    if (linkMarkDefIndex < 0) {
        return this
    }

    val linkMarkDefMap = this[linkMarkDefIndex].let { it as? JsonObject }?.toMutableMap()
    val linkMarkDefKey = linkMarkDefMap?.get("_key")?.jsonPrimitive?.content
    val newKey = getOrGenerateKey()

    val children = additionalProperties["children"]?.let { it as? JsonArray }?.map { child ->
        if (child !is JsonObject) {
            return@map child
        }
        val mutChild = child.toMutableMap()
        mutChild["_key"] = JsonPrimitive(getOrGenerateKey(child))

        if ("marks" in mutChild) {
            val childMarks = mutChild["marks"]?.let { it as? JsonArray }?.map { childMark ->
                if (childMark is JsonPrimitive && childMark == JsonPrimitive(linkMarkDefKey)) {
                    return@map JsonPrimitive(newKey)
                }
                return@map childMark
            }
            if (childMarks != null) {
                mutChild["marks"] = JsonArray(childMarks)
            }
        }
        return@map JsonObject(mutChild)
    }
    if (children != null) {
        linkMarkDefMap?.set("_key", JsonPrimitive(newKey))
        additionalProperties["children"] = JsonArray(children)
    }
    linkMarkDefMap?.let { markDefsArray.set(linkMarkDefIndex, JsonObject(it)) }
    return JsonArray(markDefsArray)
}

private fun getOrGenerateKey(obj: JsonObject? = null): String {
    val key = obj?.get("_key")?.jsonPrimitive?.contentOrNull
    if (key != null) {
        return key
    }
    return UUID.randomUUID().toString().slice(0..7)
}
