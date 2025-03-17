package no.nav.mulighetsrommet.api.fixtures

import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.tilsagn.model.Besluttelse
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
import no.nav.mulighetsrommet.model.NavIdent
import java.time.LocalDateTime
import java.util.*

fun QueryContext.setTilGodkjenning(
    uuid: UUID,
    type: Totrinnskontroll.Type,
    behandletAv: NavIdent,
    behandletTidspunkt: LocalDateTime = LocalDateTime.now(),
) = queries.totrinnskontroll.upsert(
    Totrinnskontroll(
        id = UUID.randomUUID(),
        entityId = uuid,
        behandletAv = behandletAv,
        aarsaker = emptyList(),
        forklaring = null,
        type = type,
        behandletTidspunkt = behandletTidspunkt,
        besluttelse = null,
        besluttetAv = null,
        besluttetTidspunkt = null,
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
    Totrinnskontroll(
        id = UUID.randomUUID(),
        entityId = uuid,
        behandletAv = behandletAv,
        aarsaker = emptyList(),
        forklaring = null,
        type = type,
        behandletTidspunkt = behandletTidspunkt,
        besluttelse = Besluttelse.GODKJENT,
        besluttetAv = besluttetAv,
        besluttetTidspunkt = besluttetTidspunkt,
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
    Totrinnskontroll(
        id = UUID.randomUUID(),
        entityId = uuid,
        behandletAv = behandletAv,
        aarsaker = listOf("Årsak 1"),
        forklaring = null,
        type = type,
        behandletTidspunkt = behandletTidspunkt,
        besluttelse = Besluttelse.AVVIST,
        besluttetAv = besluttetAv,
        besluttetTidspunkt = besluttetTidspunkt,
    ),
)
