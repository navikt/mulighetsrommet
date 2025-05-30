package no.nav.mulighetsrommet.model

import kotlinx.serialization.Serializable

private val KID_REGEX = "^\\d{2,24}\\d|-?$".toRegex()

@Serializable
@JvmInline
value class Kid private constructor(val value: String) {
    companion object {
        fun parse(kid: String): Kid? {
            return if (
                KID_REGEX.matches(kid) &&
                (
                    Modulus.hasValidControlDigit(kid, Modulus.Algorithm.MOD10) ||
                        Modulus.hasValidControlDigit(kid, Modulus.Algorithm.MOD11)
                    )
            ) {
                Kid(kid)
            } else {
                null
            }
        }

        fun parseOrThrow(kid: String): Kid {
            return parse(kid) ?: throw IllegalArgumentException("Ugyldig kid")
        }
    }
}
