package no.nav.mulighetsrommet.api.routes.internal

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.clients.ssb.SsbNusClient
import no.nav.mulighetsrommet.api.tasks.GenerateValidationReport
import no.nav.mulighetsrommet.api.tasks.InitialLoadTiltaksgjennomforinger
import no.nav.mulighetsrommet.api.tasks.InitialLoadTiltakstyper
import no.nav.mulighetsrommet.api.tasks.SynchronizeNavAnsatte
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import no.nav.mulighetsrommet.kafka.KafkaConsumerOrchestrator
import no.nav.mulighetsrommet.kafka.Topic
import org.koin.ktor.ext.inject
import java.util.*

fun Route.maamRoutes() {
    route("/api/internal/maam") {
        route("/tasks") {
            val generateValidationReport: GenerateValidationReport by inject()
            val initialLoadTiltaksgjennomforinger: InitialLoadTiltaksgjennomforinger by inject()
            val initialLoadTiltakstyper: InitialLoadTiltakstyper by inject()
            val synchronizeNavAnsatte: SynchronizeNavAnsatte by inject()
            val ssbNusClient: SsbNusClient by inject()

            post("generate-validation-report") {
                val taskId = generateValidationReport.schedule()

                call.respond(HttpStatusCode.Accepted, ScheduleTaskResponse(id = taskId))
            }

            post("initial-load-tiltaksgjennomforinger") {
                val input = call.receive<InitialLoadTiltaksgjennomforinger.Input>()
                val taskId = initialLoadTiltaksgjennomforinger.schedule(input)

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

            post("sync-nusdata") {
                val input = call.receive<SsbNusClient.Input>()
                val nusData = ssbNusClient.fetchNusData(input.version)
                // Upsert to db table
                call.respond(HttpStatusCode.Accepted, GeneralTaskResponse(id = "NUS data synced for version ${input.version}"))
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
data class ScheduleTaskResponse(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
)

@Serializable
data class GeneralTaskResponse(
    val id: String,
)
