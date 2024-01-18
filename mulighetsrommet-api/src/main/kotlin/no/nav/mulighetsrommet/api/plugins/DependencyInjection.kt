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
import no.nav.mulighetsrommet.api.avtaler.AvtaleValidator
import no.nav.mulighetsrommet.api.clients.arenaadapter.ArenaAdapterClient
import no.nav.mulighetsrommet.api.clients.arenaadapter.ArenaAdapterClientImpl
import no.nav.mulighetsrommet.api.clients.brreg.BrregClient
import no.nav.mulighetsrommet.api.clients.brreg.BrregClientImpl
import no.nav.mulighetsrommet.api.clients.dialog.VeilarbdialogClient
import no.nav.mulighetsrommet.api.clients.dialog.VeilarbdialogClientImpl
import no.nav.mulighetsrommet.api.clients.msgraph.MicrosoftGraphClient
import no.nav.mulighetsrommet.api.clients.msgraph.MicrosoftGraphClientImpl
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Client
import no.nav.mulighetsrommet.api.clients.norg2.Norg2ClientImpl
import no.nav.mulighetsrommet.api.clients.oppfolging.VeilarboppfolgingClient
import no.nav.mulighetsrommet.api.clients.oppfolging.VeilarboppfolgingClientImpl
import no.nav.mulighetsrommet.api.clients.person.VeilarbpersonClient
import no.nav.mulighetsrommet.api.clients.person.VeilarbpersonClientImpl
import no.nav.mulighetsrommet.api.clients.sanity.SanityClient
import no.nav.mulighetsrommet.api.clients.vedtak.VeilarbvedtaksstotteClient
import no.nav.mulighetsrommet.api.clients.vedtak.VeilarbvedtaksstotteClientImpl
import no.nav.mulighetsrommet.api.repositories.*
import no.nav.mulighetsrommet.api.services.*
import no.nav.mulighetsrommet.api.tasks.*
import no.nav.mulighetsrommet.api.tiltaksgjennomforinger.TiltaksgjennomforingValidator
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.FlywayDatabaseAdapter
import no.nav.mulighetsrommet.env.NaisEnv
import no.nav.mulighetsrommet.kafka.KafkaConsumerOrchestrator
import no.nav.mulighetsrommet.kafka.KafkaConsumerRepositoryImpl
import no.nav.mulighetsrommet.kafka.consumers.TiltaksgjennomforingTopicConsumer
import no.nav.mulighetsrommet.kafka.consumers.amt.AmtDeltakerV1TopicConsumer
import no.nav.mulighetsrommet.kafka.consumers.amt.AmtVirksomheterV1TopicConsumer
import no.nav.mulighetsrommet.kafka.producers.ArenaMigreringTiltaksgjennomforingKafkaProducer
import no.nav.mulighetsrommet.kafka.producers.TiltaksgjennomforingKafkaProducer
import no.nav.mulighetsrommet.kafka.producers.TiltakstypeKafkaProducer
import no.nav.mulighetsrommet.metrics.Metrikker
import no.nav.mulighetsrommet.notifications.NotificationRepository
import no.nav.mulighetsrommet.notifications.NotificationService
import no.nav.mulighetsrommet.slack.SlackNotifier
import no.nav.mulighetsrommet.slack.SlackNotifierImpl
import no.nav.mulighetsrommet.tasks.DbSchedulerKotlinSerializer
import no.nav.mulighetsrommet.unleash.UnleashService
import no.nav.mulighetsrommet.unleash.strategies.ByEnhetStrategy
import no.nav.mulighetsrommet.unleash.strategies.ByNavidentStrategy
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
            slack(appConfig.slack),
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

    single {
        ArenaMigreringTiltaksgjennomforingKafkaProducer(
            producerClient,
            config.producers.arenaMigreringTiltaksgjennomforinger,
        )
    }
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
            TiltaksgjennomforingTopicConsumer(
                config = config.consumers.tiltaksgjennomforingerV1,
                arenaAdapterClient = get(),
                arenaMigreringTiltaksgjennomforingKafkaProducer = get(),
                tiltaksgjennomforingRepository = get(),
            ),
            AmtDeltakerV1TopicConsumer(config = config.consumers.amtDeltakerV1, deltakere = get()),
            AmtVirksomheterV1TopicConsumer(
                config = config.consumers.amtVirksomheterV1,
                virksomhetRepository = get(),
                brregClient = get(),
            ),
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
    single { NavEnhetRepository(get()) }
    single { DeltakerRepository(get()) }
    single { NotificationRepository(get()) }
    single { NavAnsattRepository(get()) }
    single { VirksomhetRepository(get()) }
    single { KafkaConsumerRepositoryImpl(get()) }
    single { MetrikkRepository(get()) }
    single { UtkastRepository(get()) }
    single { AvtaleNotatRepository(get()) }
    single { TiltaksgjennomforingNotatRepository(get()) }
    single { VeilederJoyrideRepository(get()) }
}

