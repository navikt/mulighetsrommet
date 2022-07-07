package no.nav.mulighetsrommet.api.domain

import kotlinx.serialization.Serializable

@Serializable
data class Oppfolgingsstatus(
    val oppfolgingsenhet: Oppfolgingsenhet?,
    val veilederId: String?,
    val formidlingsgruppe: String?,
    val servicegruppe: String?,
    val hovedmaalkode: String?
)

@Serializable
data class Oppfolgingsenhet(
    val navn: String?,
    val enhetId: String?
)
