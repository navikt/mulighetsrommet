package no.nav.mulighetsrommet.api.tilskuddbehandling.model

import no.nav.mulighetsrommet.model.NavIdent
import java.util.UUID

data class AttesterTilskudd(
    val id: UUID,
    val navIdent: NavIdent,
    val forventetTotrinnskontrollId: UUID,
)

data class ReturnerTilskudd(
    val id: UUID,
    val navIdent: NavIdent,
    val aarsaker: List<TilskuddBehandlingStatusAarsak>,
    val forklaring: String?,
    val forventetTotrinnskontrollId: UUID,
)
