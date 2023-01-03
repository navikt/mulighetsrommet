package no.nav.mulighetsrommet.api.services

import no.nav.mulighetsrommet.api.clients.arenaadapter.ArenaAdaperClient
import no.nav.mulighetsrommet.domain.dto.ExchangeArenaIdForIdResponse
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingsstatusDto
import java.util.UUID

class ArenaAdapterService(
    private val arenaAdaperClient: ArenaAdaperClient
) {
    suspend fun exchangeTiltaksgjennomforingsArenaIdForId(arenaId: String): ExchangeArenaIdForIdResponse? {
        return arenaAdaperClient.exchangeTiltaksgjennomforingsArenaIdForId(arenaId)
    }

    suspend fun hentTiltaksgjennomforingsstatus(id: UUID): TiltaksgjennomforingsstatusDto? {
        return arenaAdaperClient.hentTiltaksgjennomforingsstatus(id)
    }
}
