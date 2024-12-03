package no.nav.mulighetsrommet.notifications

import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks

class ScheduleNotification(
    private val notifications: NotificationRepository,
) {
    val task: OneTimeTask<ScheduledNotification> = Tasks
        .oneTime("handle-scheduled-notification", ScheduledNotification::class.java)
        .execute { instance, _ ->
            val notification: ScheduledNotification = instance.data
            notifications.insert(notification)
        }
}
