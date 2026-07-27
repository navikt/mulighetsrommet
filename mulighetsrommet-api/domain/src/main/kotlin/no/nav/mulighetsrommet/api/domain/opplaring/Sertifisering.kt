package no.nav.mulighetsrommet.api.domain.opplaring

import kotlinx.serialization.Serializable

@Serializable
data class Sertifisering(val konseptId: Long, val label: String)
