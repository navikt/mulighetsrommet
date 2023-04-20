package no.nav.mulighetsrommet.api.clients.person

import kotlinx.serialization.Serializable

@Serializable
data class PersonDto(
    val fornavn: String,
)
