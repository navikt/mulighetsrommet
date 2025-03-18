package no.nav.mulighetsrommet.api

import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import no.nav.mulighetsrommet.api.avtale.task.NotifySluttdatoForAvtalerNarmerSeg
import no.nav.mulighetsrommet.api.clients.sanity.SanityClient
import no.nav.mulighetsrommet.api.datavarehus.kafka.DatavarehusTiltakV1KafkaProducer
import no.nav.mulighetsrommet.api.gjennomforing.kafka.ArenaMigreringTiltaksgjennomforingerV1KafkaProducer
import no.nav.mulighetsrommet.api.gjennomforing.kafka.SisteTiltaksgjennomforingerV1KafkaProducer
import no.nav.mulighetsrommet.api.gjennomforing.task.NotifySluttdatoForGjennomforingerNarmerSeg
import no.nav.mulighetsrommet.api.gjennomforing.task.UpdateApentForPamelding
import no.nav.mulighetsrommet.api.navansatt.db.NavAnsattRolle
import no.nav.mulighetsrommet.api.navansatt.task.SynchronizeNavAnsatte
import no.nav.mulighetsrommet.api.navenhet.task.SynchronizeNorgEnheter
import no.nav.mulighetsrommet.api.tasks.GenerateValidationReport
import no.nav.mulighetsrommet.api.tasks.NotifyFailedKafkaEvents
import no.nav.mulighetsrommet.api.tilsagn.OkonomiBestillingService
import no.nav.mulighetsrommet.api.tiltakstype.kafka.SisteTiltakstyperV2KafkaProducer
import no.nav.mulighetsrommet.api.utbetaling.task.GenerateUtbetaling
import no.nav.mulighetsrommet.database.DatabaseConfig
import no.nav.mulighetsrommet.database.FlywayMigrationManager
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.ktor.ServerConfig
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.unleash.UnleashService
import no.nav.mulighetsrommet.utdanning.task.SynchronizeUtdanninger
import java.time.LocalDate
import java.util.*

data class AppConfig(
    val server: ServerConfig = ServerConfig(),
    val database: DatabaseConfig,
    val flyway: FlywayMigrationManager.MigrationConfig,
    val kafka: KafkaConfig,
    val auth: AuthConfig,
    val sanity: SanityClient.Config,
    val veilarboppfolgingConfig: AuthenticatedHttpClientConfig,
    val veilarbvedtaksstotteConfig: AuthenticatedHttpClientConfig,
    val veilarbdialogConfig: AuthenticatedHttpClientConfig,
    val amtDeltakerConfig: AuthenticatedHttpClientConfig,
    val poaoTilgang: AuthenticatedHttpClientConfig,
    val arenaAdapter: AuthenticatedHttpClientConfig,
    val tiltakshistorikk: AuthenticatedHttpClientConfig,
    val pdfgen: HttpClientConfig,
    val msGraphConfig: AuthenticatedHttpClientConfig,
    val isoppfolgingstilfelleConfig: AuthenticatedHttpClientConfig,
    val norg2: HttpClientConfig,
    val tasks: TaskConfig,
    val slack: SlackConfig,
    val pamOntologi: AuthenticatedHttpClientConfig,
    val unleash: UnleashService.Config,
    val axsys: AuthenticatedHttpClientConfig,
    val pdl: AuthenticatedHttpClientConfig,
    val engine: HttpClientEngine = CIO.create(),
    val utdanning: HttpClientConfig,
    val altinn: AuthenticatedHttpClientConfig,
    val dokark: AuthenticatedHttpClientConfig,
    val okonomi: OkonomiConfig,
    val kontoregisterOrganisasjon: AuthenticatedHttpClientConfig,
)

data class OkonomiConfig(
    val minimumTilsagnPeriodeStart: Map<Tiltakskode, LocalDate>,
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
    val producerProperties: Properties,
    val consumerPreset: Properties,
    val producers: KafkaProducers,
    val consumers: KafkaConsumers,
    val clients: KafkaClients,
)

data class KafkaClients(
    val dvhGjennomforing: DatavarehusTiltakV1KafkaProducer.Config,
    val okonomiBestilling: OkonomiBestillingService.Config,
)

data class KafkaProducers(
    val gjennomforinger: SisteTiltaksgjennomforingerV1KafkaProducer.Config,
    val tiltakstyper: SisteTiltakstyperV2KafkaProducer.Config,
    val arenaMigreringTiltaksgjennomforinger: ArenaMigreringTiltaksgjennomforingerV1KafkaProducer.Config,
)

data class KafkaConsumers(
    val gjennomforingerV1: KafkaTopicConsumer.Config,
    val amtDeltakerV1: KafkaTopicConsumer.Config,
    val amtVirksomheterV1: KafkaTopicConsumer.Config,
    val amtArrangorMeldingV1: KafkaTopicConsumer.Config,
    val amtKoordinatorMeldingV1: KafkaTopicConsumer.Config,
)

data class AuthProvider(
    val issuer: String,
    val jwksUri: String,
    val audience: String,
    val tokenEndpointUrl: String,
    val privateJwk: String,
)

data class AuthenticatedHttpClientConfig(
    val url: String,
    val scope: String,
)

data class HttpClientConfig(
    val url: String,
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
    val generateUtbetaling: GenerateUtbetaling.Config,
)

data class SlackConfig(
    val token: String,
    val channel: String,
    val enable: Boolean,
)
