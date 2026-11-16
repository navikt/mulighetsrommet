package no.nav.mulighetsrommet.maam.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.mulighetsrommet.maam.MaamTasksConfig
import no.nav.mulighetsrommet.maam.TaskResult
import no.nav.mulighetsrommet.maam.dto.ExecutedTaskResponse
import no.nav.mulighetsrommet.maam.dto.ScheduleTaskResponse

internal fun Route.taskRoutes(config: MaamTasksConfig) {
    config.handlers.forEach { (name, execute) ->
        post(name) {
            when (val result = execute(call)) {
                is TaskResult.Scheduled -> call.respond(HttpStatusCode.Accepted, ScheduleTaskResponse(result.id))
                is TaskResult.Completed -> call.respond(HttpStatusCode.OK, ExecutedTaskResponse(result.message))
                is TaskResult.Rejected -> call.respond(HttpStatusCode.BadRequest, ExecutedTaskResponse(result.message))
                is TaskResult.Forbidden -> call.respond(HttpStatusCode.Forbidden, ExecutedTaskResponse(result.message))
            }
        }
    }
}
