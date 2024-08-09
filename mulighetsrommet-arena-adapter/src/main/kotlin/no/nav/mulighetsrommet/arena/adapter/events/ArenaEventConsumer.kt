package no.nav.mulighetsrommet.arena.adapter.events

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.stringDeserializer
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTable
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent
import no.nav.mulighetsrommet.arena.adapter.services.ArenaEventService
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import org.slf4j.MDC
import java.util.*

class ArenaEventConsumer(
    config: Config,
    private val arenaEventService: ArenaEventService,
) : KafkaTopicConsumer<String, JsonElement>(config, stringDeserializer(), ArenaJsonElementDeserializer()) {
    override suspend fun consume(key: String, message: JsonElement) {
        MDC.put("correlationId", key)

        val data = decodeArenaEvent(message)
        arenaEventService.processEvent(data)
    }
}

internal fun decodeArenaEvent(payload: JsonElement): ArenaEvent {
    val table = ArenaTable.fromTable(payload.getString("table"))

    val operation = ArenaEvent.Operation.fromOpType(payload.getString("op_type"))

    val data = if (operation == ArenaEvent.Operation.Delete) {
        payload.jsonObject.getValue("before").jsonObject
    } else {
        payload.jsonObject.getValue("after").jsonObject
    }

    val arenaId = when (table) {
        ArenaTable.Tiltakstype -> data.getString("TILTAKSKODE")
        ArenaTable.Sak -> data.getString("SAK_ID")
        ArenaTable.AvtaleInfo -> data.getString("AVTALE_ID")
        ArenaTable.Deltaker -> data.getString("TILTAKDELTAKER_ID")
        ArenaTable.HistDeltaker -> data.getString("HIST_TILTAKDELTAKER_ID")
        ArenaTable.Tiltaksgjennomforing -> data.getString("TILTAKGJENNOMFORING_ID")
    }

    return ArenaEvent(
        arenaTable = table,
        arenaId = arenaId,
        operation = operation,
        payload = payload,
        status = ArenaEvent.ProcessingStatus.Pending,
    )
}

private fun JsonElement.getString(key: String) = jsonObject.getValue(key).jsonPrimitive.content
