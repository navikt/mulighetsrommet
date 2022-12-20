package no.nav.mulighetsrommet.arena.adapter.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.arena.adapter.jobs.JobRunners
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent
import no.nav.mulighetsrommet.arena.adapter.services.ArenaEventService
import org.koin.ktor.ext.inject

fun Route.apiRoutes() {
    val arenaEventService: ArenaEventService by inject()

    put("api/topics/replay") {
        val request = call.receive<ReplayTopicEventsRequest>()

        JobRunners.executeBackgroundJob {
            arenaEventService.replayEvents(
                table = request.table,
                status = request.status?.let { ArenaEvent.ConsumptionStatus.valueOf(it) }
            )
        }

        call.respond(HttpStatusCode.Created)
    }

    put("api/topics/replay/{id}") {
        val arenaId = call.parameters["id"] ?: return@put call.respondText(
            "Mangler eller ugyldig arenaId",
            status = HttpStatusCode.BadRequest
        )

        val table = call.request.queryParameters["table"] ?: return@put call.respondText(
            "Mangler eller ugyldig arenatabell",
            status = HttpStatusCode.BadRequest
        )

        JobRunners.executeBackgroundJob {
            arenaEventService.replayEvent(
                table = table,
                id = arenaId
            )
        }

        call.respond(HttpStatusCode.Created)
    }
}

@Serializable
data class ReplayTopicEventsRequest(
    val table: String?,
    val status: String?
)
