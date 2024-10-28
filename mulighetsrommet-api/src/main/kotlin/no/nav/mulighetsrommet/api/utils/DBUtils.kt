package no.nav.mulighetsrommet.api.utils

object DBUtils {
    fun String.toFTSPrefixQuery() = this
        .trim()
        .split("\\s+".toRegex())
        .map { it.filter { char -> char.isLetterOrDigit() } }
        .filter { it.isNotEmpty() }
        .joinToString(separator = "&") { "$it:*" }
}
