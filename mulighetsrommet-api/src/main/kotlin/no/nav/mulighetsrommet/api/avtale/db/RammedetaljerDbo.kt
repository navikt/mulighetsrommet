package no.nav.mulighetsrommet.api.avtale.db

import no.nav.mulighetsrommet.model.Valuta
import java.util.UUID

data class RammedetaljerDbo(
    val avtaleId: UUID,
    val valuta: Valuta,
    val totalRamme: Long,
    val utbetaltArena: Long?,
)
