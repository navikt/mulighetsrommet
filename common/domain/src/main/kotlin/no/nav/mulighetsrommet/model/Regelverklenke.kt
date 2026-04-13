package no.nav.mulighetsrommet.model

import kotlinx.serialization.Serializable

@Serializable
data class Regelverklenke(
    val url: String,
    val navn: String?,
    val beskrivelse: String? = null,
)
