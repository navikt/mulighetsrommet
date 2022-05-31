package no.nav.mulighetsrommet.domain

import kotlinx.serialization.Serializable

@Serializable
data class Innsatsgruppe(
    val id: Int? = null,
    val navn: String,
)
