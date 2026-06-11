package no.nav.mulighetsrommet.api.avtale.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.Avtaletype

@Serializable
data class AvtaletypeInfo(
    val type: Avtaletype,
    val tittel: String,
)
