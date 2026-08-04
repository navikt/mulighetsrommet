package no.nav.mulighetsrommet.api.totrinnskontroll.api

import arrow.core.Either
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.TotrinnskontrollError
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.TotrinnskontrollStatus
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.TotrinnskontrollType
import no.nav.mulighetsrommet.model.FieldError
import java.util.UUID

/**
 * Validerer at [forventetTotrinnskontrollId] er id-en til den totrinnskontrollen som faktisk er til
 * behandling for ([entityId], [type]) akkurat nå. Skal kalles én gang per beslutter/attestant-endepunkt,
 * rett etter at man har bestemt hvilken totrinnskontroll som er relevant, og før man kaller
 * `godkjenn`/`returner`/`settPaVent` på den. Interne/automatiske flows har ingen ekstern forventning å
 * validere mot og skal ikke kalle denne.
 */
fun QueryContext.sjekkGjeldendeTotrinnskontroll(
    entityId: UUID,
    type: TotrinnskontrollType,
    forventetTotrinnskontrollId: UUID,
): Either<List<FieldError>, Unit> = queries.totrinnskontroll.getOrError(entityId, type)
    .sjekkGjeldende(forventetTotrinnskontrollId)
    .mapLeft { it.toFieldErrors() }
    .map { }

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

    is TotrinnskontrollError.UtdatertGrunnlag ->
        listOf(FieldError.of("Grunnlaget har endret seg siden det ble hentet. Last inn siden på nytt og prøv igjen."))
}
