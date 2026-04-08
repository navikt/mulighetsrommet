package no.nav.mulighetsrommet.api.totrinnskontroll.db

import no.nav.mulighetsrommet.api.totrinnskontroll.model.Besluttelse
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
import no.nav.mulighetsrommet.model.Agent
import java.time.LocalDateTime
import java.util.UUID

data class TotrinnskontrollDbo(
    val id: UUID,
    val entityId: UUID,
    val type: Totrinnskontroll.Type,
    val behandletAv: Agent,
    val behandletTidspunkt: LocalDateTime,
    val besluttetAv: Agent?,
    val besluttetTidspunkt: LocalDateTime?,
    val besluttelse: Besluttelse?,
    val aarsaker: List<String>,
    val forklaring: String?,
)

fun Totrinnskontroll.toDbo() = TotrinnskontrollDbo(
    id = id,
    entityId = entityId,
    type = type,
    behandletAv = behandletAv,
    behandletTidspunkt = behandletTidspunkt,
    besluttetAv = besluttetAv,
    besluttetTidspunkt = besluttetTidspunkt,
    besluttelse = besluttelse,
    aarsaker = aarsaker,
    forklaring = forklaring,
)
