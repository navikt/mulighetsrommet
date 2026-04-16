package no.nav.mulighetsrommet.api.utils

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object DatoUtils {
    const val EUROPEAN_DATE_FORMAT = "dd.MM.yyyy"

    fun LocalDate.formaterDatoTilEuropeiskDatoformat(): String {
        return format(DateTimeFormatter.ofPattern(EUROPEAN_DATE_FORMAT))
    }

    fun Instant.tilNorskDato(): LocalDate = atZone(ZoneId.of("Europe/Oslo")).toLocalDate()

    fun Instant.tilNorskLocalDateTime(): LocalDateTime = atZone(ZoneId.of("Europe/Oslo")).toLocalDateTime()

    fun String.parseOrNull(): LocalDate? {
        return try {
            LocalDate.parse(this)
        } catch (e: Exception) {
            null
        }
    }
}
