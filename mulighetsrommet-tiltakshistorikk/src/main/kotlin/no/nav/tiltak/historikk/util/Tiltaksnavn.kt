package no.nav.tiltak.historikk.util

object Tiltaksnavn {
    fun hosTitleCaseVirksomhet(tiltak: String, virksomhet: String?): String {
        if (virksomhet.isNullOrBlank()) {
            return tiltak
        }

        return "$tiltak hos ${toTitleCase(virksomhet)}"
    }
}

private val FORKORTELSER_MED_STORE_BOKSTAVER = listOf(
    "as",
    "a/s",
)

private val ORD_MED_SMA_BOKSTAVER = listOf(
    "i",
    "og",
)

private fun toTitleCase(tekst: String): String {
    return tekst.lowercase().split(Regex("(?<=\\s|-|')")).joinToString("") {
        when (it.trim()) {
            in FORKORTELSER_MED_STORE_BOKSTAVER -> it.uppercase()
            in ORD_MED_SMA_BOKSTAVER -> it
            else -> it.replaceFirstChar(Char::uppercaseChar)
        }
    }
}
