package no.nav.mulighetsrommet.api.totrinnskontroll.db

import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
import no.nav.mulighetsrommet.api.totrinnskontroll.model.TotrinnskontrollBesluttelse
import no.nav.mulighetsrommet.api.totrinnskontroll.model.TotrinnskontrollType
import no.nav.mulighetsrommet.model.Agent
import java.time.Instant
import java.util.UUID

data class TotrinnskontrollDbo(
    val id: UUID,
    val entityId: UUID,
    val type: TotrinnskontrollType,
    val behandletAv: Agent,
    val behandletTidspunkt: Instant,
    val besluttetAv: Agent?,
    val besluttetTidspunkt: Instant?,
    val besluttelse: TotrinnskontrollBesluttelse?,
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
