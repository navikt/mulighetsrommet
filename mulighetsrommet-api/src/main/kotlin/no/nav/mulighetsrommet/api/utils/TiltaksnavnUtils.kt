package no.nav.mulighetsrommet.api.utils

import no.nav.mulighetsrommet.api.domain.dto.TiltakstypeAdminDto
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.domain.Tiltakskoder.isKursTiltak

object TiltaksnavnUtils {
    fun tittelOgUnderTittel(
        navn: String,
        tiltakstypeNavn: String,
        tiltakskode: Tiltakskode,
    ): Pair<String, String> =
        if (isKursTiltak(tiltakskode)) {
            navn to tiltakstypeNavn
        } else {
            tiltakstypeNavn to navn
        }

    fun tittelOgUnderTittel(
        navn: String,
        tiltakstypeNavn: String,
        arenaKode: String,
    ): Pair<String, String> =
        if (arenaKode in listOf("ENKELAMO", "ENKFAGYRKE")) {
            navn to tiltakstypeNavn
        } else {
            tiltakstypeNavn to navn
        }

    fun tilKonstruertNavn(tiltakstype: TiltakstypeAdminDto, arrangor: String?): String {
        val casedArrangor = toTitleCase(arrangor ?: "")

        return "${tiltakstype.navn}${if (casedArrangor.isNotBlank()) " hos $casedArrangor" else ""}"
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
}
