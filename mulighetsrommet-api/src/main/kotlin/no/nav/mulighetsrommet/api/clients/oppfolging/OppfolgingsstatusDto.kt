package no.nav.mulighetsrommet.api.clients.oppfolging

import kotlinx.serialization.Serializable

@Serializable
data class OppfolgingsstatusDto(
    val oppfolgingsenhet: Oppfolgingsenhet?,
    val servicegruppe: String?,
)

@Serializable
data class Oppfolgingsenhet(
    val navn: String?,
    val enhetId: String?,
)
