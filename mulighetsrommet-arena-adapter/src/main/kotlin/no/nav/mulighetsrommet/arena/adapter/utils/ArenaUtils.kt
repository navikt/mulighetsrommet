package no.nav.mulighetsrommet.arena.adapter.utils

import no.nav.mulighetsrommet.domain.constants.ArenaMigrering.ArenaTimestampFormatter
import no.nav.mulighetsrommet.domain.dbo.Deltakerstatus
import no.nav.mulighetsrommet.domain.dto.JaNeiStatus
import java.time.LocalDateTime

object ArenaUtils {
    fun parseTimestamp(value: String): LocalDateTime {
        return LocalDateTime.parse(value, ArenaTimestampFormatter)
    }

    fun parseNullableTimestamp(value: String?): LocalDateTime? {
        return if (value != null && value != "null") {
            parseTimestamp(value)
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

    fun parseJaNei(jaNeiStreng: JaNeiStatus): Boolean {
        return when (jaNeiStreng) {
            JaNeiStatus.Ja -> true
            JaNeiStatus.Nei -> false
        }
    }

    fun parseNulleableJaNei(jaNeiStreng: JaNeiStatus?): Boolean? {
        if (jaNeiStreng == null) {
            return null
        }
        return when (jaNeiStreng) {
            JaNeiStatus.Ja -> true
            JaNeiStatus.Nei -> false
        }
    }
}
