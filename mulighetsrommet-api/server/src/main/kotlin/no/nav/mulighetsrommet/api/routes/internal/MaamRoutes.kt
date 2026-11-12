package no.nav.mulighetsrommet.api.routes.internal

import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.admin.arrangor.SyncArrangor
import no.nav.mulighetsrommet.admin.arrangor.SyncArrangorUseCase
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.arrangorflate.service.ArrangorflateUtbetalingService
import no.nav.mulighetsrommet.api.brukerutbetaling.BrukerUtbetalingService
import no.nav.mulighetsrommet.api.gjennomforing.task.InitialLoadGjennomforinger
import no.nav.mulighetsrommet.api.gjennomforing.task.UpdateGjennomforingAvtaleFreeTextSearch
import no.nav.mulighetsrommet.api.navansatt.task.SynchronizeNavAnsatte
import no.nav.mulighetsrommet.api.tilsagn.TilsagnService
import no.nav.mulighetsrommet.api.tilskuddbehandling.task.DistribuerVedtaksbrev
import no.nav.mulighetsrommet.api.tiltakstype.task.InitialLoadTiltakstyper
import no.nav.mulighetsrommet.api.utbetaling.service.UtbetalingService
import no.nav.mulighetsrommet.api.utbetaling.task.BeregnUtbetaling
import no.nav.mulighetsrommet.api.utbetaling.task.GenerateUtbetaling
import no.nav.mulighetsrommet.kafka.KafkaConsumerOrchestrator
import no.nav.mulighetsrommet.maam.Maam
import no.nav.mulighetsrommet.maam.TaskResult
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import no.nav.mulighetsrommet.utdanning.task.SynchronizeUtdanninger
import org.koin.ktor.ext.inject
import java.time.LocalDate
import java.util.UUID

