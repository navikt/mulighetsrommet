package no.nav.mulighetsrommet.api.gjennomforing.task

import arrow.core.nonEmptyListOf
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.mockk.mockk
import io.mockk.verify
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures.Oppfolging1
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.notifications.NotificationRepository
import no.nav.mulighetsrommet.notifications.NotificationService
import java.time.LocalDate
import java.util.*

class NotifySluttdatoForGjennomforingerNarmerSegTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    val domain = MulighetsrommetTestDomain(
        tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging),
        avtaler = listOf(AvtaleFixtures.oppfolging),
        gjennomforinger = listOf(
            Oppfolging1.copy(
                id = UUID.randomUUID(),
                sluttDato = LocalDate.of(2023, 5, 30),
                administratorer = listOf(NavAnsattFixture.ansatt1.navIdent),
            ),
            Oppfolging1.copy(
                id = UUID.randomUUID(),
                sluttDato = LocalDate.of(2023, 5, 23),
                administratorer = listOf(NavAnsattFixture.ansatt1.navIdent),
            ),
            Oppfolging1.copy(
                id = UUID.randomUUID(),
                sluttDato = LocalDate.of(2023, 5, 17),
                administratorer = listOf(NavAnsattFixture.ansatt1.navIdent),
            ),
            Oppfolging1.copy(
                id = UUID.randomUUID(),
                sluttDato = LocalDate.of(2023, 5, 26),
                administratorer = listOf(NavAnsattFixture.ansatt1.navIdent),
            ),
        ),
    )

    fun createTask(
        notificationService: NotificationService = NotificationService(
            database.db,
            NotificationRepository(database.db),
        ),
    ) = NotifySluttdatoForGjennomforingerNarmerSeg(
        NotifySluttdatoForGjennomforingerNarmerSeg.Config(disabled = true),
        database.db,
        notificationService,
    )

    beforeAny {
        domain.initialize(database.db)
    }

    context("getAllGjennomforingerSomNarmerSegSluttdato") {
        test("skal hente gjennomføringer som er 14, 7 eller 1 dag til sluttdato") {
            val task = createTask()

            val result = task.getAllGjennomforingerSomNarmerSegSluttdato(today = LocalDate.of(2023, 5, 16))

            result.map { Pair(it.id, it.administratorer) } shouldContainExactlyInAnyOrder listOf(
                Pair(domain.gjennomforinger[0].id, listOf(NavAnsattFixture.ansatt1.navIdent)),
                Pair(domain.gjennomforinger[1].id, listOf(NavAnsattFixture.ansatt1.navIdent)),
                Pair(domain.gjennomforinger[2].id, listOf(NavAnsattFixture.ansatt1.navIdent)),
            )
        }
    }

    context("notifySluttDatoNarmerSeg") {
        test("skal generere varsler til administratorer når sluttdato på gjennomføring nærmer seg") {
            val notificationService: NotificationService = mockk(relaxed = true)
            val task = createTask(notificationService)

            task.notifySluttDatoNarmerSeg(today = LocalDate.of(2023, 5, 25))

            verify(exactly = 1) {
                notificationService.scheduleNotification(
                    match {
                        it.targets == nonEmptyListOf(NavAnsattFixture.ansatt1.navIdent) &&
                            it.title == "Gjennomføringen \"Oppfølging 1\" utløper 26.05.2023"
                    },
                    any(),
                )
            }
        }
    }
})
