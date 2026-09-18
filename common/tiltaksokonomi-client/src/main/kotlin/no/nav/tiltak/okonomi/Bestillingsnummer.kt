package no.nav.tiltak.okonomi

import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class Bestillingsnummer(val value: String) {
    init {
        require(value.length <= 20) { "Bestillingsnummer kan maks være 20 tegn" }
        require(BESTILLINGSNUMMER_REGEX.matches(value)) { "Ugyldig Bestillingsnummer '$value'" }
    }

    override fun toString() = value

    companion object {
        private val BESTILLINGSNUMMER_REGEX = "^[A-Z]+-.+-.+$".toRegex()
    }
}
