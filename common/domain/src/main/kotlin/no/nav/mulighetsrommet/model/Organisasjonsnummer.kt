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
            return if (isValid(value)) {
                Organisasjonsnummer(value)
            } else {
                null
            }
        }
    }

    /**
     * Sjekker om organisasjonsnummeret er fiktivt (utenlandsk arrangør, utenfor breg)
     * Starter da med 1, 9 siffer
     */
    fun erUtenlandsk(): Boolean {
        return value.startsWith('1')
    }

    override fun toString() = value
}
