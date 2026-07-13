package no.nav.mulighetsrommet.api.domain.totrinnskontroll

sealed interface TotrinnskontrollError {
    data class AlleredeBesluttet(val status: TotrinnskontrollStatus) : TotrinnskontrollError

    data object KanIkkeBesluttesAvBehandler : TotrinnskontrollError

    data object KanBareTilbakestillesNarSattPaVent : TotrinnskontrollError
}
