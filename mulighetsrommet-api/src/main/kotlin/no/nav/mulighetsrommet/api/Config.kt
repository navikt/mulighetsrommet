package no.nav.mulighetsrommet.api

import no.nav.mulighetsrommet.api.clients.brreg.BrregClientImpl
import no.nav.mulighetsrommet.api.clients.sanity.SanityClient
import no.nav.mulighetsrommet.api.producers.TiltaksgjennomforingKafkaProducer
import no.nav.mulighetsrommet.api.producers.TiltakstypeKafkaProducer
import no.nav.mulighetsrommet.api.tasks.SynchronizeNavAnsatte
import no.nav.mulighetsrommet.api.tasks.SynchronizeNorgEnheter
import no.nav.mulighetsrommet.api.tasks.SynchronizeTilgjengelighetsstatuserToSanity
import no.nav.mulighetsrommet.api.tasks.SynchronizeTiltaksgjennomforingEnheter
import no.nav.mulighetsrommet.database.FlywayDatabaseAdapter
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.ktor.ServerConfig

data class Config(
    val server: ServerConfig,
    val app: AppConfig,
)

data class AppConfig(
    val database: FlywayDatabaseAdapter.Config,
    val kafka: KafkaConfig,
    val auth: AuthConfig,
    val sanity: SanityClient.Config,
    val veilarboppfolgingConfig: ServiceClientConfig,
    val veilarbvedtaksstotteConfig: ServiceClientConfig,
    val veilarbpersonConfig: ServiceClientConfig,
    val veilarbdialogConfig: ServiceClientConfig,
    val veilarbveilederConfig: ServiceClientConfig,
    val poaoTilgang: ServiceClientConfig,
    val amtEnhetsregister: ServiceClientConfig,
    val arenaAdapter: ServiceClientConfig,
    val msGraphConfig: ServiceClientConfig,
    val tasks: TaskConfig,
    val norg2: Norg2Config,
    val slack: SlackConfig,
    val brreg: BrregClientImpl.Config,
)

data class AuthConfig(
    val azure: AuthProvider,
)

data class KafkaConfig(
    val brokerUrl: String? = null,
    val producerId: String,
    val consumerGroupId: String,
    val producers: KafkaProducers,
    val consumers: KafkaConsumers,
)

data class KafkaProducers(
    val tiltaksgjennomforinger: TiltaksgjennomforingKafkaProducer.Config,
    val tiltakstyper: TiltakstypeKafkaProducer.Config,
)

data class KafkaConsumers(
    val amtDeltakerV1: KafkaTopicConsumer.Config,
)

data class AuthProvider(
    val issuer: String,
    val jwksUri: String,
    val audience: String,
    val tokenEndpointUrl: String,
)

data class ServiceClientConfig(
    val url: String,
    val scope: String,
)

data class TaskConfig(
    val synchronizeNorgEnheter: SynchronizeNorgEnheter.Config,
    val synchronizeEnheterFraSanityTilApi: SynchronizeTiltaksgjennomforingEnheter.Config,
    val synchronizeTilgjengelighetsstatuser: SynchronizeTilgjengelighetsstatuserToSanity.Config,
    val synchronizeNavAnsatte: SynchronizeNavAnsatte.Config,
)

data class Norg2Config(
    val baseUrl: String,
)

data class SlackConfig(
    val token: String,
    val channel: String,
    val enable: Boolean,
)
