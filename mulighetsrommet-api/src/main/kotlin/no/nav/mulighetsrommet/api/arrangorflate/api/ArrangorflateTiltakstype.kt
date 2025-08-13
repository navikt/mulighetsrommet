package no.nav.mulighetsrommet.api.arrangorflate.api

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.Tiltakskode

@Serializable
data class ArrangorflateTiltakstype(
    val navn: String,
    val tiltakskode: Tiltakskode,
)
