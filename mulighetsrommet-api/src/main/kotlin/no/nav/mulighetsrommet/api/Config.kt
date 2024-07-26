package no.nav.mulighetsrommet.api

import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import no.nav.mulighetsrommet.api.clients.brreg.BrregClient
import no.nav.mulighetsrommet.api.clients.sanity.SanityClient
import no.nav.mulighetsrommet.api.clients.utdanning.UtdanningClient
import no.nav.mulighetsrommet.api.domain.dbo.NavAnsattRolle
import no.nav.mulighetsrommet.api.tasks.*
import no.nav.mulighetsrommet.database.DatabaseConfig
import no.nav.mulighetsrommet.database.FlywayMigrationManager
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.kafka.producers.ArenaMigreringTiltaksgjennomforingKafkaProducer
import no.nav.mulighetsrommet.kafka.producers.TiltaksgjennomforingKafkaProducer
import no.nav.mulighetsrommet.kafka.producers.TiltakstypeKafkaProducer
import no.nav.mulighetsrommet.ktor.ServerConfig
import no.nav.mulighetsrommet.unleash.UnleashService
import java.util.*

data class Config(
    val server: ServerConfig,
    val app: AppConfig,
)

data class AppConfig(
    val database: DatabaseConfig,
    val flyway: FlywayMigrationManager.MigrationConfig,
    val migrerteTiltak: List<Tiltakskode>,
    val pameldingIModia: List<Tiltakskode>,
    val kafka: KafkaConfig,
    val auth: AuthConfig,
    val sanity: SanityClient.Config,
    val veilarboppfolgingConfig: ServiceClientConfig,
    val veilarbvedtaksstotteConfig: ServiceClientConfig,
    val veilarbdialogConfig: ServiceClientConfig,
    val veilarbveilederConfig: ServiceClientConfig,
    val amtDeltakerConfig: ServiceClientConfig,
    val poaoTilgang: ServiceClientConfig,
    val arenaAdapter: ServiceClientConfig,
    val tiltakshistorikk: ServiceClientConfig,
    val msGraphConfig: ServiceClientConfig,
    val tasks: TaskConfig,
    val norg2: Norg2Config,
    val slack: SlackConfig,
    val brreg: BrregClient.Config,
    val pamOntologi: ServiceClientConfig,
    val unleash: UnleashService.Config,
    val axsys: ServiceClientConfig,
    val pdl: ServiceClientConfig,
    val engine: HttpClientEngine = CIO.create(),
    val utdanning: UtdanningClient.Config,
)

data class AuthConfig(
    val azure: AuthProvider,
    val roles: List<AdGruppeNavAnsattRolleMapping>,
)

data class AdGruppeNavAnsattRolleMapping(
    val adGruppeId: UUID,
    val rolle: NavAnsattRolle,
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
    val arenaMigreringTiltaksgjennomforinger: ArenaMigreringTiltaksgjennomforingKafkaProducer.Config,
)

data class KafkaConsumers(
    val tiltaksgjennomforingerV1: KafkaTopicConsumer.Config,
    val amtDeltakerV1: KafkaTopicConsumer.Config,
    val amtVirksomheterV1: KafkaTopicConsumer.Config,
    val ptoSisteOppfolgingsperiodeV1: KafkaTopicConsumer.Config,
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
    val synchronizeNavAnsatte: SynchronizeNavAnsatte.Config,
    val synchronizeUtdanninger: SynchronizeUtdanninger.Config = SynchronizeUtdanninger.Config(cronPattern = ""),
    val notifySluttdatoForGjennomforingerNarmerSeg: NotifySluttdatoForGjennomforingerNarmerSeg.Config,
    val notifySluttdatoForAvtalerNarmerSeg: NotifySluttdatoForAvtalerNarmerSeg.Config,
    val notifyFailedKafkaEvents: NotifyFailedKafkaEvents.Config,
    val generateValidationReport: GenerateValidationReport.Config = GenerateValidationReport.Config(),
    val updateApentForInnsok: UpdateApentForInnsok.Config = UpdateApentForInnsok.Config(),
)

data class Norg2Config(
    val baseUrl: String,
)

data class SlackConfig(
    val token: String,
    val channel: String,
    val enable: Boolean,
)
