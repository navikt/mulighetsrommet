package no.nav.mulighetsrommet.api.clients.arenaadapter

import java.util.*

interface ArenaAdaperClient {
    suspend fun exchangeTiltaksnummerForUUID(tiltaksnummer: String): UUID?
}
