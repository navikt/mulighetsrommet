package no.nav.mulighetsrommet.api.clients.oppfolging

import kotlinx.serialization.Serializable

@Serializable
data class OppfolgingStatus(
    val erSykmeldtMedArbeidsgiver: Boolean?,
)

@Serializable
data class OppfolgingEnhetMedVeilederResponse(
    val oppfolgingsenhet: Oppfolgingsenhet?,
    val servicegruppe: String?,
)

@Serializable
data class Oppfolgingsenhet(
    val navn: String?,
    val enhetId: String?,
)
