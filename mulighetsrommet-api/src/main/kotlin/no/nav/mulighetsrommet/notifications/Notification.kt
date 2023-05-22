package no.nav.mulighetsrommet.notifications

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.serializers.InstantSerializer
import no.nav.mulighetsrommet.domain.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import java.time.Instant
import java.time.LocalDateTime
import java.util.*

@Serializable
enum class NotificationType {
    Notification,
    Task,
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
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant,
    /**
     * NAVident for each notification target
     */
    val targets: List<String>,
)

@Serializable
data class UserNotification(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val type: NotificationType,
    val title: String,
    val description: String? = null,
    val user: String,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class)
    val readAt: LocalDateTime?,
)

@Serializable
data class UserNotificationSummary(
    val unreadCount: Int,
)
