package no.nav.mulighetsrommet.model

import kotlinx.serialization.Serializable

private val SAKARKIV_NUMMER_REGEX = "^(\\d{2,4})/(\\d+)$".toRegex()

@Serializable
@JvmInline
value class SakarkivNummer(val value: String) {
    init {
        require(SAKARKIV_NUMMER_REGEX.matches(value)) {
            "The format of 'sakarkiv_nummer' is invalid. Expected '{year}/{id}'."
        }
    }
}
