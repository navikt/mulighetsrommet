package no.nav.mulighetsrommet.tiltak.okonomi

import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import no.nav.mulighetsrommet.database.DatabaseConfig
import no.nav.mulighetsrommet.database.FlywayMigrationManager
import no.nav.mulighetsrommet.ktor.ServerConfig

data class Config(
    val server: ServerConfig,
    val app: AppConfig,
)

data class AppConfig(
    val httpClientEngine: HttpClientEngine = CIO.create(),
    val database: DatabaseConfig,
    val flyway: FlywayMigrationManager.MigrationConfig = FlywayMigrationManager.MigrationConfig(),
    val auth: AuthConfig,
    val clients: ClientConfig,
)

data class ClientConfig(
    val oebsTiltakApi: AuthenticatedHttpClientConfig,
)

data class AuthConfig(
    val azure: AuthProvider,
)

data class AuthProvider(
    val issuer: String,
    val jwksUri: String,
    val audience: String,
    val tokenEndpointUrl: String,
)

data class AuthenticatedHttpClientConfig(
    val url: String,
    val scope: String,
)
