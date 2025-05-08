package no.nav.mulighetsrommet.api.plugins

import com.github.kagkarlsson.scheduler.Scheduler
import io.ktor.server.application.*
import kotlinx.coroutines.runBlocking
import net.javacrumbs.shedlock.provider.jdbc.JdbcLockProvider
import no.nav.common.client.axsys.AxsysClient
import no.nav.common.client.axsys.AxsysV2ClientImpl
import no.nav.common.job.leader_election.ShedLockLeaderElectionClient
import no.nav.common.kafka.producer.KafkaProducerClient
import no.nav.common.kafka.producer.feilhandtering.KafkaProducerRepository
import no.nav.common.kafka.producer.feilhandtering.StoredProducerRecord
import no.nav.common.kafka.producer.feilhandtering.publisher.QueuedKafkaProducerRecordPublisher
import no.nav.common.kafka.producer.feilhandtering.util.KafkaProducerRecordProcessorBuilder
import no.nav.common.kafka.producer.util.KafkaProducerClientBuilder
import no.nav.mulighetsrommet.altinn.AltinnClient
import no.nav.mulighetsrommet.altinn.AltinnRettigheterService
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.AppConfig
import no.nav.mulighetsrommet.api.SlackConfig
import no.nav.mulighetsrommet.api.TaskConfig
import no.nav.mulighetsrommet.api.arenaadapter.ArenaAdapterClient
import no.nav.mulighetsrommet.api.arenaadapter.ArenaAdapterService
import no.nav.mulighetsrommet.api.arrangor.ArrangorService
import no.nav.mulighetsrommet.api.arrangor.kafka.AmtVirksomheterV1KafkaConsumer
import no.nav.mulighetsrommet.api.arrangorflate.ArrangorFlateService
import no.nav.mulighetsrommet.api.avtale.AvtaleService
import no.nav.mulighetsrommet.api.avtale.AvtaleValidator
import no.nav.mulighetsrommet.api.avtale.OpsjonLoggService
import no.nav.mulighetsrommet.api.avtale.task.NotifySluttdatoForAvtalerNarmerSeg
import no.nav.mulighetsrommet.api.clients.amtDeltaker.AmtDeltakerClient
import no.nav.mulighetsrommet.api.clients.dialog.VeilarbdialogClient
import no.nav.mulighetsrommet.api.clients.dokark.DokarkClient
import no.nav.mulighetsrommet.api.clients.isoppfolgingstilfelle.IsoppfolgingstilfelleClient
import no.nav.mulighetsrommet.api.clients.kontoregisterOrganisasjon.KontoregisterOrganisasjonClient
import no.nav.mulighetsrommet.api.clients.msgraph.MicrosoftGraphClient
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Client
import no.nav.mulighetsrommet.api.clients.oppfolging.VeilarboppfolgingClient
import no.nav.mulighetsrommet.api.clients.pamOntologi.PamOntologiClient
import no.nav.mulighetsrommet.api.clients.pdl.PdlClient
import no.nav.mulighetsrommet.api.clients.sanity.SanityClient
import no.nav.mulighetsrommet.api.clients.tiltakshistorikk.TiltakshistorikkClient
import no.nav.mulighetsrommet.api.clients.vedtak.VeilarbvedtaksstotteClient
import no.nav.mulighetsrommet.api.datavarehus.kafka.DatavarehusTiltakV1KafkaProducer
import no.nav.mulighetsrommet.api.gjennomforing.GjennomforingService
import no.nav.mulighetsrommet.api.gjennomforing.GjennomforingValidator
import no.nav.mulighetsrommet.api.gjennomforing.kafka.AmtKoordinatorGjennomforingV1KafkaConsumer
import no.nav.mulighetsrommet.api.gjennomforing.kafka.ArenaMigreringTiltaksgjennomforingerV1KafkaProducer
import no.nav.mulighetsrommet.api.gjennomforing.kafka.SisteTiltaksgjennomforingerV1KafkaConsumer
import no.nav.mulighetsrommet.api.gjennomforing.kafka.SisteTiltaksgjennomforingerV1KafkaProducer
import no.nav.mulighetsrommet.api.gjennomforing.task.InitialLoadGjennomforinger
import no.nav.mulighetsrommet.api.gjennomforing.task.NotifySluttdatoForGjennomforingerNarmerSeg
import no.nav.mulighetsrommet.api.gjennomforing.task.UpdateApentForPamelding
import no.nav.mulighetsrommet.api.gjennomforing.task.UpdateGjennomforingStatus
import no.nav.mulighetsrommet.api.lagretfilter.LagretFilterService
import no.nav.mulighetsrommet.api.navansatt.service.NavAnsattPrincipalService
import no.nav.mulighetsrommet.api.navansatt.service.NavAnsattService
import no.nav.mulighetsrommet.api.navansatt.service.NavAnsattSyncService
import no.nav.mulighetsrommet.api.navansatt.task.SynchronizeNavAnsatte
import no.nav.mulighetsrommet.api.navenhet.NavEnhetService
import no.nav.mulighetsrommet.api.navenhet.NavEnheterSyncService
import no.nav.mulighetsrommet.api.navenhet.task.SynchronizeNorgEnheter
import no.nav.mulighetsrommet.api.pdfgen.PdfGenClient
import no.nav.mulighetsrommet.api.sanity.SanityService
import no.nav.mulighetsrommet.api.services.PoaoTilgangService
import no.nav.mulighetsrommet.api.tasks.GenerateValidationReport
import no.nav.mulighetsrommet.api.tasks.NotifyFailedKafkaEvents
import no.nav.mulighetsrommet.api.tilsagn.TilsagnService
import no.nav.mulighetsrommet.api.tilsagn.kafka.ReplicateOkonomiBestillingStatus
import no.nav.mulighetsrommet.api.tiltakstype.TiltakstypeService
import no.nav.mulighetsrommet.api.tiltakstype.kafka.SisteTiltakstyperV2KafkaProducer
import no.nav.mulighetsrommet.api.tiltakstype.task.InitialLoadTiltakstyper
import no.nav.mulighetsrommet.api.totrinnskontroll.service.TotrinnskontrollService
import no.nav.mulighetsrommet.api.utbetaling.UtbetalingService
import no.nav.mulighetsrommet.api.utbetaling.kafka.AmtArrangorMeldingV1KafkaConsumer
import no.nav.mulighetsrommet.api.utbetaling.kafka.AmtDeltakerV1KafkaConsumer
import no.nav.mulighetsrommet.api.utbetaling.kafka.OppdaterUtbetalingBeregningForGjennomforingConsumer
import no.nav.mulighetsrommet.api.utbetaling.kafka.ReplicateOkonomiFakturaStatus
import no.nav.mulighetsrommet.api.utbetaling.pdl.HentAdressebeskyttetPersonBolkPdlQuery
import no.nav.mulighetsrommet.api.utbetaling.pdl.HentAdressebeskyttetPersonMedGeografiskTilknytningBolkPdlQuery
import no.nav.mulighetsrommet.api.utbetaling.task.GenerateUtbetaling
import no.nav.mulighetsrommet.api.utbetaling.task.JournalforUtbetaling
import no.nav.mulighetsrommet.api.utbetaling.task.OppdaterUtbetalingBeregning
import no.nav.mulighetsrommet.api.veilederflate.pdl.HentBrukerPdlQuery
import no.nav.mulighetsrommet.api.veilederflate.pdl.HentHistoriskeIdenterPdlQuery
import no.nav.mulighetsrommet.api.veilederflate.services.BrukerService
import no.nav.mulighetsrommet.api.veilederflate.services.DelMedBrukerService
import no.nav.mulighetsrommet.api.veilederflate.services.TiltakshistorikkService
import no.nav.mulighetsrommet.api.veilederflate.services.VeilederflateService
import no.nav.mulighetsrommet.brreg.BrregClient
import no.nav.mulighetsrommet.clamav.ClamAvClient
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.DatabaseConfig
import no.nav.mulighetsrommet.kafka.KafkaConsumerOrchestrator
import no.nav.mulighetsrommet.metrics.Metrikker
import no.nav.mulighetsrommet.notifications.NotificationTask
import no.nav.mulighetsrommet.oppgaver.OppgaverService
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
import no.nav.mulighetsrommet.utdanning.task.SynchronizeUtdanninger
import no.nav.poao_tilgang.client.PoaoTilgangClient
import no.nav.poao_tilgang.client.PoaoTilgangHttpClient
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
    val database = Database(config.copy { metricRegistry = Metrikker.appMicrometerRegistry })
    single<Database>(createdAtStart = true) {
        database
    }
    single<ApiDatabase> { ApiDatabase(database) }
}

