package no.nav.mulighetsrommet.database.queries

import kotliquery.Session
import kotliquery.queryOf
import org.intellij.lang.annotations.Language
import java.time.Instant

data class ScheduledTaskDbo(
    val taskName: String,
    val taskInstance: String,
    val taskData: ByteArray,
    val executionTime: Instant,
    val picked: Boolean,
    val pickedBy: String?,
    val lastSuccess: Instant?,
    val lastFailure: Instant?,
    val consecutiveFailures: Int,
    val lastHeartbeat: Instant?,
    val version: Long,
    val priority: Short?,
)

class ScheduledTaskQueries(private val session: Session) {
    fun getFailedTasks(): List<ScheduledTaskDbo> {
        @Language("PostgreSQL")
        val query = """
            select * from scheduled_tasks
            where consecutive_failures > 0
        """.trimIndent()

        return session.list(queryOf(query, emptyMap())) { row ->
            ScheduledTaskDbo(
                taskName = row.string("task_name"),
                taskInstance = row.string("task_instance"),
                taskData = row.bytes("task_data"),
                executionTime = row.instant("execution_time"),
                picked = row.boolean("picked"),
                pickedBy = row.stringOrNull("picked_by"),
                lastSuccess = row.instantOrNull("last_success"),
                lastFailure = row.instantOrNull("last_failure"),
                consecutiveFailures = row.int("consecutive_failures"),
                lastHeartbeat = row.instantOrNull("last_heartbeat"),
                version = row.long("version"),
                priority = row.shortOrNull("priority"),
            )
        }
    }
}
