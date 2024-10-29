package no.nav.mulighetsrommet.domain.dto

import kotlinx.serialization.Serializable

private val KONTONUMMER_REGEX = "^\\d{11}$".toRegex()

@Serializable
@JvmInline
value class Kontonummer(val value: String) {
    init {
        require(KONTONUMMER_REGEX.matches(value)) {
            "'Kontonummer' må være på formatet '${KONTONUMMER_REGEX}'"
        }
    }
}
