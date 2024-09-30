package no.nav.mulighetsrommet.api.plugins

import com.github.kagkarlsson.scheduler.Scheduler
import io.ktor.server.application.*
import kotlinx.coroutines.runBlocking
import no.nav.common.client.axsys.AxsysClient
import no.nav.common.client.axsys.AxsysV2ClientImpl
import no.nav.common.kafka.producer.util.KafkaProducerClientBuilder
import no.nav.common.kafka.util.KafkaPropertiesBuilder
import no.nav.common.kafka.util.KafkaPropertiesPreset
import no.nav.mulighetsrommet.api.AppConfig
import no.nav.mulighetsrommet.api.SlackConfig
import no.nav.mulighetsrommet.api.TaskConfig
import no.nav.mulighetsrommet.api.avtaler.AvtaleValidator
import no.nav.mulighetsrommet.api.avtaler.OpsjonLoggValidator
import no.nav.mulighetsrommet.api.clients.amtDeltaker.AmtDeltakerClient
import no.nav.mulighetsrommet.api.clients.arenaadapter.ArenaAdapterClient
import no.nav.mulighetsrommet.api.clients.brreg.BrregClient
import no.nav.mulighetsrommet.api.clients.dialog.VeilarbdialogClient
import no.nav.mulighetsrommet.api.clients.msgraph.MicrosoftGraphClient
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Client
import no.nav.mulighetsrommet.api.clients.oppfolging.VeilarboppfolgingClient
import no.nav.mulighetsrommet.api.clients.pamOntologi.PamOntologiClient
import no.nav.mulighetsrommet.api.clients.pdl.PdlClient
import no.nav.mulighetsrommet.api.clients.sanity.SanityClient
import no.nav.mulighetsrommet.api.clients.tiltakshistorikk.TiltakshistorikkClient
import no.nav.mulighetsrommet.api.clients.utdanning.UtdanningClient
import no.nav.mulighetsrommet.api.clients.vedtak.VeilarbvedtaksstotteClient
import no.nav.mulighetsrommet.api.okonomi.refusjon.RefusjonService
import no.nav.mulighetsrommet.api.okonomi.refusjon.RefusjonskravRepository
import no.nav.mulighetsrommet.api.okonomi.tilsagn.TilsagnRepository
import no.nav.mulighetsrommet.api.okonomi.tilsagn.TilsagnService
import no.nav.mulighetsrommet.api.okonomi.tilsagn.TilsagnValidator
import no.nav.mulighetsrommet.api.repositories.*
import no.nav.mulighetsrommet.api.services.*
import no.nav.mulighetsrommet.api.services.VeilederflateService
import no.nav.mulighetsrommet.api.services.cms.SanityService
import no.nav.mulighetsrommet.api.tasks.*
import no.nav.mulighetsrommet.api.tiltaksgjennomforinger.TiltaksgjennomforingValidator
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.DatabaseConfig
import no.nav.mulighetsrommet.env.NaisEnv
import no.nav.mulighetsrommet.kafka.KafkaConsumerOrchestrator
import no.nav.mulighetsrommet.kafka.KafkaConsumerRepositoryImpl
import no.nav.mulighetsrommet.kafka.consumers.SisteTiltaksgjennomforingerV1KafkaConsumer
import no.nav.mulighetsrommet.kafka.consumers.amt.AmtDeltakerV1KafkaConsumer
import no.nav.mulighetsrommet.kafka.consumers.amt.AmtVirksomheterV1KafkaConsumer
import no.nav.mulighetsrommet.kafka.producers.ArenaMigreringTiltaksgjennomforingerV1KafkaProducer
import no.nav.mulighetsrommet.kafka.producers.SisteTiltaksgjennomforingerV1KafkaProducer
import no.nav.mulighetsrommet.kafka.producers.SisteTiltakstyperV2KafkaProducer
import no.nav.mulighetsrommet.metrics.Metrikker
import no.nav.mulighetsrommet.notifications.NotificationRepository
import no.nav.mulighetsrommet.notifications.NotificationService
import no.nav.mulighetsrommet.slack.SlackNotifier
import no.nav.mulighetsrommet.slack.SlackNotifierImpl
import no.nav.mulighetsrommet.tasks.DbSchedulerKotlinSerializer
import no.nav.mulighetsrommet.tokenprovider.AccessType
import no.nav.mulighetsrommet.tokenprovider.CachedTokenProvider
import no.nav.mulighetsrommet.unleash.UnleashService
import no.nav.mulighetsrommet.unleash.strategies.ByEnhetStrategy
import no.nav.mulighetsrommet.unleash.strategies.ByNavIdentStrategy
import no.nav.poao_tilgang.client.PoaoTilgangClient
import no.nav.poao_tilgang.client.PoaoTilgangHttpClient
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.ktor.plugin.KoinIsolated
import org.koin.logger.SLF4JLogger

