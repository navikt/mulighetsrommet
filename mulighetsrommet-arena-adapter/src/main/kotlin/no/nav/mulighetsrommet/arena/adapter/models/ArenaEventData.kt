package no.nav.mulighetsrommet.arena.adapter.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

data class ArenaEventData<T>(
    val table: String,
    val operation: Operation,
    val data: T
) {

    @Serializable
    enum class Operation {

        @SerialName("I")
        Insert,

        @SerialName("U")
        Update,

        @SerialName("D")
        Delete,
    }

    companion object {
        val json = Json { ignoreUnknownKeys = true }

        /**
         * Attempts to decode the given JSON [payload]'s `after` property into a value of type [T] when the
         * op_type is Insert or Update, or the `before` property into a value of type [T] when the op_type
         * is Delete.
         */
        inline fun <reified T> decode(payload: JsonElement): ArenaEventData<T> {
            val table = json.decodeFromJsonElement<String>(payload.jsonObject["table"]!!)

            val operation = json.decodeFromJsonElement<Operation>(payload.jsonObject["op_type"]!!)

            val data: T = if (operation == ArenaEventData.Operation.Delete) {
                json.decodeFromJsonElement(payload.jsonObject["before"]!!)
            } else {
                json.decodeFromJsonElement(payload.jsonObject["after"]!!)
            }

            return ArenaEventData(table, operation, data)
        }
    }
}
