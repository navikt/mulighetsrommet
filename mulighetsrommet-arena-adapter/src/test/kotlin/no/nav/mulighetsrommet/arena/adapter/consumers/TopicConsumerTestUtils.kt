package no.nav.mulighetsrommet.arena.adapter.consumers

import io.ktor.client.request.*
import io.ktor.http.content.*
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import no.nav.mulighetsrommet.arena.adapter.models.ArenaEventData
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent

val json = Json {
    ignoreUnknownKeys = true
}

@OptIn(InternalSerializationApi::class)
inline fun <reified T : Any> HttpRequestData.decodeRequestBody(): T {
    return json.decodeFromString(T::class.serializer(), (body as TextContent).text)
}

fun createArenaEvent(table: String, id: String, operation: ArenaEventData.Operation, data: String): ArenaEvent {
    val before = if (operation == ArenaEventData.Operation.Delete) {
        data
    } else {
        null
    }

    val after = if (operation != ArenaEventData.Operation.Delete) {
        data
    } else {
        null
    }

    val opType = Json.encodeToString(ArenaEventData.Operation.serializer(), operation)

    return ArenaEvent(
        arenaTable = table,
        arenaId = id,
        payload = Json.parseToJsonElement(
            """{
                "table": "$table",
                "op_type": $opType,
                "before": $before,
                "after": $after
            }
            """
        ),
        status = ArenaEvent.ConsumptionStatus.Pending
    )
}
