package no.nav.mulighetsrommet.api.routes

import io.ktor.server.auth.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.api.AppConfig
import no.nav.mulighetsrommet.api.okonomi.tilsagn.tilsagnRoutes
import no.nav.mulighetsrommet.api.plugins.AuthProvider
import no.nav.mulighetsrommet.api.routes.featuretoggles.featureTogglesRoute
import no.nav.mulighetsrommet.api.routes.internal.maamRoutes
import no.nav.mulighetsrommet.api.routes.v1.*

fun Route.apiRoutes(config: AppConfig) {
    authenticate(AuthProvider.AZURE_AD_TEAM_MULIGHETSROMMET.name) {
        maamRoutes()
    }

    authenticate(AuthProvider.AZURE_AD_TILTAKSGJENNOMFORING_APP.name) {
        externalRoutes()
    }

    authenticate(AuthProvider.AZURE_AD_DEFAULT_APP.name) {
        arenaAdapterRoutes()
    }

    route("/api/v1/intern") {
        authenticate(AuthProvider.AZURE_AD_NAV_IDENT.name) {
            featureTogglesRoute(config)
            lagretFilterRoutes()
            navEnhetRoutes()
            veilederflateRoutes()
        }

        authenticate(AuthProvider.AZURE_AD_TILTAKSADMINISTRASJON_GENERELL.name) {
            adminflateRoutes()
        }
    }
}

fun Route.adminflateRoutes() {
    tiltakstypeRoutes()
    tiltaksgjennomforingRoutes()
    avtaleRoutes()
    navAnsattRoutes()
    arrangorRoutes()
    brregVirksomhetRoutes()
    notificationRoutes()
    janzzRoutes()
    opsjonRoutes()
    tilsagnRoutes()
}

fun Route.veilederflateRoutes() {
    brukerRoutes()
    dialogRoutes()
    delMedBrukerRoutes()
    veilederJoyrideRoutes()
    veilederTiltakRoutes()
}
