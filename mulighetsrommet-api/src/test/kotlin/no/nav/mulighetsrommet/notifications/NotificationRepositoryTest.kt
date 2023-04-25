package no.nav.mulighetsrommet.notifications

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
})
