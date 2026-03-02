package no.nav.tiltak.okonomi.api

import io.ktor.http.HttpStatusCode
import io.ktor.resources.Resource
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.resources.get
import io.ktor.server.resources.put
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.queries.KafkaConsumerRecordDbo
import no.nav.mulighetsrommet.database.queries.KafkaConsumerRecordQueries
import no.nav.mulighetsrommet.database.queries.ScheduledTaskDbo
import no.nav.mulighetsrommet.kafka.KafkaConsumerOrchestrator
import no.nav.mulighetsrommet.kafka.Topic

@Resource("/maam")
class Maam {

    @Resource("/topics")
    class Topics(val parent: Maam = Maam()) {

        @Resource("/failed-records")
        class FailedConsumerRecords(val parent: Topics = Topics())
    }
}

fun Routing.maamRoutes(
    kafka: KafkaConsumerOrchestrator,
    db: Database,
) = authenticate {
    get<Maam.Topics> {
        val topics = kafka.getTopics()
        call.respond(topics)
    }

    get<Maam.Topics.FailedConsumerRecords> {
        val failedRecords = db.session { KafkaConsumerRecordQueries(it).getFailedRecords() }
            .map { it.toDto() }
        call.respond(failedRecords)
    }

    put<Maam.Topics> {
        val topics = call.receive<List<Topic>>()
        kafka.updateRunningTopics(topics)
        call.respond(HttpStatusCode.OK)
    }
}

@Serializable
data class ScheduledTaskDto(
    val taskName: String,
    val taskInstance: String,
    val taskData: String,
    val executionTime: String,
    val picked: Boolean,
    val pickedBy: String?,
    val lastSuccess: String?,
    val lastFailure: String?,
    val consecutiveFailures: Int,
    val lastHeartbeat: String?,
    val version: Long,
    val priority: Short?,
)

fun ScheduledTaskDbo.toDto(): ScheduledTaskDto {
    return ScheduledTaskDto(
        taskName = taskName,
        taskInstance = taskInstance,
        taskData = taskData.decodeToString(),
        executionTime = executionTime.toString(),
        picked = picked,
        pickedBy = pickedBy,
        lastSuccess = lastSuccess?.toString(),
        lastFailure = lastFailure?.toString(),
        consecutiveFailures = consecutiveFailures,
        lastHeartbeat = lastHeartbeat?.toString(),
        version = version,
        priority = priority,
    )
}

@Serializable
data class KafkaConsumerRecordDto(
    val id: Long,
    val topic: String,
    val partition: Int,
    val recordOffset: Long,
    val retries: Int,
    val lastRetry: String?,
    val key: String?,
    val value: String?,
    val headersJson: String?,
    val recordTimestamp: String?,
    val createdAt: String,
)

fun KafkaConsumerRecordDbo.toDto(): KafkaConsumerRecordDto {
    return KafkaConsumerRecordDto(
        id = id,
        topic = topic,
        partition = partition,
        recordOffset = recordOffset,
        retries = retries,
        lastRetry = lastRetry?.toString(),
        key = key?.decodeToString(),
        value = value?.decodeToString(),
        headersJson = headersJson,
        recordTimestamp = recordTimestamp?.toString(),
        createdAt = createdAt.toString(),
    )
}