fun Application.configureDependencyInjection(appConfig: AppConfig) {
    install(KoinIsolated) {
        SLF4JLogger()

        modules(
            db(appConfig.database),
            kafka(appConfig),
            repositories(),
            services(appConfig),
            tasks(appConfig.tasks),
            slack(appConfig.slack),
        )
    }
}

fun slack(slack: SlackConfig): Module = module(createdAtStart = true) {
    single<SlackNotifier> {
        SlackNotifierImpl(slack.token, slack.channel, slack.enable)
    }
}

private fun db(config: DatabaseConfig) = module {
    single<Database>(createdAtStart = true) {
        Database(config)
    }
}

private fun kafka(appConfig: AppConfig) = module {
    val config = appConfig.kafka
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
        ArenaMigreringTiltaksgjennomforingerV1KafkaProducer(
            producerClient,
            config.producers.arenaMigreringTiltaksgjennomforinger,
        )
    }
    single { SisteTiltaksgjennomforingerV1KafkaProducer(producerClient, config.producers.tiltaksgjennomforinger) }
    single { SisteTiltakstyperV2KafkaProducer(producerClient, config.producers.tiltakstyper) }

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
            SisteTiltaksgjennomforingerV1KafkaConsumer(
                config = config.consumers.tiltaksgjennomforingerV1,
                tiltakstyper = get(),
                arenaAdapterClient = get(),
                arenaMigreringTiltaksgjennomforingProducer = get(),
                tiltaksgjennomforingRepository = get(),
            ),
            AmtDeltakerV1KafkaConsumer(config = config.consumers.amtDeltakerV1, deltakere = get()),
            AmtVirksomheterV1KafkaConsumer(
                config = config.consumers.amtVirksomheterV1,
                arrangorRepository = get(),
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
    single { NavEnhetRepository(get()) }
    single { DeltakerRepository(get()) }
    single { NotificationRepository(get()) }
    single { NavAnsattRepository(get()) }
    single { ArrangorRepository(get()) }
    single { KafkaConsumerRepositoryImpl(get()) }
    single { VeilederJoyrideRepository(get()) }
    single { OpsjonLoggRepository(get()) }
    single { TilsagnRepository(get()) }
    single { RefusjonskravRepository(get()) }
    single { UtdanningRepository(get()) }
}

