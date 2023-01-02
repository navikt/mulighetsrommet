package no.nav.mulighetsrommet.api.services

import no.nav.mulighetsrommet.api.clients.arenaadapter.ArenaAdaperClient
import no.nav.mulighetsrommet.domain.dto.ExchangeTiltaksnummerForIdResponse

class ArenaAdapterService(
    private val arenaAdaperClient: ArenaAdaperClient
) {
    suspend fun exchangeTiltaksnummerForUUID(tiltaksnummer: String): ExchangeTiltaksnummerForIdResponse? {
        return arenaAdaperClient.exchangeTiltaksnummerForUUID(tiltaksnummer)
    }
}
