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

    fun Notification.asUserNotification(userId: String, seenAt: LocalDateTime? = null) = run {
        UserNotification(
            id = id,
            type = type,
            title = title,
            description = description,
            user = userId,
            createdAt = LocalDateTime.ofInstant(createdAt, ZoneId.systemDefault()),
            seenAt = seenAt,
        )
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
        val seenAtTime = LocalDateTime.of(2023, 1, 1, 0, 0, 0)
        val notifications = NotificationRepository(database.db)

        notifications.upsert(commonNotification).shouldBeRight()
        notifications.upsert(userNotification).shouldBeRight()

        notifications.setNotificationSeenAt(commonNotification.id, "ABC", seenAtTime).shouldBeRight()
        notifications.setNotificationSeenAt(userNotification.id, "ABC", seenAtTime).shouldBeRight()
        notifications.setNotificationSeenAt(commonNotification.id, "XYZ", seenAtTime).shouldBeRight()

        notifications.getUserNotifications() shouldBeRight listOf(
            commonNotification.asUserNotification("ABC", seenAtTime),
            userNotification.asUserNotification("ABC", seenAtTime),
            commonNotification.asUserNotification("XYZ", seenAtTime),
        )
    }

    // FIXME det er ikke implementert noe funksjonalitet for dette enda
    // Det beste er nok å kvitte oss med `target`-kolonnen og eksplisitt opprette notifications per bruker,
    // men dette blir vanskelig å gjøre før vi får på plass en bruker-tabell
    xtest("should not be able to set notification status for another user's notification") {
        val seenAtTime = LocalDateTime.of(2023, 1, 1, 0, 0, 0)
        val notifications = NotificationRepository(database.db)

        notifications.upsert(userNotification).shouldBeRight()

        notifications.setNotificationSeenAt(userNotification.id, "XYZ", seenAtTime).shouldBeLeft()
    }
})
