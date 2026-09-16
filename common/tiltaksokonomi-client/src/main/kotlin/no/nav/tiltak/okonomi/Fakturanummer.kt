package no.nav.tiltak.okonomi

import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class Fakturanummer(val value: String) {
    init {
        require(value.length <= 50) { "Fakturanummer kan maks være 50 tegn" }
    }

    override fun toString() = value
}
