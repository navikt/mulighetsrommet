package no.nav.mulighetsrommet.api.fixtures

import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.totrinnskontroll.db.TotrinnskontrollDbo
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Besluttelse
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
import no.nav.mulighetsrommet.model.NavIdent
import java.time.LocalDateTime
import java.util.UUID

fun QueryContext.setTilGodkjenning(
    uuid: UUID,
    type: Totrinnskontroll.Type,
    behandletAv: NavIdent,
    behandletTidspunkt: LocalDateTime = LocalDateTime.now(),
) = queries.totrinnskontroll.upsert(
    TotrinnskontrollDbo(
        id = UUID.randomUUID(),
        entityId = uuid,
        type = type,
        behandletAv = behandletAv,
        behandletTidspunkt = behandletTidspunkt,
        besluttetAv = null,
        besluttetTidspunkt = null,
        besluttelse = null,
        aarsaker = emptyList(),
        forklaring = null,
    ),
)

fun QueryContext.setGodkjent(
    uuid: UUID,
    type: Totrinnskontroll.Type,
    behandletAv: NavIdent,
    besluttetAv: NavIdent,
    behandletTidspunkt: LocalDateTime = LocalDateTime.now(),
    besluttetTidspunkt: LocalDateTime = LocalDateTime.now(),
) = queries.totrinnskontroll.upsert(
    TotrinnskontrollDbo(
        id = UUID.randomUUID(),
        entityId = uuid,
        type = type,
        behandletAv = behandletAv,
        behandletTidspunkt = behandletTidspunkt,
        besluttetAv = besluttetAv,
        besluttetTidspunkt = besluttetTidspunkt,
        besluttelse = Besluttelse.GODKJENT,
        aarsaker = emptyList(),
        forklaring = null,
    ),
)

fun QueryContext.setAvvist(
    uuid: UUID,
    type: Totrinnskontroll.Type,
    behandletAv: NavIdent,
    besluttetAv: NavIdent,
    behandletTidspunkt: LocalDateTime = LocalDateTime.now(),
    besluttetTidspunkt: LocalDateTime = LocalDateTime.now(),
) = queries.totrinnskontroll.upsert(
    TotrinnskontrollDbo(
        id = UUID.randomUUID(),
        entityId = uuid,
        type = type,
        behandletAv = behandletAv,
        behandletTidspunkt = behandletTidspunkt,
        besluttetAv = besluttetAv,
        besluttetTidspunkt = besluttetTidspunkt,
        besluttelse = Besluttelse.AVVIST,
        aarsaker = listOf("Årsak 1"),
        forklaring = null,
    ),
)
