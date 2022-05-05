package no.nav.mulighetsrommet.kafka.utils

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import no.nav.mulighetsrommet.domain.Deltakerstatus
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

    fun toDeltakerstatus(arenaStatus: String): Deltakerstatus = when (arenaStatus) {
        "AVSLAG", "IKKAKTUELL", "NEITAKK" -> Deltakerstatus.IKKE_AKTUELL
        "TILBUD", "JATAKK", "INFOMOETE", "AKTUELL", "VENTELISTE" -> Deltakerstatus.VENTER
        "GJENN" -> Deltakerstatus.DELTAR
        "DELAVB", "GJENN_AVB", "GJENN_AVL", "FULLF", "IKKEM" -> Deltakerstatus.AVSLUTTET
        else -> throw Exception("Ukjent deltakerstatus fra Arena")
    }
}
