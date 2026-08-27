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

        fun parse(value: String): Organisasjonsnummer? {
            val normalizedValue = value.filterNot { it.isWhitespace() }
            return if (isValid(normalizedValue)) {
                Organisasjonsnummer(normalizedValue)
            } else {
                null
            }
        }
    }

    override fun toString() = value
}
