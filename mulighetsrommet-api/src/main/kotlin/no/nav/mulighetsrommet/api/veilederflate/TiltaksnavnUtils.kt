package no.nav.mulighetsrommet.api.veilederflate

object TiltaksnavnUtils {
    fun tittelOgUnderTittel(
        navn: String,
        tiltakstypeNavn: String,
    ): Pair<String, String> =
        tiltakstypeNavn to navn
}


fun String.hosTitleCaseArrangor(arrangor: String?): String {
    val casedArrangor = toTitleCase(arrangor ?: "")

    return "${this}${if (casedArrangor.isNotBlank()) " hos $casedArrangor" else ""}"
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
            in FORKORTELSER_MED_STORE_BOKSTAVER -> {
                it.uppercase()
            }

            in ORD_MED_SMA_BOKSTAVER -> {
                it
            }

            else -> {
                it.replaceFirstChar(Char::uppercaseChar)
            }
        }
    }
}
