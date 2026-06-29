package no.nav.mulighetsrommet.api.totrinnskontroll.db

import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
import no.nav.mulighetsrommet.api.totrinnskontroll.model.TotrinnskontrollStatus
import no.nav.mulighetsrommet.api.totrinnskontroll.model.TotrinnskontrollType
import no.nav.mulighetsrommet.model.Agent
import java.time.Instant
import java.util.UUID

data class TotrinnskontrollDbo(
    val id: UUID,
    val entityId: UUID,
    val type: TotrinnskontrollType,
    val status: TotrinnskontrollStatus,
    val behandletAv: Agent,
    val behandletTidspunkt: Instant,
    val besluttetAv: Agent?,
    val besluttetTidspunkt: Instant?,
    val aarsaker: List<String>,
    val forklaring: String?,
)

fun Totrinnskontroll.toDbo() = TotrinnskontrollDbo(
    id = id,
    entityId = entityId,
    type = type,
    status = status,
    behandletAv = behandletAv,
    behandletTidspunkt = behandletTidspunkt,
    besluttetAv = besluttetAv,
    besluttetTidspunkt = besluttetTidspunkt,
    aarsaker = aarsaker,
    forklaring = forklaring,
)
