package no.nav.mulighetsrommet.api.okonomi.refusjon

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.fixtures.ArrangorFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.repositories.DeltakerRepository
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.truncateAll
import java.time.LocalDate

class RefusjonServiceTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    val domain = MulighetsrommetTestDomain(
        gjennomforinger = listOf(
            AFT1,
        ),
    )

    beforeEach {
        domain.initialize(database.db)
    }

    afterEach {
        database.db.truncateAll()
    }

    context("Generering av refusjonskrav") {
        val tiltaksgjennomforingRepository = TiltaksgjennomforingRepository(database.db)
        val refusjonskravRepository = RefusjonskravRepository(database.db)
        val deltakerRepository: DeltakerRepository = mockk()

        val service = RefusjonService(
            tiltaksgjennomforingRepository = tiltaksgjennomforingRepository,
            deltakerRepository = deltakerRepository,
            refusjonskravRepository = refusjonskravRepository,
            db = database.db,
        )

        test("generer et refusjonskrav") {
            tiltaksgjennomforingRepository.upsert(AFT1)
            every { deltakerRepository.getAll(AFT1.id) } returns emptyList()

            service.genererRefusjonskravForMonth(LocalDate.of(2023, 1, 1))

            val krav = service.getByArrangorIds(listOf(ArrangorFixtures.underenhet1.id))
            krav.size shouldBe 1
            krav[0].gjennomforing.id shouldBe AFT1.id
        }
    }
})
