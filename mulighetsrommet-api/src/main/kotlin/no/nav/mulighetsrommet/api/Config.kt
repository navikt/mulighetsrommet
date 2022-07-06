package no.nav.mulighetsrommet.api

import com.sksamuel.hoplite.Masked
import no.nav.mulighetsrommet.api.setup.oauth.AzureAd

data class Config(
    val server: ServerConfig,
    val app: AppConfig,
)

data class ServerConfig(
    val host: String,
    val port: Int
)

data class AppConfig(
    val database: DatabaseConfig,
    val auth: AuthConfig,
    val sanity: SanityConfig,
    val veilarbvedtaksstotteTokenConfig: TokenConfig,
    val veilarboppfolgingConfig: TokenConfig
)

data class DatabaseConfig(
    val host: String,
    val port: Int,
    val name: String,
    val schema: String?,
    val user: String,
    val password: Masked
)

data class AuthConfig(
    val azure: AuthProvider
)

data class SanityConfig(
    val dataset: String,
    val projectId: String,
    val authToken: String,
)

data class AuthProvider(
    val issuer: String,
    val jwksUri: String,
    val audience: String,
    val azureAd: AzureAd?
)

data class TokenConfig(
    val authenticationScope: String
)
