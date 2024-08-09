package no.nav.mulighetsrommet.arena.adapter.utils

import no.nav.mulighetsrommet.domain.constants.ArenaMigrering.ArenaTimestampFormatter
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
