package no.nav.mulighetsrommet.domain.dto

import kotlinx.serialization.Serializable

private val WEBSAKNUMMER_REGEX = "^(\\d{2,4})/(\\d+)$".toRegex()

@Serializable
@JvmInline
value class Websaknummer(val value: String) {
    init {
        require(WEBSAKNUMMER_REGEX.matches(value)) {
            "The format of 'Websaknummer' is invalid. Expected '{year}/{id}'."
        }
    }
}
