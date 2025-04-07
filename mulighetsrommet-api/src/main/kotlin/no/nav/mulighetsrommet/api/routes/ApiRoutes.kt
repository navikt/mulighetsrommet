package no.nav.mulighetsrommet.api.routes

import io.ktor.server.routing.*
import no.nav.mulighetsrommet.api.arenaadapter.arenaAdapterRoutes
import no.nav.mulighetsrommet.api.arrangor.arrangorRoutes
import no.nav.mulighetsrommet.api.arrangorflate.api.arrangorflateRoutes
import no.nav.mulighetsrommet.api.avtale.avtaleRoutes
import no.nav.mulighetsrommet.api.gjennomforing.gjennomforingRoutes
import no.nav.mulighetsrommet.api.lagretfilter.lagretFilterRoutes
import no.nav.mulighetsrommet.api.navansatt.api.navAnsattRoutes
import no.nav.mulighetsrommet.api.navenhet.navEnhetRoutes
import no.nav.mulighetsrommet.api.plugins.AuthProvider
import no.nav.mulighetsrommet.api.plugins.authenticate
import no.nav.mulighetsrommet.api.routes.featuretoggles.featureTogglesRoute
import no.nav.mulighetsrommet.api.routes.internal.maamRoutes
import no.nav.mulighetsrommet.api.routes.v1.externalRoutes
import no.nav.mulighetsrommet.api.routes.v1.janzzRoutes
import no.nav.mulighetsrommet.api.tilsagn.api.tilsagnRoutes
import no.nav.mulighetsrommet.api.tiltakstype.tiltakstypeRoutes
import no.nav.mulighetsrommet.api.utbetaling.api.utbetalingRoutes
import no.nav.mulighetsrommet.api.veilederflate.routes.arbeidsmarkedstiltakRoutes
import no.nav.mulighetsrommet.api.veilederflate.routes.brukerRoutes
import no.nav.mulighetsrommet.api.veilederflate.routes.delMedBrukerRoutes
import no.nav.mulighetsrommet.api.veilederflate.routes.veilederRoutes
import no.nav.mulighetsrommet.notifications.notificationRoutes
import no.nav.mulighetsrommet.oppgaver.oppgaverRoutes
import no.nav.mulighetsrommet.utdanning.utdanningRoutes

fun Route.apiRoutes() {
    authenticate(AuthProvider.AZURE_AD_TEAM_MULIGHETSROMMET) {
        maamRoutes()
    }

    authenticate(AuthProvider.AZURE_AD_TILTAKSGJENNOMFORING_APP) {
        externalRoutes()
    }

    authenticate(AuthProvider.AZURE_AD_DEFAULT_APP) {
        arenaAdapterRoutes()
    }

    route("/api/v1/intern") {
        authenticate(AuthProvider.AZURE_AD_NAV_IDENT) {
            featureTogglesRoute()
            lagretFilterRoutes()
            navEnhetRoutes()
            veilederflateRoutes()
        }

        authenticate(AuthProvider.AZURE_AD_TILTAKSADMINISTRASJON_GENERELL) {
            adminflateRoutes()
        }

        authenticate(AuthProvider.TOKEN_X_ARRANGOR_FLATE) {
            arrangorflateRoutes()
        }
    }
}

fun Route.adminflateRoutes() {
    tiltakstypeRoutes()
    gjennomforingRoutes()
    avtaleRoutes()
    navAnsattRoutes()
    arrangorRoutes()
    notificationRoutes()
    janzzRoutes()
    tilsagnRoutes()
    utdanningRoutes()
    oppgaverRoutes()
    utbetalingRoutes()
}

fun Route.veilederflateRoutes() {
    brukerRoutes()
    delMedBrukerRoutes()
    veilederRoutes()
    arbeidsmarkedstiltakRoutes()
}
