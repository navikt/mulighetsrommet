package no.nav.mulighetsrommet.api.tiltakstype.api

import kotlinx.serialization.Serializable

@Serializable
data class RedaksjoneltInnholdLenkeRequest(
    val url: String,
    val navn: String?,
    val beskrivelse: String?,
)
