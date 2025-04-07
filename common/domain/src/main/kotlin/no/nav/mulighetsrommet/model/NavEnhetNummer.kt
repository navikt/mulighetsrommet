package no.nav.mulighetsrommet.model

import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory

private val NAV_ENHET_NUMMER = "^\\d{4}$".toRegex()

private val log = LoggerFactory.getLogger(NavEnhetNummer::class.java)

@Serializable
@JvmInline
value class NavEnhetNummer(val value: String) {
    init {
        if (!NAV_ENHET_NUMMER.matches(value)) {
            log.warn("Feil format på enhetsnummer: $value")
        }
    }

    @Override
    override fun toString(): String = value
}
