package no.nav.mulighetsrommet.ktor.extensions

import io.ktor.http.*
import io.ktor.server.plugins.*
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Utility to parse a named parameter as a [JsonElement] based on the format described in OpenAPI Parameter
 * Serialization with `style=deepObject` and `explode=true`: https://swagger.io/docs/specification/serialization/
 */
fun Parameters.getJsonObject(name: String): JsonObject? {
    val nestedPropertyRegex = "$name\\[(?<property>\\w+)]".toRegex()

    return entries()
        .map { entry ->
            val match = nestedPropertyRegex.find(entry.key) ?: return@map null

            val property = match.groups["property"]?.value
                ?: throw BadRequestException("Could not parse nested property name when parsing query parameter $name as JsonObject")

            val value = entry.value.firstOrNull()
                ?.let { value -> JsonPrimitive(value) }
                ?: JsonNull

            property to value
        }
        .filterNotNull()
        .toMap()
        .let {
            if (it.isEmpty()) {
                null
            } else {
                JsonObject(it)
            }
        }
}
