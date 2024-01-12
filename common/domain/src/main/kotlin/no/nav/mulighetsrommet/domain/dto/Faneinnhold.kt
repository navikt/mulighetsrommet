package no.nav.mulighetsrommet.domain.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class Faneinnhold(
    val forHvem: List<JsonObject>? = emptyList(),
    val forHvemInfoboks: String? = null,
    val detaljerOgInnhold: List<JsonObject>? = emptyList(),
    val detaljerOgInnholdInfoboks: String? = null,
    val pameldingOgVarighet: List<JsonObject>? = emptyList(),
    val pameldingOgVarighetInfoboks: String? = null,
    val kontaktinfo: List<JsonObject>? = emptyList(),
)
