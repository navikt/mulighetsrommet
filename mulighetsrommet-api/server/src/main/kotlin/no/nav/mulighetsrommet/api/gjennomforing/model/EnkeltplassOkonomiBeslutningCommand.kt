package no.nav.mulighetsrommet.api.gjennomforing.model

import no.nav.mulighetsrommet.model.Agent
import no.nav.mulighetsrommet.model.NavIdent
import java.util.UUID

data class GodkjennOkonomi(
    val id: UUID,
    val agent: Agent,
)

data class SettOkonomiPaVent(
    val id: UUID,
    val navIdent: NavIdent,
    val forklaring: String?,
)
