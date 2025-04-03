package no.nav.mulighetsrommet.model

import kotlinx.serialization.Serializable

private val NAV_ENHET_NUMMER = "^\\d{4}$".toRegex()

@Serializable
@JvmInline
value class NavEnhetNummer(val value: String) {
    init {
        require(NAV_ENHET_NUMMER.matches(value)) {
            "'NavEnhet' må være på formatet '$NAV_ENHET_NUMMER'"
        }
    }

    @Override
    override fun toString(): String = value
}
