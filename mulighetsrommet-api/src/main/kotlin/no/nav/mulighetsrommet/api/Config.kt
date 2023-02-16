package no.nav.mulighetsrommet.api

import no.nav.mulighetsrommet.api.producers.TiltaksgjennomforingKafkaProducer
import no.nav.mulighetsrommet.api.producers.TiltakstypeKafkaProducer
import no.nav.mulighetsrommet.api.tasks.SynchronizeNorgEnheter
import no.nav.mulighetsrommet.database.FlywayDatabaseConfig
import no.nav.mulighetsrommet.ktor.ServerConfig

data class Config(
    val server: ServerConfig,
    val app: AppConfig
)

data class AppConfig(
    val database: FlywayDatabaseConfig,
    val kafka: KafkaConfig,
    val auth: AuthConfig,
    val sanity: SanityConfig,
    val swagger: SwaggerConfig? = null,
    val veilarboppfolgingConfig: ServiceClientConfig,
    val veilarbvedtaksstotteConfig: ServiceClientConfig,
    val veilarbpersonConfig: ServiceClientConfig,
    val veilarbdialogConfig: ServiceClientConfig,
    val veilarbveilederConfig: ServiceClientConfig,
    val poaoTilgang: ServiceClientConfig,
    val amtEnhetsregister: ServiceClientConfig,
    val arenaAdapter: ServiceClientConfig,
    val msGraphConfig: ServiceClientConfig,
    val tasks: TaskConfig
)

data class AuthConfig(
    val azure: AuthProvider
)

data class KafkaConfig(
    val brokerUrl: String? = null,
    val producerId: String,
    val producers: KafkaProducers
)

data class KafkaProducers(
    val tiltaksgjennomforinger: TiltaksgjennomforingKafkaProducer.Config,
    val tiltakstyper: TiltakstypeKafkaProducer.Config
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

data class TaskConfig(
    val synchronizeNorgEnheter: SynchronizeNorgEnheter.Config
)
