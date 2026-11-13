package no.nav.mulighetsrommet.maam.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.queries.ScheduledTaskDbo
import no.nav.mulighetsrommet.database.queries.ScheduledTaskQueries
import no.nav.mulighetsrommet.env.NaisEnv
import no.nav.mulighetsrommet.maam.dto.DeleteScheduledTaskRequest
import no.nav.mulighetsrommet.maam.dto.RetryScheduledTaskRequest
import no.nav.mulighetsrommet.maam.dto.ScheduledTaskDto
import java.time.ZoneOffset.UTC

internal fun Route.scheduledTaskRoutes(db: () -> Database, environment: NaisEnv) {
    route("/scheduled") {
        get("/failed") {
            val tasks = db().session { ScheduledTaskQueries(it).getFailedTasks() }
            call.respond(tasks.map { it.toDto() })
        }

        post("/retry") {
            val request = call.receive<RetryScheduledTaskRequest>()
            db().transaction {
                ScheduledTaskQueries(it).retryAt(
                    request.taskName,
                    request.taskInstance,
                    request.executionTime.toInstant(UTC),
                )
            }
            call.respond(HttpStatusCode.OK)
        }

        delete {
            if (environment.isProdGCP()) {
                return@delete call.respond(HttpStatusCode.Forbidden)
            }
            val request = call.receive<DeleteScheduledTaskRequest>()
            val deleted = db().transaction {
                ScheduledTaskQueries(it).deleteFailedTask(request.taskName, request.taskInstance)
            }
            if (deleted == 0) {
                call.respond(HttpStatusCode.Conflict, "Task is no longer failed, is running, or does not exist")
            } else {
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

private fun ScheduledTaskDbo.toDto() = ScheduledTaskDto(
    taskName = taskName,
    taskInstance = taskInstance,
    taskData = taskData?.decodeToString(),
    executionTime = executionTime.toString(),
    picked = picked,
    pickedBy = pickedBy,
    lastSuccess = lastSuccess?.toString(),
    lastFailure = lastFailure?.toString(),
    consecutiveFailures = consecutiveFailures,
    lastHeartbeat = lastHeartbeat?.toString(),
    version = version,
    priority = priority,
)
