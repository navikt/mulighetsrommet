package no.nav.mulighetsrommet.tiltakshistorikk

import io.ktor.server.application.*
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.hoplite.loadConfiguration
import no.nav.mulighetsrommet.ktor.plugins.configureMonitoring
import no.nav.mulighetsrommet.ktor.startKtorApplication
import no.nav.mulighetsrommet.tiltakshistorikk.plugins.configureAuthentication
import no.nav.mulighetsrommet.tiltakshistorikk.plugins.configureHTTP
import no.nav.mulighetsrommet.tiltakshistorikk.plugins.configureSerialization

fun main() {
    val (server, app) = loadConfiguration<Config>()

    startKtorApplication(server) {
        configure(app)
    }
}

fun Application.configure(config: AppConfig) {
    val db = Database(config.database)

    configureAuthentication(config.auth)
    configureSerialization()
    configureMonitoring({ db.isHealthy() })
    configureHTTP()
}