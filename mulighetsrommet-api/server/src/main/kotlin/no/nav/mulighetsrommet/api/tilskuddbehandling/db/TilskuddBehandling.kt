package no.nav.mulighetsrommet.api.tilskuddbehandling.db

import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingStatus
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingType
import no.nav.mulighetsrommet.model.NavEnhetNummer
import java.util.UUID

data class TilskuddBehandling(
    val id: UUID,
    val gjennomforingId: UUID,
    val tilskudd: List<TilskuddVedtak>,
    val status: TilskuddBehandlingStatus,
    val type: TilskuddBehandlingType,
    val behandlendeEnhet: NavEnhetNummer?,
)
