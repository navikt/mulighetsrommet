package no.nav.mulighetsrommet.api.clients.arenaadapter

import no.nav.mulighetsrommet.domain.dto.ArenaTiltaksgjennomforingsstatusDto
import no.nav.mulighetsrommet.domain.dto.ExchangeArenaIdForIdResponse
import java.util.*

interface ArenaAdapterClient {
    suspend fun exchangeTiltaksgjennomforingsArenaIdForId(arenaId: String): ExchangeArenaIdForIdResponse?

    suspend fun hentTiltaksgjennomforingsstatus(id: UUID): ArenaTiltaksgjennomforingsstatusDto?
}
