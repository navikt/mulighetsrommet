package no.nav.mulighetsrommet.notifications

import arrow.core.nonEmptyListOf
import com.github.kagkarlsson.scheduler.Scheduler
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.should
import io.mockk.mockk
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.domain.dto.NavIdent
import no.nav.mulighetsrommet.tasks.DbSchedulerKotlinSerializer
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.time.Duration.Companion.seconds

class NotificationServiceTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))
    val domain = MulighetsrommetTestDomain()

    beforeEach {
        domain.initialize(database.db)
    }

    val user1 = NavAnsattFixture.ansatt1.navIdent
    val user2 = NavAnsattFixture.ansatt2.navIdent

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
            type = NotificationType.NOTIFICATION,
            title = "Notifikasjon for alle brukere",
            createdAt = now,
            targets = nonEmptyListOf(user1, user2),
            metadata = NotificationMetadata(
                linkText = "Trykk på meg",
                link = "/spennende-side",
            ),
        )

        fun ScheduledNotification.asUserNotification(user: NavIdent): UserNotification = run {
            UserNotification(
                id = id,
                type = type,
                title = title,
                description = description,
                user = user,
                createdAt = LocalDateTime.ofInstant(createdAt, ZoneOffset.systemDefault()),
                doneAt = null,
                metadata = NotificationMetadata(
                    linkText = "Trykk på meg",
                    link = "/spennende-side",
                ),
            )
        }

        test("scheduled notification should eventually be created for all targets") {
            service.scheduleNotification(notification, now)

            notifications.getAll() shouldBeRight listOf()

            eventually(30.seconds) {
                notifications.getAll().shouldBeRight().should {
                    it shouldContainExactlyInAnyOrder listOf(
                        notification.asUserNotification(user1),
                        notification.asUserNotification(user2),
                    )
                }
            }
        }
    }
})
