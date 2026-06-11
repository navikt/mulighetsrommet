package no.nav.mulighetsrommet.notifications

import com.github.kagkarlsson.scheduler.SchedulerClient
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.tasks.DbSchedulerKotlinSerializer
import org.slf4j.LoggerFactory
import java.time.Instant

class NotificationTask(
    private val db: ApiDatabase,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    val task: OneTimeTask<ScheduledNotification> = Tasks
        .oneTime("handle-scheduled-notification", ScheduledNotification::class.java)
        .execute { instance, _ ->
            val notification: ScheduledNotification = instance.data
            db.session { queries.notifications.insert(notification) }
        }

    private val client = SchedulerClient.Builder
        .create(db.getDatasource(), task)
        .serializer(DbSchedulerKotlinSerializer())
        .build()

    /**
     * Schedules the [notification] to be created at the specified [instant] in time (defaults to [Instant.now]).
     */
    fun scheduleNotification(notification: ScheduledNotification, instant: Instant = Instant.now()) {
        val id = notification.id

        logger.info("Scheduling notification id=$id for time=$instant")

        val instance = task.instance(id.toString(), notification)
        client.scheduleIfNotExists(instance, instant)
    }
}
