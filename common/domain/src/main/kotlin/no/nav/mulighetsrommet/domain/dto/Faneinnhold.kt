package no.nav.mulighetsrommet.domain.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
class Faneinnhold(
    val forHvem: List<JsonObject>? = emptyList(),
    val forHvemInfoboks: String? = null,
    val detaljerOgInnhold: List<JsonObject>? = emptyList(),
    val detaljerOgInnholdInfoboks: String? = null,
    val pameldingOgVarighet: List<JsonObject>? = emptyList(),
    val pameldingOgVarighetInfoboks: String? = null,
)

fun Faneinnhold?.emptyOrNull(): Boolean =
    when (this) {
        null -> true
        else -> {
            this.forHvem == null &&
                this.forHvemInfoboks == null &&
                this.detaljerOgInnhold == null &&
                this.detaljerOgInnholdInfoboks == null &&
                this.pameldingOgVarighet == null &&
                this.pameldingOgVarighetInfoboks == null
        }
    }
