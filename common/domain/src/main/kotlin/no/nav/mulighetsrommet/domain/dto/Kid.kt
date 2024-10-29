package no.nav.mulighetsrommet.domain.dto

import kotlinx.serialization.Serializable

private val KID_REGEX = "^\\d{1,25}$".toRegex()

@Serializable
@JvmInline
value class Kid(val value: String) {
    init {
        require(KID_REGEX.matches(value)) {
            "'Kid' må være på formatet '$KID_REGEX'"
        }
    }
}
