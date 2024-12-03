package no.nav.mulighetsrommet.api.avtale.task

import arrow.core.nonEmptyListOf
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.mockk.mockk
import io.mockk.verify
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.notifications.NotificationRepository
import java.time.LocalDate
import java.util.*

class NotifySluttdatoForAvtalerNarmerSegTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    val currentDate = LocalDate.of(2023, 5, 31)

    val domain = MulighetsrommetTestDomain(
        arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
        tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging),
        avtaler = listOf(
            AvtaleFixtures.oppfolging.copy(
                id = UUID.randomUUID(),
                startDato = LocalDate.of(2021, 1, 1),
                sluttDato = currentDate.plusMonths(8),
                administratorer = listOf(NavAnsattFixture.ansatt1.navIdent),
            ),
            AvtaleFixtures.oppfolging.copy(
                id = UUID.randomUUID(),
                startDato = LocalDate.of(2021, 1, 1),
                sluttDato = currentDate.plusMonths(6),
                administratorer = listOf(NavAnsattFixture.ansatt1.navIdent),
            ),
            AvtaleFixtures.oppfolging.copy(
                id = UUID.randomUUID(),
                startDato = LocalDate.of(2021, 1, 1),
                sluttDato = currentDate.plusMonths(3),
                administratorer = listOf(NavAnsattFixture.ansatt1.navIdent),
            ),
            AvtaleFixtures.oppfolging.copy(
                id = UUID.randomUUID(),
                startDato = LocalDate.of(2021, 1, 1),
                sluttDato = currentDate.plusDays(14),
                administratorer = listOf(NavAnsattFixture.ansatt1.navIdent),
            ),
            AvtaleFixtures.oppfolging.copy(
                id = UUID.randomUUID(),
                startDato = LocalDate.of(2021, 1, 1),
                sluttDato = currentDate.plusDays(7),
                administratorer = listOf(NavAnsattFixture.ansatt1.navIdent),
            ),
            AvtaleFixtures.oppfolging.copy(
                id = UUID.randomUUID(),
                startDato = LocalDate.of(2022, 6, 7),
                sluttDato = LocalDate.of(2024, 1, 1),
                administratorer = listOf(NavAnsattFixture.ansatt1.navIdent),
            ),
        ),
    )

    beforeAny {
        domain.initialize(database.db)
    }

    context("getAllAvtalerSomNarmerSegSluttdato") {
        test("skal hente avtaler som har sluttdato om 8 md, 6 md, 3 md, 14 dager og 7 dager") {
            val task = NotifySluttdatoForAvtalerNarmerSeg(
                NotifySluttdatoForAvtalerNarmerSeg.Config(disabled = true),
                database.db,
                mockk(),
            )

            val result = task.getAllAvtalerSomNarmerSegSluttdato(today = currentDate)

            result.map { it.id } shouldContainExactlyInAnyOrder listOf(
                domain.avtaler[0].id,
                domain.avtaler[1].id,
                domain.avtaler[2].id,
                domain.avtaler[3].id,
                domain.avtaler[4].id,
            )
        }
    }

    context("notifySluttDatoNarmerSeg") {
        test("skal generere varsler til administratorer når sluttdato på gjennomføring nærmer seg") {
            val notifications: NotificationRepository = mockk(relaxed = true)
            val task = NotifySluttdatoForAvtalerNarmerSeg(
                NotifySluttdatoForAvtalerNarmerSeg.Config(disabled = true),
                database.db,
                notifications,
            )

            task.notifySluttDatoNarmerSeg(today = LocalDate.of(2023, 5, 1))

            verify(exactly = 1) {
                notifications.insert(
                    match {
                        it.targets == nonEmptyListOf(NavAnsattFixture.ansatt1.navIdent) &&
                            it.title == "Avtalen \"Avtalenavn\" utløper 01.01.2024"
                    },
                )
            }
        }
    }
})
