package no.nav.mulighetsrommet.api.services

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.ktor.http.*
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.domain.dbo.AvtaleNotatDbo
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.repositories.AvtaleNotatRepository
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import java.util.*

class AvtaleNotatServiceTest : FunSpec({
    context("AvtalenotatService") {
        val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))
        val avtaleFixture = AvtaleFixtures(database)
        val domain = MulighetsrommetTestDomain()

        beforeEach {
            avtaleFixture.runBeforeTests()
            domain.initialize(database.db)
        }

        test("Skal få slette notat når du har opprettet notatet") {
            val avtaleNotatRepository = AvtaleNotatRepository(database.db)
            val avtaleNotatService = AvtaleNotatService(avtaleNotatRepository)
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

            // Upsert notater
            avtaleNotater.upsert(notat1).shouldBeRight()
            avtaleNotater.upsert(notat2).shouldBeRight()
            avtaleNotater.upsert(notat3).shouldBeRight()

            avtaleNotatService.delete(notat1.id, domain.ansatt1.navIdent).shouldBeRight(1)
        }

        test("Skal ikke få slette notat når du ikke har opprettet notatet selv") {
            val avtaleNotatRepository = AvtaleNotatRepository(database.db)
            val avtaleNotatService = AvtaleNotatService(avtaleNotatRepository)
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

            // Upsert notater
            avtaleNotater.upsert(notat1).shouldBeRight()
            avtaleNotater.upsert(notat2).shouldBeRight()
            avtaleNotater.upsert(notat3).shouldBeRight()

            avtaleNotatService.delete(notat3.id, domain.ansatt1.navIdent).shouldBeLeft().should {
                it.status shouldBe HttpStatusCode.Forbidden
                it.message shouldBe "Kan ikke slette notat som du ikke har opprettet selv."
            }
        }
    }
})
