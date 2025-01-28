package no.nav.mulighetsrommet.model

import kotlinx.serialization.Serializable

private val ORGANISASJONSNUMMER_REGEX = "^\\d{9}$".toRegex()

@Serializable
@JvmInline
value class Organisasjonsnummer(val value: String) {
    init {
        require(isValid(value)) {
            "'Organisasjonsnummer' må være på formatet '${ORGANISASJONSNUMMER_REGEX}'"
        }
    }

    companion object {
        fun isValid(value: String): Boolean = ORGANISASJONSNUMMER_REGEX.matches(value)
    }
}
