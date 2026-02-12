package no.nav.mulighetsrommet.api.utbetaling.api

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.Valuta

@Serializable
data class ValutaBelopRequest(
    val belop: Int?,
    val valuta: Valuta?,
)
