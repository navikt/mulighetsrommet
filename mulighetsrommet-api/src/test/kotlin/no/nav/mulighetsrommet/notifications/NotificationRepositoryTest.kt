package no.nav.mulighetsrommet.notifications

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.*

class NotificationRepositoryTest : FunSpec({

    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    val now = Instant.now().truncatedTo(ChronoUnit.MILLIS)

    val commonNotification = Notification(
        id = UUID.randomUUID(),
        type = NotificationType.Notification,
        title = "Notifikasjon for alle brukere",
        user = null,
        createdAt = now,
    )
    val userNotification = Notification(
        id = UUID.randomUUID(),
        type = NotificationType.Notification,
        title = "Notifikasjon for spesifikk bruker",
        user = "ABC",
        createdAt = now,
    )

    fun Notification.asUserNotification(userId: String, readAt: LocalDateTime? = null) = run {
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

        notifications.upsert(commonNotification).shouldBeRight()
        notifications.upsert(userNotification).shouldBeRight()

        notifications.getAll() shouldBeRight listOf(userNotification, commonNotification)

        notifications.delete(userNotification.id).shouldBeRight()

        notifications.getAll() shouldBeRight listOf(commonNotification)
    }

    test("get notifications for specified user") {
        val notifications = NotificationRepository(database.db)

        notifications.upsert(commonNotification).shouldBeRight()
        notifications.upsert(userNotification).shouldBeRight()

        notifications.getUserNotifications("ABC") shouldBeRight listOf(
            userNotification.asUserNotification("ABC"),
            commonNotification.asUserNotification("ABC"),
        )

        notifications.getUserNotifications("XYZ") shouldBeRight listOf(
            commonNotification.asUserNotification("XYZ"),
        )
    }

    test("set notification status for user") {
        val readAtTime = LocalDateTime.of(2023, 1, 1, 0, 0, 0)
        val notifications = NotificationRepository(database.db)

        notifications.upsert(commonNotification).shouldBeRight()
        notifications.upsert(userNotification).shouldBeRight()

        notifications.setNotificationReadAt(commonNotification.id, "ABC", readAtTime).shouldBeRight()
        notifications.setNotificationReadAt(userNotification.id, "ABC", readAtTime).shouldBeRight()
        notifications.setNotificationReadAt(commonNotification.id, "XYZ", readAtTime).shouldBeRight()

        notifications.getUserNotifications() shouldBeRight listOf(
            commonNotification.asUserNotification("ABC", readAtTime),
            userNotification.asUserNotification("ABC", readAtTime),
            commonNotification.asUserNotification("XYZ", readAtTime),
        )
    }

    // FIXME det er ikke implementert noe funksjonalitet for dette enda
    // Det beste er nok å kvitte oss med `target`-kolonnen og eksplisitt opprette notifications per bruker,
    // men dette blir vanskelig å gjøre før vi får på plass en bruker-tabell
    xtest("should not be able to set notification status for another user's notification") {
        val readAtTime = LocalDateTime.of(2023, 1, 1, 0, 0, 0)
        val notifications = NotificationRepository(database.db)

        notifications.upsert(userNotification).shouldBeRight()

        notifications.setNotificationReadAt(userNotification.id, "XYZ", readAtTime).shouldBeLeft()
    }

    test("get notification summary for user") {
        val notifications = NotificationRepository(database.db)

        notifications.upsert(commonNotification).shouldBeRight()
        notifications.upsert(userNotification).shouldBeRight()

        notifications.getUserNotificationSummary("ABC") shouldBeRight UserNotificationSummary(
            unreadCount = 2,
        )
        notifications.getUserNotificationSummary("XYZ") shouldBeRight UserNotificationSummary(
            unreadCount = 1,
        )

        notifications.setNotificationReadAt(commonNotification.id, "ABC", LocalDateTime.now()).shouldBeRight()

        notifications.getUserNotificationSummary("ABC") shouldBeRight UserNotificationSummary(
            unreadCount = 1,
        )
        notifications.getUserNotificationSummary("XYZ") shouldBeRight UserNotificationSummary(
            unreadCount = 1,
        )
    }
})
