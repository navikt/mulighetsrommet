package no.nav.mulighetsrommet.api.routes.internal

import io.ktor.http.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.gjennomforing.task.InitialLoadTiltaksgjennomforinger
import no.nav.mulighetsrommet.api.navansatt.task.SynchronizeNavAnsatte
import no.nav.mulighetsrommet.api.refusjon.task.GenerateRefusjonskrav
import no.nav.mulighetsrommet.api.tasks.*
import no.nav.mulighetsrommet.api.tiltakstype.task.InitialLoadTiltakstyper
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import no.nav.mulighetsrommet.kafka.KafkaConsumerOrchestrator
import no.nav.mulighetsrommet.kafka.Topic
import no.nav.mulighetsrommet.utdanning.task.SynchronizeUtdanninger
import org.koin.ktor.ext.inject
import java.time.LocalDate
import java.util.*

fun Route.maamRoutes() {
    route("/api/intern/maam") {
        route("/tasks") {
            val generateValidationReport: GenerateValidationReport by inject()
            val initialLoadTiltaksgjennomforinger: InitialLoadTiltaksgjennomforinger by inject()
            val initialLoadTiltakstyper: InitialLoadTiltakstyper by inject()
            val synchronizeNavAnsatte: SynchronizeNavAnsatte by inject()
            val synchronizeUtdanninger: SynchronizeUtdanninger by inject()
            val generateRefusjonskrav: GenerateRefusjonskrav by inject()

            post("generate-validation-report") {
                val taskId = generateValidationReport.schedule()

                call.respond(HttpStatusCode.Accepted, ScheduleTaskResponse(id = taskId))
            }

            post("initial-load-tiltaksgjennomforinger") {
                val input = call.receive<StartInitialLoadTiltaksgjennomforingRequest>()

                val taskInput = if (input.id != null) {
                    val ids = input.id.split(",").map { UUID.fromString(it.trim()) }
                    InitialLoadTiltaksgjennomforinger.Input(ids = ids)
                } else if (input.tiltakstyper != null) {
                    InitialLoadTiltaksgjennomforinger.Input(
                        tiltakskoder = input.tiltakstyper,
                        opphav = input.opphav,
                    )
                } else {
                    throw BadRequestException("Ugyldig input")
                }

                val taskId = initialLoadTiltaksgjennomforinger.schedule(taskInput)

                call.respond(HttpStatusCode.Accepted, ScheduleTaskResponse(id = taskId))
            }

            post("initial-load-tiltakstyper") {
                val taskId = initialLoadTiltakstyper.schedule()

                call.respond(HttpStatusCode.Accepted, ScheduleTaskResponse(id = taskId))
            }

            post("sync-navansatte") {
                val taskId = synchronizeNavAnsatte.schedule()
                call.respond(HttpStatusCode.Accepted, ScheduleTaskResponse(id = taskId))
            }

            post("sync-utdanning") {
                synchronizeUtdanninger.syncUtdanninger()
                call.respond(HttpStatusCode.OK, GeneralTaskResponse(id = "Synkronisering av utdanning.no OK"))
            }

            post("generate-refusjonskrav") {
                val (dayInMonth) = call.receive<GenerateRefusjonskravRequest>()
                generateRefusjonskrav.runTask(dayInMonth)
                call.respond(HttpStatusCode.OK, GeneralTaskResponse(id = "Generering av refusjonskrav OK"))
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
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
)

@Serializable
data class GeneralTaskResponse(
    val id: String,
)
