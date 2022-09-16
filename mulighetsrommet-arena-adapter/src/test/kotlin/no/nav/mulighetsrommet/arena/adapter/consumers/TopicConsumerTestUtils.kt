package no.nav.mulighetsrommet.arena.adapter.no.nav.mulighetsrommet.arena.adapter.consumers

import io.ktor.client.request.*
import io.ktor.http.content.*
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

@OptIn(InternalSerializationApi::class)
inline fun <reified T : Any> HttpRequestData.decodeRequestBody(): T {
    return Json.decodeFromString(T::class.serializer(), (body as TextContent).text)
}

object ArenaEvent {
    fun createInsertEvent(data: String) = Json.parseToJsonElement(
        """{
            "op_type": "I",
            "after": $data
        }
        """
    )
}
