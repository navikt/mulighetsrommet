package no.nav.mulighetsrommet.model

import kotlinx.serialization.Serializable

private val KID_REGEX = "^\\d{2,25}$".toRegex()

@Serializable
@JvmInline
value class Kid(val value: String) {
    init {
        require(KID_REGEX.matches(value)) {
            "'Kid' må være på formatet '$KID_REGEX'"
        }
        val intValue = value.toLongOrNull()
        requireNotNull(intValue) {
            "'Kid' må være et tall"
        }
        require(intValue % 10 == 0L || intValue % 11 == 0L) {
            "'Kid' er ugyldig"
        }
    }
}
