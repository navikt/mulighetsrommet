package no.nav.mulighetsrommet.notifications

import arrow.core.getOrElse
import com.github.kagkarlsson.scheduler.SchedulerClient
import com.github.kagkarlsson.scheduler.task.ExecutionComplete
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.InternalServerError
import io.ktor.server.plugins.*
import no.nav.mulighetsrommet.api.utils.NotificationFilter
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.utils.getOrThrow
import no.nav.mulighetsrommet.ktor.exception.StatusException
import no.nav.mulighetsrommet.slack.SlackNotifier
import no.nav.mulighetsrommet.tasks.DbSchedulerKotlinSerializer
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalDateTime
import java.util.*

class NotificationService(
    database: Database,
    private val slack: SlackNotifier,
    private val notifications: NotificationRepository,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val handleScheduledNotification: OneTimeTask<ScheduledNotification> = Tasks
        .oneTime("handle-scheduled-notification", ScheduledNotification::class.java)
        .onFailure { x: ExecutionComplete, _ ->
            val name = x.execution.taskInstance.taskName
            val id = x.execution.taskInstance.id
            slack.sendMessage("Klarte ikke kjøre task '$name' med id=$id. Se loggene for hva som gikk galt.")
        }
        .execute { instance, _ ->
            val notification: ScheduledNotification = instance.data

            logger.info("Running task ${instance.taskName} for notification id=${notification.id}")

            notifications.insert(notification).getOrThrow()
        }

    private val client = SchedulerClient.Builder
        .create(database.getDatasource(), handleScheduledNotification)
        .serializer(DbSchedulerKotlinSerializer())
        .build()

    fun getScheduledNotificationTask() = handleScheduledNotification

    /**
     * Schedules the [notification] to be created at the specified [instant] in time (defaults to [Instant.now]).
     */
    fun scheduleNotification(notification: ScheduledNotification, instant: Instant = Instant.now()) {
        val id = notification.id

        logger.info("Scheduling notification id=$id for time=$instant")

        val instance = handleScheduledNotification.instance(id.toString(), notification)
        client.schedule(instance, instant)
    }

    fun getNotifications(userId: String, filter: NotificationFilter): List<UserNotification> {
        return notifications.getUserNotifications(userId, filter)
            .getOrElse {
                logger.error("Failed to get notifications for user=$userId", it.error)
                throw StatusException(InternalServerError, "Failed to get notifications for user=$userId")
            }
    }

    fun getNotificationSummary(userId: String): UserNotificationSummary {
        return notifications.getUserNotificationSummary(userId)
            .getOrElse {
                logger.error("Failed to get summary for user=$userId", it.error)
                throw StatusException(InternalServerError, "Failed to get summary for user=$userId")
            }
    }

    fun markNotificationAsRead(id: UUID, userId: String) {
        notifications.setNotificationReadAt(id, userId, LocalDateTime.now())
            .onLeft {
                logger.error("Failed to mark notification as read", it.error)
                throw StatusException(InternalServerError, "Failed to mark notification as read")
            }
            .onRight { updated ->
                if (updated == 0) {
                    throw StatusException(BadRequest, "Could not mark notification=$id as read for user=$userId")
                }
            }
    }

    fun markNotificationAsUnread(id: UUID, userId: String) {
        notifications.setNotificationReadAt(id, userId, null)
            .onLeft {
                logger.error("Failed to mark notification as unread", it.error)
                throw StatusException(InternalServerError, "Failed to mark notification as unread")
            }
            .onRight { updated ->
                if (updated == 0) {
                    throw StatusException(BadRequest, "Could not mark notification=$id as unread for user=$userId")
                }
            }
    }
}
