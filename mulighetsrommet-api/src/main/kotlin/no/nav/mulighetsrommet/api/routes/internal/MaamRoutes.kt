package no.nav.mulighetsrommet.api.routes.internal

import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.arrangor.ArrangorService
import no.nav.mulighetsrommet.api.gjennomforing.task.InitialLoadGjennomforinger
import no.nav.mulighetsrommet.api.navansatt.task.SynchronizeNavAnsatte
import no.nav.mulighetsrommet.api.tilsagn.TilsagnService
import no.nav.mulighetsrommet.api.tilsagn.task.DistribuerTilsagnsbrev
import no.nav.mulighetsrommet.api.tilsagn.task.JournalforTilsagnsbrev
import no.nav.mulighetsrommet.api.tiltakstype.task.InitialLoadTiltakstyper
import no.nav.mulighetsrommet.api.utbetaling.UtbetalingService
import no.nav.mulighetsrommet.api.utbetaling.task.BeregnUtbetaling
import no.nav.mulighetsrommet.api.utbetaling.task.GenerateUtbetaling
import no.nav.mulighetsrommet.arena.ArenaMigrering
import no.nav.mulighetsrommet.kafka.KafkaConsumerOrchestrator
import no.nav.mulighetsrommet.kafka.Topic
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
    val arrangor: ArrangorService by inject()
    val tilsagnService: TilsagnService by inject()
    val utbetalingService: UtbetalingService by inject()

    val initialLoadGjennomforinger: InitialLoadGjennomforinger by inject()
    val initialLoadTiltakstyper: InitialLoadTiltakstyper by inject()
    val synchronizeNavAnsatte: SynchronizeNavAnsatte by inject()
    val synchronizeUtdanninger: SynchronizeUtdanninger by inject()
    val generateUtbetaling: GenerateUtbetaling by inject()
    val beregnUtbetaling: BeregnUtbetaling by inject()
    val journalforTilsagnsbrev: JournalforTilsagnsbrev by inject()
    val distribuerTilsagnsbrev: DistribuerTilsagnsbrev by inject()

    route("/api/intern/maam") {
        route("/tasks") {
            post("initial-load-gjennomforinger") {
                val request = call.receive<StartInitialLoadTiltaksgjennomforingRequest>()

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
                    throw BadRequestException("Ugyldig input")
                }

                call.respond(HttpStatusCode.Accepted, ScheduleTaskResponse(id = taskId))
            }

            post("initial-load-tiltakstyper") {
                val taskId = initialLoadTiltakstyper.schedule()

                call.respond(HttpStatusCode.Accepted, ScheduleTaskResponse(id = taskId))
            }

            post("republish-opprett-bestilling") {
                val params = call.receive<RepublishOpprettBestillingRequest>()

                val bestillinger = params.bestillingsnummer.split(",").map { it.trim() }
                val tilsagn = bestillinger.map { bestillingsnummer ->
                    tilsagnService.republishOpprettBestilling(bestillingsnummer)
                }

                val response = ExecutedTaskResponse("Republiserte ${tilsagn.size} tilsagn til økonomi")
                call.respond(HttpStatusCode.OK, response)
            }

            post("republish-opprett-faktura") {
                val params = call.receive<RepublishOpprettFakturaRequest>()

                val fakturaer = params.fakturanummer.split(",").map { it.trim() }
                val delutbetalinger = fakturaer.map { fakturanummer ->
                    utbetalingService.republishFaktura(fakturanummer)
                }

                val response = ExecutedTaskResponse("Republiserte ${delutbetalinger.size} fakturaer til økonomi")
                call.respond(HttpStatusCode.OK, response)
            }

            post("sync-navansatte") {
                val taskId = synchronizeNavAnsatte.schedule()
                call.respond(HttpStatusCode.Accepted, ScheduleTaskResponse(id = taskId))
            }

            post("sync-utdanning") {
                synchronizeUtdanninger.syncUtdanninger()
                call.respond(HttpStatusCode.OK, ExecutedTaskResponse("Synkronisering av utdanning.no OK"))
            }

            post("sync-arrangorer") {
                val input = call.receive<SynchronizeArrangorerRequest>()

                input.organisasjonsnummer
                    .split(",")
                    .map { Organisasjonsnummer(it.trim()) }
                    .forEach { arrangor.syncArrangorFromBrreg(it) }

                call.respond(HttpStatusCode.OK, ExecutedTaskResponse("Synkronisert! :)"))
            }

            post("generate-utbetaling") {
                val request = call.receive<GenerateUtbetalingRequest>()
                val periode = Periode.forMonthOf(request.date)
                val utbetalinger = generateUtbetaling.runTask(periode)
                val response = ExecutedTaskResponse("Genererte ${utbetalinger.size} utbetalinger for periode $periode")
                call.respond(HttpStatusCode.OK, response)
            }

            post("beregn-utbetaling") {
                val request = call.receive<BeregnUtbetalingRequest>()
                val periode = Periode.forMonthOf(request.date)
                val id = beregnUtbetaling.schedule(BeregnUtbetaling.Input(periode))
                val response = ScheduleTaskResponse(id)
                call.respond(HttpStatusCode.Accepted, response)
            }

            post("journalfor-tilsagnsbrev") {
                val request = call.receive<TilsagnIdRequest>()
                val taskId = journalforTilsagnsbrev.schedule(request.tilsagnId)
                call.respond(ScheduleTaskResponse(taskId))
            }

            post("distribuer-tilsagnsbrev") {
                val request = call.receive<TilsagnIdRequest>()
                val taskId = distribuerTilsagnsbrev.schedule(request.tilsagnId)
                call.respond(ScheduleTaskResponse(taskId))
            }
        }

        route("/topics") {
            val kafka: KafkaConsumerOrchestrator by inject()

            get {
                val topics = kafka.getTopics()
                call.respond(topics)
            }

            put {
                val topics = call.receive<List<Topic>>()
                kafka.updateRunningTopics(topics)
                call.respond(HttpStatusCode.OK)
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
data class BeregnUtbetalingRequest(
    @Serializable(with = LocalDateSerializer::class)
    val date: LocalDate,
)

@Serializable
data class StartInitialLoadTiltaksgjennomforingRequest(
    val id: String? = null,
    val tiltakstyper: List<Tiltakskode>? = null,
    val opphav: ArenaMigrering.Opphav? = null,
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
data class ScheduleTaskResponse(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
)

@Serializable
data class ExecutedTaskResponse(
    val message: String,
)

@Serializable
data class TilsagnIdRequest(
    @Serializable(with = UUIDSerializer::class)
    val tilsagnId: UUID,
)
