package no.nav.mulighetsrommet.arena.adapter.utils

import no.nav.mulighetsrommet.domain.dto.Deltakerstatus
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

    enum class ArenaDeltakerstauts {
        AVSLAG,
        IKKAKTUELL,
        NEITAKK,
        TILBUD,
        JATAKK,
        INFOMOETE,
        AKTUELL,
        VENTELISTE,
        GJENN,
        DELAVB,
        GJENN_AVB,
        GJENN_AVL,
        FULLF,
        IKKEM
    }

    fun getArenaDateFormat(): DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun getArenaDateFromTo(value: String?): LocalDateTime? {
        return if (value != null && value != "null") {
            LocalDateTime.parse(value, getArenaDateFormat())
        } else {
            null
        }
    }
    fun toDeltakerstatus(arenaStatus: String): Deltakerstatus = when (arenaStatus) {
        "AVSLAG", "IKKAKTUELL", "NEITAKK" -> Deltakerstatus.IKKE_AKTUELL
        "TILBUD", "JATAKK", "INFOMOETE", "AKTUELL", "VENTELISTE" -> Deltakerstatus.VENTER
        "GJENN" -> Deltakerstatus.DELTAR
        "DELAVB", "GJENN_AVB", "GJENN_AVL", "FULLF", "IKKEM" -> Deltakerstatus.AVSLUTTET
        else -> throw Exception("Ukjent deltakerstatus fra Arena")
    }
}
