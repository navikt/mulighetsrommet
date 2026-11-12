package no.nav.mulighetsrommet.api.utbetaling.api

import kotlinx.serialization.Serializable

@Serializable
data class UtbetalingVedleggDto(
    val dokumentInfoId: String,
    val tittel: String?,
    val variantFormat: String,
)
