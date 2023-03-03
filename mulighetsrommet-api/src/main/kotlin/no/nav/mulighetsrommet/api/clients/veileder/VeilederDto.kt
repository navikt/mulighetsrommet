package no.nav.mulighetsrommet.api.clients.veileder

import kotlinx.serialization.Serializable

@Serializable
data class VeilederDto(
    val etternavn: String? = null,
    val fornavn: String? = null,
    val ident: String? = null,
    val navn: String? = null
)
