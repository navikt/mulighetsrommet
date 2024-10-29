package no.nav.mulighetsrommet.api.routes

import io.ktor.server.routing.*
import no.nav.mulighetsrommet.api.AppConfig
import no.nav.mulighetsrommet.api.okonomi.refusjon.arrangorflateRoutes
import no.nav.mulighetsrommet.api.okonomi.tilsagn.tilsagnRoutes
import no.nav.mulighetsrommet.api.plugins.AuthProvider
import no.nav.mulighetsrommet.api.plugins.authenticate
import no.nav.mulighetsrommet.api.routes.featuretoggles.featureTogglesRoute
import no.nav.mulighetsrommet.api.routes.internal.maamRoutes
import no.nav.mulighetsrommet.api.routes.v1.*
import no.nav.mulighetsrommet.api.veilederflate.routes.arbeidsmarkedstiltakRoutes
import no.nav.mulighetsrommet.api.veilederflate.routes.brukerRoutes
import no.nav.mulighetsrommet.api.veilederflate.routes.delMedBrukerRoutes
import no.nav.mulighetsrommet.api.veilederflate.routes.veilederRoutes
import no.nav.mulighetsrommet.utdanning.api.utdanningRoutes

fun Route.apiRoutes(config: AppConfig) {
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
            featureTogglesRoute(config)
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
    tiltaksgjennomforingRoutes()
    avtaleRoutes()
    navAnsattRoutes()
    arrangorRoutes()
    brregVirksomhetRoutes()
    notificationRoutes()
    janzzRoutes()
    tilsagnRoutes()
    utdanningRoutes()
}

fun Route.veilederflateRoutes() {
    brukerRoutes()
    delMedBrukerRoutes()
    veilederRoutes()
    arbeidsmarkedstiltakRoutes()
}
