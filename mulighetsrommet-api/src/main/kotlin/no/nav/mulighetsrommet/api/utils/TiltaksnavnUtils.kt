package no.nav.mulighetsrommet.api.utils

import no.nav.mulighetsrommet.api.domain.dto.TiltakstypeAdminDto

class TiltaksnavnUtils {
    companion object {
        fun tilKonstruertNavn(tiltakstype: TiltakstypeAdminDto, arrangor: String?): String {
            val casedTiltakstype = toTitleCase(tiltakstype.navn)
            val casedArrangor = toTitleCase(arrangor ?: "")

            return "${casedTiltakstype}${if (casedArrangor.isNotBlank()) " hos $casedArrangor" else ""}"
        }

        val FORKORTELSER_MED_STORE_BOKSTAVER = listOf(
            "as",
            "a/s",
        )

        val ORD_MED_SMA_BOKSTAVER = listOf(
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
}
