package no.nav.mulighetsrommet.domain.dto

import kotlinx.serialization.Serializable

@Serializable
data class TiltaksgjennomforingKontaktperson(
    val navIdent: String,
    val navn: String,
    val epost: String,
    val mobilnummer: String? = null,
    val navEnheter: List<String>,
    val hovedenhet: String,
)
