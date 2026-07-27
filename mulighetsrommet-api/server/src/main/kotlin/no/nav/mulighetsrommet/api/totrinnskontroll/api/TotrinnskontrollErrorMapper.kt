package no.nav.mulighetsrommet.api.totrinnskontroll.api

import no.nav.mulighetsrommet.api.domain.totrinnskontroll.TotrinnskontrollError
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.TotrinnskontrollStatus
import no.nav.mulighetsrommet.model.FieldError

fun TotrinnskontrollError.toFieldErrors(): List<FieldError> = when (this) {
    is TotrinnskontrollError.AlleredeBesluttet -> {
        val beskrivelse = when (status) {
            TotrinnskontrollStatus.RETURNERT -> "returnert"
            TotrinnskontrollStatus.GODKJENT -> "godkjent"
            TotrinnskontrollStatus.SATT_PA_VENT -> "satt på vent"
            TotrinnskontrollStatus.TIL_BEHANDLING -> error("Totrinnskontroll er til behandling")
        }
        listOf(FieldError.of("Totrinnskontrollen er allerede $beskrivelse"))
    }

    TotrinnskontrollError.KanIkkeBesluttesAvBehandler ->
        listOf(FieldError.of("Du kan ikke beslutte noe du selv har behandlet"))

    TotrinnskontrollError.KanBareTilbakestillesNarSattPaVent ->
        listOf(FieldError.of("Totrinnskontrollen kan bare tilbakestilles når den er satt på vent"))
}
