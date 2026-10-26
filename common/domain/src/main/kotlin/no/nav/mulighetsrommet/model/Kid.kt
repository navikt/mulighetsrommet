package no.nav.mulighetsrommet.model

import kotlinx.serialization.Serializable

private val KID_REGEX = "^\\d{2,24}[\\d-]$".toRegex()

@Serializable
@JvmInline
value class Kid private constructor(val value: String) {

    companion object {
        fun parse(kid: String): Kid? {
            val normalizedKid = kid.trim()
            if (normalizedKid.isEmpty()) {
                return null
            }

            return if (
                KID_REGEX.matches(normalizedKid) &&
                (
                    Modulus.hasValidControlDigit(normalizedKid, Modulus.Algorithm.MOD10) ||
                        Modulus.hasValidControlDigit(normalizedKid, Modulus.Algorithm.MOD11)
                    )
            ) {
                Kid(normalizedKid)
            } else {
                null
            }
        }

        fun parseOrThrow(kid: String): Kid {
            return parse(kid) ?: throw IllegalArgumentException("Ugyldig kid")
        }
    }

    override fun toString() = value
}
