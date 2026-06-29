package no.nav.mulighetsrommet.api.fixtures

import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.totrinnskontroll.db.TotrinnskontrollDbo
import no.nav.mulighetsrommet.api.totrinnskontroll.model.TotrinnskontrollStatus
import no.nav.mulighetsrommet.api.totrinnskontroll.model.TotrinnskontrollType
import no.nav.mulighetsrommet.model.NavIdent
import java.time.Instant
import java.util.UUID

fun QueryContext.setTilBehandling(
    uuid: UUID,
    type: TotrinnskontrollType,
    behandletAv: NavIdent,
    behandletTidspunkt: Instant = Instant.now(),
) = queries.totrinnskontroll.upsert(
    TotrinnskontrollDbo(
        id = UUID.randomUUID(),
        entityId = uuid,
        type = type,
        status = TotrinnskontrollStatus.TIL_BEHANDLING,
        behandletAv = behandletAv,
        behandletTidspunkt = behandletTidspunkt,
        besluttetAv = null,
        besluttetTidspunkt = null,
        aarsaker = emptyList(),
        forklaring = null,
    ),
)

fun QueryContext.setGodkjent(
    uuid: UUID,
    type: TotrinnskontrollType,
    behandletAv: NavIdent,
    besluttetAv: NavIdent,
    behandletTidspunkt: Instant = Instant.now(),
    besluttetTidspunkt: Instant = Instant.now(),
) = queries.totrinnskontroll.upsert(
    TotrinnskontrollDbo(
        id = UUID.randomUUID(),
        entityId = uuid,
        type = type,
        behandletAv = behandletAv,
        behandletTidspunkt = behandletTidspunkt,
        besluttetAv = besluttetAv,
        besluttetTidspunkt = besluttetTidspunkt,
        status = TotrinnskontrollStatus.GODKJENT,
        aarsaker = emptyList(),
        forklaring = null,
    ),
)

fun QueryContext.setAvvist(
    uuid: UUID,
    type: TotrinnskontrollType,
    behandletAv: NavIdent,
    besluttetAv: NavIdent,
    behandletTidspunkt: Instant = Instant.now(),
    besluttetTidspunkt: Instant = Instant.now(),
) = queries.totrinnskontroll.upsert(
    TotrinnskontrollDbo(
        id = UUID.randomUUID(),
        entityId = uuid,
        type = type,
        behandletAv = behandletAv,
        behandletTidspunkt = behandletTidspunkt,
        besluttetAv = besluttetAv,
        besluttetTidspunkt = besluttetTidspunkt,
        status = TotrinnskontrollStatus.AVVIST,
        aarsaker = listOf("Årsak 1"),
        forklaring = null,
    ),
)
