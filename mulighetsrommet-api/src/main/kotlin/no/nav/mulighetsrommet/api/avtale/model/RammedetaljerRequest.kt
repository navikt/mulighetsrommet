package no.nav.mulighetsrommet.api.avtale.model

import kotlinx.serialization.Serializable

@Serializable
data class RammedetaljerRequest(
    val totalRamme: Long,
    val utbetaltArena: Long?,
)
