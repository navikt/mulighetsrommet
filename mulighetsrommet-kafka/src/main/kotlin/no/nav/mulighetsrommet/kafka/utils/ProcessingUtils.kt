package no.nav.mulighetsrommet.kafka.utils

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

enum class ArenaEventOperationType(val type: String) {
    INSERT("I"),
    UPDATE("U");

    companion object {
        private val map = ArenaEventOperationType.values().associateBy(ArenaEventOperationType::type)
        operator fun get(value: String) = map[value]
    }
}

object ProcessingUtils {

    fun getArenaDateFormat(): DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun getArenaDateFromTo(value: String): LocalDateTime? {
        return if (value != "null") {
            LocalDateTime.parse(value, getArenaDateFormat())
        } else {
            null
        }
    }

    fun getArenaOperationType(json: JsonObject) = ArenaEventOperationType[json["op_type"]!!.jsonPrimitive.content]

    fun isInsertArenaOperation(json: JsonObject) =
        ArenaEventOperationType[json["op_type"]!!.jsonPrimitive.content] == ArenaEventOperationType.INSERT

    fun isUpdateArenaOperation(json: JsonObject) =
        ArenaEventOperationType[json["op_type"]!!.jsonPrimitive.content] == ArenaEventOperationType.UPDATE
}
