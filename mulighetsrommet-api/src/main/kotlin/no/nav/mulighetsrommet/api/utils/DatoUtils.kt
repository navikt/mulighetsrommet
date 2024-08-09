package no.nav.mulighetsrommet.api.utils

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object DatoUtils {
    val europeanDatePattern = "dd.MM.yyyy"
    fun LocalDate?.formaterDatoTilEuropeiskDatoformat(): String? {
        return this?.format(DateTimeFormatter.ofPattern(europeanDatePattern))
    }
}
