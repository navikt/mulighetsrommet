package no.nav.mulighetsrommet.domain.dto

import kotlinx.serialization.Serializable

private val ORGANISASJONSNUMMER_REGEX = "^\\d{9}$".toRegex()

@Serializable
@JvmInline
value class Organisasjonsnummer(val value: String) {
    init {
        require(ORGANISASJONSNUMMER_REGEX.matches(value)) {
            "'Organisasjonsnummer' må være på formatet '${ORGANISASJONSNUMMER_REGEX}'"
        }
    }
}
