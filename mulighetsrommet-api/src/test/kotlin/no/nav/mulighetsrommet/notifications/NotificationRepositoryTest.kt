package no.nav.mulighetsrommet.notifications

import arrow.core.nonEmptyListOf
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.truncateAll
import no.nav.mulighetsrommet.domain.dto.NavIdent
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.*

class NotificationRepositoryTest : FunSpec({

    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    val domain = MulighetsrommetTestDomain()

    beforeEach {
        domain.initialize(database.db)
    }

    afterEach {
        database.db.truncateAll()
    }

    val user1 = NavAnsattFixture.ansatt1.navIdent
    val user2 = NavAnsattFixture.ansatt2.navIdent

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
        val notifications = NotificationRepository(database.db)

        notifications.insert(notification1)
        notifications.insert(notification2)

        notifications.getAll() shouldBeRight listOf(
            notification1.asUserNotification(user1),
            notification1.asUserNotification(user2),
            notification2.asUserNotification(user1),
        )

        notifications.delete(notification2.id).shouldBeRight()

        notifications.getAll() shouldBeRight listOf(
            notification1.asUserNotification(user1),
            notification1.asUserNotification(user2),
        )
    }

    test("get notifications for specified user") {
        val notifications = NotificationRepository(database.db)

        notifications.insert(notification1)
        notifications.insert(notification2)

        notifications.getUserNotifications(user1) shouldBeRight listOf(
            notification1.asUserNotification(user1),
            notification2.asUserNotification(user1),
        )

        notifications.getUserNotifications(user2) shouldBeRight listOf(
            notification1.asUserNotification(user2),
        )
    }

    test("should only set done_at for the specific user when the notification type is NOTIFICATION") {
        val doneAtTime = LocalDateTime.of(2023, 1, 1, 0, 0, 0)
        val notifications = NotificationRepository(database.db)

        notifications.insert(notification1)
        notifications.insert(notification2)

        notifications.setNotificationDoneAt(notification1.id, user1, doneAtTime).shouldBeRight()

        notifications.getUserNotifications() shouldBeRight listOf(
            notification1.asUserNotification(user2),
            notification2.asUserNotification(user1),
            notification1.asUserNotification(user1, doneAtTime),
        )
    }

    test("should set done_at for all users when the notification type is TASK") {
        val doneAtTime = LocalDateTime.of(2023, 1, 1, 0, 0, 0)
        val notifications = NotificationRepository(database.db)

        val task = notification1.copy(type = NotificationType.TASK)
        notifications.insert(task)
        notifications.insert(notification2)

        notifications.setNotificationDoneAt(task.id, user1, doneAtTime).shouldBeRight()

        notifications.getUserNotifications() shouldBeRight listOf(
            notification2.asUserNotification(user1),
            task.asUserNotification(user1, doneAtTime),
            task.asUserNotification(user2, doneAtTime),
        )
    }

    test("filter on notification status") {
        val doneAtTime = LocalDateTime.of(2023, 1, 1, 0, 0, 0)
        val notifications = NotificationRepository(database.db)

        notifications.insert(notification1)
        notifications.insert(notification2)

        notifications.getUserNotifications(user1, NotificationStatus.NOT_DONE) shouldBeRight listOf(
            notification1.asUserNotification(user1),
            notification2.asUserNotification(user1),
        )

        notifications.getUserNotifications(user1, NotificationStatus.DONE) shouldBeRight emptyList()

        notifications.setNotificationDoneAt(notification2.id, user1, doneAtTime).shouldBeRight(1)
        notifications.setNotificationDoneAt(notification1.id, user1, doneAtTime).shouldBeRight(1)

        notifications.getUserNotifications(user1, NotificationStatus.NOT_DONE) shouldBeRight emptyList()

        notifications.getUserNotifications(user1, NotificationStatus.DONE) shouldBeRight listOf(
            notification1.asUserNotification(user1, doneAtTime),
            notification2.asUserNotification(user1, doneAtTime),
        )
    }

    test("should not be able to set notification status for another user's notification") {
        val doneAtTime = LocalDateTime.of(2023, 1, 1, 0, 0, 0)
        val notifications = NotificationRepository(database.db)

        notifications.insert(notification2)

        notifications.setNotificationDoneAt(notification2.id, user2, doneAtTime).shouldBeRight(0)

        notifications.getUserNotifications(user1) shouldBeRight listOf(
            notification2.asUserNotification(user1, null),
        )
        notifications.getUserNotifications(user2) shouldBeRight listOf()
    }

    test("get notification summary for user") {
        val notifications = NotificationRepository(database.db)

        notifications.insert(notification1)
        notifications.insert(notification2)

        notifications.getUserNotificationSummary(user1) shouldBeRight UserNotificationSummary(
            notDoneCount = 2,
        )
        notifications.getUserNotificationSummary(user2) shouldBeRight UserNotificationSummary(
            notDoneCount = 1,
        )

        notifications.setNotificationDoneAt(notification1.id, user1, LocalDateTime.now())
            .shouldBeRight()

        notifications.getUserNotificationSummary(user1) shouldBeRight UserNotificationSummary(
            notDoneCount = 1,
        )
        notifications.getUserNotificationSummary(user2) shouldBeRight UserNotificationSummary(
            notDoneCount = 1,
        )
    }
})
