package no.nav.mulighetsrommet.arena.adapter.consumers.helpers

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

object ArenaEventHelpers {
    val json = Json { ignoreUnknownKeys = true }

    /**
     * Attempts to decode the given JSON [event] payload's `after` property into a value of type [T].
     */
    inline fun <reified T> decodeAfter(event: JsonElement): T {
        return json.decodeFromJsonElement(event.jsonObject["after"]!!)
    }
}
