package no.nav.mulighetsrommet.api.plugins

import com.github.kagkarlsson.scheduler.Scheduler
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.RSAKey
import io.ktor.server.application.*
import no.nav.common.kafka.producer.util.KafkaProducerClientBuilder
import no.nav.common.kafka.util.KafkaPropertiesBuilder
import no.nav.common.kafka.util.KafkaPropertiesPreset
import no.nav.common.token_client.builder.AzureAdTokenClientBuilder
import no.nav.common.token_client.client.MachineToMachineTokenClient
import no.nav.common.token_client.client.OnBehalfOfTokenClient
import no.nav.mulighetsrommet.api.AppConfig
import no.nav.mulighetsrommet.api.KafkaConfig
import no.nav.mulighetsrommet.api.SlackConfig
import no.nav.mulighetsrommet.api.TaskConfig
import no.nav.mulighetsrommet.api.clients.arenaadapter.ArenaAdapterClient
import no.nav.mulighetsrommet.api.clients.arenaadapter.ArenaAdapterClientImpl
import no.nav.mulighetsrommet.api.clients.dialog.VeilarbdialogClient
import no.nav.mulighetsrommet.api.clients.dialog.VeilarbdialogClientImpl
import no.nav.mulighetsrommet.api.clients.enhetsregister.AmtEnhetsregisterClient
import no.nav.mulighetsrommet.api.clients.enhetsregister.AmtEnhetsregisterClientImpl
import no.nav.mulighetsrommet.api.clients.msgraph.MicrosoftGraphClient
import no.nav.mulighetsrommet.api.clients.msgraph.MicrosoftGraphClientImpl
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Client
import no.nav.mulighetsrommet.api.clients.norg2.Norg2ClientImpl
import no.nav.mulighetsrommet.api.clients.oppfolging.VeilarboppfolgingClient
import no.nav.mulighetsrommet.api.clients.oppfolging.VeilarboppfolgingClientImpl
import no.nav.mulighetsrommet.api.clients.person.VeilarbpersonClient
import no.nav.mulighetsrommet.api.clients.person.VeilarbpersonClientImpl
import no.nav.mulighetsrommet.api.clients.vedtak.VeilarbvedtaksstotteClient
import no.nav.mulighetsrommet.api.clients.vedtak.VeilarbvedtaksstotteClientImpl
import no.nav.mulighetsrommet.api.clients.veileder.VeilarbveilederClient
import no.nav.mulighetsrommet.api.clients.veileder.VeilarbveilederClientImpl
import no.nav.mulighetsrommet.api.producers.TiltaksgjennomforingKafkaProducer
import no.nav.mulighetsrommet.api.producers.TiltakstypeKafkaProducer
import no.nav.mulighetsrommet.api.repositories.*
import no.nav.mulighetsrommet.api.services.*
import no.nav.mulighetsrommet.api.tasks.SynchronizeNorgEnheter
import no.nav.mulighetsrommet.api.tasks.SynchronizeTiltaksgjennomforingsstatuserToKafka
import no.nav.mulighetsrommet.api.tasks.SynchronizeTiltakstypestatuserToKafka
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.FlywayDatabaseAdapter
import no.nav.mulighetsrommet.env.NaisEnv
import no.nav.mulighetsrommet.kafka.KafkaConsumerOrchestrator
import no.nav.mulighetsrommet.kafka.amt.AmtDeltakerV1TopicConsumer
import no.nav.mulighetsrommet.ktor.plugins.Metrikker
import no.nav.mulighetsrommet.slack_notifier.SlackNotifier
import no.nav.mulighetsrommet.slack_notifier.SlackNotifierImpl
import no.nav.poao_tilgang.client.PoaoTilgangClient
import no.nav.poao_tilgang.client.PoaoTilgangHttpClient
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.SLF4JLogger
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey

fun Application.configureDependencyInjection(appConfig: AppConfig) {
    install(Koin) {
        SLF4JLogger()

        modules(
            db(appConfig.database),
            kafka(appConfig.kafka),
            repositories(),
            services(appConfig),
            tasks(appConfig.tasks),
            slack(appConfig.slack)
        )
    }
}

fun slack(slack: SlackConfig): Module {
    return module(createdAtStart = true) {
        single<SlackNotifier> {
            SlackNotifierImpl(slack.token, slack.channel, slack.enable)
        }
    }
}

