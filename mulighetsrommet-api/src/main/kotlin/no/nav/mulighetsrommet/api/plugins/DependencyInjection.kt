package no.nav.mulighetsrommet.api.plugins

import com.github.kagkarlsson.scheduler.Scheduler
import io.ktor.server.application.*
import kotlinx.coroutines.runBlocking
import no.nav.common.client.axsys.AxsysClient
import no.nav.common.client.axsys.AxsysV2ClientImpl
import no.nav.common.kafka.producer.util.KafkaProducerClientBuilder
import no.nav.common.kafka.util.KafkaPropertiesBuilder
import no.nav.common.kafka.util.KafkaPropertiesPreset
import no.nav.mulighetsrommet.altinn.AltinnClient
import no.nav.mulighetsrommet.altinn.AltinnRettigheterService
import no.nav.mulighetsrommet.altinn.db.AltinnRettigheterRepository
import no.nav.mulighetsrommet.api.AppConfig
import no.nav.mulighetsrommet.api.SlackConfig
import no.nav.mulighetsrommet.api.TaskConfig
import no.nav.mulighetsrommet.api.arenaadapter.ArenaAdapterClient
import no.nav.mulighetsrommet.api.arenaadapter.ArenaAdapterService
import no.nav.mulighetsrommet.api.arrangor.ArrangorService
import no.nav.mulighetsrommet.api.arrangor.db.ArrangorRepository
import no.nav.mulighetsrommet.api.arrangor.kafka.AmtVirksomheterV1KafkaConsumer
import no.nav.mulighetsrommet.api.avtale.AvtaleService
import no.nav.mulighetsrommet.api.avtale.AvtaleValidator
import no.nav.mulighetsrommet.api.avtale.OpsjonLoggService
import no.nav.mulighetsrommet.api.avtale.OpsjonLoggValidator
import no.nav.mulighetsrommet.api.avtale.db.AvtaleRepository
import no.nav.mulighetsrommet.api.avtale.db.OpsjonLoggRepository
import no.nav.mulighetsrommet.api.avtale.task.NotifySluttdatoForAvtalerNarmerSeg
import no.nav.mulighetsrommet.api.clients.amtDeltaker.AmtDeltakerClient
import no.nav.mulighetsrommet.api.clients.brreg.BrregClient
import no.nav.mulighetsrommet.api.clients.dialog.VeilarbdialogClient
import no.nav.mulighetsrommet.api.clients.dokark.DokarkClient
import no.nav.mulighetsrommet.api.clients.isoppfolgingstilfelle.IsoppfolgingstilfelleClient
import no.nav.mulighetsrommet.api.clients.msgraph.MicrosoftGraphClient
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Client
import no.nav.mulighetsrommet.api.clients.oppfolging.VeilarboppfolgingClient
import no.nav.mulighetsrommet.api.clients.pamOntologi.PamOntologiClient
import no.nav.mulighetsrommet.api.clients.pdl.PdlClient
import no.nav.mulighetsrommet.api.clients.sanity.SanityClient
import no.nav.mulighetsrommet.api.clients.tiltakshistorikk.TiltakshistorikkClient
import no.nav.mulighetsrommet.api.clients.vedtak.VeilarbvedtaksstotteClient
import no.nav.mulighetsrommet.api.gjennomforing.TiltaksgjennomforingService
import no.nav.mulighetsrommet.api.gjennomforing.TiltaksgjennomforingValidator
import no.nav.mulighetsrommet.api.gjennomforing.db.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.gjennomforing.kafka.ArenaMigreringTiltaksgjennomforingerV1KafkaProducer
import no.nav.mulighetsrommet.api.gjennomforing.kafka.SisteTiltaksgjennomforingerV1KafkaConsumer
import no.nav.mulighetsrommet.api.gjennomforing.kafka.SisteTiltaksgjennomforingerV1KafkaProducer
import no.nav.mulighetsrommet.api.gjennomforing.task.InitialLoadTiltaksgjennomforinger
import no.nav.mulighetsrommet.api.gjennomforing.task.NotifySluttdatoForGjennomforingerNarmerSeg
import no.nav.mulighetsrommet.api.gjennomforing.task.UpdateApentForPamelding
import no.nav.mulighetsrommet.api.gjennomforing.task.UpdateTiltaksgjennomforingStatus
import no.nav.mulighetsrommet.api.navansatt.NavAnsattService
import no.nav.mulighetsrommet.api.navansatt.NavAnsattSyncService
import no.nav.mulighetsrommet.api.navansatt.db.NavAnsattRepository
import no.nav.mulighetsrommet.api.navansatt.task.SynchronizeNavAnsatte
import no.nav.mulighetsrommet.api.navenhet.NavEnhetService
import no.nav.mulighetsrommet.api.navenhet.NavEnheterSyncService
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetRepository
import no.nav.mulighetsrommet.api.navenhet.task.SynchronizeNorgEnheter
import no.nav.mulighetsrommet.api.refusjon.HentAdressebeskyttetPersonBolkPdlQuery
import no.nav.mulighetsrommet.api.refusjon.RefusjonService
import no.nav.mulighetsrommet.api.refusjon.db.DeltakerForslagRepository
import no.nav.mulighetsrommet.api.refusjon.db.DeltakerRepository
import no.nav.mulighetsrommet.api.refusjon.db.RefusjonskravRepository
import no.nav.mulighetsrommet.api.refusjon.kafka.AmtArrangorMeldingV1KafkaConsumer
import no.nav.mulighetsrommet.api.refusjon.kafka.AmtDeltakerV1KafkaConsumer
import no.nav.mulighetsrommet.api.refusjon.task.GenerateRefusjonskrav
import no.nav.mulighetsrommet.api.refusjon.task.JournalforRefusjonskrav
import no.nav.mulighetsrommet.api.services.EndringshistorikkService
import no.nav.mulighetsrommet.api.services.LagretFilterService
import no.nav.mulighetsrommet.api.services.PoaoTilgangService
import no.nav.mulighetsrommet.api.services.cms.SanityService
import no.nav.mulighetsrommet.api.tasks.GenerateValidationReport
import no.nav.mulighetsrommet.api.tasks.NotifyFailedKafkaEvents
import no.nav.mulighetsrommet.api.tilsagn.TilsagnService
import no.nav.mulighetsrommet.api.tilsagn.TilsagnValidator
import no.nav.mulighetsrommet.api.tilsagn.db.TilsagnRepository
import no.nav.mulighetsrommet.api.tiltakstype.TiltakstypeService
import no.nav.mulighetsrommet.api.tiltakstype.db.TiltakstypeRepository
import no.nav.mulighetsrommet.api.tiltakstype.kafka.SisteTiltakstyperV2KafkaProducer
import no.nav.mulighetsrommet.api.tiltakstype.task.InitialLoadTiltakstyper
import no.nav.mulighetsrommet.api.veilederflate.VeilederJoyrideRepository
import no.nav.mulighetsrommet.api.veilederflate.VeilederflateTiltakRepository
import no.nav.mulighetsrommet.api.veilederflate.services.BrukerService
import no.nav.mulighetsrommet.api.veilederflate.services.DelMedBrukerService
import no.nav.mulighetsrommet.api.veilederflate.services.TiltakshistorikkService
import no.nav.mulighetsrommet.api.veilederflate.services.VeilederflateService
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.DatabaseConfig
import no.nav.mulighetsrommet.env.NaisEnv
import no.nav.mulighetsrommet.kafka.KafkaConsumerOrchestrator
import no.nav.mulighetsrommet.kafka.KafkaConsumerRepositoryImpl
import no.nav.mulighetsrommet.metrics.Metrikker
import no.nav.mulighetsrommet.notifications.NotificationRepository
import no.nav.mulighetsrommet.notifications.NotificationService
import no.nav.mulighetsrommet.slack.SlackNotifier
import no.nav.mulighetsrommet.slack.SlackNotifierImpl
import no.nav.mulighetsrommet.tasks.DbSchedulerKotlinSerializer
import no.nav.mulighetsrommet.tasks.OpenTelemetrySchedulerListener
import no.nav.mulighetsrommet.tasks.SlackNotifierSchedulerListener
import no.nav.mulighetsrommet.tokenprovider.AccessType
import no.nav.mulighetsrommet.tokenprovider.CachedTokenProvider
import no.nav.mulighetsrommet.tokenprovider.M2MTokenProvider
import no.nav.mulighetsrommet.tokenprovider.createMaskinportenM2mTokenClient
import no.nav.mulighetsrommet.unleash.UnleashService
import no.nav.mulighetsrommet.utdanning.client.UtdanningClient
import no.nav.mulighetsrommet.utdanning.db.UtdanningRepository
import no.nav.mulighetsrommet.utdanning.task.SynchronizeUtdanninger
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

    val consumerPreset = when (NaisEnv.current()) {
        NaisEnv.Local -> KafkaPropertiesBuilder.consumerBuilder()
            .withBaseProperties()
            .withConsumerGroupId(config.defaultConsumerGroupId)
            .withBrokerUrl(config.brokerUrl)
            .withDeserializers(ByteArrayDeserializer::class.java, ByteArrayDeserializer::class.java)
            .build()

        else -> KafkaPropertiesPreset.aivenDefaultConsumerProperties(config.defaultConsumerGroupId)
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
            AmtDeltakerV1KafkaConsumer(
                config = config.consumers.amtDeltakerV1,
                tiltakstyper = get(),
                deltakere = get(),
                refusjonService = get(),
            ),
            AmtVirksomheterV1KafkaConsumer(
                config = config.consumers.amtVirksomheterV1,
                arrangorRepository = get(),
                brregClient = get(),
            ),
            AmtArrangorMeldingV1KafkaConsumer(
                config = config.consumers.amtArrangorMeldingV1,
                deltakerForslagRepository = get(),
            ),
        )
        KafkaConsumerOrchestrator(
            consumerPreset = consumerPreset,
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
    single { AltinnRettigheterRepository(get()) }
    single { VeilederflateTiltakRepository(get()) }
    single { DeltakerForslagRepository(get()) }
}

