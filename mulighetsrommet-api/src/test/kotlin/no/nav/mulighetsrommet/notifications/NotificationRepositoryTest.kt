package no.nav.mulighetsrommet.notifications

import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
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

    val now = Instant.now().truncatedTo(ChronoUnit.MILLIS)

    val notification1 = ScheduledNotification(
        id = UUID.randomUUID(),
        type = NotificationType.Notification,
        title = "Notifikasjon for flere brukere",
        createdAt = now,
        targets = listOf("ABC", "XYZ"),
    )
    val notification2 = ScheduledNotification(
        id = UUID.randomUUID(),
        type = NotificationType.Notification,
        title = "Notifikasjon for spesifikk bruker",
        createdAt = now,
        targets = listOf("ABC"),
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

    beforeEach {
        database.db.migrate()
    }

    afterEach {
        database.db.clean()
    }

    test("CRUD") {
        val notifications = NotificationRepository(database.db)

        notifications.insert(notification1).shouldBeRight()
        notifications.insert(notification2).shouldBeRight()

        notifications.getAll() shouldBeRight listOf(
            notification1.asUserNotification("ABC"),
            notification1.asUserNotification("XYZ"),
            notification2.asUserNotification("ABC"),
        )

        notifications.delete(notification2.id).shouldBeRight()

        notifications.getAll() shouldBeRight listOf(
            notification1.asUserNotification("ABC"),
            notification1.asUserNotification("XYZ"),
        )
    }

    test("get notifications for specified user") {
        val notifications = NotificationRepository(database.db)

        notifications.insert(notification1).shouldBeRight()
        notifications.insert(notification2).shouldBeRight()

        notifications.getUserNotifications("ABC", filter) shouldBeRight listOf(
            notification1.asUserNotification("ABC"),
            notification2.asUserNotification("ABC"),
        )

        notifications.getUserNotifications("XYZ", filter) shouldBeRight listOf(
            notification1.asUserNotification("XYZ"),
        )
    }

    test("set notification status for user") {
        val readAtTime = LocalDateTime.of(2023, 1, 1, 0, 0, 0)
        val notifications = NotificationRepository(database.db)

        notifications.insert(notification1).shouldBeRight()
        notifications.insert(notification2).shouldBeRight()

        notifications.setNotificationReadAt(notification1.id, "ABC", readAtTime).shouldBeRight()
        notifications.setNotificationReadAt(notification2.id, "ABC", readAtTime).shouldBeRight()
        notifications.setNotificationReadAt(notification1.id, "XYZ", readAtTime).shouldBeRight()

        notifications.getUserNotifications(filter = filter) shouldBeRight listOf(
            notification1.asUserNotification("ABC", readAtTime),
            notification2.asUserNotification("ABC", readAtTime),
            notification1.asUserNotification("XYZ", readAtTime),
        )
    }

    test("filter for notification status") {
        val readAtTime = LocalDateTime.of(2023, 1, 1, 0, 0, 0)
        val notifications = NotificationRepository(database.db)
        val readFilter = NotificationFilter(status = NotificationStatus.Read)
        val unreadFilter = NotificationFilter(status = NotificationStatus.Unread)

        notifications.insert(notification1).shouldBeRight()
        notifications.insert(notification2).shouldBeRight()

        notifications.getUserNotifications("ABC", unreadFilter) shouldBeRight listOf(
            notification1.asUserNotification("ABC"),
            notification2.asUserNotification("ABC"),
        )

        notifications.getUserNotifications("ABC", readFilter) shouldBeRight emptyList()

        notifications.setNotificationReadAt(notification2.id, "ABC", readAtTime).shouldBeRight(1)
        notifications.setNotificationReadAt(notification1.id, "ABC", readAtTime).shouldBeRight(1)

        notifications.getUserNotifications("ABC", unreadFilter) shouldBeRight emptyList()

        notifications.getUserNotifications("ABC", readFilter) shouldBeRight listOf(
            notification1.asUserNotification("ABC", readAtTime),
            notification2.asUserNotification("ABC", readAtTime),
        )
    }

    test("should not be able to set notification status for another user's notification") {
        val readAtTime = LocalDateTime.of(2023, 1, 1, 0, 0, 0)
        val notifications = NotificationRepository(database.db)

        notifications.insert(notification2).shouldBeRight()

        notifications.setNotificationReadAt(notification2.id, "XYZ", readAtTime).shouldBeRight(0)

        notifications.getUserNotifications("ABC", filter) shouldBeRight listOf(
            notification2.asUserNotification("ABC", null),
        )
        notifications.getUserNotifications("XYZ", filter) shouldBeRight listOf()
    }

    test("get notification summary for user") {
        val notifications = NotificationRepository(database.db)

        notifications.insert(notification1).shouldBeRight()
        notifications.insert(notification2).shouldBeRight()

        notifications.getUserNotificationSummary("ABC") shouldBeRight UserNotificationSummary(
            unreadCount = 2,
        )
        notifications.getUserNotificationSummary("XYZ") shouldBeRight UserNotificationSummary(
            unreadCount = 1,
        )

        notifications.setNotificationReadAt(notification1.id, "ABC", LocalDateTime.now()).shouldBeRight()

        notifications.getUserNotificationSummary("ABC") shouldBeRight UserNotificationSummary(
            unreadCount = 1,
        )
        notifications.getUserNotificationSummary("XYZ") shouldBeRight UserNotificationSummary(
            unreadCount = 1,
        )
    }
})
