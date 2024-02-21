package no.nav.mulighetsrommet.api.services

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.ktor.http.*
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.domain.dbo.AvtaleNotatDbo
import no.nav.mulighetsrommet.api.domain.dbo.TiltaksgjennomforingNotatDbo
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.repositories.AvtaleNotatRepository
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingNotatRepository
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.repositories.TiltakstypeRepository
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import java.util.*

class NotatServiceTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    context("NotatService") {
        val domain = MulighetsrommetTestDomain()

        beforeEach {
            domain.initialize(database.db)
        }

        test("Skal få slette notat for avtale når du har opprettet notatet") {
            val avtaleNotatRepository = AvtaleNotatRepository(database.db)
            val tiltaksgjennomforingNotatRepository = TiltaksgjennomforingNotatRepository(database.db)
            val notatService = NotatService(avtaleNotatRepository, tiltaksgjennomforingNotatRepository)
            val avtale = AvtaleFixtures.oppfolging
            val avtaleNotater = AvtaleNotatRepository(database.db)

            val notat1 = AvtaleNotatDbo(
                id = UUID.randomUUID(),
                avtaleId = avtale.id,
                createdAt = null,
                updatedAt = null,
                opprettetAv = NavAnsattFixture.ansatt1.navIdent,
                innhold = "Mitt første notat",
            )

            val notat2 = AvtaleNotatDbo(
                id = UUID.randomUUID(),
                avtaleId = avtale.id,
                createdAt = null,
                updatedAt = null,
                opprettetAv = NavAnsattFixture.ansatt1.navIdent,
                innhold = "Mitt andre notat",
            )

            val notat3 = AvtaleNotatDbo(
                id = UUID.randomUUID(),
                avtaleId = avtale.id,
                createdAt = null,
                updatedAt = null,
                opprettetAv = NavAnsattFixture.ansatt2.navIdent,
                innhold = "En kollega sitt notat",
            )

            // Upsert notater
            avtaleNotater.upsert(notat1).shouldBeRight()
            avtaleNotater.upsert(notat2).shouldBeRight()
            avtaleNotater.upsert(notat3).shouldBeRight()

            notatService.deleteAvtaleNotat(notat1.id, NavAnsattFixture.ansatt1.navIdent).shouldBeRight(1)
        }

        test("Skal ikke få slette notat for avtale når du ikke har opprettet notatet selv") {
            val avtaleNotatRepository = AvtaleNotatRepository(database.db)
            val tiltaksgjennomforingNotatRepository = TiltaksgjennomforingNotatRepository(database.db)
            val notatService = NotatService(avtaleNotatRepository, tiltaksgjennomforingNotatRepository)
            val avtale = AvtaleFixtures.oppfolging
            val avtaleNotater = AvtaleNotatRepository(database.db)

            val notat1 = AvtaleNotatDbo(
                id = UUID.randomUUID(),
                avtaleId = avtale.id,
                createdAt = null,
                updatedAt = null,
                opprettetAv = NavAnsattFixture.ansatt1.navIdent,
                innhold = "Mitt første notat",
            )

            val notat2 = AvtaleNotatDbo(
                id = UUID.randomUUID(),
                avtaleId = avtale.id,
                createdAt = null,
                updatedAt = null,
                opprettetAv = NavAnsattFixture.ansatt1.navIdent,
                innhold = "Mitt andre notat",
            )

            val notat3 = AvtaleNotatDbo(
                id = UUID.randomUUID(),
                avtaleId = avtale.id,
                createdAt = null,
                updatedAt = null,
                opprettetAv = NavAnsattFixture.ansatt2.navIdent,
                innhold = "En kollega sitt notat",
            )

            // Upsert notater
            avtaleNotater.upsert(notat1).shouldBeRight()
            avtaleNotater.upsert(notat2).shouldBeRight()
            avtaleNotater.upsert(notat3).shouldBeRight()

            notatService.deleteAvtaleNotat(notat3.id, NavAnsattFixture.ansatt1.navIdent).shouldBeLeft().should {
                it.status shouldBe HttpStatusCode.Forbidden
                it.message shouldBe "Kan ikke slette notat som du ikke har opprettet selv."
            }
        }

        test("Skal få slette notat for tiltaksgjennomføring når du har opprettet notatet selv") {
            val tiltaksgjennomforingNotatRepository = TiltaksgjennomforingNotatRepository(database.db)
            val avtaleNotatRepository = AvtaleNotatRepository(database.db)
            val notatService = NotatService(avtaleNotatRepository, tiltaksgjennomforingNotatRepository)
            val tiltakstypeFixture = TiltakstypeFixtures
            val tiltakstyper = TiltakstypeRepository(database.db)
            tiltakstyper.upsert(tiltakstypeFixture.Arbeidstrening)
            tiltakstyper.upsert(tiltakstypeFixture.Oppfolging)
            val avtale = AvtaleFixtures.oppfolging
            val tiltaksgjennomforingFixtures = TiltaksgjennomforingFixtures
            val gjennomforing = tiltaksgjennomforingFixtures.Oppfolging1.copy(avtaleId = avtale.id)
            val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)
            tiltaksgjennomforinger.upsert(gjennomforing)

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

            // Upsert notater
            tiltaksgjennomforingNotatRepository.upsert(notat1).shouldBeRight()
            tiltaksgjennomforingNotatRepository.upsert(notat2).shouldBeRight()
            tiltaksgjennomforingNotatRepository.upsert(notat3).shouldBeRight()

            notatService.deleteTiltaksgjennomforingNotat(notat1.id, NavAnsattFixture.ansatt1.navIdent).shouldBeRight(1)
        }

        test("Skal ikke få slette notat for tiltaksgjennomføring når du ikke har opprettet notatet selv") {
            val tiltaksgjennomforingNotatRepository = TiltaksgjennomforingNotatRepository(database.db)
            val avtaleNotatRepository = AvtaleNotatRepository(database.db)
            val notatService = NotatService(avtaleNotatRepository, tiltaksgjennomforingNotatRepository)
            val tiltakstypeFixture = TiltakstypeFixtures
            val tiltakstyper = TiltakstypeRepository(database.db)
            tiltakstyper.upsert(tiltakstypeFixture.Arbeidstrening)
            tiltakstyper.upsert(tiltakstypeFixture.Oppfolging)
            val avtale = AvtaleFixtures.oppfolging
            val tiltaksgjennomforingFixtures = TiltaksgjennomforingFixtures
            val gjennomforing = tiltaksgjennomforingFixtures.Oppfolging1.copy(avtaleId = avtale.id)
            val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)
            tiltaksgjennomforinger.upsert(gjennomforing)

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

            // Upsert notater
            tiltaksgjennomforingNotatRepository.upsert(notat1).shouldBeRight()
            tiltaksgjennomforingNotatRepository.upsert(notat2).shouldBeRight()
            tiltaksgjennomforingNotatRepository.upsert(notat3).shouldBeRight()

            notatService.deleteTiltaksgjennomforingNotat(notat3.id, NavAnsattFixture.ansatt1.navIdent).shouldBeLeft()
                .should {
                    it.status shouldBe HttpStatusCode.Forbidden
                    it.message shouldBe "Kan ikke slette notat som du ikke har opprettet selv."
                }
        }
    }
})
