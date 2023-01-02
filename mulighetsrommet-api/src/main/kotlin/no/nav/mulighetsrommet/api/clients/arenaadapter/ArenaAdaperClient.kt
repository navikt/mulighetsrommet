package no.nav.mulighetsrommet.api.clients.arenaadapter

import no.nav.mulighetsrommet.domain.dto.ExchangeTiltaksnummerForIdResponse

interface ArenaAdaperClient {
    suspend fun exchangeTiltaksnummerForUUID(tiltaksnummer: String): ExchangeTiltaksnummerForIdResponse?
}
