package no.nav.mulighetsrommet.api.tasks

import arrow.core.nonEmptyListOf
import com.github.kagkarlsson.scheduler.Scheduler
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.should
import io.mockk.mockk
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.api.gjennomforing.task.InitialLoadTiltaksgjennomforinger
import no.nav.mulighetsrommet.api.navansatt.task.SynchronizeNavAnsatte
import no.nav.mulighetsrommet.api.refusjon.task.GenerateRefusjonskrav
import no.nav.mulighetsrommet.api.refusjon.task.JournalforRefusjonskrav
import no.nav.mulighetsrommet.api.tiltakstype.task.InitialLoadTiltakstyper
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.domain.dto.NavIdent
import no.nav.mulighetsrommet.notifications.*
import no.nav.mulighetsrommet.tasks.DbSchedulerKotlinSerializer
import no.nav.mulighetsrommet.utdanning.task.SynchronizeUtdanninger
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.time.Duration.Companion.seconds

class DbSchedulerClientTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))
    val domain = MulighetsrommetTestDomain()

    beforeEach {
        domain.initialize(database.db)
    }

    val user1 = NavAnsattFixture.ansatt1.navIdent
    val user2 = NavAnsattFixture.ansatt2.navIdent

    val journalforRefusjonskrav: JournalforRefusjonskrav = mockk(relaxed = true)
    val initialLoadTiltaksgjennomforinger: InitialLoadTiltaksgjennomforinger = mockk(relaxed = true)
    val initialLoadTiltakstyper: InitialLoadTiltakstyper = mockk(relaxed = true)
    val generateValidationReport: GenerateValidationReport = mockk(relaxed = true)
    val synchronizeNavAnsatte: SynchronizeNavAnsatte = mockk(relaxed = true)
    val synchronizeUtdanninger: SynchronizeUtdanninger = mockk(relaxed = true)
    val generateRefusjonskrav: GenerateRefusjonskrav = mockk(relaxed = true)

    context("DbSchedulerClient") {
        val notifications = NotificationRepository(database.db)
        val scheduleNotification = ScheduleNotification(notifications)
        val dbSchedulerClient = DbSchedulerClient(
            database.db,
            journalforRefusjonskrav,
            initialLoadTiltaksgjennomforinger,
            initialLoadTiltakstyper,
            generateValidationReport,
            synchronizeNavAnsatte,
            synchronizeUtdanninger,
            generateRefusjonskrav,
            scheduleNotification,
        )

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

        beforeEach {
            Scheduler
                .create(database.db.getDatasource(), scheduleNotification.task)
                .serializer(DbSchedulerKotlinSerializer())
                .build()
                .start()
        }

        // Denne tar 10 sek så har ikke lyst til å teste alle mulige tasks...
        test("scheduled notification should eventually be created for all targets") {
            dbSchedulerClient.scheduleNotification(notification, now)

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
