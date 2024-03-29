package no.nav.mulighetsrommet.api.routes.internal

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.tasks.*
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
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

            post("generate-validation-report") {
                val taskId = generateValidationReport.schedule()

                call.respond(HttpStatusCode.Accepted, ScheduleTaskResponse(id = taskId))
            }

            post("initial-load-tiltaksgjennomforinger") {
                val input = InitialLoadTiltaksgjennomforingerInput(opphav = null)
                val taskId = initialLoadTiltaksgjennomforinger.schedule(input)

                call.respond(HttpStatusCode.Accepted, ScheduleTaskResponse(id = taskId))
            }

            post("initial-load-mulighetsrommet-tiltaksgjennomforinger") {
                val input = InitialLoadTiltaksgjennomforingerInput(opphav = ArenaMigrering.Opphav.MR_ADMIN_FLATE)
                val taskId = initialLoadTiltaksgjennomforinger.schedule(input)

                call.respond(HttpStatusCode.Accepted, ScheduleTaskResponse(id = taskId))
            }

            post("initial-load-mulighetsrommet-tiltakstyper") {
                val taskId = initialLoadTiltakstyper.schedule()

                call.respond(HttpStatusCode.Accepted, ScheduleTaskResponse(id = taskId))
            }

            post("sync-navansatte") {
                val taskId = synchronizeNavAnsatte.schedule()
                call.respond(HttpStatusCode.Accepted, ScheduleTaskResponse(id = taskId))
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
