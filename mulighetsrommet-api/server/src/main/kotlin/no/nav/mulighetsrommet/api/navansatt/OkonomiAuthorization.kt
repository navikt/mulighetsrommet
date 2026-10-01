package no.nav.mulighetsrommet.api.navansatt

import no.nav.mulighetsrommet.api.domain.navansatt.NavAnsatt
import no.nav.mulighetsrommet.api.domain.navansatt.Rolle
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingType

object OkonomiAuthorization {
    fun kanLeseTilsagn(ansatt: NavAnsatt, type: GjennomforingType): Boolean = when (type) {
        GjennomforingType.AVTALE -> hasAnyRole(
            ansatt,
            Rolle.OKONOMI_LES,
            Rolle.SAKSBEHANDLER_OKONOMI,
            Rolle.BESLUTTER_TILSAGN,
            Rolle.ATTESTANT_UTBETALING,
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
            Rolle.SAKSBEHANDLER_OKONOMI,
            Rolle.BESLUTTER_TILSAGN,
            Rolle.ATTESTANT_UTBETALING,
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
        GjennomforingType.AVTALE -> ansatt.hasGenerellRolle(Rolle.SAKSBEHANDLER_OKONOMI)
        GjennomforingType.ENKELTPLASS -> ansatt.hasGenerellRolle(Rolle.OKONOMI_SAKSBEHANDLER_ENKELTPLASS)
        GjennomforingType.ARENA -> false
    }

    private fun hasAnyRole(ansatt: NavAnsatt, vararg requiredRoles: Rolle): Boolean {
        return ansatt.roller.any { it.rolle in requiredRoles }
    }
}
