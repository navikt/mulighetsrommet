package no.nav.mulighetsrommet.api.clients.oppfolging

import kotlinx.serialization.Serializable

@Serializable
data class ManuellStatusDto(
    val erUnderManuellOppfolging: Boolean,
    val krrStatus: KrrStatus,
)

@Serializable
data class KrrStatus(
    val kanVarsles: Boolean,
    val erReservert: Boolean,
)
