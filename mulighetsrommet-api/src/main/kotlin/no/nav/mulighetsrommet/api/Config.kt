package no.nav.mulighetsrommet.api

import io.ktor.client.*
import no.nav.mulighetsrommet.api.setup.http.baseClient
import no.nav.mulighetsrommet.database.DatabaseConfig
import no.nav.mulighetsrommet.ktor.ServerConfig
import no.nav.mulighetsrommet.ktor.plugins.SentryConfig

data class Config(
    val server: ServerConfig,
    val app: AppConfig
)

data class AppConfig(
    val database: DatabaseConfig,
    val auth: AuthConfig,
    val sanity: SanityConfig,
    val sentry: SentryConfig? = null,
    val veilarboppfolgingConfig: VeilarboppfolgingConfig,
    val veilarbvedtaksstotteConfig: VeilarbvedtaksstotteConfig,
    val veilarbpersonConfig: VeilarbpersonConfig,
    val veilarbdialogConfig: VeilarbdialogConfig,
    val veilarbveilederConfig: VeilarbveilederConfig,
    val swagger: SwaggerConfig? = null
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

data class VeilarbpersonConfig(
    val url: String,
    val scope: String,
    val httpClient: HttpClient = baseClient
)

data class VeilarbdialogConfig(
    val url: String,
    val scope: String,
    val httpClient: HttpClient = baseClient
)

data class VeilarbveilederConfig(
    val url: String,
    val scope: String,
    val httpClient: HttpClient = baseClient
)

data class SwaggerConfig(
    val enable: Boolean
)
