package no.nav.mulighetsrommet.api

import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import no.nav.mulighetsrommet.api.avtale.task.NotifySluttdatoForAvtalerNarmerSeg
import no.nav.mulighetsrommet.api.clients.sanity.SanityClient
import no.nav.mulighetsrommet.api.gjennomforing.task.NotifySluttdatoForGjennomforingerNarmerSeg
import no.nav.mulighetsrommet.api.gjennomforing.task.UpdateApentForPamelding
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.navansatt.service.NavAnsattSyncService
import no.nav.mulighetsrommet.api.navansatt.task.SynchronizeNavAnsatte
import no.nav.mulighetsrommet.api.navenhet.task.SynchronizeNorgEnheter
import no.nav.mulighetsrommet.api.tasks.GenerateValidationReport
import no.nav.mulighetsrommet.api.utbetaling.task.BeregnUtbetaling
import no.nav.mulighetsrommet.api.utbetaling.task.GenerateUtbetaling
import no.nav.mulighetsrommet.database.DatabaseConfig
import no.nav.mulighetsrommet.database.FlywayMigrationManager
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.ktor.ServerConfig
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.tokenprovider.TexasClient
import no.nav.mulighetsrommet.unleash.UnleashService
import no.nav.mulighetsrommet.utdanning.task.SynchronizeUtdanninger
import java.time.LocalDate
import java.util.*

data class AppConfig(
    val engine: HttpClientEngine = CIO.create(),
    val server: ServerConfig = ServerConfig(),
    val database: DatabaseConfig,
    val flyway: FlywayMigrationManager.MigrationConfig,
    val kafka: KafkaConfig,
    val auth: AuthConfig,
    val sanity: SanityClient.Config,
    val navAnsattSync: NavAnsattSyncService.Config,
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
    val pdl: AuthenticatedHttpClientConfig,
    val utdanning: HttpClientConfig,
    val altinn: AuthenticatedHttpClientConfig,
    val dokark: AuthenticatedHttpClientConfig,
    val okonomi: OkonomiConfig,
    val kontoregisterOrganisasjon: AuthenticatedHttpClientConfig,
    val clamav: HttpClientConfig,
)

data class OkonomiConfig(
    val minimumTilsagnPeriodeStart: Map<Tiltakskode, LocalDate>,
)

data class AuthConfig(
    val azure: AuthProvider,
    val tokenx: AuthProvider,
    val maskinporten: AuthProvider,
    val roles: Set<EntraGroupNavAnsattRolleMapping>,
    val texas: TexasClient.Config,
)

data class EntraGroupNavAnsattRolleMapping(
    val entraGroupId: UUID,
    val rolle: Rolle,
    val enheter: Set<NavEnhetNummer> = setOf(),
    val kommentar: String? = null,
)

data class KafkaConfig(
    val producerProperties: Properties,
    val topics: KafkaTopics = KafkaTopics(),
    val clients: KafkaClients,
)

data class KafkaTopics(
    val okonomiBestillingTopic: String = "team-mulighetsrommet.tiltaksokonomi.bestillinger-v1",
    val sisteTiltaksgjennomforingerTopic: String = "team-mulighetsrommet.siste-tiltaksgjennomforinger-v1",
    val sisteTiltakstyperTopic: String = "team-mulighetsrommet.siste-tiltakstyper-v3",
    val arenaMigreringGjennomforingTopic: String = "team-mulighetsrommet.arena-migrering-tiltaksgjennomforinger-v1",
    val datavaehusTiltakTopic: String = "team-mulighetsrommet.datavarehus-tiltak-v1",
)

class KafkaClients(
    getConsumerProperties: (consumerGroupId: String) -> Properties,
    block: (KafkaClients.() -> Unit)? = null,
) {
    var arenaMigreringGjennomforingerConsumer: KafkaTopicConsumer.Config = KafkaTopicConsumer.Config(
        id = "arena-migrering-gjennomforinger",
        topic = "team-mulighetsrommet.siste-tiltaksgjennomforinger-v1",
        consumerProperties = getConsumerProperties("mulighetsrommet-api.arena-migrering-gjennomforing.v1"),
    )
    var datavarehusGjennomforingerConsumer: KafkaTopicConsumer.Config = KafkaTopicConsumer.Config(
        id = "dvh-gjennomforing-consumer",
        topic = "team-mulighetsrommet.siste-tiltaksgjennomforinger-v1",
        consumerProperties = getConsumerProperties("mulighetsrommet-api.datavarehus-gjennomforing.v1"),
    )
    var oppdaterUtbetalingForGjennomforing: KafkaTopicConsumer.Config = KafkaTopicConsumer.Config(
        id = "oppdater-utbetaling-for-gjennomforing",
        topic = "team-mulighetsrommet.siste-tiltaksgjennomforinger-v1",
        consumerProperties = getConsumerProperties("mulighetsrommet-api.oppdater-utbetaling-for-gjennomforing.v1"),
    )
    var replicateBestillingStatus: KafkaTopicConsumer.Config = KafkaTopicConsumer.Config(
        id = "replicate-bestilling-status",
        topic = "team-mulighetsrommet.tiltaksokonomi.bestilling-status-v1",
        consumerProperties = getConsumerProperties("mulighetsrommet-api.bestilling-status.v1"),
    )
    var replicateFakturaStatus: KafkaTopicConsumer.Config = KafkaTopicConsumer.Config(
        id = "replicate-faktura-status",
        topic = "team-mulighetsrommet.tiltaksokonomi.faktura-status-v1",
        consumerProperties = getConsumerProperties("mulighetsrommet-api.faktura-status.v2"),
    )
    var amtDeltakerV1: KafkaTopicConsumer.Config = KafkaTopicConsumer.Config(
        id = "amt-deltaker",
        topic = "amt.deltaker-v1",
        consumerProperties = getConsumerProperties("mulighetsrommet-api.deltaker.v2"),
    )
    var amtVirksomheterV1: KafkaTopicConsumer.Config = KafkaTopicConsumer.Config(
        id = "amt-virksomheter",
        topic = "amt.virksomheter-v1",
        consumerProperties = getConsumerProperties("mulighetsrommet-api.amt-virksomheter.v1"),
    )
    var amtArrangorMeldingV1: KafkaTopicConsumer.Config = KafkaTopicConsumer.Config(
        id = "amt-arrangor-melding",
        topic = "amt.arrangor-melding-v1",
        consumerProperties = getConsumerProperties("mulighetsrommet-api.amt-arrangor-melding.v1"),
    )
    var amtKoordinatorMeldingV1: KafkaTopicConsumer.Config = KafkaTopicConsumer.Config(
        id = "amt-tiltakskoordinators-deltakerliste",
        topic = "amt.tiltakskoordinators-deltakerliste-v1",
        consumerProperties = getConsumerProperties("mulighetsrommet-api.tiltakskoordinators-deltakerliste.v1"),
    )

    init {
        block?.invoke(this)
    }
}

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
    val generateValidationReport: GenerateValidationReport.Config = GenerateValidationReport.Config(),
    val updateApentForPamelding: UpdateApentForPamelding.Config = UpdateApentForPamelding.Config(),
    val generateUtbetaling: GenerateUtbetaling.Config,
    val beregnUtbetaling: BeregnUtbetaling.Config = BeregnUtbetaling.Config(),
)

data class SlackConfig(
    val token: String,
    val channel: String,
    val enable: Boolean,
)
