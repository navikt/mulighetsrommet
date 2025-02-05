package no.nav.tiltak.okonomi

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.mulighetsrommet.brreg.BrregClient
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.FlywayMigrationManager
import no.nav.mulighetsrommet.hoplite.loadConfiguration
import no.nav.mulighetsrommet.ktor.plugins.configureMonitoring
import no.nav.mulighetsrommet.tokenprovider.CachedTokenProvider
import no.nav.tiltak.okonomi.api.okonomiRoutes
import no.nav.tiltak.okonomi.db.OkonomiDatabase
import no.nav.tiltak.okonomi.oebs.OebsService
import no.nav.tiltak.okonomi.oebs.OebsTiltakApiClient
import no.nav.tiltak.okonomi.plugins.configureAuthentication
import no.nav.tiltak.okonomi.plugins.configureHTTP
import no.nav.tiltak.okonomi.plugins.configureSerialization

fun main() {
    val (server, app) = loadConfiguration<Config>()

    embeddedServer(
        Netty,
        port = server.port,
        host = server.host,
        module = { configure(app) },
    ).start(wait = true)
}

fun Application.configure(config: AppConfig) {
    val db = Database(config.database)

    FlywayMigrationManager(config.flyway).migrate(db)

    configureAuthentication(config.auth)
    configureSerialization()
    configureMonitoring({ db.isHealthy() })
    configureHTTP()

    val okonomiDb = OkonomiDatabase(db)

    val cachedTokenProvider = CachedTokenProvider.init(config.auth.azure.audience, config.auth.azure.tokenEndpointUrl)

    val oebsClient = OebsTiltakApiClient(
        engine = config.httpClientEngine,
        baseUrl = config.clients.oebsTiltakApi.url,
        tokenProvider = cachedTokenProvider.withScope(config.clients.oebsTiltakApi.scope),
    )

    val brreg = BrregClient(config.httpClientEngine, config.clients.brreg.url)

    val oebsService = OebsService(okonomiDb, oebsClient, brreg)

    okonomiRoutes(okonomiDb, oebsService)

    monitor.subscribe(ApplicationStopPreparing) {
        db.close()
    }
}
