package no.nav.mulighetsrommet.api.repositories

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.fixtures.NavEnhetDboFixture
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
            val enhetSomSkalSlettes1 = NavEnhetDboFixture.enhetDbo.copy(enhetNr = "1")
            val enhetSomSkalSlettes2 = NavEnhetDboFixture.enhetDbo.copy(enhetNr = "2")
            val enhetSomSkalSlettes3 = NavEnhetDboFixture.enhetDbo.copy(enhetNr = "3")
            val enhetSomSkalBeholdes1 = NavEnhetDboFixture.enhetDbo.copy(enhetNr = "4")
            val enhetSomSkalBeholdes2 = NavEnhetDboFixture.enhetDbo.copy(enhetNr = "5")
            val overordnetEnhet = NavEnhetDboFixture.enhetDbo.copy(enhetNr = "1200")

            enheter.upsert(overordnetEnhet).getOrThrow()
            enheter.upsert(enhetSomSkalSlettes1).getOrThrow()
            enheter.upsert(enhetSomSkalSlettes2).getOrThrow()
            enheter.upsert(enhetSomSkalSlettes3).getOrThrow()
            enheter.upsert(enhetSomSkalBeholdes1).getOrThrow()
            enheter.upsert(enhetSomSkalBeholdes2).getOrThrow()

            val count = enheter.getAll().size
            count shouldBe 6

            val idErForSletting = listOf("1", "2", "3")
            enheter.deleteWhereEnhetsnummer(idErForSletting)
            val countAfterDeletion = enheter.getAll().size
            countAfterDeletion shouldBe 3
        }
    }
})