private fun services(appConfig: AppConfig) = module {
    val azure = appConfig.auth.azure
    val cachedTokenProvider = CachedTokenProvider.init(azure.audience, azure.tokenEndpointUrl)

    single {
        VeilarboppfolgingClient(
            baseUrl = appConfig.veilarboppfolgingConfig.url,
            tokenProvider = cachedTokenProvider.withScope(appConfig.veilarboppfolgingConfig.scope),
        )
    }
    single {
        VeilarbvedtaksstotteClient(
            baseUrl = appConfig.veilarbvedtaksstotteConfig.url,
            tokenProvider = cachedTokenProvider.withScope(appConfig.veilarbvedtaksstotteConfig.scope),
        )
    }
    single {
        VeilarbdialogClient(
            baseUrl = appConfig.veilarbdialogConfig.url,
            tokenProvider = cachedTokenProvider.withScope(appConfig.veilarbdialogConfig.scope),
        )
    }
    single {
        PdlClient(
            baseUrl = appConfig.pdl.url,
            tokenProvider = cachedTokenProvider.withScope(appConfig.pdl.scope),
        )
    }

    single<PoaoTilgangClient> {
        PoaoTilgangHttpClient(
            baseUrl = appConfig.poaoTilgang.url,
            tokenProvider = {
                runBlocking {
                    cachedTokenProvider.withScope(appConfig.poaoTilgang.scope).exchange(AccessType.M2M)
                }
            },
        )
    }
    single {
        MicrosoftGraphClient(
            baseUrl = appConfig.msGraphConfig.url,
            tokenProvider = cachedTokenProvider.withScope(appConfig.msGraphConfig.scope),
        )
    }
    single {
        ArenaAdapterClient(
            baseUrl = appConfig.arenaAdapter.url,
            tokenProvider = cachedTokenProvider.withScope(appConfig.arenaAdapter.scope),
        )
    }
    single {
        TiltakshistorikkClient(
            baseUrl = appConfig.tiltakshistorikk.url,
            tokenProvider = cachedTokenProvider.withScope(appConfig.tiltakshistorikk.scope),
        )
    }
    single {
        Norg2Client(
            baseUrl = appConfig.norg2.baseUrl,
        )
    }

    single {
        SanityClient(
            config = appConfig.sanity,
        )
    }
    single { SanityService(get()) }
    single {
        BrregClient(baseUrl = appConfig.brreg.baseUrl, clientEngine = appConfig.engine)
    }
    single {
        AmtDeltakerClient(
            baseUrl = appConfig.amtDeltakerConfig.url,
            clientEngine = appConfig.engine,
            tokenProvider = cachedTokenProvider.withScope(appConfig.amtDeltakerConfig.scope),
        )
    }
    single {
        PamOntologiClient(
            baseUrl = appConfig.pamOntologi.url,
            clientEngine = appConfig.engine,
            tokenProvider = cachedTokenProvider.withScope(appConfig.pamOntologi.scope),
        )
    }
    single { UtdanningClient(config = appConfig.utdanning) }
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
        )
    }
    single {
        AvtaleService(
            get(),
            get(),
            tiltakstyperMigrert = appConfig.migrerteTiltak,
            get(),
            get(),
            get(),
            get(),
            get(),
        )
    }
    single { TiltakshistorikkService(get(), get(), get(), get(), get()) }
    single { VeilederflateService(get(), get(), get(), get()) }
    single { BrukerService(get(), get(), get(), get(), get()) }
    single { NavAnsattService(appConfig.auth.roles, get(), get()) }
    single { NavAnsattSyncService(get(), get(), get(), get(), get(), get(), get()) }
    single { PoaoTilgangService(get()) }
    single { DelMedBrukerService(get(), get(), get()) }
    single { MicrosoftGraphService(get()) }
    single {
        TiltaksgjennomforingService(
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
    single { TiltakstypeService(get(), appConfig.migrerteTiltak) }
    single { NavEnheterSyncService(get(), get(), get(), get()) }
    single { NavEnhetService(get()) }
    single { NavVeilederService(get()) }
    single { NotificationService(get(), get(), get()) }
    single { ArrangorService(get(), get()) }
    single { RefusjonService(get(), get(), get(), get()) }
    single {
        val byEnhetStrategy = ByEnhetStrategy(get())
        val byNavidentStrategy = ByNavIdentStrategy()
        UnleashService(appConfig.unleash, byEnhetStrategy, byNavidentStrategy)
    }
    single<AxsysClient> {
        AxsysV2ClientImpl(
            appConfig.axsys.url,
        ) { runBlocking { cachedTokenProvider.withScope(appConfig.axsys.scope).exchange(AccessType.M2M) } }
    }
    single { AvtaleValidator(get(), get(), get(), get(), get()) }
    single { TiltaksgjennomforingValidator(get(), get(), get(), get()) }
    single { OpsjonLoggValidator() }
    single { TilsagnValidator(get()) }
    single { OpsjonLoggService(get(), get(), get(), get(), get()) }
    single { LagretFilterService(get()) }
    single { TilsagnService(get(), get(), get(), get()) }
}

private fun tasks(config: TaskConfig) = module {
    single { GenerateValidationReport(config.generateValidationReport, get(), get(), get(), get(), get()) }
    single { InitialLoadTiltaksgjennomforinger(get(), get(), get(), get()) }
    single { InitialLoadTiltakstyper(get(), get(), get(), get()) }
    single { SynchronizeNavAnsatte(config.synchronizeNavAnsatte, get(), get(), get()) }
    single { SynchronizeUtdanninger(get(), get(), config.synchronizeUtdanninger, get()) }
    single { GenerateRefusjonskrav(config.generateRefusjonskrav, get(), get()) }
    single {
        val updateTiltaksgjennomforingStatus = UpdateTiltaksgjennomforingStatus(
            get(),
            get(),
            get(),
        )
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
        val updateApentForInnsok = UpdateApentForInnsok(config.updateApentForInnsok, get(), get())
        val notificationService: NotificationService by inject()
        val generateValidationReport: GenerateValidationReport by inject()
        val initialLoadTiltaksgjennomforinger: InitialLoadTiltaksgjennomforinger by inject()
        val initialLoadTiltakstyper: InitialLoadTiltakstyper by inject()
        val synchronizeNavAnsatte: SynchronizeNavAnsatte by inject()
        val synchronizeUtdanninger: SynchronizeUtdanninger by inject()
        val generateRefusjonskrav: GenerateRefusjonskrav by inject()

        val db: Database by inject()

        Scheduler
            .create(
                db.getDatasource(),
                notificationService.getScheduledNotificationTask(),
                generateValidationReport.task,
                initialLoadTiltaksgjennomforinger.task,
                initialLoadTiltakstyper.task,
            )
            .startTasks(
                synchronizeNorgEnheterTask.task,
                updateTiltaksgjennomforingStatus.task,
                synchronizeNavAnsatte.task,
                synchronizeUtdanninger.task,
                notifySluttdatoForGjennomforingerNarmerSeg.task,
                notifySluttdatoForAvtalerNarmerSeg.task,
                notifyFailedKafkaEvents.task,
                updateApentForInnsok.task,
                generateRefusjonskrav.task,
            )
            .serializer(DbSchedulerKotlinSerializer())
            .build()
    }
}