private fun services(appConfig: AppConfig) = module {
    val azure = appConfig.auth.azure
    val cachedTokenProvider = CachedTokenProvider.init(azure.audience, azure.tokenEndpointUrl)
    val maskinportenTokenProvider = createMaskinportenM2mTokenClient(
        appConfig.auth.maskinporten.audience,
        appConfig.auth.maskinporten.tokenEndpointUrl,
        appConfig.auth.maskinporten.issuer,
    )

    single {
        VeilarboppfolgingClient(
            baseUrl = appConfig.veilarboppfolgingConfig.url,
            tokenProvider = cachedTokenProvider.withScope(appConfig.veilarboppfolgingConfig.scope),
            clientEngine = appConfig.engine,
        )
    }
    single {
        VeilarbvedtaksstotteClient(
            baseUrl = appConfig.veilarbvedtaksstotteConfig.url,
            tokenProvider = cachedTokenProvider.withScope(appConfig.veilarbvedtaksstotteConfig.scope),
            clientEngine = appConfig.engine,
        )
    }
    single {
        VeilarbdialogClient(
            baseUrl = appConfig.veilarbdialogConfig.url,
            tokenProvider = cachedTokenProvider.withScope(appConfig.veilarbdialogConfig.scope),
            clientEngine = appConfig.engine,
        )
    }
    single {
        PdlClient(
            config = PdlClient.Config(appConfig.pdl.url, maxRetries = 3),
            tokenProvider = cachedTokenProvider.withScope(appConfig.pdl.scope),
            clientEngine = appConfig.engine,
        )
    }
    single { HentAdressebeskyttetPersonBolkPdlQuery(get()) }
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
            clientEngine = appConfig.engine,
        )
    }
    single {
        TiltakshistorikkClient(
            baseUrl = appConfig.tiltakshistorikk.url,
            tokenProvider = cachedTokenProvider.withScope(appConfig.tiltakshistorikk.scope),
            clientEngine = appConfig.engine,
        )
    }
    single {
        Norg2Client(
            clientEngine = appConfig.engine,
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
    single {
        AltinnClient(
            baseUrl = appConfig.altinn.url,
            altinnApiKey = appConfig.altinn.apiKey,
            clientEngine = appConfig.engine,
            tokenProvider = maskinportenTokenProvider?.withScope(
                scope = appConfig.altinn.scope,
                targetAudience = appConfig.altinn.url,
            ) ?: M2MTokenProvider { "dummy" }, // TODO: Remove when prod
        )
    }
    single {
        IsoppfolgingstilfelleClient(
            baseUrl = appConfig.isoppfolgingstilfelleConfig.url,
            clientEngine = appConfig.engine,
            tokenProvider = cachedTokenProvider.withScope(appConfig.isoppfolgingstilfelleConfig.scope),
        )
    }
    single {
        DokarkClient(
            baseUrl = appConfig.dokark.url,
            clientEngine = appConfig.engine,
            tokenProvider = cachedTokenProvider.withScope(appConfig.dokark.scope),
        )
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
        )
    }
    single {
        AvtaleService(
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
        )
    }
    single { TiltakshistorikkService(get(), get(), get(), get(), get()) }
    single { VeilederflateService(get(), get(), get(), get()) }
    single { BrukerService(get(), get(), get(), get(), get(), get()) }
    single { NavAnsattService(appConfig.auth.roles, get(), get()) }
    single { NavAnsattSyncService(get(), get(), get(), get(), get(), get(), get()) }
    single { PoaoTilgangService(get()) }
    single { DelMedBrukerService(get(), get(), get()) }
    single {
        TiltaksgjennomforingService(
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
        )
    }
    single { TiltakstypeService(get()) }
    single { NavEnheterSyncService(get(), get(), get(), get()) }
    single { NavEnhetService(get()) }
    single { NotificationService(get(), get()) }
    single { ArrangorService(get(), get()) }
    single { RefusjonService(get(), get(), get(), get()) }
    single { UnleashService(appConfig.unleash, get()) }
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
    single { AltinnRettigheterService(get(), get()) }
}

