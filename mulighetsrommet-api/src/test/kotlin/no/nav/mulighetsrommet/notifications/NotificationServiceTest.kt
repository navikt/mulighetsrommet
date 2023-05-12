package no.nav.mulighetsrommet.notifications

import com.github.kagkarlsson.scheduler.Scheduler
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.timing.eventually
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.should
import io.mockk.mockk
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.tasks.DbSchedulerKotlinSerializer
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.time.Duration.Companion.seconds

class NotificationServiceTest : FunSpec({

    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    context("NotificationService") {
        val notifications = NotificationRepository(database.db)

        val service = NotificationService(database.db, mockk(), notifications)

        beforeEach {
            val scheduler = Scheduler
                .create(database.db.getDatasource(), service.getScheduledNotificationTask())
                .serializer(DbSchedulerKotlinSerializer())
                .build()

            scheduler.start()
        }

        val now = Instant.now().truncatedTo(ChronoUnit.SECONDS)

        val notification = ScheduledNotification(
            id = UUID.randomUUID(),
            type = NotificationType.Notification,
            title = "Notifikasjon for alle brukere",
            createdAt = now,
            targets = listOf("ABC", "XYZ"),
        )

        fun ScheduledNotification.asUserNotification(user: String): UserNotification = run {
            UserNotification(
                id = id,
                type = type,
                title = title,
                description = description,
                user = user,
                createdAt = LocalDateTime.ofInstant(createdAt, ZoneOffset.systemDefault()),
                readAt = null,
            )
        }

        test("scheduled notification should eventually be created for all targets") {
            service.scheduleNotification(notification, now)

            notifications.getAll() shouldBeRight listOf()

            eventually(30.seconds) {
                notifications.getAll().shouldBeRight().should {
                    it shouldContainExactlyInAnyOrder listOf(
                        notification.asUserNotification("ABC"),
                        notification.asUserNotification("XYZ"),
                    )
                }
            }
        }
    }
})
