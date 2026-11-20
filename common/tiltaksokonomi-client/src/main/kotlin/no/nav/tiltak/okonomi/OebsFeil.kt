package no.nav.tiltak.okonomi

import kotlinx.serialization.Serializable

@Serializable
data class OebsFeil(
    val kode: String?,
    val melding: String?,
)
