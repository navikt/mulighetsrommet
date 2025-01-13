package no.nav.mulighetsrommet.domain.dto

import kotlinx.serialization.Serializable

private val TILTAKSNUMMER_REGEX = "^(\\d{4})[#/](\\d+)$".toRegex()

@Serializable
@JvmInline
value class Tiltaksnummer(val value: String) {
    init {
        require(TILTAKSNUMMER_REGEX.matches(value)) {
            "The format of 'Tiltaksnummer' is invalid. Expected '{year}/{lopenummer}' or '{year}#{lopenummer}."
        }
    }

    val aar: Int
        get() = value.split("/", "#").first().toInt()

    val lopenummer: Int
        get() = value.split("/", "#").last().toInt()
}
