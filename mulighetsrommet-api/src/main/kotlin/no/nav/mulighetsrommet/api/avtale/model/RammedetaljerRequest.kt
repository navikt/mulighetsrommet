package no.nav.mulighetsrommet.api.avtale.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.Valuta

@Serializable
data class RammedetaljerRequest(
    val totalRamme: Long,
    val utbetaltArena: Long?,
)

@Serializable
data class RammedetaljerDefaults(
    val valuta: Valuta,
    val totalRamme: Long,
    val utbetaltArena: Long?,
)
