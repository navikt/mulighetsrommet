package no.nav.mulighetsrommet.notifications

import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.utils.NotificationFilter
import no.nav.mulighetsrommet.api.utils.NotificationStatus
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
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

    val user1 = domain.ansatt1.navIdent
    val user2 = domain.ansatt2.navIdent

    val now = Instant.now().truncatedTo(ChronoUnit.MILLIS)

    val notification1 = ScheduledNotification(
        id = UUID.randomUUID(),
        type = NotificationType.Notification,
        title = "Notifikasjon for flere brukere",
        createdAt = now,
        targets = listOf(user1, user2),
    )
    val notification2 = ScheduledNotification(
        id = UUID.randomUUID(),
        type = NotificationType.Notification,
        title = "Notifikasjon for spesifikk bruker",
        createdAt = now,
        targets = listOf(user1),
    )

    val filter = NotificationFilter()

    fun ScheduledNotification.asUserNotification(userId: String, readAt: LocalDateTime? = null) = run {
        UserNotification(
            id = id,
            type = type,
            title = title,
            description = description,
            user = userId,
            createdAt = LocalDateTime.ofInstant(createdAt, ZoneId.systemDefault()),
            readAt = readAt,
        )
    }

    test("CRUD") {
        val notifications = NotificationRepository(database.db)

        notifications.insert(notification1).shouldBeRight()
        notifications.insert(notification2).shouldBeRight()

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

        notifications.insert(notification1).shouldBeRight()
        notifications.insert(notification2).shouldBeRight()

        notifications.getUserNotifications(user1, filter) shouldBeRight listOf(
            notification1.asUserNotification(user1),
            notification2.asUserNotification(user1),
        )

        notifications.getUserNotifications(user2, filter) shouldBeRight listOf(
            notification1.asUserNotification(user2),
        )
    }

    test("set notification status for user") {
        val readAtTime = LocalDateTime.of(2023, 1, 1, 0, 0, 0)
        val notifications = NotificationRepository(database.db)

        notifications.insert(notification1).shouldBeRight()
        notifications.insert(notification2).shouldBeRight()

        notifications.setNotificationReadAt(notification1.id, user1, readAtTime).shouldBeRight()
        notifications.setNotificationReadAt(notification2.id, user1, readAtTime).shouldBeRight()
        notifications.setNotificationReadAt(notification1.id, user2, readAtTime).shouldBeRight()

        notifications.getUserNotifications(filter = filter) shouldBeRight listOf(
            notification1.asUserNotification(user1, readAtTime),
            notification2.asUserNotification(user1, readAtTime),
            notification1.asUserNotification(user2, readAtTime),
        )
    }

    test("filter for notification status") {
        val readAtTime = LocalDateTime.of(2023, 1, 1, 0, 0, 0)
        val notifications = NotificationRepository(database.db)
        val readFilter = NotificationFilter(status = NotificationStatus.Read)
        val unreadFilter = NotificationFilter(status = NotificationStatus.Unread)

        notifications.insert(notification1).shouldBeRight()
        notifications.insert(notification2).shouldBeRight()

        notifications.getUserNotifications(user1, unreadFilter) shouldBeRight listOf(
            notification1.asUserNotification(user1),
            notification2.asUserNotification(user1),
        )

        notifications.getUserNotifications(user1, readFilter) shouldBeRight emptyList()

        notifications.setNotificationReadAt(notification2.id, user1, readAtTime)
            .shouldBeRight(1)
        notifications.setNotificationReadAt(notification1.id, user1, readAtTime)
            .shouldBeRight(1)

        notifications.getUserNotifications(user1, unreadFilter) shouldBeRight emptyList()

        notifications.getUserNotifications(user1, readFilter) shouldBeRight listOf(
            notification1.asUserNotification(user1, readAtTime),
            notification2.asUserNotification(user1, readAtTime),
        )
    }

    test("should not be able to set notification status for another user's notification") {
        val readAtTime = LocalDateTime.of(2023, 1, 1, 0, 0, 0)
        val notifications = NotificationRepository(database.db)

        notifications.insert(notification2).shouldBeRight()

        notifications.setNotificationReadAt(notification2.id, user2, readAtTime)
            .shouldBeRight(0)

        notifications.getUserNotifications(user1, filter) shouldBeRight listOf(
            notification2.asUserNotification(user1, null),
        )
        notifications.getUserNotifications(user2, filter) shouldBeRight listOf()
    }

    test("get notification summary for user") {
        val notifications = NotificationRepository(database.db)

        notifications.insert(notification1).shouldBeRight()
        notifications.insert(notification2).shouldBeRight()

        notifications.getUserNotificationSummary(user1) shouldBeRight UserNotificationSummary(
            unreadCount = 2,
        )
        notifications.getUserNotificationSummary(user2) shouldBeRight UserNotificationSummary(
            unreadCount = 1,
        )

        notifications.setNotificationReadAt(notification1.id, user1, LocalDateTime.now())
            .shouldBeRight()

        notifications.getUserNotificationSummary(user1) shouldBeRight UserNotificationSummary(
            unreadCount = 1,
        )
        notifications.getUserNotificationSummary(user2) shouldBeRight UserNotificationSummary(
            unreadCount = 1,
        )
    }
})
