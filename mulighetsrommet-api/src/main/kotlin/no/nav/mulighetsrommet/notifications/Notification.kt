package no.nav.mulighetsrommet.notifications

import arrow.core.NonEmptyList
import arrow.core.serialization.NonEmptyListSerializer
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.dto.NavIdent
import no.nav.mulighetsrommet.domain.serializers.InstantSerializer
import no.nav.mulighetsrommet.domain.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import java.time.Instant
import java.time.LocalDateTime
import java.util.*

enum class NotificationType {
    NOTIFICATION,
    TASK,
}

enum class NotificationStatus {
    DONE,
    NOT_DONE,
}

/**
 * A notification scheduled for all [targets]
 */
@Serializable
data class ScheduledNotification(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID = UUID.randomUUID(),
    val type: NotificationType,
    val title: String,
    val description: String? = null,
    val metadata: NotificationMetadata? = null,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant,
    /**
     * NAVident for each notification target
     */
    @Serializable(with = NonEmptyListSerializer::class)
    val targets: NonEmptyList<NavIdent>,
)

@Serializable
data class UserNotification(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val type: NotificationType,
    val title: String,
    val description: String? = null,
    val user: NavIdent,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class)
    val doneAt: LocalDateTime?,
    val metadata: NotificationMetadata? = null,
)

@Serializable
data class UserNotificationSummary(
    val notDoneCount: Int,
)

@Serializable
data class NotificationMetadata(
    val linkText: String,
    val link: String,
)
