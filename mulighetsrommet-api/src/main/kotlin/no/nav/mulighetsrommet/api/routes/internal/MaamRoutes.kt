package no.nav.mulighetsrommet.api.routes.internal

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.gjennomforing.task.InitialLoadTiltaksgjennomforinger
import no.nav.mulighetsrommet.api.refusjon.task.GenerateRefusjonskrav
import no.nav.mulighetsrommet.api.tasks.*
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.kafka.KafkaConsumerOrchestrator
import no.nav.mulighetsrommet.kafka.Topic
import org.koin.ktor.ext.inject
import java.time.Instant
import java.time.LocalDate
import java.util.*

fun Route.maamRoutes() {
    route("/api/intern/maam") {
        route("/tasks") {
            val dbSchedulerClient: DbSchedulerClient by inject()

            post("generate-validation-report") {
                val taskId = dbSchedulerClient.scheduleGenerateValidationReport(Instant.now())

                call.respond(HttpStatusCode.Accepted, ScheduleTaskResponse(id = taskId))
            }

            post("initial-load-tiltaksgjennomforinger") {
                val input = call.receive<StartInitialLoadTiltaksgjennomforingRequest>()

                val taskInput = if (input.id != null) {
                    val ids = input.id.split(",").map { UUID.fromString(it.trim()) }
                    InitialLoadTiltaksgjennomforinger.TaskInput(ids = ids)
                } else if (input.tiltakstyper != null) {
                    InitialLoadTiltaksgjennomforinger.TaskInput(
                        tiltakskoder = input.tiltakstyper,
                        opphav = input.opphav,
                    )
                } else {
                    throw BadRequestException("Ugyldig input")
                }

                val taskId = dbSchedulerClient.scheduleInitialLoadTiltaksgjennomforinger(taskInput, startTime = Instant.now())

                call.respond(HttpStatusCode.Accepted, ScheduleTaskResponse(id = taskId))
            }

            post("initial-load-tiltakstyper") {
                val taskId = dbSchedulerClient.scheduleInitialLoadTiltakstyper(Instant.now())

                call.respond(HttpStatusCode.Accepted, ScheduleTaskResponse(id = taskId))
            }

            post("sync-navansatte") {
                val taskId = dbSchedulerClient.scheduleSynchronizeNavAnsatte(Instant.now())
                call.respond(HttpStatusCode.Accepted, ScheduleTaskResponse(id = taskId))
            }

            post("sync-utdanning") {
                val taskId = dbSchedulerClient.scheduleSynchronizeUtdanninger(Instant.now())
                call.respond(HttpStatusCode.OK, GeneralTaskResponse(id = taskId))
            }

            post("generate-refusjonskrav") {
                val (dayInMonth) = call.receive<GenerateRefusjonskravRequest>()
                val taskId = dbSchedulerClient.scheduleGenerateRefusjonskrav(GenerateRefusjonskrav.TaskInput(dayInMonth), Instant.now())
                call.respond(HttpStatusCode.OK, GeneralTaskResponse(id = taskId))
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
data class GenerateRefusjonskravRequest(
    @Serializable(with = LocalDateSerializer::class)
    val dayInMonth: LocalDate,
)

@Serializable
data class StartInitialLoadTiltaksgjennomforingRequest(
    val id: String? = null,
    val tiltakstyper: List<Tiltakskode>? = null,
    val opphav: ArenaMigrering.Opphav? = null,
)

@Serializable
data class ScheduleTaskResponse(
    val id: String,
)

@Serializable
data class GeneralTaskResponse(
    val id: String,
)
