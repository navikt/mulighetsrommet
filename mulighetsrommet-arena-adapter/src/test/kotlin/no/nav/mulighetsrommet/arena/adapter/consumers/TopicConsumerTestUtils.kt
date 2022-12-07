package no.nav.mulighetsrommet.arena.adapter.consumers

import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.arena.adapter.models.ArenaEventData
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent

fun createArenaEvent(
    table: String,
    id: String,
    operation: ArenaEventData.Operation,
    data: String,
    status: ArenaEvent.ConsumptionStatus = ArenaEvent.ConsumptionStatus.Pending
): ArenaEvent {
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
        status = status
    )
}
