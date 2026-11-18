package no.nav.mulighetsrommet.api.domain.navansatt

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.mulighetsrommet.api.domain.gjennomforing.GjennomforingType
import no.nav.mulighetsrommet.model.NavEnhetNummer

object OkonomiAuthorization {
    fun kanLeseEnkeltplass(ansatt: NavAnsatt): Either<OkonomiAuthorizationError, HarTilgang> = hasAnyRole(
        ansatt,
        Rolle.OKONOMI_LES,
        Rolle.OKONOMI_SAKSBEHANDLER_ENKELTPLASS,
        Rolle.OKONOMI_BESLUTTER_ENKELTPLASS,
    )

    fun kanLeseTilsagn(ansatt: NavAnsatt, type: GjennomforingType): Either<OkonomiAuthorizationError, HarTilgang> = when (type) {
        GjennomforingType.AVTALE -> hasAnyRole(
            ansatt,
            Rolle.OKONOMI_LES,
            Rolle.OKONOMI_SAKSBEHANDLER_GRUPPETILTAK,
            Rolle.OKONOMI_BESLUTTER_GRUPPETILTAK,
            Rolle.OKONOMI_ATTESTANT_GRUPPETILTAK,
        )

        GjennomforingType.ENKELTPLASS -> hasAnyRole(
            ansatt,
            Rolle.OKONOMI_LES,
            Rolle.OKONOMI_SAKSBEHANDLER_ENKELTPLASS,
            Rolle.OKONOMI_BESLUTTER_ENKELTPLASS,
        )

        GjennomforingType.ARENA -> OkonomiAuthorizationError.InvalidState()
            .left()
    }

    fun kanLeseUtbetaling(ansatt: NavAnsatt, type: GjennomforingType): Either<OkonomiAuthorizationError, HarTilgang> = when (type) {
        GjennomforingType.AVTALE -> hasAnyRole(
            ansatt,
            Rolle.OKONOMI_LES,
            Rolle.OKONOMI_SAKSBEHANDLER_GRUPPETILTAK,
            Rolle.OKONOMI_BESLUTTER_GRUPPETILTAK,
            Rolle.OKONOMI_ATTESTANT_GRUPPETILTAK,
        )

        GjennomforingType.ENKELTPLASS -> hasAnyRole(
            ansatt,
            Rolle.OKONOMI_LES,
            Rolle.OKONOMI_SAKSBEHANDLER_ENKELTPLASS,
            Rolle.OKONOMI_BESLUTTER_ENKELTPLASS,
        )

        GjennomforingType.ARENA -> OkonomiAuthorizationError.InvalidState()
            .left()
    }

    fun erSaksbehandler(ansatt: NavAnsatt, type: GjennomforingType): Either<OkonomiAuthorizationError, HarTilgang> = when (type) {
        GjennomforingType.AVTALE -> harGenerellRolle(ansatt, Rolle.OKONOMI_SAKSBEHANDLER_GRUPPETILTAK)

        GjennomforingType.ENKELTPLASS -> erSaksbehandlerEnkeltplass(ansatt)

        GjennomforingType.ARENA -> OkonomiAuthorizationError.InvalidState()
            .left()
    }

    fun erOkonomiBeslutter(
        ansatt: NavAnsatt,
        kontekst: OkonomiBeslutningContext,
    ): Either<OkonomiAuthorizationError, HarTilgang> = erBeslutterTilsagn(ansatt, kontekst).fold(
        ifLeft = { manglerBeslutterError ->
            erAttestantUtbetaling(ansatt, kontekst).mapLeft { manglerAttestantUtbetalingError ->
                kombinerFeil(manglerBeslutterError, manglerAttestantUtbetalingError)
            }
        },
        ifRight = {
            it.right()
        },
    )

    private fun kombinerFeil(
        errorA: OkonomiAuthorizationError,
        errorB: OkonomiAuthorizationError,
    ): OkonomiAuthorizationError = when {
        errorA is OkonomiAuthorizationError.ManglerTilgang && errorB is OkonomiAuthorizationError.ManglerTilgang -> {
            OkonomiAuthorizationError.ManglerKontorspesifikkTilgang(errorA.requiredRoles + errorB.requiredRoles)
        }

        errorA is OkonomiAuthorizationError.ManglerKontorspesifikkTilgang && errorB is OkonomiAuthorizationError.ManglerKontorspesifikkTilgang -> {
            OkonomiAuthorizationError.ManglerKontorspesifikkTilgang(errorA.requiredRoles + errorB.requiredRoles)
        }

        errorA is OkonomiAuthorizationError.ManglerTilgang -> errorA

        errorA is OkonomiAuthorizationError.ManglerKontorspesifikkTilgang -> errorA

        errorB is OkonomiAuthorizationError.ManglerTilgang -> errorB

        errorB is OkonomiAuthorizationError.ManglerKontorspesifikkTilgang -> errorB

        else -> OkonomiAuthorizationError.InvalidState()
    }

