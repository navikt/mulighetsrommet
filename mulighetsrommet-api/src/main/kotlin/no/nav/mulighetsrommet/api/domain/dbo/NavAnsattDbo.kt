package no.nav.mulighetsrommet.api.domain.dbo

import no.nav.mulighetsrommet.api.domain.dto.NavAnsattDto
import java.util.*

data class NavAnsattDbo(
    /**
     * Azure AD Object Id
     */
    val oid: UUID,
    val navIdent: String,
    val fornavn: String,
    val etternavn: String,
    /**
     * Enhetsnummer til den ansattes hovedenhet.
     */
    val hovedenhet: String,
) {
    companion object {
        fun fromDto(dto: NavAnsattDto) = dto.run {
            NavAnsattDbo(
                oid = oid,
                navIdent = navident,
                fornavn = fornavn,
                etternavn = etternavn,
                hovedenhet = hovedenhetKode,
            )
        }
    }
}
