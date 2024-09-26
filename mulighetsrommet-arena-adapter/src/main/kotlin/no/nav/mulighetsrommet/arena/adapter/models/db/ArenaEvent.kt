package no.nav.mulighetsrommet.arena.adapter.models.db

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTable
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import java.util.*

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
        Delete,
        ;

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

        /** Event should be replayed by replay events task */
        Replay,
    }

    inline fun <reified T> decodePayload(): T {
        return if (operation == Operation.Delete) {
            JsonIgnoreUnknownKeys.decodeFromJsonElement(payload.jsonObject["before"]!!)
        } else {
            JsonIgnoreUnknownKeys.decodeFromJsonElement(payload.jsonObject["after"]!!)
        }
    }

    fun getEksternID(): UUID? = when (arenaTable) {
        ArenaTable.Tiltaksgjennomforing -> decodePayload<EksternIdPayload>().EKSTERN_ID?.let { UUID.fromString(it) }
        else -> null
    }
}

@kotlinx.serialization.Serializable
data class EksternIdPayload(
    val EKSTERN_ID: String? = null,
)
