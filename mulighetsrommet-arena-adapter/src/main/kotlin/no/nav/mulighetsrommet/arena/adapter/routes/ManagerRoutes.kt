package no.nav.mulighetsrommet.arena.adapter.routes

import io.ktor.http.*
import io.ktor.server.application.call
import io.ktor.server.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.arena.adapter.services.TopicService

fun Route.managerRoutes(topicService: TopicService) {
    singlePageApplication {
        applicationRoute = "manager"
        useResources = true
        react("web/dist")
    }
    get("/assets/*") {
        call.respondRedirect("/manager" + call.request.uri)
    }
    get("/manager/topics") {
        val topics = topicService.getTopics()
        call.respond(topics)
    }
    put("/manager/topics") {
        val topics = call.receive<List<TopicService.Topic>>()
        topicService.updateRunningStateByTopics(topics)
        call.respond(HttpStatusCode.OK, "Alt gikk faen meg fint")
    }
}
