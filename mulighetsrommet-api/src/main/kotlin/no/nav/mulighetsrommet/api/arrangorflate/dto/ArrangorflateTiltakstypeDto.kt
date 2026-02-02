package no.nav.mulighetsrommet.api.arrangorflate.dto

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.Tiltakskode

@Serializable
data class ArrangorflateTiltakstypeDto(
    val navn: String,
    val tiltakskode: Tiltakskode,
)
