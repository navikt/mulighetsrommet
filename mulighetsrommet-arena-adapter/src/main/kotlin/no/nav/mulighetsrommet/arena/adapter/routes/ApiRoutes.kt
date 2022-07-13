package no.nav.mulighetsrommet.arena.adapter.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.arena.adapter.services.TopicService

fun Route.apiRoutes(
    topicService: TopicService
) {
    get("api/topics") {
        val topics = topicService.getTopics()
        call.respond(topics)
    }

    put("api/topics") {
        val request = call.receive<ReplayTopicEventsRequest>()

        topicService.replayEvents(topic = request.topic)

        call.respond(HttpStatusCode.Created)
    }
}

@Serializable
data class ReplayTopicEventsRequest(
    val topic: String,
    val offset: Int? = null,
    val key: String? = null,
)
