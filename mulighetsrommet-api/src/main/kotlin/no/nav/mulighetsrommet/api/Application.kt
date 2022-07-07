package no.nav.mulighetsrommet.api

import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.RSAKey
import com.sksamuel.hoplite.ConfigLoader
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import no.nav.common.token_client.builder.AzureAdTokenClientBuilder
import no.nav.mulighetsrommet.api.clients.oppfolging.VeilarboppfolgingClientImpl
import no.nav.mulighetsrommet.api.clients.vedtak.VeilarbvedtaksstotteClientImpl
import no.nav.mulighetsrommet.api.plugins.*
import no.nav.mulighetsrommet.api.routes.internalRoutes
import no.nav.mulighetsrommet.api.routes.swaggerRoutes
import no.nav.mulighetsrommet.api.routes.v1.*
import no.nav.mulighetsrommet.api.services.BrukerService
import no.nav.mulighetsrommet.api.setup.http.baseClient
import org.slf4j.LoggerFactory
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey

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
    val tokenClient = when (erLokalUtvikling()) {
        true -> AzureAdTokenClientBuilder.builder()
            .withClientId(config.auth.azure.audience)
            .withPrivateJwk(createRSAKey("azure").toJSONString())
            .withTokenEndpointUrl(config.auth.azure.tokenEndpointUrl)
            .buildOnBehalfOfTokenClient()
        false -> AzureAdTokenClientBuilder.builder().withNaisDefaults().buildOnBehalfOfTokenClient()
    }

    val veilarboppfolgingClientImpl = VeilarboppfolgingClientImpl(
        baseUrl = config.veilarboppfolgingConfig.url,
        tokenClient,
        config,
        client = baseClient
    )

    val veilarbvedtaksstotteClientImpl = VeilarbvedtaksstotteClientImpl(
        baseUrl = config.veilarbvedtaksstotteConfig.url,
        tokenClient,
        config,
        client = baseClient
    )

    val brukerService = BrukerService(veilarboppfolgingClientImpl, veilarbvedtaksstotteClientImpl)

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

fun erLokalUtvikling(): Boolean {
    return System.getenv("NAIS_CLUSTER_NAME") == null
}

fun createRSAKey(keyID: String): RSAKey = KeyPairGenerator
    .getInstance("RSA").let {
        it.initialize(2048)
        it.generateKeyPair()
    }.let {
        RSAKey.Builder(it.public as RSAPublicKey)
            .privateKey(it.private as RSAPrivateKey)
            .keyUse(KeyUse.SIGNATURE)
            .keyID(keyID)
            .build()
    }