private fun kafka(appConfig: AppConfig) = module {
    val config = appConfig.kafka

    single<KafkaProducerClient<ByteArray, ByteArray?>> {
        KafkaProducerClientBuilder.builder<ByteArray, ByteArray?>()
            .withProperties(config.producerProperties)
            .withMetrics(Metrikker.appMicrometerRegistry)
            .build()
    }
    single {
        ArenaMigreringTiltaksgjennomforingerV1KafkaProducer(
            get(),
            config.clients.arenaMigreringTiltaksgjennomforinger,
        )
    }
    single { SisteTiltaksgjennomforingerV1KafkaProducer(get(), config.clients.gjennomforinger) }
    single { SisteTiltakstyperV2KafkaProducer(get(), config.clients.tiltakstyper) }

    single {
        val consumers = listOf(
            DatavarehusTiltakV1KafkaProducer(
                config = config.clients.dvhGjennomforing,
                kafkaProducerClient = get(),
                db = get(),
            ),
            SisteTiltaksgjennomforingerV1KafkaConsumer(
                config = config.clients.gjennomforingerV1,
                db = get(),
                tiltakstyper = get(),
                arenaAdapterClient = get(),
                arenaMigreringTiltaksgjennomforingProducer = get(),
            ),
            AmtDeltakerV1KafkaConsumer(
                config = config.clients.amtDeltakerV1,
                db = get(),
                oppdaterUtbetaling = get(),
            ),
            AmtVirksomheterV1KafkaConsumer(config.clients.amtVirksomheterV1, get()),
            AmtArrangorMeldingV1KafkaConsumer(config.clients.amtArrangorMeldingV1, get()),
            AmtKoordinatorGjennomforingV1KafkaConsumer(config.clients.amtKoordinatorMeldingV1, get()),
            ReplicateOkonomiBestillingStatus(config.clients.replicateBestillingStatus, get()),
            ReplicateOkonomiFakturaStatus(config.clients.replicateFakturaStatus, get()),
            OppdaterUtbetalingBeregningForGjennomforingConsumer(
                config.clients.oppdaterUtbetalingForGjennomforing,
                get(),
                get(),
            ),
        )
        KafkaConsumerOrchestrator(
            consumerPreset = config.consumerPreset,
            db = get(),
            consumers = consumers,
        )
    }
    single<ShedLockLeaderElectionClient> {
        val db = get<ApiDatabase>()
        ShedLockLeaderElectionClient(JdbcLockProvider(db.getDatasource()))
    }
    single {
        val db = get<ApiDatabase>()
        val shedLockLeaderElectionClient = get<ShedLockLeaderElectionClient>()
        val repository = object : KafkaProducerRepository {
            override fun storeRecord(record: StoredProducerRecord?): Long {
                error("Not used")
            }

            override fun deleteRecords(ids: List<Long>) {
                return db.session { queries.kafkaProducerRecord.deleteRecords(ids) }
            }

            override fun getRecords(maxMessages: Int): List<StoredProducerRecord> {
                return db.session { queries.kafkaProducerRecord.getRecords(maxMessages) }
            }

            override fun getRecords(maxMessages: Int, topics: List<String>): List<StoredProducerRecord> {
                return db.session { queries.kafkaProducerRecord.getRecords(maxMessages, topics) }
            }
        }
        KafkaProducerRecordProcessorBuilder.builder()
            .withProducerRepository(repository)
            .withLeaderElectionClient(shedLockLeaderElectionClient)
            .withRecordPublisher(QueuedKafkaProducerRecordPublisher(get()))
            .build()
    }
}

