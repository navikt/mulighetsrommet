package no.nav.mulighetsrommet.notifications

import arrow.core.nonEmptyListOf
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.model.NavIdent
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.*

class NotificationQueriesTest : FunSpec({

    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    val domain = MulighetsrommetTestDomain(
        arrangorer = listOf(),
        tiltakstyper = listOf(),
        avtaler = listOf(),
    )

    val user1 = NavAnsattFixture.DonaldDuck.navIdent
    val user2 = NavAnsattFixture.MikkeMus.navIdent

    val now = Instant.now().truncatedTo(ChronoUnit.MILLIS)

    val notification1 = ScheduledNotification(
        id = UUID.randomUUID(),
        type = NotificationType.NOTIFICATION,
        title = "Notifikasjon for flere brukere",
        createdAt = now,
        targets = nonEmptyListOf(user1, user2),
    )
    val notification2 = ScheduledNotification(
        id = UUID.randomUUID(),
        type = NotificationType.NOTIFICATION,
        title = "Notifikasjon for spesifikk bruker",
        createdAt = now,
        targets = nonEmptyListOf(user1),
    )

    fun ScheduledNotification.asUserNotification(userId: NavIdent, doneAt: LocalDateTime? = null) = run {
        UserNotification(
            id = id,
            type = type,
            title = title,
            description = description,
            user = userId,
            createdAt = LocalDateTime.ofInstant(createdAt, ZoneId.systemDefault()),
            doneAt = doneAt,
        )
    }

    test("CRUD") {
        database.runAndRollback { session ->
            domain.setup(session)

            val queries = NotificationQueries(session)

            queries.insert(notification1)
            queries.insert(notification2)

            queries.getAll() shouldContainExactlyInAnyOrder listOf(
                notification1.asUserNotification(user1),
                notification1.asUserNotification(user2),
                notification2.asUserNotification(user1),
            )

            queries.delete(notification2.id)

            queries.getAll() shouldContainExactlyInAnyOrder listOf(
                notification1.asUserNotification(user1),
                notification1.asUserNotification(user2),
            )
        }
    }

    test("get notifications for specified user") {
        database.runAndRollback { session ->
            domain.setup(session)

            val queries = NotificationQueries(session)

            queries.insert(notification1)
            queries.insert(notification2)

            queries.getUserNotifications(user1) shouldContainExactlyInAnyOrder listOf(
                notification1.asUserNotification(user1),
                notification2.asUserNotification(user1),
            )

            queries.getUserNotifications(user2) shouldContainExactlyInAnyOrder listOf(
                notification1.asUserNotification(user2),
            )
        }
    }

    val doneAtTime = LocalDateTime.of(2023, 1, 1, 0, 0, 0)

    test("should only set done_at for the specific user when the notification type is NOTIFICATION") {
        database.runAndRollback { session ->
            domain.setup(session)

            val queries = NotificationQueries(session)

            queries.insert(notification1)
            queries.insert(notification2)

            queries.setNotificationDoneAt(notification1.id, user1, doneAtTime)

            queries.getUserNotifications() shouldContainExactlyInAnyOrder listOf(
                notification1.asUserNotification(user2),
                notification2.asUserNotification(user1),
                notification1.asUserNotification(user1, doneAtTime),
            )
        }
    }

    test("should set done_at for all users when the notification type is TASK") {
        database.runAndRollback { session ->
            domain.setup(session)

            val notifications = NotificationQueries(session)

            val task = notification1.copy(type = NotificationType.TASK)
            notifications.insert(task)
            notifications.insert(notification2)

            notifications.setNotificationDoneAt(task.id, user1, doneAtTime)

            notifications.getUserNotifications() shouldContainExactlyInAnyOrder listOf(
                notification2.asUserNotification(user1),
                task.asUserNotification(user1, doneAtTime),
                task.asUserNotification(user2, doneAtTime),
            )
        }
    }

    test("filter on notification status") {
        database.runAndRollback { session ->
            domain.setup(session)

            val notifications = NotificationQueries(session)

            notifications.insert(notification1)
            notifications.insert(notification2)

            notifications.getUserNotifications(
                user1,
                NotificationStatus.NOT_DONE,
            ) shouldContainExactlyInAnyOrder listOf(
                notification1.asUserNotification(user1),
                notification2.asUserNotification(user1),
            )

            notifications.getUserNotifications(user1, NotificationStatus.DONE).shouldBeEmpty()

            notifications.setNotificationDoneAt(notification2.id, user1, doneAtTime) shouldBe 1
            notifications.setNotificationDoneAt(notification1.id, user1, doneAtTime) shouldBe 1

            notifications.getUserNotifications(user1, NotificationStatus.NOT_DONE).shouldBeEmpty()

            notifications.getUserNotifications(user1, NotificationStatus.DONE) shouldContainExactlyInAnyOrder listOf(
                notification1.asUserNotification(user1, doneAtTime),
                notification2.asUserNotification(user1, doneAtTime),
            )
        }
    }

    test("should not be able to set notification status for another user's notification") {
        database.runAndRollback { session ->
            domain.setup(session)

            val queries = NotificationQueries(session)

            queries.insert(notification2)

            queries.setNotificationDoneAt(notification2.id, user2, doneAtTime) shouldBe 0

            queries.getUserNotifications(user1) shouldContainExactlyInAnyOrder listOf(
                notification2.asUserNotification(user1, null),
            )
            queries.getUserNotifications(user2).shouldBeEmpty()
        }
    }

    test("get notification summary for user") {
        database.runAndRollback { session ->
            domain.setup(session)

            val queries = NotificationQueries(session)

            queries.insert(notification1)
            queries.insert(notification2)

            queries.getUserNotificationSummary(user1) shouldBe UserNotificationSummary(
                notDoneCount = 2,
            )
            queries.getUserNotificationSummary(user2) shouldBe UserNotificationSummary(
                notDoneCount = 1,
            )

            queries.setNotificationDoneAt(notification1.id, user1, LocalDateTime.now())

            queries.getUserNotificationSummary(user1) shouldBe UserNotificationSummary(
                notDoneCount = 1,
            )
            queries.getUserNotificationSummary(user2) shouldBe UserNotificationSummary(
                notDoneCount = 1,
            )
        }
    }
})
