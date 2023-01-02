package no.nav.mulighetsrommet.arena.adapter.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.arena.adapter.services.ArenaEventService
import no.nav.mulighetsrommet.arena.adapter.tasks.ReplayEvents
import no.nav.mulighetsrommet.arena.adapter.tasks.ReplayEventsTaskData
import org.koin.ktor.ext.inject

fun Route.apiRoutes() {
    val arenaEventService: ArenaEventService by inject()
    val replayEvents: ReplayEvents by inject()

    put("api/events/replay") {
        val request = call.receive<ReplayEventsTaskData>()

        replayEvents.schedule(request)

        call.respond(HttpStatusCode.Created)
    }

    put("api/event/replay") {
        val request = call.receive<ReplayTopicEventRequest>()

        arenaEventService.replayEvent(
            table = request.table,
            id = request.arenaId
        )

        call.respond(HttpStatusCode.Created)
    }
}

@Serializable
data class ReplayTopicEventRequest(
    val table: String,
    val arenaId: String
)
