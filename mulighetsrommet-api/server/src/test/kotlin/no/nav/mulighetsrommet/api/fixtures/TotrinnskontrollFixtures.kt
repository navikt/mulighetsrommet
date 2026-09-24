package no.nav.mulighetsrommet.api.fixtures

import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.Totrinnskontroll
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.TotrinnskontrollStatus
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.TotrinnskontrollType
import no.nav.mulighetsrommet.model.NavIdent
import java.time.Instant
import java.util.UUID

fun QueryContext.setTilBehandling(
    uuid: UUID,
    type: TotrinnskontrollType,
    behandletAv: NavIdent,
    behandletTidspunkt: Instant = Instant.now(),
) = queries.totrinnskontroll.upsert(
    Totrinnskontroll(
        id = UUID.randomUUID(),
        entityId = uuid,
        type = type,
        status = TotrinnskontrollStatus.TIL_BEHANDLING,
        behandletAv = behandletAv,
        behandletTidspunkt = behandletTidspunkt,
        behandletBegrunnelse = null,
        behandletAarsaker = emptyList(),
        besluttetAv = null,
        besluttetTidspunkt = null,
        besluttetBegrunnelse = null,
        besluttetAarsaker = emptyList(),
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
    Totrinnskontroll(
        id = UUID.randomUUID(),
        entityId = uuid,
        type = type,
        behandletAv = behandletAv,
        behandletTidspunkt = behandletTidspunkt,
        behandletBegrunnelse = null,
        behandletAarsaker = emptyList(),
        besluttetAv = besluttetAv,
        besluttetTidspunkt = besluttetTidspunkt,
        besluttetBegrunnelse = null,
        besluttetAarsaker = emptyList(),
        status = TotrinnskontrollStatus.GODKJENT,
    ),
)

fun QueryContext.setReturnert(
    uuid: UUID,
    type: TotrinnskontrollType,
    behandletAv: NavIdent,
    besluttetAv: NavIdent,
    behandletTidspunkt: Instant = Instant.now(),
    besluttetTidspunkt: Instant = Instant.now(),
) = queries.totrinnskontroll.upsert(
    Totrinnskontroll(
        id = UUID.randomUUID(),
        entityId = uuid,
        type = type,
        behandletAv = behandletAv,
        behandletTidspunkt = behandletTidspunkt,
        behandletBegrunnelse = null,
        behandletAarsaker = emptyList(),
        besluttetAv = besluttetAv,
        besluttetTidspunkt = besluttetTidspunkt,
        besluttetBegrunnelse = null,
        besluttetAarsaker = listOf("Årsak 1"),
        status = TotrinnskontrollStatus.RETURNERT,
    ),
)

fun QueryContext.setPaVent(
    uuid: UUID,
    type: TotrinnskontrollType,
    behandletAv: NavIdent,
    besluttetAv: NavIdent,
    behandletTidspunkt: Instant = Instant.now(),
    besluttetTidspunkt: Instant = Instant.now(),
) = queries.totrinnskontroll.upsert(
    Totrinnskontroll(
        id = UUID.randomUUID(),
        entityId = uuid,
        type = type,
        behandletAv = behandletAv,
        behandletTidspunkt = behandletTidspunkt,
        behandletBegrunnelse = null,
        behandletAarsaker = emptyList(),
        besluttetAv = besluttetAv,
        besluttetTidspunkt = besluttetTidspunkt,
        besluttetBegrunnelse = null,
        besluttetAarsaker = emptyList(),
        status = TotrinnskontrollStatus.SATT_PA_VENT,
    ),
)
