package no.nav.mulighetsrommet.api.navansatt

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.mulighetsrommet.api.clients.msgraph.AzureAdNavAnsatt
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures
import no.nav.mulighetsrommet.api.navansatt.db.NavAnsattDbo
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsattDto
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsattRolle.*
import no.nav.mulighetsrommet.api.navenhet.NavEnhetService
import no.nav.mulighetsrommet.api.sanity.SanityService
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.notifications.NotificationTask
import no.nav.mulighetsrommet.notifications.NotificationType
import no.nav.mulighetsrommet.notifications.ScheduledNotification
import java.time.LocalDate

class NavAnsattSyncServiceTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    val domain = MulighetsrommetTestDomain(
        avtaler = listOf(),
    )

    beforeEach {
        domain.initialize(database.db)
    }

    afterEach {
        database.truncateAll()
    }

    fun toAzureAdNavAnsattDto(dbo: NavAnsattDbo) = AzureAdNavAnsatt(
        azureId = dbo.azureId,
        navIdent = dbo.navIdent,
        fornavn = dbo.fornavn,
        etternavn = dbo.etternavn,
        hovedenhetKode = NavEnhetFixtures.Innlandet.enhetsnummer,
        hovedenhetNavn = NavEnhetFixtures.Innlandet.navn,
        mobilnummer = dbo.mobilnummer,
        epost = dbo.epost,
    )

    val ansatt1 = toAzureAdNavAnsattDto(NavAnsattFixture.ansatt1)
    val ansatt2 = toAzureAdNavAnsattDto(NavAnsattFixture.ansatt2)

    val notificationTask: NotificationTask = mockk()
    val sanityService: SanityService = mockk(relaxed = true)
    val navAnsattService: NavAnsattService = mockk()

    fun createaService() = NavAnsattSyncService(
        db = database.db,
        navAnsattService = navAnsattService,
        sanityService = sanityService,
        navEnhetService = NavEnhetService(database.db),
        notificationTask = notificationTask,
    )

    context("should schedule nav_ansatt to be deleted when they are not in the list of ansatte to sync") {
        val today = LocalDate.now()
        val deletionDate = today.plusDays(1)

        val service = createaService()

        test("begge finnes i azure => ingen skal slettes") {
            coEvery { navAnsattService.getNavAnsatteFromAzure() } returns listOf(
                NavAnsattDto.fromAzureAdNavAnsatt(ansatt1, setOf(TILTAKADMINISTRASJON_GENERELL)),
                NavAnsattDto.fromAzureAdNavAnsatt(ansatt2, setOf(TILTAKADMINISTRASJON_GENERELL)),
            )

            service.synchronizeNavAnsatte(today, deletionDate)

            database.run {
                queries.ansatt.getAll() shouldContainExactlyInAnyOrder listOf(
                    NavAnsattDto.fromAzureAdNavAnsatt(ansatt1, setOf(TILTAKADMINISTRASJON_GENERELL)),
                    NavAnsattDto.fromAzureAdNavAnsatt(ansatt2, setOf(TILTAKADMINISTRASJON_GENERELL)),
                )
            }
        }

        test("kontaktperson fra azure => den andre satt til sletting") {
            coEvery { navAnsattService.getNavAnsatteFromAzure() } returns listOf(
                NavAnsattDto.fromAzureAdNavAnsatt(ansatt2, setOf(KONTAKTPERSON)),
            )

            service.synchronizeNavAnsatte(today, deletionDate)

            database.run {
                queries.ansatt.getAll() shouldContainExactlyInAnyOrder listOf(
                    NavAnsattDto.fromAzureAdNavAnsatt(ansatt1, setOf()).copy(
                        skalSlettesDato = deletionDate,
                    ),
                    NavAnsattDto.fromAzureAdNavAnsatt(ansatt2, setOf(KONTAKTPERSON)),
                )
            }
        }

        test("ingen fra azure => begge satt til sletting") {
            coEvery { navAnsattService.getNavAnsatteFromAzure() } returns emptyList()
            service.synchronizeNavAnsatte(today, deletionDate)

            database.run {
                queries.ansatt.getAll() shouldContainExactlyInAnyOrder listOf(
                    NavAnsattDto.fromAzureAdNavAnsatt(ansatt1, setOf()).copy(
                        skalSlettesDato = deletionDate,
                    ),
                    NavAnsattDto.fromAzureAdNavAnsatt(ansatt2, setOf()).copy(
                        skalSlettesDato = deletionDate,
                    ),
                )
            }
        }
    }

    context("should delete nav_ansatt when their deletion date matches the provided deletion date") {
        val service = createaService()

        val today = LocalDate.now()

        test("kontaktperson fra azure => da skal den ikke slettes") {
            coEvery { navAnsattService.getNavAnsatteFromAzure() } returns listOf(
                NavAnsattDto.fromAzureAdNavAnsatt(ansatt2, setOf(KONTAKTPERSON)),
            )
            service.synchronizeNavAnsatte(today, deletionDate = today)

            database.run {
                queries.ansatt.getAll() shouldContainExactlyInAnyOrder listOf(
                    NavAnsattDto.fromAzureAdNavAnsatt(ansatt2, setOf(KONTAKTPERSON)),
                )
            }
        }

        test("ingen fra azure => begge har blitt slettet") {
            coEvery { navAnsattService.getNavAnsatteFromAzure() } returns emptyList()
            service.synchronizeNavAnsatte(today, deletionDate = today)

            database.run {
                queries.ansatt.getAll().shouldBeEmpty()
            }
        }
    }

    test("varsler administratorer basert på hovedenhet når avtale ikke lengre har administrator") {
        MulighetsrommetTestDomain(
            avtaler = listOf(AvtaleFixtures.AFT.copy(administratorer = listOf(ansatt1.navIdent))),
        ).initialize(database.db)

        every { notificationTask.scheduleNotification(any(), any()) } returns Unit

        coEvery { navAnsattService.getNavAnsatteFromAzure() } returns listOf(
            NavAnsattDto.fromAzureAdNavAnsatt(ansatt2, setOf(AVTALER_SKRIV)),
        )

        val today = LocalDate.now()
        val service = createaService()
        service.synchronizeNavAnsatte(today, deletionDate = today)

        verify(exactly = 1) {
            val expectedNotification: ScheduledNotification = match {
                it.type == NotificationType.TASK && it.targets.containsAll(listOf(ansatt2.navIdent))
            }
            notificationTask.scheduleNotification(expectedNotification, any())
        }
    }
})
