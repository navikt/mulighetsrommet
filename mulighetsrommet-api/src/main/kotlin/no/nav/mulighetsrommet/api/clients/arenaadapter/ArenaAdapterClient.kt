package no.nav.mulighetsrommet.api.clients.arenaadapter

import no.nav.mulighetsrommet.domain.dto.ArenaTiltaksgjennomforingDto
import no.nav.mulighetsrommet.domain.dto.ExchangeArenaIdForIdResponse
import java.util.*

interface ArenaAdapterClient {
    suspend fun exchangeTiltaksgjennomforingsArenaIdForId(arenaId: String): ExchangeArenaIdForIdResponse?

    suspend fun hentArenadata(id: UUID): ArenaTiltaksgjennomforingDto?
}
