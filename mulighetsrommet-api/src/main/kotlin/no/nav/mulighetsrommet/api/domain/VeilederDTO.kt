package no.nav.mulighetsrommet.api.domain

import kotlinx.serialization.Serializable

@Serializable
data class VeilederDTO(
    val etternavn: String?,
    val fornavn: String?,
    val ident: String?,
    val navn: String?
)
