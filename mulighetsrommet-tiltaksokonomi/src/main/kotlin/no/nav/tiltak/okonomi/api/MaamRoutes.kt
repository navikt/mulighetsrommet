package no.nav.tiltak.okonomi.api

import io.ktor.http.*
import io.ktor.resources.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.resources.put
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.kafka.KafkaConsumerOrchestrator
import no.nav.mulighetsrommet.kafka.Topic

@Resource("/maam")
class Maam {

    @Resource("/topics")
    class Topics(val parent: Maam = Maam())
}

fun Routing.maamRoutes(
    kafka: KafkaConsumerOrchestrator,
) = authenticate {
    get<Maam.Topics> {
        val topics = kafka.getTopics()
        call.respond(topics)
    }

    put<Maam.Topics> {
        val topics = call.receive<List<Topic>>()
        kafka.updateRunningTopics(topics)
        call.respond(HttpStatusCode.OK)
    }
}
