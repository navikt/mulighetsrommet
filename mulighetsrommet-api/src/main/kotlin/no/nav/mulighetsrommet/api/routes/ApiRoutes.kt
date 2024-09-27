package no.nav.mulighetsrommet.api.routes

import io.ktor.server.routing.*
import no.nav.mulighetsrommet.api.AppConfig
import no.nav.mulighetsrommet.api.okonomi.refusjon.refusjonRoutes
import no.nav.mulighetsrommet.api.okonomi.tilsagn.tilsagnRoutes
import no.nav.mulighetsrommet.api.plugins.AuthProvider
import no.nav.mulighetsrommet.api.plugins.authenticate
import no.nav.mulighetsrommet.api.routes.featuretoggles.featureTogglesRoute
import no.nav.mulighetsrommet.api.routes.internal.maamRoutes
import no.nav.mulighetsrommet.api.routes.v1.*
import no.nav.mulighetsrommet.api.routes.v1.arbeidsmarkedstiltakRoutes
import no.nav.mulighetsrommet.api.routes.v1.brukerRoutes
import no.nav.mulighetsrommet.api.routes.v1.delMedBrukerRoutes

fun Route.apiRoutes(config: AppConfig) {
    authenticate(AuthProvider.AZURE_AD_TEAM_MULIGHETSROMMET) {
        maamRoutes()
    }

    refusjonRoutes()

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
