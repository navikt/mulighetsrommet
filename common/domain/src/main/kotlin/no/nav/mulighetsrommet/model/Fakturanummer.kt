package no.nav.mulighetsrommet.model

import kotlinx.serialization.Serializable

private val FAKTURANUMMER_REGEX = "[A-Za-z0-9/._-]+[0-9]".toRegex()

/**
 * Fakturanummer er et lovkrav fra Bokføringsloven som selgere/bedrifter må rette seg etter.
 * Dette er ment til å representere et fakturanumer med opphav fra ekstern kilde, som feks en arrangørs faktura til NAV.
 */
@Serializable
@JvmInline
value class Fakturanummer private constructor(val value: String) {

    companion object {
        fun parse(fakturanummer: String): Fakturanummer? {
            val normalizedFakturanummer = fakturanummer.trim()
            if (normalizedFakturanummer.isEmpty()) {
                return null
            }

            return if (normalizedFakturanummer.length <= 30 && FAKTURANUMMER_REGEX.matches(normalizedFakturanummer)) {
                Fakturanummer(normalizedFakturanummer)
            } else {
                null
            }
        }

        fun parseOrThrow(fakturanummer: String): Fakturanummer {
            return parse(fakturanummer) ?: throw IllegalArgumentException("Ugyldig fakturanummer")
        }
    }

    override fun toString() = value
}
