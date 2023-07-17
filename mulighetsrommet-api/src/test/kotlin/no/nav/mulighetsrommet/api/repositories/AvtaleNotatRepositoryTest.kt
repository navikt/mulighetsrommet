package no.nav.mulighetsrommet.api.repositories

import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.domain.dbo.AvtaleNotatDbo
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.utils.AvtaleNotatFilter
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import java.util.*

class AvtaleNotatRepositoryTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))
    val avtaleFixture = AvtaleFixtures(database)
    val domain = MulighetsrommetTestDomain()

    beforeEach {
        avtaleFixture.runBeforeTests()
    }

    context("Notater for avtale - CRUD") {
        test("CRUD") {
            domain.initialize(database.db)
            val avtale = avtaleFixture.createAvtaleForTiltakstype(id = UUID.randomUUID())
            val avtaleNotater = AvtaleNotatRepository(database.db)
            avtaleFixture.upsertAvtaler(listOf(avtale))

            val notat1 = AvtaleNotatDbo(
                id = UUID.randomUUID(),
                avtaleId = avtale.id,
                createdAt = null,
                updatedAt = null,
                opprettetAv = domain.ansatt1.navIdent,
                innhold = "Mitt første notat",
            )

            val notat2 = AvtaleNotatDbo(
                id = UUID.randomUUID(),
                avtaleId = avtale.id,
                createdAt = null,
                updatedAt = null,
                opprettetAv = domain.ansatt1.navIdent,
                innhold = "Mitt andre notat",
            )

            val notat3 = AvtaleNotatDbo(
                id = UUID.randomUUID(),
                avtaleId = avtale.id,
                createdAt = null,
                updatedAt = null,
                opprettetAv = domain.ansatt2.navIdent,
                innhold = "En kollega sitt notat",
            )

            val notatSomIkkeEksisterer = AvtaleNotatDbo(
                id = UUID.randomUUID(),
                avtaleId = avtale.id,
                createdAt = null,
                updatedAt = null,
                opprettetAv = domain.ansatt2.navIdent,
                innhold = "En kollega sitt notat",
            )

            // Upsert notater
            avtaleNotater.upsert(notat1).shouldBeRight()
            avtaleNotater.upsert(notat2).shouldBeRight()
            avtaleNotater.upsert(notat3).shouldBeRight()

            avtaleNotater.getAll(filter = AvtaleNotatFilter(avtaleId = avtale.id, opprettetAv = null)).shouldBeRight()
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

            avtaleNotater.getAll(filter = AvtaleNotatFilter(avtaleId = avtale.id, opprettetAv = null)).shouldBeRight()
                .should { it.size shouldBe 2 }
        }
    }
})
