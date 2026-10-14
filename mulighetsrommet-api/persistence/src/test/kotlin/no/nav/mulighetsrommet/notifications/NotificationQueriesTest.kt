package no.nav.mulighetsrommet.notifications

import arrow.core.nonEmptyListOf
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.domain.testing.fixture.NavAnsattFixture
import no.nav.mulighetsrommet.api.persistence.AdminTestDomain
import no.nav.mulighetsrommet.api.persistence.SqlAdminDatabaseTestListener
import no.nav.mulighetsrommet.model.NavIdent
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.UUID

class NotificationQueriesTest : FunSpec({
    val database = extension(SqlAdminDatabaseTestListener())

    val domain = AdminTestDomain(
        arrangorer = listOf(),
        tiltakstyper = listOf(),
    )

    val user1 = NavAnsattFixture.DonaldDuck.navIdent
    val user2 = NavAnsattFixture.MikkeMus.navIdent

    val now = Instant.now().truncatedTo(ChronoUnit.MILLIS)

    val notification1 = ScheduledNotification(
        id = UUID.randomUUID(),
        title = "Notifikasjon for flere brukere",
        createdAt = now,
        targets = nonEmptyListOf(user1, user2),
    )
    val notification2 = ScheduledNotification(
        id = UUID.randomUUID(),
        title = "Notifikasjon for spesifikk bruker",
        createdAt = now,
        targets = nonEmptyListOf(user1),
    )

    fun ScheduledNotification.asUserNotification(userId: NavIdent, readAt: LocalDateTime? = null) = run {
        UserNotification(
            id = id,
            title = title,
            description = description,
            user = userId,
            createdAt = LocalDateTime.ofInstant(createdAt, ZoneId.systemDefault()),
            readAt = readAt,
        )
    }

    test("CRUD") {
        database.runAndRollback {
            domain.initialize()

            notifications.insert(notification1)
            notifications.insert(notification2)

            notifications.getAll() shouldContainExactlyInAnyOrder listOf(
                notification1.asUserNotification(user1),
                notification1.asUserNotification(user2),
                notification2.asUserNotification(user1),
            )

            notifications.delete(notification2.id)

            notifications.getAll() shouldContainExactlyInAnyOrder listOf(
                notification1.asUserNotification(user1),
                notification1.asUserNotification(user2),
            )
        }
    }

    test("get notifications for specified user") {
        database.runAndRollback {
            domain.initialize()

            notifications.insert(notification1)
            notifications.insert(notification2)

            notifications.getUserNotifications(user1) shouldContainExactlyInAnyOrder listOf(
                notification1.asUserNotification(user1),
                notification2.asUserNotification(user1),
            )

            notifications.getUserNotifications(user2) shouldContainExactlyInAnyOrder listOf(
                notification1.asUserNotification(user2),
            )
        }
    }

    val readAtTime = LocalDateTime.of(2023, 1, 1, 0, 0, 0)

    test("should only set read_at for the specific user") {
        database.runAndRollback {
            domain.initialize()

            notifications.insert(notification1)
            notifications.insert(notification2)

            notifications.setNotificationReadAt(notification1.id, user1, readAtTime)

            notifications.getUserNotifications() shouldContainExactlyInAnyOrder listOf(
                notification1.asUserNotification(user2),
                notification2.asUserNotification(user1),
                notification1.asUserNotification(user1, readAtTime),
            )
        }
    }

    test("filter on notification status") {
        database.runAndRollback {
            domain.initialize()

            notifications.insert(notification1)
            notifications.insert(notification2)

            notifications.getUserNotifications(
                user1,
                NotificationStatus.UNREAD,
            ) shouldContainExactlyInAnyOrder listOf(
                notification1.asUserNotification(user1),
                notification2.asUserNotification(user1),
            )

            notifications.getUserNotifications(user1, NotificationStatus.READ).shouldBeEmpty()

            notifications.setNotificationReadAt(notification2.id, user1, readAtTime) shouldBe 1
            notifications.setNotificationReadAt(notification1.id, user1, readAtTime) shouldBe 1

            notifications.getUserNotifications(user1, NotificationStatus.UNREAD).shouldBeEmpty()

            notifications.getUserNotifications(user1, NotificationStatus.READ) shouldContainExactlyInAnyOrder listOf(
                notification1.asUserNotification(user1, readAtTime),
                notification2.asUserNotification(user1, readAtTime),
            )
        }
    }

    test("should not be able to set notification status for another user's notification") {
        database.runAndRollback {
            domain.initialize()

            notifications.insert(notification2)

            notifications.setNotificationReadAt(notification2.id, user2, readAtTime) shouldBe 0

            notifications.getUserNotifications(user1) shouldContainExactlyInAnyOrder listOf(
                notification2.asUserNotification(user1, null),
            )
            notifications.getUserNotifications(user2).shouldBeEmpty()
        }
    }

    test("get notification summary for user") {
        database.runAndRollback {
            domain.initialize()

            notifications.insert(notification1)
            notifications.insert(notification2)

            notifications.getUserNotificationSummary(user1) shouldBe UserNotificationSummary(
                readCount = 0,
                unreadCount = 2,
            )
            notifications.getUserNotificationSummary(user2) shouldBe UserNotificationSummary(
                readCount = 0,
                unreadCount = 1,
            )

            notifications.setNotificationReadAt(notification1.id, user1, LocalDateTime.now())

            notifications.getUserNotificationSummary(user1) shouldBe UserNotificationSummary(
                readCount = 1,
                unreadCount = 1,
            )
            notifications.getUserNotificationSummary(user2) shouldBe UserNotificationSummary(
                readCount = 0,
                unreadCount = 1,
            )
        }
    }
})
