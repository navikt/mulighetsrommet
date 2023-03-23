package no.nav.mulighetsrommet.api

import com.github.kagkarlsson.scheduler.Scheduler
import io.ktor.server.application.*
import io.ktor.server.auth.*
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
    configureSwagger(config.swagger)
    configureStatusPagesForStatusException()

    routing {
        authenticate(AuthProvider.AzureAdNavIdent.name) {
            tiltakstypeRoutes()
            avtaleRoutes()
            tiltaksgjennomforingRoutes()
            sanityRoutes()
            brukerRoutes()
            ansattRoute()
            frontendLoggerRoutes()
            dialogRoutes()
            delMedBrukerRoutes()
            enhetRoutes()
        }
        authenticate(AuthProvider.AzureAdDefaultApp.name) {
            arenaAdapterRoutes()
        }
        authenticate(AuthProvider.AzureAdTiltaksgjennomforingApp.name) {
            externalRoutes()
        }
    }

    val scheduler: Scheduler by inject()

    environment.monitor.subscribe(ApplicationStarted) {
        scheduler.start()
    }

    environment.monitor.subscribe(ApplicationStopPreparing) {
        kafka.stopPollingTopicChanges()

        scheduler.stop()
    }
}
