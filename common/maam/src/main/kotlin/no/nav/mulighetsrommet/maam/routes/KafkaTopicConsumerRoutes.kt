package no.nav.mulighetsrommet.maam.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import no.nav.mulighetsrommet.kafka.KafkaConsumerOrchestrator
import no.nav.mulighetsrommet.kafka.Topic
import no.nav.mulighetsrommet.kafka.TopicType
import no.nav.mulighetsrommet.maam.dto.KafkaTopicConsumerDto

internal fun Route.kafkaTopicConsumerRoutes(kafka: () -> KafkaConsumerOrchestrator) {
    route("/consumers") {
        get {
            val consumers = kafka().getTopics().map { it.toDto() }
            call.respond(consumers)
        }

        put {
            val consumers = call.receive<List<KafkaTopicConsumerDto>>()
            kafka().updateRunningTopics(consumers.map { it.toTopic() })
            call.respond(HttpStatusCode.OK)
        }
    }
}

private fun Topic.toDto() = KafkaTopicConsumerDto(
    id = id,
    topic = topic,
    running = running,
)

private fun KafkaTopicConsumerDto.toTopic() = Topic(
    id = id,
    topic = topic,
    type = TopicType.CONSUMER,
    running = running,
)
