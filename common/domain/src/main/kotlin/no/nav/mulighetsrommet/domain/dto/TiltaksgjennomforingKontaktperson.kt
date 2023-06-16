package no.nav.mulighetsrommet.domain.dto

import kotlinx.serialization.Serializable

@Serializable
data class TiltaksgjennomforingKontaktperson(
    val navIdent: String,
    val navn: String? = null,
    val epost: String? = null,
    val mobilnummer: String? = null,
    val navEnheter: List<String>,
)
