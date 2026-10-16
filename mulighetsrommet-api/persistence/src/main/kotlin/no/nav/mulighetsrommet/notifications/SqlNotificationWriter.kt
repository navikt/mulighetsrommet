package no.nav.mulighetsrommet.notifications

import arrow.core.NonEmptyList
import no.nav.mulighetsrommet.admin.notifications.NotificationWriter
import no.nav.mulighetsrommet.model.NavIdent
import java.time.Instant

class SqlNotificationWriter(private val queries: NotificationQueries) : NotificationWriter {
    override fun schedule(title: String, targets: NonEmptyList<NavIdent>, createdAt: Instant) {
        queries.insert(
            ScheduledNotification(
                title = title,
                targets = targets,
                createdAt = createdAt,
            ),
        )
    }
}
