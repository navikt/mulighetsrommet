package no.nav.mulighetsrommet.arena.adapter.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class ArenaOrdsArrangor(
    val virksomhetsnummer: String,
    val organisasjonsnummerMorselskap: String,
)
