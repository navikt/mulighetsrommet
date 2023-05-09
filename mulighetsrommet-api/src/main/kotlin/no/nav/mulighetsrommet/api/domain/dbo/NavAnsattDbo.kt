package no.nav.mulighetsrommet.api.domain.dbo

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
)
