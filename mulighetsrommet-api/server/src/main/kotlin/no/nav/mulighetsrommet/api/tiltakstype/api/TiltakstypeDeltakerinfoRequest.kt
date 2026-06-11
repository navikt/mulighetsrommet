package no.nav.mulighetsrommet.api.tiltakstype.api

import kotlinx.serialization.Serializable

@Serializable
data class TiltakstypeDeltakerinfoRequest(
    val ledetekst: String?,
    val innholdskoder: List<String>,
)
