package no.nav.mulighetsrommet.notifications

import arrow.core.getOrElse
import com.github.kagkarlsson.scheduler.SchedulerClient
import com.github.kagkarlsson.scheduler.task.ExecutionComplete
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import io.ktor.http.*
import no.nav.mulighetsrommet.database.DatabaseAdapter
import no.nav.mulighetsrommet.database.utils.getOrThrow
import no.nav.mulighetsrommet.ktor.exception.StatusException
import no.nav.mulighetsrommet.slack.SlackNotifier
import no.nav.mulighetsrommet.tasks.DbSchedulerKotlinSerializer
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalDateTime
import java.util.*

class NotificationService(
    database: DatabaseAdapter,
    private val slack: SlackNotifier,
    private val notifications: NotificationRepository,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val handleScheduledNotification: OneTimeTask<Notification> = Tasks
        .oneTime("handle-scheduled-notification", Notification::class.java)
        .onFailure { x: ExecutionComplete, _ ->
            val name = x.execution.taskInstance.taskName
            val id = x.execution.taskInstance.id
            slack.sendMessage("Klarte ikke kjøre task '$name' med id=$id. Se loggene for hva som gikk galt.")
        }
        .execute { instance, _ ->
            val notification: Notification = instance.data

            logger.info("Running task ${instance.taskName} for notification id=${notification.id}")

            notifications.upsert(notification).getOrThrow()
        }

    private val client = SchedulerClient.Builder
        .create(database.getDatasource(), handleScheduledNotification)
        .serializer(DbSchedulerKotlinSerializer())
        .build()

    fun getScheduledNotificationTask() = handleScheduledNotification

    /**
     * Schedules the [notification] to be created at the specified [instant] in time (defaults to [Instant.now]).
     */
    fun scheduleNotification(notification: Notification, instant: Instant = Instant.now()) {
        val id = notification.id

        logger.info("Scheduling notification id=$id for time=$instant")

        val instance = handleScheduledNotification.instance(id.toString(), notification)
        client.schedule(instance, instant)
    }

    fun getNotifications(userId: String): List<UserNotification> {
        return notifications.getUserNotifications(userId)
            .getOrElse {
                logger.error("Failed to get notifications for user=$userId", it.error)
                throw StatusException(
                    HttpStatusCode.InternalServerError,
                    "Failed to get notifications for user=$userId",
                )
            }
    }

    fun markNotificationAsRead(id: UUID, userId: String) {
        notifications.setNotificationReadAt(id, userId, LocalDateTime.now())
            .onLeft {
                logger.error("Failed to mark notification as read", it.error)
                throw StatusException(HttpStatusCode.InternalServerError, "Failed to mark notification as read")
            }
    }

    fun markNotificationAsUnread(id: UUID, userId: String) {
        notifications.setNotificationReadAt(id, userId, null)
            .onLeft {
                logger.error("Failed to mark notification as unread", it.error)
                throw StatusException(HttpStatusCode.InternalServerError, "Failed to mark notification as unread")
            }
    }
}
