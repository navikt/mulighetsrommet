package no.nav.mulighetsrommet.api.domain

import kotlinx.serialization.Serializable

@Serializable
data class VeilederDTO(
    val etternavn: String? = null,
    val fornavn: String? = null,
    val ident: String? = null,
    val navn: String? = null
)
