package no.nav.mulighetsrommet.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*
import kotlinx.serialization.serializer
import no.nav.mulighetsrommet.model.ProblemDetail
import kotlin.reflect.full.starProjectedType

object ProblemDetailSerializer : KSerializer<ProblemDetail> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ProblemDetail")

    override fun deserialize(decoder: Decoder): ProblemDetail {
        throw UnsupportedOperationException("Deserialization is not supported for ProblemDetail")
    }

    override fun serialize(
        encoder: Encoder,
        value: ProblemDetail,
    ) {
        val output =
            encoder as? JsonEncoder ?: throw Exception("Encoder could not be parsed to JsonEncoder")
        val problem = mapProblemToJsonObject(value)
        output.encodeJsonElement(problem)
    }

    private fun mapProblemToJsonObject(value: ProblemDetail): JsonObject {
        val elements = mutableMapOf<String, JsonElement>()
        elements[ProblemDetail::type.name] = JsonPrimitive(value.type)
        elements[ProblemDetail::status.name] = JsonPrimitive(value.status)
        if (value.title.isNotBlank()) {
            elements[ProblemDetail::title.name] = JsonPrimitive(value.title)
        }
        if (value.detail.isNotBlank()) {
            elements[ProblemDetail::detail.name] = JsonPrimitive(value.detail)
        }
        if (!value.instance.isNullOrBlank()) {
            elements[ProblemDetail::instance.name] = JsonPrimitive(value.instance)
        }
        if (!value.extensions.isNullOrEmpty()) {
            val extensions =
                value.extensions!!.map { (key, value) ->
                    key to mapAnyToJsonElement(value)
                }
            elements += extensions
        }
        return JsonObject(elements)
    }

    private fun mapAnyToJsonElement(value: Any?): JsonElement = when (value) {
        is Iterable<*> -> {
            JsonArray(value.map { mapAnyToJsonElement(it!!) })
        }
        null -> JsonNull
        else -> Json.encodeToJsonElement(serializer(value::class.starProjectedType), value)
    }
}