private fun tasks(config: TaskConfig) = module {
    single { GenerateValidationReport(config.generateValidationReport, get(), get(), get(), get(), get()) }
    single { InitialLoadTiltaksgjennomforinger(get(), get(), get(), get()) }
    single { InitialLoadTiltakstyper(get(), get(), get(), get()) }
    single { SynchronizeNavAnsatte(config.synchronizeNavAnsatte, get(), get()) }
    single { SynchronizeUtdanninger(config.synchronizeUtdanninger, get(), get()) }
    single { GenerateRefusjonskrav(config.generateRefusjonskrav, get()) }
    single { JournalforRefusjonskrav(get(), get(), get(), get(), get()) }
    single {
        val updateTiltaksgjennomforingStatus = UpdateTiltaksgjennomforingStatus(
            get(),
            get(),
        )
        val synchronizeNorgEnheterTask = SynchronizeNorgEnheter(config.synchronizeNorgEnheter, get())
        val notifySluttdatoForGjennomforingerNarmerSeg = NotifySluttdatoForGjennomforingerNarmerSeg(
            config.notifySluttdatoForGjennomforingerNarmerSeg,
            get(),
            get(),
        )
        val notifySluttdatoForAvtalerNarmerSeg = NotifySluttdatoForAvtalerNarmerSeg(
            config.notifySluttdatoForAvtalerNarmerSeg,
            get(),
            get(),
        )
        val notifyFailedKafkaEvents = NotifyFailedKafkaEvents(
            config.notifyFailedKafkaEvents,
            get(),
            get(),
            get(),
        )
        val updateApentForPamelding = UpdateApentForPamelding(config.updateApentForPamelding, get(), get())
        val notificationService: NotificationService by inject()
        val generateValidationReport: GenerateValidationReport by inject()
        val initialLoadTiltaksgjennomforinger: InitialLoadTiltaksgjennomforinger by inject()
        val initialLoadTiltakstyper: InitialLoadTiltakstyper by inject()
        val synchronizeNavAnsatte: SynchronizeNavAnsatte by inject()
        val synchronizeUtdanninger: SynchronizeUtdanninger by inject()
        val generateRefusjonskrav: GenerateRefusjonskrav by inject()
        val journalforRefusjonskrav: JournalforRefusjonskrav by inject()

        val db: Database by inject()

        Scheduler
            .create(
                db.getDatasource(),
                notificationService.getScheduledNotificationTask(),
                generateValidationReport.task,
                initialLoadTiltaksgjennomforinger.task,
                initialLoadTiltakstyper.task,
                journalforRefusjonskrav.task,
            )
            .addSchedulerListener(SlackNotifierSchedulerListener(get()))
            .addSchedulerListener(OpenTelemetrySchedulerListener())
            .startTasks(
                synchronizeNorgEnheterTask.task,
                updateTiltaksgjennomforingStatus.task,
                synchronizeNavAnsatte.task,
                synchronizeUtdanninger.task,
                notifySluttdatoForGjennomforingerNarmerSeg.task,
                notifySluttdatoForAvtalerNarmerSeg.task,
                notifyFailedKafkaEvents.task,
                updateApentForPamelding.task,
                generateRefusjonskrav.task,
            )
            .serializer(DbSchedulerKotlinSerializer())
            .registerShutdownHook()
            .build()
    }
}
