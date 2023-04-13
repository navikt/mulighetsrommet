package no.nav.mulighetsrommet.api.repositories

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.fixtures.Norg2EnhetFixture
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.utils.getOrThrow

class EnhetRepositoryTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    beforeEach {
        database.db.clean()
        database.db.migrate()
    }

    context("Synkronisering av enheter") {
        val enheter = EnhetRepository(database.db)

        test("Skal slette enheter fra liste med id'er") {
            val enhetSomSkalSlettes1 = Norg2EnhetFixture.enhet.copy(enhetId = 1).toNavEnhetDbo()
            val enhetSomSkalSlettes2 = Norg2EnhetFixture.enhet.copy(enhetId = 2).toNavEnhetDbo()
            val enhetSomSkalSlettes3 = Norg2EnhetFixture.enhet.copy(enhetId = 3).toNavEnhetDbo()
            val enhetSomSkalBeholdes1 = Norg2EnhetFixture.enhet.copy(enhetId = 4).toNavEnhetDbo()
            val enhetSomSkalBeholdes2 = Norg2EnhetFixture.enhet.copy(enhetId = 5).toNavEnhetDbo()

            enheter.upsert(enhetSomSkalSlettes1).getOrThrow()
            enheter.upsert(enhetSomSkalSlettes2).getOrThrow()
            enheter.upsert(enhetSomSkalSlettes3).getOrThrow()
            enheter.upsert(enhetSomSkalBeholdes1).getOrThrow()
            enheter.upsert(enhetSomSkalBeholdes2).getOrThrow()

            val count = enheter.getAll().size
            count shouldBe 5

            val idErForSletting = listOf(1, 2, 3)
            enheter.deleteWhereIds(idErForSletting)
            val countAfterDeletion = enheter.getAll().size
            countAfterDeletion shouldBe 2
        }
    }
})
