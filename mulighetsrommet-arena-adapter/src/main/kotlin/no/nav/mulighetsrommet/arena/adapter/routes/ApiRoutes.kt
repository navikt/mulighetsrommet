package no.nav.mulighetsrommet.arena.adapter.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.arena.adapter.jobs.JobRunners
import no.nav.mulighetsrommet.arena.adapter.services.TopicService
import no.nav.mulighetsrommet.domain.DateSerializer
import java.time.LocalDateTime

fun Route.apiRoutes(topicService: TopicService) {
    put("api/topics/replay") {
        val request = call.receive<ReplayTopicEventsRequest>()

        JobRunners.executeBackgroundJob {
            topicService.replayEvents(topic = request.topic, createdAfter = request.createdAfter)
        }

        call.respond(HttpStatusCode.Created)
    }
}

@Serializable
data class ReplayTopicEventsRequest(
    val topic: String,
    @Serializable(DateSerializer::class)
    val createdAfter: LocalDateTime? = null,
)
