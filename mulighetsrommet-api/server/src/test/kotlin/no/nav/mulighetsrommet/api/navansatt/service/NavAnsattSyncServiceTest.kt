package no.nav.mulighetsrommet.api.navansatt.service

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.mulighetsrommet.api.domain.navansatt.NavAnsattRolle
import no.nav.mulighetsrommet.api.domain.navansatt.Rolle.TILTAKADMINISTRASJON_GENERELL
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures
import no.nav.mulighetsrommet.api.sanity.SanityService
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import java.time.LocalDate

class NavAnsattSyncServiceTest : FunSpec({
    val database = extension(ApiDatabaseTestListener())

    val ansatt1 = NavAnsattFixture.DonaldDuck.copy(
        roller = setOf(NavAnsattRolle.generell(TILTAKADMINISTRASJON_GENERELL)),
    )
    val ansatt2 = NavAnsattFixture.MikkeMus.copy(
        roller = setOf(NavAnsattRolle.generell(TILTAKADMINISTRASJON_GENERELL)),
    )
    val domain = MulighetsrommetTestDomain(
        navEnheter = listOf(NavEnhetFixtures.Innlandet),
        ansatte = listOf(ansatt1, ansatt2),
        arrangorer = listOf(),
    )

    beforeEach {
        domain.initialize(database.db)
    }

    afterEach {
        database.truncateAll()
    }

    val sanityService: SanityService = mockk(relaxed = true)
    val navAnsattService: NavAnsattService = mockk()

    fun createService() = NavAnsattSyncService(
        db = database.db,
        navAnsattService = navAnsattService,
        sanityService = sanityService,
    )

    context("should schedule nav_ansatt to be deleted when they are not in the list of ansatte to sync") {
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)

        val service = createService()

        test("begge finnes i azure => ingen skal slettes") {
            coEvery { navAnsattService.getNavAnsatteForAllRoles() } returns listOf(
                ansatt1,
                ansatt2,
            )

            service.synchronizeNavAnsatte(today, deletionDate = tomorrow)

            database.run {
                queries.ansatt.getAll() shouldContainExactlyInAnyOrder listOf(ansatt1, ansatt2)
            }
        }

        test("bare en ansatt i gruppen => den andre satt til sletting og roller blir fratatt") {
            coEvery { navAnsattService.getNavAnsatteForAllRoles() } returns listOf(
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
            coEvery { navAnsattService.getNavAnsatteForAllRoles() } returns emptyList()

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
        val service = createService()

        val today = LocalDate.now()

        test("bare en ansatt i gruppen => den andre blir slettet") {
            coEvery { navAnsattService.getNavAnsatteForAllRoles() } returns listOf(
                ansatt2,
            )

            service.synchronizeNavAnsatte(today, deletionDate = today)

            database.run {
                queries.ansatt.getAll() shouldContainExactlyInAnyOrder listOf(ansatt2)
            }
        }

        test("ingen ansatt i gruppen => begge blir slettet") {
            coEvery { navAnsattService.getNavAnsatteForAllRoles() } returns emptyList()

            service.synchronizeNavAnsatte(today, deletionDate = today)

            database.run {
                queries.ansatt.getAll().shouldBeEmpty()
            }
        }
    }
})
