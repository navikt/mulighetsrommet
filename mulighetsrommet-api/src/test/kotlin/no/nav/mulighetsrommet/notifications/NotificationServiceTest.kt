package no.nav.mulighetsrommet.notifications

import com.github.kagkarlsson.scheduler.Scheduler
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.timing.eventually
import io.kotest.core.spec.style.FunSpec
import io.mockk.mockk
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.slack.SlackNotifier
import no.nav.mulighetsrommet.tasks.DbSchedulerKotlinSerializer
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.time.Duration.Companion.seconds

class NotificationServiceTest : FunSpec({

    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    context("NotificationService") {
        val slack: SlackNotifier = mockk()
        val notifications = NotificationRepository(database.db)
        val service = NotificationService(database.db, slack, notifications)

        beforeEach {
            val scheduler = Scheduler
                .create(database.db.getDatasource(), service.getScheduledNotificationTask())
                .serializer(DbSchedulerKotlinSerializer())
                .build()

            scheduler.start()
        }

        test("scheduled notification should eventually be created") {
            val now = Instant.now().truncatedTo(ChronoUnit.SECONDS)

            val commonNotification = Notification(
                id = UUID.randomUUID(),
                type = NotificationType.Notification,
                title = "Notifikasjon for alle brukere",
                user = null,
                createdAt = now,
            )

            service.scheduleNotification(commonNotification, now)

            notifications.getAll() shouldBeRight listOf()

            eventually(30.seconds) {
                notifications.getAll() shouldBeRight listOf(commonNotification)
            }
        }
    }
})
