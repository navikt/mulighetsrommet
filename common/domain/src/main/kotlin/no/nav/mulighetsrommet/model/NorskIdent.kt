package no.nav.mulighetsrommet.model

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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

fun NorskIdent.fodselsDato(): LocalDate = this.value.slice(0..6)
    .let { foedselsnummer -> LocalDate.parse(foedselsnummer, DateTimeFormatter.ofPattern("ddMMyyyy")) }
