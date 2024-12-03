package no.nav.mulighetsrommet.notifications

import arrow.core.getOrElse
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.InternalServerError
import no.nav.mulighetsrommet.domain.dto.NavIdent
import no.nav.mulighetsrommet.ktor.exception.StatusException
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

class NotificationService(
    private val notifications: NotificationRepository,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun getNotifications(userId: NavIdent, filter: NotificationFilter): List<UserNotification> {
        return notifications.getUserNotifications(userId, filter.status)
            .getOrElse {
                logger.error("Failed to get notifications for user=$userId", it.error)
                throw StatusException(InternalServerError, "Klarte ikke hente notifikasjoner")
            }
    }

    fun getNotificationSummary(userId: NavIdent): UserNotificationSummary {
        return notifications.getUserNotificationSummary(userId)
            .getOrElse {
                logger.error("Failed to get summary for user=$userId", it.error)
                throw StatusException(InternalServerError, "Klarte ikke hente notifikasjoner")
            }
    }

    fun setNotificationStatus(id: UUID, userId: NavIdent, status: NotificationStatus) {
        val doneAt = when (status) {
            NotificationStatus.DONE -> LocalDateTime.now()
            NotificationStatus.NOT_DONE -> null
        }
        notifications.setNotificationDoneAt(id, userId, doneAt)
            .onLeft {
                logger.error("Failed to set notification status", it.error)
                throw StatusException(InternalServerError, "Klarte ikke oppdatere notifikasjon med ny status")
            }
            .onRight { updated ->
                if (updated == 0) {
                    throw StatusException(BadRequest, "Klarte ikke oppdatere notifikasjon med ny status")
                }
            }
    }
}
