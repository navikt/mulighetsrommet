package no.nav.mulighetsrommet.model

import kotlinx.serialization.Serializable

@Serializable
data class Regelverklenke(
    val regelverkUrl: String,
    val regelverkLenkeNavn: String?,
    val beskrivelse: String? = null,
)
