package no.nav.mulighetsrommet.api

import com.github.kagkarlsson.scheduler.Scheduler
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.api.plugins.*
import no.nav.mulighetsrommet.api.plugins.AuthProvider
import no.nav.mulighetsrommet.api.routes.internal.frontendLoggerRoutes
import no.nav.mulighetsrommet.api.routes.v1.*
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.hoplite.loadConfiguration
import no.nav.mulighetsrommet.kafka.KafkaConsumerOrchestrator
import no.nav.mulighetsrommet.ktor.plugins.configureMonitoring
import no.nav.mulighetsrommet.ktor.plugins.configureStatusPagesForStatusException
import no.nav.mulighetsrommet.ktor.startKtorApplication
import org.koin.ktor.ext.inject

fun main() {
    val (server, app) = loadConfiguration<Config>()

    startKtorApplication(server) {
        configure(app)
    }
}

fun Application.configure(config: AppConfig) {
    val db by inject<Database>()
    val kafka: KafkaConsumerOrchestrator by inject()

    configureDependencyInjection(config)
    configureAuthentication(config.auth)
    configureRouting()
    configureSecurity()
    configureHTTP()
    configureMonitoring({ db.isHealthy() })
    configureSerialization()
    configureStatusPagesForStatusException()

    routing {
        swaggerUI(path = "/swagger-ui/internal", swaggerFile = "web/openapi.yaml")
        authenticate(AuthProvider.AzureAdNavIdent.name) {
            tiltakstypeRoutes()
            tiltaksgjennomforingRoutes()
            avtaleRoutes()
            sanityRoutes()
            brukerRoutes()
            navAnsattRoutes()
            frontendLoggerRoutes()
            dialogRoutes()
            delMedBrukerRoutes()
            navEnhetRoutes()
            virksomhetRoutes()
            notificationRoutes()
        }

        authenticate(AuthProvider.AzureAdDefaultApp.name) {
            arenaAdapterRoutes()
        }

        swaggerUI(path = "/swagger-ui/external", swaggerFile = "web/openapi-external.yaml")
        authenticate(AuthProvider.AzureAdTiltaksgjennomforingApp.name) {
            externalRoutes()
        }
    }

    val scheduler: Scheduler by inject()

    environment.monitor.subscribe(ApplicationStarted) {
        kafka.enableFailedRecordProcessor()

        scheduler.start()
    }

    environment.monitor.subscribe(ApplicationStopPreparing) {
        kafka.disableFailedRecordProcessor()
        kafka.stopPollingTopicChanges()

        scheduler.stop()
    }
}
