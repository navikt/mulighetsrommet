package no.nav.mulighetsrommet.api

import com.codahale.metrics.health.HealthCheckRegistry
import com.sksamuel.hoplite.ConfigLoader
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.mulighetsrommet.api.plugins.*
import no.nav.mulighetsrommet.api.routes.internalRoutes
import no.nav.mulighetsrommet.api.routes.swaggerRoutes
import no.nav.mulighetsrommet.api.routes.v1.*
import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory

fun main() {
    val logger = LoggerFactory.getLogger(Application::class.java)
    val config = ConfigLoader().loadConfigOrThrow<Config>("/application.yaml")

    val databaseConfig = config.app.database

    val jdbcUrl = "jdbc:postgresql://${databaseConfig.host}:${databaseConfig.port}/${databaseConfig.name}"

    val hikariConfig = HikariConfig()
    hikariConfig.jdbcUrl = jdbcUrl
    databaseConfig.schema?.let { hikariConfig.schema = databaseConfig.schema }
    hikariConfig.driverClassName = "org.postgresql.Driver"
    hikariConfig.username = databaseConfig.user
    hikariConfig.password = databaseConfig.password.value
    hikariConfig.maximumPoolSize = 1
    hikariConfig.healthCheckRegistry = HealthCheckRegistry()
    hikariConfig.validate()

    val dataSource = HikariDataSource(hikariConfig)

    val flyway = Flyway
        .configure()
        .dataSource(jdbcUrl, databaseConfig.user, databaseConfig.password.value)
        .apply {
            databaseConfig.schema?.let { schemas(it) }
        }
        .load()

    logger.info("Sletter 'flyway_schema_history' fra database")
    using(sessionOf(dataSource)) {
        it.run(queryOf("DROP TABLE flyway_schema_history;").asExecute)
    }
    logger.info("Kjører baseline for database")
    flyway.baseline()
    logger.info("Migrerer database")
    flyway.migrate()

    logger.info("Ferdig med sletting, baseline og migrering.")
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
            brukerRoutes()
            frontendLoggerRoutes()
        }
    }
}
