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

    val readAtTime = LocalDateTime.of(2023, 1, 1, 0, 0, 0)

    test("should only set read_at for the specific user") {
        database.runAndRollback { session ->
            domain.setup(session)

            val queries = NotificationQueries(session)

            queries.insert(notification1)
            queries.insert(notification2)

            queries.setNotificationReadAt(notification1.id, user1, readAtTime)

            queries.getUserNotifications() shouldContainExactlyInAnyOrder listOf(
                notification1.asUserNotification(user2),
                notification2.asUserNotification(user1),
                notification1.asUserNotification(user1, readAtTime),
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
        database.runAndRollback { session ->
            domain.setup(session)

            val queries = NotificationQueries(session)

            queries.insert(notification2)

            queries.setNotificationReadAt(notification2.id, user2, readAtTime) shouldBe 0

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
                readCount = 0,
                unreadCount = 2,
            )
            queries.getUserNotificationSummary(user2) shouldBe UserNotificationSummary(
                readCount = 0,
                unreadCount = 1,
            )

            queries.setNotificationReadAt(notification1.id, user1, LocalDateTime.now())

            queries.getUserNotificationSummary(user1) shouldBe UserNotificationSummary(
                readCount = 1,
                unreadCount = 1,
            )
            queries.getUserNotificationSummary(user2) shouldBe UserNotificationSummary(
                readCount = 0,
                unreadCount = 1,
            )
        }
    }
})
