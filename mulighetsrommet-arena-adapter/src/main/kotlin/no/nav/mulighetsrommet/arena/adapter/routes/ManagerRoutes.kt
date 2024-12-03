package no.nav.mulighetsrommet.arena.adapter.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTable
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEntityMapping
import no.nav.mulighetsrommet.arena.adapter.services.ArenaEventService
import no.nav.mulighetsrommet.arena.adapter.tasks.ReplayEvents
import no.nav.mulighetsrommet.kafka.KafkaConsumerOrchestrator
import no.nav.mulighetsrommet.kafka.Topic
import org.koin.ktor.ext.inject

fun Route.managerRoutes() {
    val kafka: KafkaConsumerOrchestrator by inject()
    val arenaEventService: ArenaEventService by inject()
    val replayEvents: ReplayEvents by inject()

    get("/topics") {
        val topics = kafka.getTopics()
        call.respond(topics)
    }

    put("/topics") {
        val topics = call.receive<List<Topic>>()
        kafka.updateRunningTopics(topics)
        call.respond(HttpStatusCode.OK)
    }

    get("/arena-tables") {
        call.respond(ArenaTable.entries.toTypedArray())
    }

    put("/events/replay") {
        val (table, status) = call.receive<ReplayEventsTaskData>()

        CoroutineScope(Job()).launch {
            try {
                arenaEventService.setReplayStatusForEvents(table = table, status = status)

                replayEvents.schedule()
            } catch (e: Throwable) {
                application.log.error("Failed to schedule task ${replayEvents.task.name}", e)
            }
        }

        call.respond(HttpStatusCode.Created)
    }

    put("/event/replay") {
        val request = call.receive<ReplayTopicEventRequest>()

        arenaEventService.replayEvent(
            table = request.table,
            id = request.arenaId,
        )

        call.respond(HttpStatusCode.Created)
    }

    delete("/events") {
        val request = call.receive<DeleteEventsRequest>()

        arenaEventService.deleteEntities(
            table = request.table,
            ids = request.arenaIds,
        )

        call.respond(HttpStatusCode.OK)
    }
}

@Serializable
data class ReplayTopicEventRequest(
    val table: ArenaTable,
    val arenaId: String,
)

@Serializable
data class DeleteEventsRequest(
    val table: ArenaTable,
    val arenaIds: List<String>,
)

@Serializable
data class ReplayEventsTaskData(
    val table: ArenaTable,
    val status: ArenaEntityMapping.Status,
)
