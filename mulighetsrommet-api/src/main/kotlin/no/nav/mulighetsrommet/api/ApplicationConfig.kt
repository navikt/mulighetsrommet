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
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsattRolle
import no.nav.mulighetsrommet.api.navansatt.task.SynchronizeNavAnsatte
import no.nav.mulighetsrommet.api.navenhet.task.SynchronizeNorgEnheter
import no.nav.mulighetsrommet.api.tasks.GenerateValidationReport
import no.nav.mulighetsrommet.api.tasks.NotifyFailedKafkaEvents
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
    val roles: Set<AdGruppeNavAnsattRolleMapping>,
)

data class AdGruppeNavAnsattRolleMapping(
    val adGruppeId: UUID,
    val rolle: NavAnsattRolle,
)

data class KafkaConfig(
    val producerProperties: Properties,
    val consumerPreset: Properties,
    val clients: KafkaClients,
)

data class KafkaClients(
    val dvhGjennomforing: DatavarehusTiltakV1KafkaProducer.Config = DatavarehusTiltakV1KafkaProducer.Config(
        consumerId = "dvh-gjennomforing-consumer",
        consumerGroupId = "mulighetsrommet-api.datavarehus-gjennomforing.v1",
        consumerTopic = "team-mulighetsrommet.siste-tiltaksgjennomforinger-v1",
        producerTopic = "team-mulighetsrommet.datavarehus-tiltak-v1",
    ),
    val okonomiBestillingTopic: String = "team-mulighetsrommet.tiltaksokonomi.bestillinger-v1",
    val tiltakstyper: SisteTiltakstyperV2KafkaProducer.Config = SisteTiltakstyperV2KafkaProducer.Config(
        topic = "team-mulighetsrommet.siste-tiltakstyper-v3",
    ),
    val gjennomforinger: SisteTiltaksgjennomforingerV1KafkaProducer.Config = SisteTiltaksgjennomforingerV1KafkaProducer.Config(
        topic = "team-mulighetsrommet.siste-tiltaksgjennomforinger-v1",
    ),
    val arenaMigreringTiltaksgjennomforinger: ArenaMigreringTiltaksgjennomforingerV1KafkaProducer.Config = ArenaMigreringTiltaksgjennomforingerV1KafkaProducer.Config(
        topic = "team-mulighetsrommet.arena-migrering-tiltaksgjennomforinger-v1",
    ),
    val gjennomforingerV1: KafkaTopicConsumer.Config = KafkaTopicConsumer.Config(
        id = "siste-tiltaksgjennomforinger",
        topic = "team-mulighetsrommet.siste-tiltaksgjennomforinger-v1",
    ),
    val oppdaterUtbetalingForGjennomforing: KafkaTopicConsumer.Config = KafkaTopicConsumer.Config(
        id = "oppdater-utbetaling-for-gjennomforing",
        topic = "team-mulighetsrommet.siste-tiltaksgjennomforinger-v1",
        consumerGroupId = "mulighetsrommet-api.oppdater-utbetaling-for-gjennomforing.v1",
    ),
    val replicateBestillingStatus: KafkaTopicConsumer.Config = KafkaTopicConsumer.Config(
        id = "replicate-bestilling-status",
        topic = "team-mulighetsrommet.tiltaksokonomi.bestilling-status-v1",
        consumerGroupId = "mulighetsrommet-api.bestilling-status.v1",
    ),
    val replicateFakturaStatus: KafkaTopicConsumer.Config = KafkaTopicConsumer.Config(
        id = "replicate-faktura-status",
        topic = "team-mulighetsrommet.tiltaksokonomi.faktura-status-v1",
        consumerGroupId = "mulighetsrommet-api.faktura-status.v1",
    ),
    val amtDeltakerV1: KafkaTopicConsumer.Config = KafkaTopicConsumer.Config(
        id = "amt-deltaker",
        topic = "amt.deltaker-v1",
        consumerGroupId = "mulighetsrommet-api.deltaker.v1",
    ),
    val amtVirksomheterV1: KafkaTopicConsumer.Config = KafkaTopicConsumer.Config(
        id = "amt-virksomheter",
        topic = "amt.virksomheter-v1",
    ),
    val amtArrangorMeldingV1: KafkaTopicConsumer.Config = KafkaTopicConsumer.Config(
        id = "amt-arrangor-melding",
        topic = "amt.arrangor-melding-v1",
    ),
    val amtKoordinatorMeldingV1: KafkaTopicConsumer.Config = KafkaTopicConsumer.Config(
        id = "amt-tiltakskoordinators-deltakerliste",
        topic = "amt.tiltakskoordinators-deltakerliste-v1",
    ),
)

data class AuthProvider(
    val issuer: String,
    val jwksUri: String,
    val audience: String,
    val tokenEndpointUrl: String,
    val privateJwk: String,
)

data class AuthenticatedHttpClientConfig(
    val engine: HttpClientEngine? = null,
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
