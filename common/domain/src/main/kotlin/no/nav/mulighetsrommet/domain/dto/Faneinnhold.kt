package no.nav.mulighetsrommet.domain.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class Faneinnhold(
    val forHvem: List<JsonObject>? = null,
    val forHvemInfoboks: String? = null,
    val detaljerOgInnhold: List<JsonObject>? = null,
    val detaljerOgInnholdInfoboks: String? = null,
    val pameldingOgVarighet: List<JsonObject>? = null,
    val pameldingOgVarighetInfoboks: String? = null,
    val kontaktinfo: List<JsonObject>? = null,
    val kontaktinfoInfoboks: String? = null,
)
