package no.nav.mulighetsrommet.notifications

import arrow.core.NonEmptyList
import arrow.core.serialization.NonEmptyListSerializer
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.serializers.InstantSerializer
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.Instant
import java.time.LocalDateTime
import java.util.*

enum class NotificationStatus {
    READ,
    UNREAD,
}

/**
 * A notification scheduled for all [targets]
 */
@Serializable
data class ScheduledNotification(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID = UUID.randomUUID(),
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
    val title: String,
    val description: String? = null,
    val user: NavIdent,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class)
    val readAt: LocalDateTime?,
    val metadata: NotificationMetadata? = null,
)

@Serializable
data class UserNotificationSummary(
    val readCount: Int,
    val unreadCount: Int,
)

@Serializable
data class NotificationMetadata(
    val linkText: String,
    val link: String,
)
