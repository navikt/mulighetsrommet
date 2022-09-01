package no.nav.mulighetsrommet.api.domain

import kotlinx.serialization.Serializable

@Serializable
data class ManuellStatusDTO(
    val erUnderManuellOppfolging: Boolean,
    val krrStatus: KrrStatus
)

@Serializable
data class KrrStatus(
    val kanVarsles: Boolean,
    val erReservert: Boolean,
)
