package no.nav.mulighetsrommet.api

import io.ktor.client.*
import no.nav.mulighetsrommet.api.setup.http.baseClient
import no.nav.mulighetsrommet.database.DatabaseConfig
import no.nav.mulighetsrommet.ktor.ServerConfig

data class Config(
    val server: ServerConfig,
    val app: AppConfig
)

data class AppConfig(
    val database: DatabaseConfig,
    val auth: AuthConfig,
    val sanity: SanityConfig,
    val veilarboppfolgingConfig: VeilarboppfolgingConfig,
    val veilarbvedtaksstotteConfig: VeilarbvedtaksstotteConfig
)

data class AuthConfig(
    val azure: AuthProvider
)

data class SanityConfig(
    val dataset: String,
    val projectId: String,
    val authToken: String
)

data class AuthProvider(
    val issuer: String,
    val jwksUri: String,
    val audience: String,
    val tokenEndpointUrl: String
)

data class VeilarboppfolgingConfig(
    val url: String,
    val scope: String,
    val httpClient: HttpClient = baseClient
)

data class VeilarbvedtaksstotteConfig(
    val url: String,
    val scope: String,
    val httpClient: HttpClient = baseClient
)
