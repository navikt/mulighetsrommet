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

// TODO: kan fjernes til fordel for UserNotification når vi får på plass en bruker-tabell
@Serializable
data class Notification(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val type: NotificationType,
    val title: String,
    val description: String? = null,
    /**
     * NAVident, or null if the notification targets all users
     */
    val user: String?,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant,
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
    val seenAt: LocalDateTime?,
)

/**
 * Internal representation of notifications
 */
internal data class NotificationDbo(
    val id: UUID,
    val type: NotificationType,
    // TODO kan antagelig fjernes når vi får et bedre konsept for "applikasjonsbrukere"
    // Da kan notifikasjoner opprettes per bruker i stedet for at vi benytter dette flagget for felles-notifikasjoner
    val target: NotificationTarget,
    val title: String,
    val description: String? = null,
    val createdAt: Instant,
)

internal data class UserNotificationDbo(
    val notificationId: UUID,
    val userId: String,
    val seenAt: LocalDateTime?,
)

internal enum class NotificationTarget {
    All,
    User,
}
