package no.nav.mulighetsrommet.arena.adapter.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.arena.adapter.jobs.JobRunners
import no.nav.mulighetsrommet.arena.adapter.services.TopicService
import org.koin.ktor.ext.inject

fun Route.apiRoutes() {
    val topicService: TopicService by inject()

    put("api/topics/replay") {
        val request = call.receive<ReplayTopicEventsRequest>()

        JobRunners.executeBackgroundJob {
            topicService.replayEvents(topic = request.topic, id = request.id)
        }

        call.respond(HttpStatusCode.Created)
    }

    put("api/topics/replay/{id}") {
        val id = call.request.queryParameters["id"]?.toInt() ?: return@put call.respond(
            HttpStatusCode.BadRequest,
            "'id' must be specified"
        )

        val event = topicService.replayEvent(id)
        if (event != null) {
            call.respond(HttpStatusCode.OK)
        } else {
            call.respond(HttpStatusCode.NotFound)
        }
    }
}

@Serializable
data class ReplayTopicEventsRequest(
    val topic: String,
    val id: Int? = null,
)
