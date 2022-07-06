package no.nav.mulighetsrommet.api

import com.sksamuel.hoplite.ConfigLoader
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.api.clients.oppfolging.VeilarboppfolgingClientImpl
import no.nav.mulighetsrommet.api.plugins.*
import no.nav.mulighetsrommet.api.routes.internalRoutes
import no.nav.mulighetsrommet.api.routes.swaggerRoutes
import no.nav.mulighetsrommet.api.routes.v1.*
import no.nav.mulighetsrommet.api.services.BrukerService
import no.nav.mulighetsrommet.api.setup.http.baseClient
import no.nav.mulighetsrommet.api.setup.oauth.AzureAdClient
import no.nav.mulighetsrommet.api.utils.TokenProviders
import org.slf4j.LoggerFactory

fun main() {
    val config = ConfigLoader().loadConfigOrThrow<Config>("/application.yaml")
    initializeServer(config)
}

fun initializeServer(config: Config) {
    val server = embeddedServer(
        Netty,
        environment = applicationEngineEnvironment {
            log = LoggerFactory.getLogger("ktor.application")

            module {
                configure(config.app)
            }

            connector {
                port = config.server.port
                host = config.server.host
            }
        }
    )
    server.start(true)
}

fun Application.configure(config: AppConfig) {
    val azureAdClient = AzureAdClient(config.auth.azure.azureAd)

    val tokenProviders = TokenProviders(azureAdClient, config)

    val veilarboppfolgingClientImpl: VeilarboppfolgingClientImpl = VeilarboppfolgingClientImpl(
        baseUrl = "https://veilarboppfolging.dev-fss-pub.nais.io/veilarboppfolging/api", // TODO Hent base-url basert på miljø
        tokenProviders.veilarboppfolgingTokenProvider,
        client = baseClient()
    )

    val brukerService = BrukerService(veilarboppfolgingClientImpl)

    configureDependencyInjection(config)
    configureAuthentication(config.auth)
    configureRouting()
    configureSecurity()
    configureHTTP()
    configureMonitoring()
    configureSerialization()
    configureWebjars()

    routing {
        internalRoutes()
        swaggerRoutes()

        authenticate {
            tiltakstypeRoutes()
            tiltaksgjennomforingRoutes()
            innsatsgruppeRoutes()
            arenaRoutes()
            sanityRoutes()
            brukerRoutes(brukerService = brukerService)
        }
    }
}
