package no.nav.mulighetsrommet.api.domain.dto

import java.util.*

data class NavAnsattDto(
    /**
     * Azure AD Object Id
     */
    val oid: UUID,
    val navident: String,
    val fornavn: String,
    val etternavn: String,
    val hovedenhetKode: String,
    val hovedenhetNavn: String,
)
