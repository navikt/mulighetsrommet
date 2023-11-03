package no.nav.mulighetsrommet.api.routes.internal

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.tasks.GenerateValidationReport
import no.nav.mulighetsrommet.api.tasks.InitialLoadTiltaksgjennomforinger
import no.nav.mulighetsrommet.api.tasks.SynchronizeNavAnsatte
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import org.koin.ktor.ext.inject
import java.util.*

fun Route.tasks() {
    val generateValidationReport: GenerateValidationReport by inject()
    val initialLoadTiltaksgjennomforinger: InitialLoadTiltaksgjennomforinger by inject()
    val synchronizeNavAnsatte: SynchronizeNavAnsatte by inject()

    route("api/v1/internal/tasks") {
        post("generate-validation-report") {
            val taskId = generateValidationReport.schedule()

            call.respond(HttpStatusCode.Accepted, ScheduleTaskResponse(id = taskId))
        }

        post("initial-load-tiltaksgjennomforinger") {
            val taskId = initialLoadTiltaksgjennomforinger.schedule()

            call.respond(HttpStatusCode.Accepted, ScheduleTaskResponse(id = taskId))
        }

        post("sync-navansatte") {
            val taskId = synchronizeNavAnsatte.schedule()
            call.respond(HttpStatusCode.Accepted, ScheduleTaskResponse(id = taskId))
        }
    }
}

@Serializable
data class ScheduleTaskResponse(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
)
