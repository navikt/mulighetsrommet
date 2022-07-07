package no.nav.mulighetsrommet.api.domain

import kotlinx.serialization.Serializable

@Serializable
data class Oppfolgingsstatus(
    val oppfolgingsenhet: Oppfolgingsenhet?
)

@Serializable
data class Oppfolgingsenhet(
    val navn: String?,
    val enhetId: String?
)
