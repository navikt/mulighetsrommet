package no.nav.mulighetsrommet.arena.adapter.routes

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.arena.adapter.services.TopicService

fun Route.apiRoutes(
    topicService: TopicService
) {
    get("api/topics") {
        val topics = topicService.getTopics()
        call.respond(topics)
    }
}
