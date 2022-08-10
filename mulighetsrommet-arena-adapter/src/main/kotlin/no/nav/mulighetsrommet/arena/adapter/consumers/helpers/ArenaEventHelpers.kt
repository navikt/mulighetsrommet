package no.nav.mulighetsrommet.arena.adapter.consumers.helpers

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

object ArenaEventHelpers {
    val json = Json { ignoreUnknownKeys = true }

    /**
     * Attempts to decode the given JSON [event] payload's `after` property into a value of type [T].
     */
    inline fun <reified T> decodeEvent(event: JsonElement): ArenaEvent<T> {
        val operation = json.decodeFromJsonElement<ArenaOperation>(event.jsonObject["op_type"]!!)
        val data = json.decodeFromJsonElement<T>(event.jsonObject["after"]!!)
        return ArenaEvent(
            operation = operation,
            data = data,
        )
    }
}

data class ArenaEvent<T>(
    val operation: ArenaOperation,
    val data: T
)

@Serializable
enum class ArenaOperation {

    @SerialName("I")
    Insert,

    @SerialName("U")
    Update,

    @SerialName("D")
    Delete,
}