private fun db(config: FlywayDatabaseAdapter.Config): Module {
    return module(createdAtStart = true) {
        single<Database> {
            FlywayDatabaseAdapter(config, get())
        }
    }
}

private fun kafka(config: KafkaConfig) = module {
    val producerProperties = when (NaisEnv.current()) {
        NaisEnv.Local -> KafkaPropertiesBuilder.producerBuilder()
            .withBaseProperties()
            .withProducerId(config.producerId)
            .withBrokerUrl(config.brokerUrl)
            .withSerializers(StringSerializer::class.java, StringSerializer::class.java)
            .build()

        else -> KafkaPropertiesPreset.aivenDefaultProducerProperties(config.producerId)
    }

    val producerClient = KafkaProducerClientBuilder.builder<String, String?>()
        .withProperties(producerProperties)
        .withMetrics(Metrikker.appMicrometerRegistry)
        .build()

    single { TiltaksgjennomforingKafkaProducer(producerClient, config.producers.tiltaksgjennomforinger) }
    single { TiltakstypeKafkaProducer(producerClient, config.producers.tiltakstyper) }

    val properties = when (NaisEnv.current()) {
        NaisEnv.Local -> KafkaPropertiesBuilder.consumerBuilder()
            .withBaseProperties()
            .withConsumerGroupId(config.consumerGroupId)
            .withBrokerUrl(config.brokerUrl)
            .withDeserializers(ByteArrayDeserializer::class.java, ByteArrayDeserializer::class.java)
            .build()

        else -> KafkaPropertiesPreset.aivenDefaultConsumerProperties(config.consumerGroupId)
    }

    single {
        val consumers = listOf(
            AmtDeltakerV1TopicConsumer(config = config.consumers.amtDeltakerV1, deltakere = get())
        )
        KafkaConsumerOrchestrator(
            consumerPreset = properties,
            db = get(),
            consumers = consumers,
        )
    }
}

private fun repositories() = module {
    single { AvtaleRepository(get()) }
    single { TiltaksgjennomforingRepository(get()) }
    single { TiltakstypeRepository(get()) }
    single { TiltakshistorikkRepository(get()) }
    single { AnsattTiltaksgjennomforingRepository(get()) }
    single { EnhetRepository(get()) }
    single { DeltakerRepository(get()) }
}

private fun services(appConfig: AppConfig) = module {
    val m2mTokenProvider = createM2mTokenClient(appConfig)
    val oboTokenProvider = createOboTokenClient(appConfig)

    single<AmtEnhetsregisterClient> {
        AmtEnhetsregisterClientImpl(
            baseUrl = appConfig.amtEnhetsregister.url,
            tokenProvider = {
                m2mTokenProvider.createMachineToMachineToken(appConfig.amtEnhetsregister.scope)
            }
        )
    }
    single<VeilarboppfolgingClient> {
        VeilarboppfolgingClientImpl(
            baseUrl = appConfig.veilarboppfolgingConfig.url,
            tokenProvider = { token ->
                oboTokenProvider.exchangeOnBehalfOfToken(appConfig.veilarboppfolgingConfig.scope, token)
            }
        )
    }
    single<VeilarbvedtaksstotteClient> {
        VeilarbvedtaksstotteClientImpl(
            baseUrl = appConfig.veilarbvedtaksstotteConfig.url,
            tokenProvider = { token ->
                oboTokenProvider.exchangeOnBehalfOfToken(appConfig.veilarbvedtaksstotteConfig.scope, token)
            }
        )
    }
    single<VeilarbpersonClient> {
        VeilarbpersonClientImpl(
            baseUrl = appConfig.veilarbpersonConfig.url,
            tokenProvider = { token ->
                oboTokenProvider.exchangeOnBehalfOfToken(appConfig.veilarbpersonConfig.scope, token)
            }
        )
    }
    single<VeilarbdialogClient> {
        VeilarbdialogClientImpl(
            baseUrl = appConfig.veilarbdialogConfig.url,
            tokenProvider = { token ->
                oboTokenProvider.exchangeOnBehalfOfToken(appConfig.veilarbdialogConfig.scope, token)
            }
        )
    }
    single<VeilarbveilederClient> {
        VeilarbveilederClientImpl(
            baseUrl = appConfig.veilarbveilederConfig.url,
            tokenProvider = { token ->
                oboTokenProvider.exchangeOnBehalfOfToken(appConfig.veilarbveilederConfig.scope, token)
            }
        )
    }
    single<PoaoTilgangClient> {
        PoaoTilgangHttpClient(
            baseUrl = appConfig.poaoTilgang.url,
            tokenProvider = { m2mTokenProvider.createMachineToMachineToken(appConfig.poaoTilgang.scope) }
        )
    }
    single<MicrosoftGraphClient> {
        MicrosoftGraphClientImpl(
            baseUrl = appConfig.msGraphConfig.url,
            tokenProvider = { token ->
                oboTokenProvider.exchangeOnBehalfOfToken(appConfig.msGraphConfig.scope, token)
            }
        )
    }
    single<ArenaAdapterClient> {
        ArenaAdapterClientImpl(
            baseUrl = appConfig.arenaAdapter.url,
            machineToMachineTokenClient = { m2mTokenProvider.createMachineToMachineToken(appConfig.arenaAdapter.scope) }
        )
    }
    single<Norg2Client> {
        Norg2ClientImpl(
            baseUrl = appConfig.norg2.baseUrl
        )
    }
    single { ArenaAdapterService(get(), get(), get(), get(), get(), get(), get()) }
    single { AvtaleService(get(), get(), get(), get()) }
    single { TiltakshistorikkService(get(), get()) }
    single { SanityService(appConfig.sanity, get()) }
    single { ArrangorService(get()) }
    single { BrukerService(get(), get(), get()) }
    single { DialogService(get()) }
    single { AnsattService(get(), get(), get()) }
    single { PoaoTilgangService(get()) }
    single { DelMedBrukerService(get()) }
    single { MicrosoftGraphService(get()) }
    single { TiltaksgjennomforingService(get(), get()) }
    single { TiltakstypeService(get(), get(), get(), get()) }
    single { Norg2Service(get(), get()) }
    single { KafkaSyncService(get(), get(), get(), get()) }
    single { NavEnhetService(get()) }
}