fun Route.maamRoutes() {
    val db: ApiDatabase by inject()
    val kafka: KafkaConsumerOrchestrator by inject()
    val arrangor: SyncArrangorUseCase by inject()
    val tilsagnService: TilsagnService by inject()
    val utbetalingService: UtbetalingService by inject()
    val arrangorflateUtbetalingService: ArrangorflateUtbetalingService by inject()

    val initialLoadGjennomforinger: InitialLoadGjennomforinger by inject()
    val initialLoadTiltakstyper: InitialLoadTiltakstyper by inject()
    val synchronizeNavAnsatte: SynchronizeNavAnsatte by inject()
    val synchronizeUtdanninger: SynchronizeUtdanninger by inject()
    val generateUtbetaling: GenerateUtbetaling by inject()
    val beregnUtbetaling: BeregnUtbetaling by inject()
    val distribuerVedtaksbrev: DistribuerVedtaksbrev by inject()
    val updateGjennomforingAvtaleFreeTextSearch: UpdateGjennomforingAvtaleFreeTextSearch by inject()
    val brukerUtbetalingService: BrukerUtbetalingService by inject()

    route("/api/intern/maam") {
        install(Maam) {
            database = { db.db }

            kafka(orchestrator = { kafka }) {
                consumers()
                failedRecords()
            }

            tasks {
                scheduled()

                task<StartInitialLoadTiltaksgjennomforingRequest>("initial-load-gjennomforinger") { request ->
                    val taskId = if (request.id != null) {
                        val ids = request.id.split(",").map { UUID.fromString(it.trim()) }
                        initialLoadGjennomforinger.schedule(InitialLoadGjennomforinger.Input(ids = ids))
                    } else if (request.tiltakstyper != null) {
                        request.tiltakstyper
                            .map { tiltakskode ->
                                val input = InitialLoadGjennomforinger.Input(tiltakskode = tiltakskode)
                                initialLoadGjennomforinger.schedule(input)
                            }
                            .first()
                    } else {
                        return@task TaskResult.Rejected("Ugyldig input")
                    }

                    TaskResult.Scheduled(taskId)
                }

                task("initial-load-tiltakstyper") {
                    TaskResult.Scheduled(initialLoadTiltakstyper.schedule())
                }

                task<RepublishOpprettBestillingRequest>("republish-opprett-bestilling") { params ->
                    val bestillinger = params.bestillingsnummer.split(",").map { it.trim() }
                    val tilsagn = bestillinger.map { bestillingsnummer ->
                        tilsagnService.republishOpprettBestilling(bestillingsnummer)
                    }

                    TaskResult.Completed("Republiserte ${tilsagn.size} tilsagn til økonomi")
                }

                task<RepublishOpprettFakturaRequest>("republish-opprett-faktura") { params ->
                    val fakturaer = params.fakturanummer.split(",").map { it.trim() }
                    val utbetalingLinjer = fakturaer.map { fakturanummer ->
                        db.transaction { utbetalingService.republishFaktura(fakturanummer) }
                    }

                    TaskResult.Completed("Republiserte ${utbetalingLinjer.size} fakturaer til økonomi")
                }

                task("sync-navansatte") {
                    TaskResult.Scheduled(synchronizeNavAnsatte.schedule())
                }

                task("sync-utdanning") {
                    synchronizeUtdanninger.syncUtdanninger()
                    TaskResult.Completed("Synkronisering av utdanning.no OK")
                }

                task<SynchronizeArrangorerRequest>("sync-arrangorer") { input ->
                    input.organisasjonsnummer
                        .split(",")
                        .map { Organisasjonsnummer(it.trim()) }
                        .forEach { arrangor.execute(SyncArrangor(it)) }

                    TaskResult.Completed("Synkronisert! :)")
                }

                task<GenerateUtbetalingRequest>("generate-utbetaling") { request ->
                    val periode = Periode.forMonthOf(request.date)
                    val utbetalinger = generateUtbetaling.runTask(periode)
                    TaskResult.Completed("Genererte ${utbetalinger.size} utbetalinger for periode $periode")
                }

                task<RegenererUtbetalingRequest>("regenerer-utbetaling") { request ->
                    val utbetaling = arrangorflateUtbetalingService.getOrError(request.utbetalingId)

                    arrangorflateUtbetalingService.regenererUtbetaling(utbetaling).fold(
                        { errors ->
                            TaskResult.Rejected(
                                errors.joinToString(prefix = "Utbetalingen kan ikke regenereres: ") { it.detail },
                            )
                        },
                        {
                            TaskResult.Completed(
                                "Regenererte utbetaling med id=${request.utbetalingId} på vegne av arrangør",
                            )
                        },
                    )
                }

                task<BeregnUtbetalingRequest>("beregn-utbetaling") { request ->
                    val periode = Periode.forMonthOf(request.date)
                    TaskResult.Scheduled(beregnUtbetaling.schedule(BeregnUtbetaling.Input(periode)))
                }

                task<VedtakIdRequest>("distribuer-vedtaksbrev") { request ->
                    TaskResult.Scheduled(distribuerVedtaksbrev.schedule(request.vedtakId))
                }

                task("sync-gjennomforing-avtale-fts") {
                    TaskResult.Scheduled(updateGjennomforingAvtaleFreeTextSearch.schedule())
                }

                task("hel-ved-utbetaling") {
                    val utbetalingId = brukerUtbetalingService.produceTilskuddUtbetalingTest()
                    if (utbetalingId == null) {
                        TaskResult.Forbidden("Hel Ved-testutbetaling er ikke tillatt i produksjon")
                    } else {
                        TaskResult.Completed("Produserte hel ved utbetaling med id=$utbetalingId")
                    }
                }
            }
        }
    }
}

@Serializable
data class GenerateUtbetalingRequest(
    @Serializable(with = LocalDateSerializer::class)
    val date: LocalDate,
)

@Serializable
data class RegenererUtbetalingRequest(
    @Serializable(with = UUIDSerializer::class)
    val utbetalingId: UUID,
)

@Serializable
data class BeregnUtbetalingRequest(
    @Serializable(with = LocalDateSerializer::class)
    val date: LocalDate,
)

@Serializable
data class StartInitialLoadTiltaksgjennomforingRequest(
    val id: String? = null,
    val tiltakstyper: List<Tiltakskode>? = null,
)

@Serializable
data class RepublishOpprettBestillingRequest(
    val bestillingsnummer: String,
)

@Serializable
data class RepublishOpprettFakturaRequest(
    val fakturanummer: String,
)

@Serializable
data class SynchronizeArrangorerRequest(
    val organisasjonsnummer: String,
)

@Serializable
data class VedtakIdRequest(
    @Serializable(with = UUIDSerializer::class)
    val vedtakId: UUID,
)
