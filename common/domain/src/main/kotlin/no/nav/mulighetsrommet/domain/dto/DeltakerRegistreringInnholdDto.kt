package no.nav.mulighetsrommet.domain.dto

import kotlinx.serialization.Serializable

@Serializable
data class DeltakerRegistreringInnholdDto(
    val innholdselementer: List<Innholdselement>,
    val ledetekst: String,
)

@Serializable
data class Innholdselement(
    val tekst: String,
    val innholdskode: String,
)
