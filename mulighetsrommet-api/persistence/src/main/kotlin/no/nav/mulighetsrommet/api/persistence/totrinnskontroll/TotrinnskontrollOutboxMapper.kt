package no.nav.mulighetsrommet.api.persistence.totrinnskontroll

import no.nav.mulighetsrommet.api.contracts.totrinnskontroll.TotrinnskontrollAgent
import no.nav.mulighetsrommet.api.contracts.totrinnskontroll.TotrinnskontrollHendelse
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
    besluttetAv = besluttetAv?.toAgentHendelse(),
    besluttetTidspunkt = besluttetTidspunkt,
    aarsaker = aarsaker,
    forklaring = forklaring,
)

private fun Agent.toAgentHendelse(): TotrinnskontrollAgent = when (this) {
    is NavIdent -> TotrinnskontrollAgent.NavAnsatt(NavIdent(value))

    Arena,
    Tiltaksadministrasjon,
    -> TotrinnskontrollAgent.System(toString())

    Arrangor -> TotrinnskontrollAgent.Arrangor
}
