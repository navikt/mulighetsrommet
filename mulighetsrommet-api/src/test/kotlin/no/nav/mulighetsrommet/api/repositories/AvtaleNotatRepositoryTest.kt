package no.nav.mulighetsrommet.api.repositories

import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.AvtaleNotatFixture
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.api.utils.NotatFilter
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.truncateAll
import java.util.*

class AvtaleNotatRepositoryTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))
    val domain = MulighetsrommetTestDomain()

    beforeEach {
        database.db.truncateAll()
        domain.initialize(database.db)
    }

    context("Notater for avtale - CRUD") {
        test("tx") {
            val avtaleNotater = AvtaleNotatRepository(database.db)
            val notat1 = AvtaleNotatFixture.Notat1.copy(id = UUID.randomUUID(), innhold = "Mitt første notat")
            val notat2 = AvtaleNotatFixture.Notat1.copy(id = UUID.randomUUID(), innhold = "Mitt andre notat")

            shouldThrow<Exception> {
                database.db.transaction { tx ->
                    avtaleNotater.upsert(notat1, tx).shouldBeRight()
                    avtaleNotater.upsert(notat2, tx).shouldBeRight()
                    throw Exception("asdf")
                }
            }

            avtaleNotater.get(notat1.id).shouldBeRight(null)
        }

        test("CRUD") {
            val avtaleNotater = AvtaleNotatRepository(database.db)

            val notat1 = AvtaleNotatFixture.Notat1.copy(id = UUID.randomUUID(), innhold = "Mitt første notat")
            val notat2 = AvtaleNotatFixture.Notat1.copy(id = UUID.randomUUID(), innhold = "Mitt andre notat")
            val notat3 = AvtaleNotatFixture.Notat1.copy(
                id = UUID.randomUUID(),
                innhold = "En kollega sitt notat",
                opprettetAv = NavAnsattFixture.ansatt2.navIdent,
            )
            val notatSomIkkeEksisterer = AvtaleNotatFixture.Notat1.copy(innhold = "Dette notatet eksisterer ikke...👻")

            // Upsert notater
            avtaleNotater.upsert(notat1).shouldBeRight()
            avtaleNotater.upsert(notat2).shouldBeRight()
            avtaleNotater.upsert(notat3).shouldBeRight()

            avtaleNotater.getAll(filter = NotatFilter(avtaleId = AvtaleFixtures.avtale1.id, opprettetAv = null))
                .shouldBeRight()
                .should { it.size shouldBe 3 }

            // Les notater
            avtaleNotater.get(notat1.id).shouldBeRight().should {
                it?.innhold shouldBe "Mitt første notat"
            }

            avtaleNotater.upsert(notat1.copy(innhold = "Mitt første notat med oppdatert innhold")).shouldBeRight()
            avtaleNotater.get(notat1.id).shouldBeRight().should {
                it?.innhold shouldBe "Mitt første notat med oppdatert innhold"
            }

            avtaleNotater.get(notatSomIkkeEksisterer.id).shouldBeRight(null)

            // Slett notater
            avtaleNotater.delete(notat1.id).shouldBeRight()
            avtaleNotater.get(notat1.id).shouldBeRight(null)

            avtaleNotater.getAll(filter = NotatFilter(avtaleId = AvtaleFixtures.avtale1.id, opprettetAv = null))
                .shouldBeRight()
                .should { it.size shouldBe 2 }
        }
    }
})
