package no.nav.mulighetsrommet.api.kafka

import java.time.format.DateTimeFormatter

object ProcessingUtils {

    fun getArenaDateFormat() = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun removeDoubleQuotes(value: String): String {
        return value.replace("\"", "")
    }
}
