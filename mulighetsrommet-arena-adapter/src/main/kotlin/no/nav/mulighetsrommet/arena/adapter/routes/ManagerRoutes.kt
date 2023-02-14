package no.nav.mulighetsrommet.arena.adapter.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.arena.adapter.kafka.KafkaConsumerOrchestrator
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent
import no.nav.mulighetsrommet.arena.adapter.repositories.Topic
import no.nav.mulighetsrommet.arena.adapter.services.ArenaEventService
import no.nav.mulighetsrommet.arena.adapter.tasks.ReplayEvents
import org.koin.ktor.ext.inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun Route.managerRoutes() {
    val kafka: KafkaConsumerOrchestrator by inject()
    val arenaEventService: ArenaEventService by inject()

    val logger: Logger = LoggerFactory.getLogger(javaClass)

    singlePageApplication {
        useResources = true
        react("web")
    }
    get("/topics") {
        val topics = kafka.getTopics()
        call.respond(topics)
    }
    put("/topics") {
        val topics = call.receive<List<Topic>>()
        kafka.updateRunningTopics(topics)
        call.respond(HttpStatusCode.OK)
    }

    val replayEvents: ReplayEvents by inject()

    put("/events/replay") {
        val (table, status) = call.receive<ReplayEventsTaskData>()

        launch {
            try {
                arenaEventService.setReplayStatusForEvents(table = table, status = status)

                replayEvents.schedule()
            } catch (e: Throwable) {
                logger.error("Replaying of events failed with error: $e")
            }
        }

        call.respond(HttpStatusCode.Created)
    }

    put("/event/replay") {
        val request = call.receive<ReplayTopicEventRequest>()

        arenaEventService.replayEvent(
            table = request.table,
            id = request.arenaId
        )

        call.respond(HttpStatusCode.Created)
    }

    delete("/events") {
        val request = call.receive<DeleteEventsRequest>()

        arenaEventService.deleteEntities(
            table = request.table,
            ids = request.arenaIds
        )

        call.respond(HttpStatusCode.OK)
    }
}

@Serializable
data class ReplayTopicEventRequest(
    val table: String,
    val arenaId: String
)

@Serializable
data class DeleteEventsRequest(
    val table: String,
    val arenaIds: List<String>
)

@Serializable
data class ReplayEventsTaskData(
    val table: String,
    val status: ArenaEvent.ConsumptionStatus?
)
