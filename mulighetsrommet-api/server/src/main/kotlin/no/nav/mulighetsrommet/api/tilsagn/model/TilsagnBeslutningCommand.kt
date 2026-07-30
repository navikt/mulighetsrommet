package no.nav.mulighetsrommet.api.tilsagn.model

import no.nav.mulighetsrommet.model.Agent
import no.nav.mulighetsrommet.model.NavIdent
import java.util.UUID

data class GodkjennTilsagn(
    val id: UUID,
    val agent: Agent,
)

data class ReturnerTilsagn(
    val id: UUID,
    val navIdent: NavIdent,
    val aarsaker: List<TilsagnStatusAarsak>,
    val forklaring: String?,
)
