package no.nav.mulighetsrommet.maam.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.queries.KafkaConsumerRecordDbo
import no.nav.mulighetsrommet.database.queries.KafkaConsumerRecordQueries
import no.nav.mulighetsrommet.env.NaisEnv
import no.nav.mulighetsrommet.maam.dto.DeleteKafkaRecordRequest
import no.nav.mulighetsrommet.maam.dto.KafkaConsumerRecordDto
import no.nav.mulighetsrommet.maam.dto.RetryKafkaRecordRequest
import java.time.ZoneOffset.UTC

internal fun Route.failedKafkaRecordRoutes(db: () -> Database, environment: NaisEnv) {
    route("/failed-records") {
        get {
            val records = db().session { KafkaConsumerRecordQueries(it).getFailedRecords() }
            call.respond(records.map { it.toDto() })
        }

        put {
            val request = call.receive<RetryKafkaRecordRequest>()
            db().transaction {
                KafkaConsumerRecordQueries(it).retryAt(
                    request.id,
                    request.topic,
                    request.executionTime.toInstant(UTC),
                )
            }
            call.respond(HttpStatusCode.OK)
        }

        delete {
            if (environment.isProdGCP()) {
                return@delete call.respond(HttpStatusCode.Forbidden)
            }
            val request = call.receive<DeleteKafkaRecordRequest>()
            val deleted = db().transaction {
                KafkaConsumerRecordQueries(it).deleteFailedRecord(request.id, request.topic)
            }
            if (deleted == 0) {
                call.respond(HttpStatusCode.Conflict, "Record is no longer failed or does not exist")
            } else {
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

private fun KafkaConsumerRecordDbo.toDto() = KafkaConsumerRecordDto(
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
