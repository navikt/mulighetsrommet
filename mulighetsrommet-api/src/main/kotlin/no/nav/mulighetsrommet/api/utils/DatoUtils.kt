package no.nav.mulighetsrommet.api.utils

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object DatoUtils {
    val europeanDatePattern = "dd.MM.yyyy"

    fun LocalDate.formaterDatoTilEuropeiskDatoformat(): String {
        return format(DateTimeFormatter.ofPattern(europeanDatePattern))
    }

    fun Instant.tilNorskDato(): LocalDate = atZone(ZoneId.of("Europe/Oslo")).toLocalDate()

    fun Instant.tilNorskLocalDateTime(): LocalDateTime = atZone(ZoneId.of("Europe/Oslo")).toLocalDateTime()
}
