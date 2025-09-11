package no.nav.mulighetsrommet.api.avtale.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.Faneinnhold

@Serializable
data class RedaksjoneltInnhold(
    val beskrivelse: String?,
    val faneinnhold: Faneinnhold?,
)
