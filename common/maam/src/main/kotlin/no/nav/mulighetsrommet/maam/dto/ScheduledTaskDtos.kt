package no.nav.mulighetsrommet.maam.dto

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import java.time.LocalDateTime

@Serializable
data class RetryScheduledTaskRequest(
    val taskName: String,
    val taskInstance: String,
    @Serializable(with = LocalDateTimeSerializer::class)
    val executionTime: LocalDateTime,
)

@Serializable
data class DeleteScheduledTaskRequest(
    val taskName: String,
    val taskInstance: String,
)

@Serializable
data class ScheduledTaskDto(
    val taskName: String,
    val taskInstance: String,
    val taskData: String?,
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
