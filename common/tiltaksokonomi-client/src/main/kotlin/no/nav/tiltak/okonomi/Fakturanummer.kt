package no.nav.tiltak.okonomi

import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class Fakturanummer(val value: String) {
    init {
        require(value.length <= 50) { "Fakturanummer kan maks være 50 tegn" }
        require(FAKTURANUMMER_REGEX.matches(value)) { "Ugyldig Fakturanummer '$value'" }
    }

    override fun toString() = value

    companion object {
        private val FAKTURANUMMER_REGEX = "^[A-Z]+-.+-.+-.+$".toRegex()
    }
}
