package no.nav.mulighetsrommet.api.navansatt

import no.nav.mulighetsrommet.api.domain.navansatt.NavAnsatt
import no.nav.mulighetsrommet.api.domain.navansatt.Rolle
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingType
import no.nav.mulighetsrommet.model.NavEnhetNummer

object OkonomiAuthorization {
    fun kanLeseTilsagn(ansatt: NavAnsatt, type: GjennomforingType): Boolean = when (type) {
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

        GjennomforingType.ARENA -> false
    }

    fun kanLeseUtbetaling(ansatt: NavAnsatt, type: GjennomforingType): Boolean = when (type) {
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

        GjennomforingType.ARENA -> false
    }

    fun erSaksbehandler(ansatt: NavAnsatt, type: GjennomforingType): Boolean = when (type) {
        GjennomforingType.AVTALE -> ansatt.hasGenerellRolle(Rolle.OKONOMI_SAKSBEHANDLER_GRUPPETILTAK)
        GjennomforingType.ENKELTPLASS -> erSaksbehandlerEnkeltplass(ansatt)
        GjennomforingType.ARENA -> false
    }

    fun erOkonomiBeslutter(ansatt: NavAnsatt, kontekst: OkonomiBeslutningContext): Boolean {
        return erBeslutterTilsagn(ansatt, kontekst) || erAttestantUtbetaling(ansatt, kontekst)
    }

    fun erBeslutterTilsagn(ansatt: NavAnsatt, kontekst: OkonomiBeslutningContext): Boolean = when (kontekst.gjennomforingType) {
        GjennomforingType.AVTALE -> ansatt.hasKontorspesifikkRolle(
            Rolle.OKONOMI_BESLUTTER_GRUPPETILTAK,
            kontekst.kostnadssteder,
        )

        GjennomforingType.ENKELTPLASS -> erBeslutterEnkeltplass(ansatt, kontekst.kostnadssteder)

        GjennomforingType.ARENA -> false
    }

    fun erAttestantUtbetaling(ansatt: NavAnsatt, kontekst: OkonomiBeslutningContext): Boolean = when (kontekst.gjennomforingType) {
        GjennomforingType.AVTALE -> ansatt.hasKontorspesifikkRolle(
            Rolle.OKONOMI_ATTESTANT_GRUPPETILTAK,
            kontekst.kostnadssteder,
        )

        GjennomforingType.ENKELTPLASS -> erBeslutterEnkeltplass(ansatt, kontekst.kostnadssteder)

        GjennomforingType.ARENA -> false
    }

    fun erSaksbehandlerEnkeltplass(ansatt: NavAnsatt): Boolean {
        return ansatt.hasGenerellRolle(Rolle.OKONOMI_SAKSBEHANDLER_ENKELTPLASS)
    }

    fun erBeslutterEnkeltplass(ansatt: NavAnsatt, ansvarligeEnheter: Set<NavEnhetNummer>): Boolean {
        return ansatt.hasKontorspesifikkRolle(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS, ansvarligeEnheter)
    }

    private fun hasAnyRole(ansatt: NavAnsatt, vararg requiredRoles: Rolle): Boolean {
        return ansatt.roller.any { it.rolle in requiredRoles }
    }
}