    fun erBeslutterTilsagn(
        ansatt: NavAnsatt,
        kontekst: OkonomiBeslutningContext,
    ): Either<OkonomiAuthorizationError, HarTilgang> = when (kontekst.gjennomforingType) {
        GjennomforingType.AVTALE -> harKontorspesifikkRolle(
            Rolle.OKONOMI_BESLUTTER_GRUPPETILTAK,
            ansatt,
            kontekst.kostnadssteder,
        )

        GjennomforingType.ENKELTPLASS -> erBeslutterEnkeltplass(ansatt, kontekst.kostnadssteder)

        GjennomforingType.ARENA -> OkonomiAuthorizationError.InvalidState()
            .left()
    }

    fun erAttestantUtbetaling(
        ansatt: NavAnsatt,
        kontekst: OkonomiBeslutningContext,
    ): Either<OkonomiAuthorizationError, HarTilgang> = when (kontekst.gjennomforingType) {
        GjennomforingType.AVTALE -> harKontorspesifikkRolle(
            Rolle.OKONOMI_ATTESTANT_GRUPPETILTAK,
            ansatt,
            kontekst.kostnadssteder,
        )

        GjennomforingType.ENKELTPLASS -> erBeslutterEnkeltplass(ansatt, kontekst.kostnadssteder)

        GjennomforingType.ARENA -> OkonomiAuthorizationError.InvalidState()
            .left()
    }

    fun erSaksbehandlerEnkeltplass(ansatt: NavAnsatt): Either<OkonomiAuthorizationError, HarTilgang> = harGenerellRolle(ansatt, Rolle.OKONOMI_SAKSBEHANDLER_ENKELTPLASS)

    fun erBeslutterEnkeltplass(
        ansatt: NavAnsatt,
        ansvarligeEnheter: Set<NavEnhetNummer>,
    ): Either<OkonomiAuthorizationError.ManglerKontorspesifikkTilgang, HarTilgang> = harKontorspesifikkRolle(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS, ansatt, ansvarligeEnheter)

    private fun harKontorspesifikkRolle(
        rolle: Rolle,
        ansatt: NavAnsatt,
        ansvarligeEnheter: Set<NavEnhetNummer>,
    ): Either<OkonomiAuthorizationError.ManglerKontorspesifikkTilgang, HarTilgang> = if (ansatt.hasKontorspesifikkRolle(rolle, ansvarligeEnheter)) {
        HarTilgang(setOf(rolle)).right()
    } else {
        OkonomiAuthorizationError.ManglerKontorspesifikkTilgang(setOf(rolle)).left()
    }

    private fun harGenerellRolle(
        ansatt: NavAnsatt,
        rolle: Rolle,
    ): Either<OkonomiAuthorizationError.ManglerTilgang, HarTilgang> = if (ansatt.hasGenerellRolle(rolle)) {
        HarTilgang(setOf(rolle)).right()
    } else {
        OkonomiAuthorizationError.ManglerTilgang(setOf(rolle)).left()
    }

    private fun hasAnyRole(
        ansatt: NavAnsatt,
        vararg requiredRoles: Rolle,
    ): Either<OkonomiAuthorizationError.ManglerTilgang, HarTilgang> {
        val roller = ansatt.roller.filter { it.rolle in requiredRoles }.map { it.rolle }.toSet()
        return if (roller.isEmpty()) {
            OkonomiAuthorizationError.ManglerTilgang(requiredRoles.toSet()).left()
        } else {
            HarTilgang(roller).right()
        }
    }
}

sealed class OkonomiAuthorizationError {
    data class ManglerTilgang(val requiredRoles: Set<Rolle>) : OkonomiAuthorizationError()
    data class ManglerKontorspesifikkTilgang(val requiredRoles: Set<Rolle>) : OkonomiAuthorizationError()
    data class InvalidState(val message: String = "Kan ikke behandle tilsagn, utbetaling eller gjennomføringer fra Arena i Tiltaksadministrasjon") : OkonomiAuthorizationError()
}

data class HarTilgang(
    val roller: Set<Rolle>,
)
