package no.nav.mulighetsrommet.database.utils

fun String.toFTSPrefixQuery(): String = this
    .trim()
    .split("\\s+".toRegex())
    .mapNotNull { token ->
        val cleaned = token.replace(Regex("""[<?`*&|!():'"\\]"""), "")
        if (cleaned.isNotBlank()) "$cleaned:*" else null
    }
    .joinToString(" & ")
