package no.nav.mulighetsrommet.api.tilskuddbehandling.db

import no.nav.mulighetsrommet.model.NavEnhetNummer
import java.util.UUID

data class TilskuddBehandlingDbo(
    val id: UUID,
    val gjennomforingId: UUID,
    val status: TilskuddBehandlingStatus,
    val type: TilskuddBehandlingType,
    val behandlendeEnhet: NavEnhetNummer?,
)
