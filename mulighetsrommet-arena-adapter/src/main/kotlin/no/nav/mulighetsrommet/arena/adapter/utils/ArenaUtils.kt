package no.nav.mulighetsrommet.arena.adapter.utils

import no.nav.mulighetsrommet.domain.constants.ArenaMigrering.ArenaTimestampFormatter
import no.nav.mulighetsrommet.domain.dbo.Deltakerstatus
import no.nav.mulighetsrommet.domain.dto.JaNeiStatus
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.LocalTime

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

    fun parseFremmoteTidspunkt(datoValue: String?, klokkeValue: String?): LocalDateTime? {
        if (datoValue == null || datoValue == "null") {
            return null
        }
        val dato = LocalDateTime.parse(datoValue, ArenaTimestampFormatter)
        val hourMinute = parseKlokketid(klokkeValue) ?: return dato

        return LocalDateTime.of(dato.toLocalDate(), LocalTime.of(hourMinute.first, hourMinute.second))
    }

    fun parseKlokketid(value: String?): Pair<Int, Int>? {
        if (value == null) return null
        // Remove any non-digit characters
        val cleanedInput = value.filter { it.isDigit() }

        // Parse hours and minutes based on the length of the cleaned input
        return when (cleanedInput.length) {
            1 -> cleanedInput.toInt() to 0 // e.g., "9" represents 9:00
            2 -> cleanedInput.toInt() to 0 // e.g., "12" represents 12:00
            3 -> cleanedInput.substring(0, 1).toInt() to cleanedInput.substring(1).toInt() // e.g., "930" represents 9:30
            4 -> cleanedInput.substring(0, 2).toInt() to cleanedInput.substring(2).toInt() // e.g., "0930" represents 09:30
            else -> {
                throw IllegalArgumentException("Klarte ikke parse KLOKKETID_FREMMOTE: $value")
            }
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
