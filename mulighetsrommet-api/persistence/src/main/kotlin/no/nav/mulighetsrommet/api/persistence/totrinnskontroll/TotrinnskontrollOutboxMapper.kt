package no.nav.mulighetsrommet.api.persistence.totrinnskontroll

import no.nav.mulighetsrommet.api.contracts.totrinnskontroll.TotrinnskontrollAgent
import no.nav.mulighetsrommet.api.contracts.totrinnskontroll.TotrinnskontrollHendelse
import no.nav.mulighetsrommet.api.contracts.totrinnskontroll.TotrinnskontrollHendelseV1
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.Totrinnskontroll
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.TotrinnskontrollStatus
import no.nav.mulighetsrommet.model.Agent
import no.nav.mulighetsrommet.model.Arena
import no.nav.mulighetsrommet.model.Arrangor
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Tiltaksadministrasjon

fun Totrinnskontroll.toTotrinnskontrollHendelse(): TotrinnskontrollHendelse = TotrinnskontrollHendelse(
    id = id,
    entityId = entityId,
    type = type,
    status = when (status) {
        TotrinnskontrollStatus.TIL_BEHANDLING -> TotrinnskontrollHendelse.Status.TIL_BEHANDLING
        TotrinnskontrollStatus.SATT_PA_VENT -> TotrinnskontrollHendelse.Status.SATT_PA_VENT
        TotrinnskontrollStatus.GODKJENT -> TotrinnskontrollHendelse.Status.GODKJENT
        TotrinnskontrollStatus.RETURNERT -> TotrinnskontrollHendelse.Status.RETURNERT
    },
    behandletAv = behandletAv.toAgentHendelse(),
    behandletTidspunkt = behandletTidspunkt,
    behandletBegrunnelse = behandletBegrunnelse,
    behandletAarsaker = behandletAarsaker,
    besluttetAv = besluttetAv?.toAgentHendelse(),
    besluttetTidspunkt = besluttetTidspunkt,
    besluttetBegrunnelse = besluttetBegrunnelse,
    besluttetAarsaker = besluttetAarsaker,
)

fun Totrinnskontroll.toTotrinnskontrollHendelseV1(): TotrinnskontrollHendelseV1 {
    val hendelse = toTotrinnskontrollHendelse()
    val behandlet = status in setOf(TotrinnskontrollStatus.TIL_BEHANDLING, TotrinnskontrollStatus.GODKJENT)

    return TotrinnskontrollHendelseV1(
        id = hendelse.id,
        entityId = hendelse.entityId,
        type = hendelse.type,
        status = hendelse.status,
        behandletAv = hendelse.behandletAv,
        behandletTidspunkt = hendelse.behandletTidspunkt,
        besluttetAv = hendelse.besluttetAv,
        besluttetTidspunkt = hendelse.besluttetTidspunkt,
        aarsaker = if (behandlet) behandletAarsaker else besluttetAarsaker,
        forklaring = if (behandlet) behandletBegrunnelse else besluttetBegrunnelse,
    )
}

private fun Agent.toAgentHendelse(): TotrinnskontrollAgent = when (this) {
    is NavIdent -> TotrinnskontrollAgent.NavAnsatt(NavIdent(value))

    Arena,
    Tiltaksadministrasjon,
    -> TotrinnskontrollAgent.System(toString())

    Arrangor -> TotrinnskontrollAgent.Arrangor
}
