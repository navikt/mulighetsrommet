package no.nav.mulighetsrommet.api.routes.internal

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.api.tasks.GenerateValidationReport
import no.nav.mulighetsrommet.api.tasks.InitialLoadTiltaksgjennomforinger
import org.koin.ktor.ext.inject

fun Route.tasks() {
    val generateValidationReport: GenerateValidationReport by inject()
    val initialLoadTiltaksgjennomforinger: InitialLoadTiltaksgjennomforinger by inject()

    route("api/v1/internal/tasks") {
        post("generate-validation-report") {
            val taskId = generateValidationReport.schedule()

            call.respond(HttpStatusCode.Accepted, taskId.toString())
        }

        post("initial-load-tiltaksgjennomforinger") {
            val taskId = initialLoadTiltaksgjennomforinger.schedule()

            call.respond(HttpStatusCode.Accepted, taskId.toString())
        }
    }
}
