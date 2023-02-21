package no.nav.mulighetsrommet.arena.adapter.models.db

import kotlinx.serialization.json.*
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTable

data class ArenaEvent(
    val arenaTable: ArenaTable,
    val arenaId: String,
    val operation: Operation,
    val payload: JsonElement,
    val status: ProcessingStatus,
    val retries: Int = 0,
    val message: String? = null,
) {

    enum class Operation {
        Insert,
        Update,
        Delete;

        val opType
            get(): String = when (this) {
                Insert -> "I"
                Update -> "U"
                Delete -> "D"
            }

        companion object {
            fun fromOpType(opType: String): Operation = when (opType) {
                "I" -> Insert
                "U" -> Update
                "D" -> Delete
                else -> throw IllegalArgumentException("Unsupported operation: $opType")
            }
        }
    }

    enum class ProcessingStatus {
        /** Event processing is pending and will be started (or retried) on the next schedule */
        Pending,

        /** Event has been processed */
        Processed,

        /** Processing has failed, event processing can be retried */
        Failed,

        /** Event has been ignored, but is kept in case of future relevance */
        Ignored,

        /** Event payload is invalid and needs manual intervention */
        Invalid,

        /** Event should be replayed by replay events task */
        Replay,
    }

    inline fun <reified T> decodePayload(): T {
        return if (operation == Operation.Delete) {
            json.decodeFromJsonElement(payload.jsonObject["before"]!!)
        } else {
            json.decodeFromJsonElement(payload.jsonObject["after"]!!)
        }
    }

    companion object {
        val json = Json { ignoreUnknownKeys = true }

        fun decodeFromJson(payload: JsonElement): ArenaEvent {
            val table = ArenaTable.fromTable(json.decodeFromJsonElement(payload.jsonObject.getValue("table")))

            val operation = Operation.fromOpType(payload.jsonObject.getValue("op_type").jsonPrimitive.content)

            val data = if (operation == Operation.Delete) {
                payload.jsonObject.getValue("before").jsonObject
            } else {
                payload.jsonObject.getValue("after").jsonObject
            }

            val arenaId = when (table) {
                ArenaTable.Tiltakstype -> data.getValue("TILTAKSKODE").jsonPrimitive.content
                ArenaTable.Sak -> data.getValue("SAK_ID").jsonPrimitive.int.toString()
                ArenaTable.AvtaleInfo -> data.getValue("AVTALE_ID").jsonPrimitive.int.toString()
                ArenaTable.Deltaker -> data.getValue("TILTAKDELTAKER_ID").jsonPrimitive.int.toString()
                ArenaTable.Tiltaksgjennomforing -> data.getValue("TILTAKGJENNOMFORING_ID").jsonPrimitive.int.toString()
            }

            return ArenaEvent(
                arenaTable = table,
                arenaId = arenaId,
                operation = operation,
                payload = payload,
                status = ProcessingStatus.Pending
            )
        }
    }
}