private fun services(appConfig: AppConfig) = module {
    val azure = appConfig.auth.azure
    val cachedTokenProvider = CachedTokenProvider.init(azure.audience, azure.tokenEndpointUrl, azure.privateJwk)
    val maskinportenTokenProvider = createMaskinportenM2mTokenClient(
        clientId = appConfig.auth.maskinporten.audience,
        issuer = appConfig.auth.maskinporten.issuer,
        tokenEndpointUrl = appConfig.auth.maskinporten.tokenEndpointUrl,
        privateJwk = appConfig.auth.maskinporten.privateJwk,
    )

    single {
        VeilarboppfolgingClient(
            baseUrl = appConfig.veilarboppfolgingConfig.url,
            tokenProvider = cachedTokenProvider.withScope(appConfig.veilarboppfolgingConfig.scope),
            clientEngine = appConfig.engine,
        )
    }
    single {
        PdfGenClient(
            clientEngine = appConfig.engine,
            baseUrl = appConfig.pdfgen.url,
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
            clientEngine = appConfig.pdl.engine ?: appConfig.engine,
        )
    }
    single { HentAdressebeskyttetPersonBolkPdlQuery(get()) }
    single { HentAdressebeskyttetPersonMedGeografiskTilknytningBolkPdlQuery(get()) }
    single { HentHistoriskeIdenterPdlQuery(get()) }
    single { HentBrukerPdlQuery(get()) }
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
            engine = appConfig.engine,
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
            baseUrl = appConfig.norg2.url,
        )
    }
    single {
        SanityClient(
            config = appConfig.sanity,
        )
    }
    single { SanityService(get()) }
    single {
        BrregClient(clientEngine = appConfig.engine)
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
    single { UtdanningClient(baseUrl = appConfig.utdanning.url) }
    single {
        AltinnClient(
            baseUrl = appConfig.altinn.url,
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
    single {
        KontoregisterOrganisasjonClient(
            clientEngine = appConfig.kontoregisterOrganisasjon.engine ?: appConfig.engine,
            baseUrl = appConfig.kontoregisterOrganisasjon.url,
            tokenProvider = cachedTokenProvider.withScope(appConfig.kontoregisterOrganisasjon.scope),
        )
    }
    single {
        ArenaAdapterService(
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
        )
    }
    single { TiltakshistorikkService(get(), get(), get(), get(), get()) }
    single { VeilederflateService(get(), get(), get(), get()) }
    single { BrukerService(get(), get(), get(), get(), get(), get()) }
    single { NavAnsattService(appConfig.auth.roles, get(), get()) }
    single { NavAnsattSyncService(appConfig.navAnsattSync, get(), get(), get(), get(), get()) }
    single { NavAnsattPrincipalService(get(), get()) }
    single { PoaoTilgangService(get()) }
    single { DelMedBrukerService(get(), get(), get()) }
    single {
        GjennomforingService(
            get(),
            get(),
            get(),
            get(),
        )
    }
    single { TiltakstypeService(get()) }
    single { NavEnheterSyncService(get(), get(), get(), get()) }
    single { NavEnhetService(get()) }
    single { ArrangorService(get(), get()) }
    single {
        UtbetalingService(
            UtbetalingService.Config(
                bestillingTopic = appConfig.kafka.clients.okonomiBestillingTopic,
            ),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
        )
    }
    single { UnleashService(appConfig.unleash, get()) }
    single<AxsysClient> {
        AxsysV2ClientImpl(
            appConfig.axsys.url,
        ) { runBlocking { cachedTokenProvider.withScope(appConfig.axsys.scope).exchange(AccessType.M2M) } }
    }
    single { AvtaleValidator(get(), get(), get(), get()) }
    single { GjennomforingValidator(get()) }
    single { OpsjonLoggService(get()) }
    single { LagretFilterService(get()) }
    single {
        TilsagnService(
            config = TilsagnService.Config(
                okonomiConfig = appConfig.okonomi,
                bestillingTopic = appConfig.kafka.clients.okonomiBestillingTopic,
            ),
            db = get(),
            notification = get(),
            navAnsattService = get(),
        )
    }
    single { AltinnRettigheterService(get(), get()) }
    single { OppgaverService(get()) }
    single { ArrangorFlateService(get(), get(), get()) }
    single { TotrinnskontrollService(get()) }
    single {
        ClamAvClient(
            baseUrl = appConfig.clamav.url,
            clientEngine = appConfig.engine,
        )
    }
}

private fun tasks(config: TaskConfig) = module {
    single { GenerateValidationReport(config.generateValidationReport, get(), get(), get()) }
    single { InitialLoadGjennomforinger(get(), get()) }
    single { InitialLoadTiltakstyper(get(), get(), get()) }
    single { SynchronizeNavAnsatte(config.synchronizeNavAnsatte, get(), get()) }
    single { SynchronizeUtdanninger(config.synchronizeUtdanninger, get(), get()) }
    single { GenerateUtbetaling(config.generateUtbetaling, get()) }
    single { JournalforUtbetaling(get(), get(), get(), get()) }
    single { NotificationTask(get()) }
    single { OppdaterUtbetalingBeregning(get()) }
    single {
        val updateGjennomforingStatus = UpdateGjennomforingStatus(
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
        )
        val updateApentForPamelding = UpdateApentForPamelding(config.updateApentForPamelding, get(), get())
        val notificationTask: NotificationTask by inject()
        val generateValidationReport: GenerateValidationReport by inject()
        val initialLoadGjennomforinger: InitialLoadGjennomforinger by inject()
        val initialLoadTiltakstyper: InitialLoadTiltakstyper by inject()
        val synchronizeNavAnsatte: SynchronizeNavAnsatte by inject()
        val synchronizeUtdanninger: SynchronizeUtdanninger by inject()
        val generateUtbetaling: GenerateUtbetaling by inject()
        val journalforUtbetaling: JournalforUtbetaling by inject()
        val oppdaterUtbetalingBeregning: OppdaterUtbetalingBeregning by inject()

        val db: Database by inject()

        Scheduler
            .create(
                db.getDatasource(),
                notificationTask.task,
                generateValidationReport.task,
                initialLoadGjennomforinger.task,
                initialLoadTiltakstyper.task,
                journalforUtbetaling.task,
                oppdaterUtbetalingBeregning.task,
            )
            .addSchedulerListener(SlackNotifierSchedulerListener(get()))
            .addSchedulerListener(OpenTelemetrySchedulerListener())
            .startTasks(
                synchronizeNorgEnheterTask.task,
                updateGjennomforingStatus.task,
                synchronizeNavAnsatte.task,
                synchronizeUtdanninger.task,
                notifySluttdatoForGjennomforingerNarmerSeg.task,
                notifySluttdatoForAvtalerNarmerSeg.task,
                notifyFailedKafkaEvents.task,
                updateApentForPamelding.task,
                generateUtbetaling.task,
            )
            .serializer(DbSchedulerKotlinSerializer())
            .registerShutdownHook()
            .build()
    }
}
