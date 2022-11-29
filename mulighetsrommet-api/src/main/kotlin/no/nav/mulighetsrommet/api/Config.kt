package no.nav.mulighetsrommet.api

import no.nav.mulighetsrommet.database.DatabaseConfig
import no.nav.mulighetsrommet.ktor.ServerConfig

data class Config(
    val server: ServerConfig,
    val app: AppConfig
)

data class AppConfig(
    val database: DatabaseConfig,
    val kafka: KafkaConfig,
    val auth: AuthConfig,
    val sanity: SanityConfig,
    val swagger: SwaggerConfig? = null,
    val veilarboppfolgingConfig: ServiceClientConfig,
    val veilarbvedtaksstotteConfig: ServiceClientConfig,
    val veilarbpersonConfig: ServiceClientConfig,
    val veilarbdialogConfig: ServiceClientConfig,
    val veilarbveilederConfig: ServiceClientConfig,
    val veilarbarenaConfig: ServiceClientConfig,
    val poaoGcpProxy: ServiceClientConfig,
    val poaoTilgang: ServiceClientConfig,
    val amtEnhetsregister: ServiceClientConfig,
    val arenaOrdsProxy: ServiceClientConfig
)

data class AuthConfig(
    val azure: AuthProvider
)

data class KafkaConfig(
    val producerId: String,
    val brokerUrl: String? = null
)

data class AuthProvider(
    val issuer: String,
    val jwksUri: String,
    val audience: String,
    val tokenEndpointUrl: String
)

data class SanityConfig(
    val dataset: String,
    val projectId: String,
    val authToken: String
)

data class SwaggerConfig(
    val enable: Boolean
)

data class ServiceClientConfig(
    val url: String,
    val scope: String
)
