package no.nav.mulighetsrommet.api.routes.internal

import io.ktor.http.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.arrangor.ArrangorService
import no.nav.mulighetsrommet.api.gjennomforing.task.InitialLoadGjennomforinger
import no.nav.mulighetsrommet.api.navansatt.task.SynchronizeNavAnsatte
import no.nav.mulighetsrommet.api.tasks.GenerateValidationReport
import no.nav.mulighetsrommet.api.tilsagn.TilsagnService
import no.nav.mulighetsrommet.api.tiltakstype.task.InitialLoadTiltakstyper
import no.nav.mulighetsrommet.api.utbetaling.UtbetalingService
import no.nav.mulighetsrommet.api.utbetaling.task.GenerateUtbetaling
import no.nav.mulighetsrommet.arena.ArenaMigrering
import no.nav.mulighetsrommet.kafka.KafkaConsumerOrchestrator
import no.nav.mulighetsrommet.kafka.Topic
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import no.nav.mulighetsrommet.utdanning.task.SynchronizeUtdanninger
import org.koin.ktor.ext.inject
import java.util.*

fun Route.maamRoutes() {
    val arrangor: ArrangorService by inject()
    val tilsagnService: TilsagnService by inject()
    val utbetalingService: UtbetalingService by inject()

    val generateValidationReport: GenerateValidationReport by inject()
    val initialLoadGjennomforinger: InitialLoadGjennomforinger by inject()
    val initialLoadTiltakstyper: InitialLoadTiltakstyper by inject()
    val synchronizeNavAnsatte: SynchronizeNavAnsatte by inject()
    val synchronizeUtdanninger: SynchronizeUtdanninger by inject()
    val generateUtbetaling: GenerateUtbetaling by inject()

    route("/api/intern/maam") {
        route("/tasks") {
            post("generate-validation-report") {
                val taskId = generateValidationReport.schedule()

                call.respond(HttpStatusCode.Accepted, ScheduleTaskResponse(id = taskId))
            }

            post("initial-load-gjennomforinger") {
                val input = call.receive<StartInitialLoadTiltaksgjennomforingRequest>()

                val taskInput = if (input.id != null) {
                    val ids = input.id.split(",").map { UUID.fromString(it.trim()) }
                    InitialLoadGjennomforinger.Input(ids = ids)
                } else if (input.tiltakstyper != null) {
                    InitialLoadGjennomforinger.Input(tiltakskoder = input.tiltakstyper)
                } else {
                    throw BadRequestException("Ugyldig input")
                }

                val taskId = initialLoadGjennomforinger.schedule(taskInput)

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
                val (month) = call.receive<GenerateUtbetalingRequest>()
                val utbetalinger = generateUtbetaling.runTask(month)
                val response = ExecutedTaskResponse("Genererte ${utbetalinger.size} utbetalinger for måned $month")
                call.respond(HttpStatusCode.OK, response)
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
    val month: Int,
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