private fun services(appConfig: AppConfig) = module {
    val m2mTokenProvider = createM2mTokenClient(appConfig)
    val oboTokenProvider = createOboTokenClient(appConfig)

    single<VeilarboppfolgingClient> {
        VeilarboppfolgingClientImpl(
            baseUrl = appConfig.veilarboppfolgingConfig.url,
            tokenProvider = { token ->
                oboTokenProvider.exchangeOnBehalfOfToken(appConfig.veilarboppfolgingConfig.scope, token)
            },
        )
    }
    single<VeilarbvedtaksstotteClient> {
        VeilarbvedtaksstotteClientImpl(
            baseUrl = appConfig.veilarbvedtaksstotteConfig.url,
            tokenProvider = { token ->
                oboTokenProvider.exchangeOnBehalfOfToken(appConfig.veilarbvedtaksstotteConfig.scope, token)
            },
        )
    }
    single<VeilarbpersonClient> {
        VeilarbpersonClientImpl(
            baseUrl = appConfig.veilarbpersonConfig.url,
            tokenProvider = { token ->
                oboTokenProvider.exchangeOnBehalfOfToken(appConfig.veilarbpersonConfig.scope, token)
            },
        )
    }
    single<VeilarbdialogClient> {
        VeilarbdialogClientImpl(
            baseUrl = appConfig.veilarbdialogConfig.url,
            tokenProvider = { token ->
                oboTokenProvider.exchangeOnBehalfOfToken(appConfig.veilarbdialogConfig.scope, token)
            },
        )
    }

    single<PoaoTilgangClient> {
        PoaoTilgangHttpClient(
            baseUrl = appConfig.poaoTilgang.url,
            tokenProvider = { m2mTokenProvider.createMachineToMachineToken(appConfig.poaoTilgang.scope) },
        )
    }
    single<MicrosoftGraphClient> {
        MicrosoftGraphClientImpl(
            baseUrl = appConfig.msGraphConfig.url,
            tokenProvider = { token ->
                if (token == null) {
                    m2mTokenProvider.createMachineToMachineToken(appConfig.msGraphConfig.scope)
                } else {
                    oboTokenProvider.exchangeOnBehalfOfToken(appConfig.msGraphConfig.scope, token)
                }
            },
        )
    }
    single<ArenaAdapterClient> {
        ArenaAdapterClientImpl(
            baseUrl = appConfig.arenaAdapter.url,
            machineToMachineTokenClient = { m2mTokenProvider.createMachineToMachineToken(appConfig.arenaAdapter.scope) },
        )
    }
    single<Norg2Client> {
        Norg2ClientImpl(
            baseUrl = appConfig.norg2.baseUrl,
        )
    }
    single {
        SanityClient(
            config = appConfig.sanity,
        )
    }
    single<BrregClient> {
        BrregClientImpl(baseUrl = appConfig.brreg.baseUrl, clientEngine = appConfig.engine)
    }
    single { EndringshistorikkService(get()) }
    single {
        ArenaAdapterService(
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
        )
    }
    single { AvtaleService(get(), get(), get(), get(), get(), get(), get(), get()) }
    single { TiltakshistorikkService(get(), get()) }
    single { VeilederflateService(get(), get(), get(), get()) }
    single { BrukerService(get(), get(), get(), get()) }
    single { DialogService(get()) }
    single { NavAnsattService(appConfig.auth.roles, get(), get(), get()) }
    single { PoaoTilgangService(get()) }
    single { DelMedBrukerService(get()) }
    single { MicrosoftGraphService(get()) }
    single { TiltaksgjennomforingService(get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    single { SanityTiltaksgjennomforingService(get(), get(), get()) }
    single { TiltakstypeService(get()) }
    single { NavEnheterSyncService(get(), get(), get(), get()) }
    single { KafkaSyncService(get(), get(), get(), get()) }
    single { NavEnhetService(get()) }
    single { NavVeilederService(get()) }
    single { NotificationService(get(), get(), get()) }
    single { VirksomhetService(get(), get()) }
    single { ExcelService() }
    single { MetrikkService(get()) }
    single { UtkastService(get()) }
    single { NotatServiceImpl(get(), get()) }
    single {
        val byEnhetStrategy = ByEnhetStrategy(get())
        val byNavidentStrategy = ByNavidentStrategy()
        UnleashService(appConfig.unleash, byEnhetStrategy, byNavidentStrategy)
    }
    single { AxsysService(appConfig.axsys) { m2mTokenProvider.createMachineToMachineToken(appConfig.axsys.scope) } }
    single { AvtaleValidator(get(), get(), get()) }
    single { TiltaksgjennomforingValidator(get()) }
}

private fun tasks(config: TaskConfig) = module {
    single { GenerateValidationReport(config.generateValidationReport, get(), get(), get(), get(), get()) }
    single { InitialLoadTiltaksgjennomforinger(get(), get(), get()) }
    single { SynchronizeNavAnsatte(config.synchronizeNavAnsatte, get(), get(), get()) }
    single {
        val deleteExpiredTiltakshistorikk = DeleteExpiredTiltakshistorikk(
            config.deleteExpiredTiltakshistorikk,
            get(),
            get(),
        )
        val synchronizeTiltaksgjennomforingsstatuserToKafka = SynchronizeTiltaksgjennomforingsstatuserToKafka(
            get(),
            get(),
        )
        val synchronizeTiltakstypestatuserToKafka = SynchronizeTiltakstypestatuserToKafka(get(), get())
        val synchronizeNorgEnheterTask = SynchronizeNorgEnheter(config.synchronizeNorgEnheter, get(), get())
        val notifySluttdatoForGjennomforingerNarmerSeg = NotifySluttdatoForGjennomforingerNarmerSeg(
            config.notifySluttdatoForGjennomforingerNarmerSeg,
            get(),
            get(),
            get(),
        )
        val notifySluttdatoForAvtalerNarmerSeg = NotifySluttdatoForAvtalerNarmerSeg(
            config.notifySluttdatoForAvtalerNarmerSeg,
            get(),
            get(),
            get(),
        )
        val notifyFailedKafkaEvents = NotifyFailedKafkaEvents(
            config.notifyFailedKafkaEvents,
            get(),
            get(),
            get(),
        )
        val notifySluttdatoForMidlertidigStengtGjennomforingerNarmerSeg =
            NotifySluttdatoForMidlertidigStengtGjennomforingerNarmerSeg(
                config.notifySluttdatoForMidlertidigStengtGjennomforingerNarmerSeg,
                get(),
                get(),
                get(),
            )
        val oppdaterMetrikker = OppdaterMetrikker(config.oppdaterMetrikker, get(), get())
        val notificationService: NotificationService by inject()
        val generateValidationReport: GenerateValidationReport by inject()
        val initialLoadTiltaksgjennomforinger: InitialLoadTiltaksgjennomforinger by inject()
        val synchronizeNavAnsatte: SynchronizeNavAnsatte by inject()

        val db: Database by inject()

        Scheduler
            .create(
                db.getDatasource(),
                notificationService.getScheduledNotificationTask(),
                generateValidationReport.task,
                initialLoadTiltaksgjennomforinger.task,
            )
            .startTasks(
                deleteExpiredTiltakshistorikk.task,
                synchronizeNorgEnheterTask.task,
                synchronizeTiltaksgjennomforingsstatuserToKafka.task,
                synchronizeTiltakstypestatuserToKafka.task,
                synchronizeNavAnsatte.task,
                notifySluttdatoForGjennomforingerNarmerSeg.task,
                notifySluttdatoForAvtalerNarmerSeg.task,
                notifyFailedKafkaEvents.task,
                notifySluttdatoForMidlertidigStengtGjennomforingerNarmerSeg.task,
                oppdaterMetrikker.task,
            )
            .serializer(DbSchedulerKotlinSerializer())
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
