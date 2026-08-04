package no.nav.mulighetsrommet.api.domain.totrinnskontroll

import java.util.UUID

sealed interface TotrinnskontrollError {
    data class AlleredeBesluttet(val status: TotrinnskontrollStatus) : TotrinnskontrollError

    data object KanIkkeBesluttesAvBehandler : TotrinnskontrollError

    data object KanBareTilbakestillesNarSattPaVent : TotrinnskontrollError

    /**
     * Klienten sendte inn en annen totrinnskontroll-id enn den som faktisk er til behandling.
     * Betyr at grunnlaget klienten viser er utdatert, f.eks. fordi saksbehandler har endret og
     * sendt inn på nytt mens beslutter så på en tidligere versjon.
     */
    data class UtdatertGrunnlag(val gjeldendeId: UUID) : TotrinnskontrollError
}
