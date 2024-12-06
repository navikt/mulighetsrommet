package no.nav.mulighetsrommet.api

import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import no.nav.mulighetsrommet.altinn.AltinnClient
import no.nav.mulighetsrommet.api.avtale.task.NotifySluttdatoForAvtalerNarmerSeg
import no.nav.mulighetsrommet.api.clients.brreg.BrregClient
import no.nav.mulighetsrommet.api.clients.sanity.SanityClient
import no.nav.mulighetsrommet.api.gjennomforing.kafka.ArenaMigreringTiltaksgjennomforingerV1KafkaProducer
import no.nav.mulighetsrommet.api.gjennomforing.kafka.SisteTiltaksgjennomforingerV1KafkaProducer
import no.nav.mulighetsrommet.api.gjennomforing.task.NotifySluttdatoForGjennomforingerNarmerSeg
import no.nav.mulighetsrommet.api.gjennomforing.task.UpdateApentForPamelding
import no.nav.mulighetsrommet.api.navansatt.db.NavAnsattRolle
import no.nav.mulighetsrommet.api.navansatt.task.SynchronizeNavAnsatte
import no.nav.mulighetsrommet.api.navenhet.task.SynchronizeNorgEnheter
import no.nav.mulighetsrommet.api.refusjon.task.GenerateRefusjonskrav
import no.nav.mulighetsrommet.api.tasks.GenerateValidationReport
import no.nav.mulighetsrommet.api.tasks.NotifyFailedKafkaEvents
import no.nav.mulighetsrommet.api.tiltakstype.kafka.SisteTiltakstyperV2KafkaProducer
import no.nav.mulighetsrommet.database.DatabaseConfig
import no.nav.mulighetsrommet.database.FlywayMigrationManager
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.ktor.ServerConfig
import no.nav.mulighetsrommet.unleash.UnleashService
import no.nav.mulighetsrommet.utdanning.client.UtdanningClient
import no.nav.mulighetsrommet.utdanning.task.SynchronizeUtdanninger
import java.util.*

data class Config(
    val server: ServerConfig,
    val app: AppConfig,
)

data class AppConfig(
    val database: DatabaseConfig,
    val flyway: FlywayMigrationManager.MigrationConfig,
    val kafka: KafkaConfig,
    val auth: AuthConfig,
    val sanity: SanityClient.Config,
    val veilarboppfolgingConfig: ServiceClientConfig,
    val veilarbvedtaksstotteConfig: ServiceClientConfig,
    val veilarbdialogConfig: ServiceClientConfig,
    val amtDeltakerConfig: ServiceClientConfig,
    val poaoTilgang: ServiceClientConfig,
    val arenaAdapter: ServiceClientConfig,
    val tiltakshistorikk: ServiceClientConfig,
    val msGraphConfig: ServiceClientConfig,
    val isoppfolgingstilfelleConfig: ServiceClientConfig,
    val norg2: Norg2Config,
    val tasks: TaskConfig,
    val slack: SlackConfig,
    val brreg: BrregClient.Config,
    val pamOntologi: ServiceClientConfig,
    val unleash: UnleashService.Config,
    val axsys: ServiceClientConfig,
    val pdl: ServiceClientConfig,
    val engine: HttpClientEngine = CIO.create(),
    val utdanning: UtdanningClient.Config,
    val altinn: AltinnClient.Config,
    val dokark: ServiceClientConfig,
)

data class AuthConfig(
    val azure: AuthProvider,
    val tokenx: AuthProvider,
    val maskinporten: AuthProvider,
    val roles: List<AdGruppeNavAnsattRolleMapping>,
)

data class AdGruppeNavAnsattRolleMapping(
    val adGruppeId: UUID,
    val rolle: NavAnsattRolle,
)

data class KafkaConfig(
    val brokerUrl: String? = null,
    val producerId: String,
    val defaultConsumerGroupId: String,
    val producers: KafkaProducers,
    val consumers: KafkaConsumers,
)

data class KafkaProducers(
    val tiltaksgjennomforinger: SisteTiltaksgjennomforingerV1KafkaProducer.Config,
    val tiltakstyper: SisteTiltakstyperV2KafkaProducer.Config,
    val arenaMigreringTiltaksgjennomforinger: ArenaMigreringTiltaksgjennomforingerV1KafkaProducer.Config,
)

data class KafkaConsumers(
    val tiltaksgjennomforingerV1: KafkaTopicConsumer.Config,
    val amtDeltakerV1: KafkaTopicConsumer.Config,
    val amtVirksomheterV1: KafkaTopicConsumer.Config,
    val amtArrangorMeldingV1: KafkaTopicConsumer.Config,
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
    val updateApentForPamelding: UpdateApentForPamelding.Config = UpdateApentForPamelding.Config(),
    val generateRefusjonskrav: GenerateRefusjonskrav.Config,
)

data class Norg2Config(
    val baseUrl: String,
)

data class SlackConfig(
    val token: String,
    val channel: String,
    val enable: Boolean,
)
