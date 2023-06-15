package no.nav.mulighetsrommet.domain.dto

import kotlinx.serialization.Serializable

@Serializable
data class TiltaksgjennomforingKontaktperson(
    val navIdent: String,
    val navEnheter: List<String>,
)
