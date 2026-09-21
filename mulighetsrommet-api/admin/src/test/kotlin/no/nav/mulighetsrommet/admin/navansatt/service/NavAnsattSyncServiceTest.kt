package no.nav.mulighetsrommet.admin.navansatt.service

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.mulighetsrommet.admin.testing.TestAdminDatabase
import no.nav.mulighetsrommet.api.domain.navansatt.NavAnsattRolle
import no.nav.mulighetsrommet.api.domain.navansatt.Rolle.TILTAKADMINISTRASJON_GENERELL
import no.nav.mulighetsrommet.api.domain.testing.fixture.NavAnsattFixture
import java.time.LocalDate

class NavAnsattSyncServiceTest : FunSpec({
    val ansatt1 = NavAnsattFixture.DonaldDuck.medRoller(
        roller = setOf(NavAnsattRolle.generell(TILTAKADMINISTRASJON_GENERELL)),
    )
    val ansatt2 = NavAnsattFixture.MikkeMus.medRoller(
        roller = setOf(NavAnsattRolle.generell(TILTAKADMINISTRASJON_GENERELL)),
    )

    fun setup(): Triple<TestAdminDatabase, NavAnsattService, NavAnsattSyncService> {
        val db = TestAdminDatabase()
        db.repository.navAnsatt.save(ansatt1)
        db.repository.navAnsatt.save(ansatt2)
        val navAnsattService = mockk<NavAnsattService>()
        val service = NavAnsattSyncService(db = db, navAnsattService = navAnsattService)
        return Triple(db, navAnsattService, service)
    }

    context("should schedule nav_ansatt to be deleted when they are not in the list of ansatte to sync") {
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)

        test("begge finnes i azure => ingen skal slettes") {
            val (db, navAnsattService, service) = setup()

            coEvery { navAnsattService.getNavAnsatteForAllRoles() } returns listOf(
                ansatt1,
                ansatt2,
            )

            service.synchronizeNavAnsatte(today, deletionDate = tomorrow)

            db.repository.navAnsatt.getAll() shouldContainExactlyInAnyOrder listOf(ansatt1, ansatt2)
        }

        test("bare en ansatt i gruppen => den andre satt til sletting og roller blir fratatt") {
            val (db, navAnsattService, service) = setup()

            coEvery { navAnsattService.getNavAnsatteForAllRoles() } returns listOf(
                ansatt2,
            )

            service.synchronizeNavAnsatte(today, deletionDate = tomorrow)

            db.repository.navAnsatt.getAll() shouldContainExactlyInAnyOrder listOf(
                ansatt1.skalSlettes(tomorrow),
                ansatt2,
            )
        }

        test("ingen fra azure => begge satt til sletting") {
            val (db, navAnsattService, service) = setup()

            coEvery { navAnsattService.getNavAnsatteForAllRoles() } returns emptyList()

            service.synchronizeNavAnsatte(today, deletionDate = tomorrow)

            db.repository.navAnsatt.getAll() shouldContainExactlyInAnyOrder listOf(
                ansatt1.skalSlettes(tomorrow),
                ansatt2.skalSlettes(tomorrow),
            )
        }
    }

    context("should delete nav_ansatt when their deletion date matches the provided deletion date") {
        val today = LocalDate.now()

        test("bare en ansatt i gruppen => den andre blir slettet") {
            val (db, navAnsattService, service) = setup()

            coEvery { navAnsattService.getNavAnsatteForAllRoles() } returns listOf(
                ansatt2,
            )

            service.synchronizeNavAnsatte(today, deletionDate = today)

            db.repository.navAnsatt.getAll() shouldContainExactlyInAnyOrder listOf(ansatt2)
        }

        test("ingen ansatt i gruppen => begge blir slettet") {
            val (db, navAnsattService, service) = setup()

            coEvery { navAnsattService.getNavAnsatteForAllRoles() } returns emptyList()

            service.synchronizeNavAnsatte(today, deletionDate = today)

            db.repository.navAnsatt.getAll().shouldBeEmpty()
        }
    }
})
