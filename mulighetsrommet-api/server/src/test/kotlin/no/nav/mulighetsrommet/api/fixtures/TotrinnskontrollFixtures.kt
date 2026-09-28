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
        behandling = Totrinnskontroll.Behandling(
            utfortAv = behandletAv,
            tidspunkt = behandletTidspunkt,
            begrunnelse = null,
            aarsaker = emptyList(),
        ),
        beslutning = null,
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
        behandling = Totrinnskontroll.Behandling(
            utfortAv = behandletAv,
            tidspunkt = behandletTidspunkt,
            begrunnelse = null,
            aarsaker = emptyList(),
        ),
        beslutning = Totrinnskontroll.Beslutning(
            utfortAv = besluttetAv,
            tidspunkt = besluttetTidspunkt,
            begrunnelse = null,
            aarsaker = emptyList(),
        ),
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
        behandling = Totrinnskontroll.Behandling(
            utfortAv = behandletAv,
            tidspunkt = behandletTidspunkt,
            begrunnelse = null,
            aarsaker = emptyList(),
        ),
        beslutning = Totrinnskontroll.Beslutning(
            utfortAv = besluttetAv,
            tidspunkt = besluttetTidspunkt,
            begrunnelse = null,
            aarsaker = listOf("Årsak 1"),
        ),
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
        behandling = Totrinnskontroll.Behandling(
            utfortAv = behandletAv,
            tidspunkt = behandletTidspunkt,
            begrunnelse = null,
            aarsaker = emptyList(),
        ),
        beslutning = Totrinnskontroll.Beslutning(
            utfortAv = besluttetAv,
            tidspunkt = besluttetTidspunkt,
            begrunnelse = null,
            aarsaker = emptyList(),
        ),
        status = TotrinnskontrollStatus.SATT_PA_VENT,
    ),
)
