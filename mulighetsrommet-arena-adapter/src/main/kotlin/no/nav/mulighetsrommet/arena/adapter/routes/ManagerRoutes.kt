package no.nav.mulighetsrommet.arena.adapter.routes

import io.ktor.http.*
import io.ktor.server.application.call
import io.ktor.server.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.arena.adapter.kafka.KafkaConsumerOrchestrator
import no.nav.mulighetsrommet.arena.adapter.repositories.Topic
import org.koin.ktor.ext.inject

fun Route.managerRoutes() {
    val kafka: KafkaConsumerOrchestrator by inject()
    singlePageApplication {
        useResources = true
        react("web")
    }
//    get("/*") {
//        call.respondRedirect("/" + call.request.uri)
//    }
    get("/topics") {
        val topics = kafka.getTopics()
        call.respond(topics)
    }
    put("/topics") {
        val topics = call.receive<List<Topic>>()
        kafka.updateRunningTopics(topics)
        call.respond(HttpStatusCode.OK)
    }
}
