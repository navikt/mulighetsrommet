package no.nav.mulighetsrommet.domain.dto

import kotlinx.serialization.Serializable

private val NORSK_IDENT_REGEX = "^\\d{11}$".toRegex()

@Serializable
@JvmInline
value class NorskIdent(val value: String) {
    init {
        require(NORSK_IDENT_REGEX.matches(value)) {
            "'NorskIdent' må være på formatet '$NORSK_IDENT_REGEX'"
        }
    }
}
