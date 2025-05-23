package no.nav.mulighetsrommet.api.navansatt.service

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures
import no.nav.mulighetsrommet.api.navansatt.db.NavAnsattDbo
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsatt
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsattRolle
import no.nav.mulighetsrommet.api.navansatt.model.Rolle.TILTAKADMINISTRASJON_GENERELL
import no.nav.mulighetsrommet.api.navenhet.NavEnhetService
import no.nav.mulighetsrommet.api.sanity.SanityService
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.notifications.NotificationTask
import java.time.LocalDate
import java.util.*

class NavAnsattSyncServiceTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    val domain = MulighetsrommetTestDomain(
        navEnheter = listOf(NavEnhetFixtures.Innlandet),
        ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
        arrangorer = listOf(),
        avtaler = listOf(),
    ) {
        queries.ansatt.setRoller(
            NavAnsattFixture.DonaldDuck.navIdent,
            setOf(NavAnsattRolle.generell(TILTAKADMINISTRASJON_GENERELL)),
        )
        queries.ansatt.setRoller(
            NavAnsattFixture.MikkeMus.navIdent,
            setOf(NavAnsattRolle.generell(TILTAKADMINISTRASJON_GENERELL)),
        )
    }

    beforeEach {
        domain.initialize(database.db)
    }

    afterEach {
        database.truncateAll()
    }

    val notificationTask: NotificationTask = mockk()
    val sanityService: SanityService = mockk(relaxed = true)
    val navAnsattService: NavAnsattService = mockk()

    val ansattGroupsToSync = setOf(UUID.randomUUID())

    fun createService(groupsToSync: Set<UUID>) = NavAnsattSyncService(
        config = NavAnsattSyncService.Config(groupsToSync),
        db = database.db,
        navAnsattService = navAnsattService,
        sanityService = sanityService,
        navEnhetService = NavEnhetService(database.db),
        notificationTask = notificationTask,
    )

    val ansatt1 =
        NavAnsattFixture.DonaldDuck.toNavAnsattDto(setOf(NavAnsattRolle.generell(TILTAKADMINISTRASJON_GENERELL)))
    val ansatt2 =
        NavAnsattFixture.MikkeMus.toNavAnsattDto(setOf(NavAnsattRolle.generell(TILTAKADMINISTRASJON_GENERELL)))

    context("should schedule nav_ansatt to be deleted when they are not in the list of ansatte to sync") {
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)

        val service = createService(ansattGroupsToSync)

        test("begge finnes i azure => ingen skal slettes") {
            coEvery { navAnsattService.getNavAnsatteInGroups(ansattGroupsToSync) } returns listOf(
                ansatt1,
                ansatt2,
            )

            service.synchronizeNavAnsatte(today, deletionDate = tomorrow)

            database.run {
                queries.ansatt.getAll() shouldContainExactlyInAnyOrder listOf(ansatt1, ansatt2)
            }
        }

        test("bare en ansatt i gruppen => den andre satt til sletting og roller blir fratatt") {
            coEvery { navAnsattService.getNavAnsatteInGroups(ansattGroupsToSync) } returns listOf(
                ansatt2,
            )

            service.synchronizeNavAnsatte(today, deletionDate = tomorrow)

            database.run {
                queries.ansatt.getAll() shouldContainExactlyInAnyOrder listOf(
                    ansatt1.copy(
                        roller = setOf(),
                        skalSlettesDato = tomorrow,
                    ),
                    ansatt2,
                )
            }
        }

        test("ingen fra azure => begge satt til sletting") {
            coEvery { navAnsattService.getNavAnsatteInGroups(ansattGroupsToSync) } returns emptyList()

            service.synchronizeNavAnsatte(today, deletionDate = tomorrow)

            database.run {
                queries.ansatt.getAll() shouldContainExactlyInAnyOrder listOf(
                    ansatt1.copy(
                        roller = setOf(),
                        skalSlettesDato = tomorrow,
                    ),
                    ansatt2.copy(
                        roller = setOf(),
                        skalSlettesDato = tomorrow,
                    ),
                )
            }
        }
    }

    context("should delete nav_ansatt when their deletion date matches the provided deletion date") {
        val service = createService(ansattGroupsToSync)

        val today = LocalDate.now()

        test("bare en ansatt i gruppen => den andre blir slettet") {
            coEvery { navAnsattService.getNavAnsatteInGroups(ansattGroupsToSync) } returns listOf(
                ansatt2,
            )

            service.synchronizeNavAnsatte(today, deletionDate = today)

            database.run {
                queries.ansatt.getAll() shouldContainExactlyInAnyOrder listOf(ansatt2)
            }
        }

        test("ingen ansatt i gruppen => begge blir slettet") {
            coEvery { navAnsattService.getNavAnsatteInGroups(ansattGroupsToSync) } returns emptyList()

            service.synchronizeNavAnsatte(today, deletionDate = today)

            database.run {
                queries.ansatt.getAll().shouldBeEmpty()
            }
        }
    }
})

private fun NavAnsattDbo.toNavAnsattDto(roller: Set<NavAnsattRolle>): NavAnsatt = NavAnsatt(
    entraObjectId = entraObjectId,
    navIdent = navIdent,
    fornavn = fornavn,
    etternavn = etternavn,
    hovedenhet = NavAnsatt.Hovedenhet(
        enhetsnummer = NavEnhetFixtures.Innlandet.enhetsnummer,
        navn = NavEnhetFixtures.Innlandet.navn,
    ),
    mobilnummer = mobilnummer,
    epost = epost,
    roller = roller,
    skalSlettesDato = null,
)
