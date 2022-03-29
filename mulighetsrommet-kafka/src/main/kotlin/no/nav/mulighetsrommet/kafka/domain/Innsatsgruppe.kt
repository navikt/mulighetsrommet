package no.nav.mulighetsrommet.kafka.domain

import kotlinx.serialization.Serializable

@Serializable
data class Innsatsgruppe(
    val id: Int? = 0,
    val tittel: String,
    val beskrivelse: String,
)
