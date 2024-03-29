package no.nav.mulighetsrommet.api.repositories

import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.domain.dbo.TiltaksgjennomforingNotatDbo
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.utils.NotatFilter
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import java.util.*

class TiltaksgjennomforingNotatRepositoryTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    val domain = MulighetsrommetTestDomain(
        arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
        tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging),
        avtaler = listOf(AvtaleFixtures.oppfolging),
        gjennomforinger = listOf(TiltaksgjennomforingFixtures.Oppfolging1),
    )

    context("Notater for tiltaksgjennomføring - CRUD") {
        test("CRUD") {
            domain.initialize(database.db)
            val gjennomforing = domain.gjennomforinger[0]

            val notater = TiltaksgjennomforingNotatRepository(database.db)

            val notat1 = TiltaksgjennomforingNotatDbo(
                id = UUID.randomUUID(),
                tiltaksgjennomforingId = gjennomforing.id,
                createdAt = null,
                updatedAt = null,
                opprettetAv = NavAnsattFixture.ansatt1.navIdent,
                innhold = "Mitt første notat",
            )

            val notat2 = TiltaksgjennomforingNotatDbo(
                id = UUID.randomUUID(),
                tiltaksgjennomforingId = gjennomforing.id,
                createdAt = null,
                updatedAt = null,
                opprettetAv = NavAnsattFixture.ansatt1.navIdent,
                innhold = "Mitt andre notat",
            )

            val notat3 = TiltaksgjennomforingNotatDbo(
                id = UUID.randomUUID(),
                tiltaksgjennomforingId = gjennomforing.id,
                createdAt = null,
                updatedAt = null,
                opprettetAv = NavAnsattFixture.ansatt2.navIdent,
                innhold = "En kollega sitt notat",
            )

            val notatSomIkkeEksisterer = TiltaksgjennomforingNotatDbo(
                id = UUID.randomUUID(),
                tiltaksgjennomforingId = gjennomforing.id,
                createdAt = null,
                updatedAt = null,
                opprettetAv = NavAnsattFixture.ansatt2.navIdent,
                innhold = "En kollega sitt notat",
            )

            // Upsert notater
            notater.upsert(notat1).shouldBeRight()
            notater.upsert(notat2).shouldBeRight()
            notater.upsert(notat3).shouldBeRight()

            notater.getAll(
                filter = NotatFilter(
                    tiltaksgjennomforingId = gjennomforing.id,
                    opprettetAv = null,
                    avtaleId = null,
                ),
            ).shouldBeRight().should {
                it.size shouldBe 3
            }

            // Les notater
            notater.get(notat1.id).shouldBeRight().should {
                it?.innhold shouldBe "Mitt første notat"
            }

            notater.upsert(notat1.copy(innhold = "Mitt første notat med oppdatert innhold")).shouldBeRight()
            notater.get(notat1.id).shouldBeRight().should {
                it?.innhold shouldBe "Mitt første notat med oppdatert innhold"
            }

            notater.get(notatSomIkkeEksisterer.id).shouldBeRight(null)

            // Slett notater
            notater.delete(notat1.id).shouldBeRight()
            notater.get(notat1.id).shouldBeRight(null)

            notater.getAll(
                filter = NotatFilter(
                    tiltaksgjennomforingId = gjennomforing.id,
                    opprettetAv = null,
                    avtaleId = UUID.randomUUID(),
                ),
            ).shouldBeRight()
                .should { it.size shouldBe 2 }
        }
    }
})