private fun tasks(config: TaskConfig) = module {
    single {
        val synchronizeTiltaksgjennomforingsstatuserToKafka =
            SynchronizeTiltaksgjennomforingsstatuserToKafka(get(), get())
        val synchronizeTiltakstypestatuserToKafka = SynchronizeTiltakstypestatuserToKafka(get(), get())
        val synchronizeNorgEnheterTask = SynchronizeNorgEnheter(config.synchronizeNorgEnheter, get(), get())

        val db: Database by inject()

        Scheduler
            .create(db.getDatasource())
            .startTasks(
                synchronizeNorgEnheterTask.task,
                synchronizeTiltaksgjennomforingsstatuserToKafka.task,
                synchronizeTiltakstypestatuserToKafka.task
            )
            .registerShutdownHook()
            .build()
    }
}

private fun createOboTokenClient(config: AppConfig): OnBehalfOfTokenClient {
    return when (NaisEnv.current()) {
        NaisEnv.Local -> AzureAdTokenClientBuilder.builder()
            .withClientId(config.auth.azure.audience)
            .withPrivateJwk(createMockRSAKey("azure").toJSONString())
            .withTokenEndpointUrl(config.auth.azure.tokenEndpointUrl)
            .buildOnBehalfOfTokenClient()

        else -> AzureAdTokenClientBuilder.builder().withNaisDefaults().buildOnBehalfOfTokenClient()
    }
}

private fun createM2mTokenClient(config: AppConfig): MachineToMachineTokenClient {
    return when (NaisEnv.current()) {
        NaisEnv.Local -> AzureAdTokenClientBuilder.builder()
            .withClientId(config.auth.azure.audience)
            .withPrivateJwk(createMockRSAKey("azure").toJSONString())
            .withTokenEndpointUrl(config.auth.azure.tokenEndpointUrl)
            .buildMachineToMachineTokenClient()

        else -> AzureAdTokenClientBuilder.builder().withNaisDefaults().buildMachineToMachineTokenClient()
    }
}

private fun createMockRSAKey(keyID: String): RSAKey = KeyPairGenerator
    .getInstance("RSA").let {
        it.initialize(2048)
        it.generateKeyPair()
    }.let {
        RSAKey.Builder(it.public as RSAPublicKey)
            .privateKey(it.private as RSAPrivateKey)
            .keyUse(KeyUse.SIGNATURE)
            .keyID(keyID)
            .build()
    }
