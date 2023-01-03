package no.nav.mulighetsrommet.api.clients.arenaadapter

import no.nav.mulighetsrommet.domain.dto.ExchangeArenaIdForIdResponse
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingsstatusDto
import java.util.*

interface ArenaAdaperClient {
    suspend fun exchangeTiltaksgjennomforingsArenaIdForId(arenaId: String): ExchangeArenaIdForIdResponse?

    suspend fun hentTiltaksgjennomforingsstatus(id: UUID): TiltaksgjennomforingsstatusDto?
}
