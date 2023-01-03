package no.nav.mulighetsrommet.api.clients.arenaadapter

import no.nav.mulighetsrommet.domain.dto.ExchangeArenaIdForIdResponse

interface ArenaAdaperClient {
    suspend fun exchangeTiltaksgjennomforingsArenaIdForId(arenaId: String): ExchangeArenaIdForIdResponse?
}
