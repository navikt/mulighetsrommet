package no.nav.mulighetsrommet.arena.adapter.consumers

import io.ktor.client.request.*
import io.ktor.http.content.*
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent

val json = Json {
    ignoreUnknownKeys = true
}

@OptIn(InternalSerializationApi::class)
inline fun <reified T : Any> HttpRequestData.decodeRequestBody(): T {
    return json.decodeFromString(T::class.serializer(), (body as TextContent).text)
}

fun createArenaInsertEvent(table: String, id: String, data: String) = ArenaEvent(
    topic = table,
    key = id,
    payload = Json.parseToJsonElement(
        """{
                "table": "$table",
                "op_type": "I",
                "after": $data
            }
            """
    ),
    status = ArenaEvent.ConsumptionStatus.Pending
)
