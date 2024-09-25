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
import no.nav.mulighetsrommet.domain.dbo.DeltakerDbo
import no.nav.mulighetsrommet.domain.dbo.Deltakeropphav
import no.nav.mulighetsrommet.domain.dbo.Deltakerstatus
import no.nav.mulighetsrommet.domain.dto.Organisasjonsnummer
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

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

            val krav = service.getByOrgnr(listOf(Organisasjonsnummer(ArrangorFixtures.underenhet1.organisasjonsnummer)))
            krav.size shouldBe 1
            krav[0].tiltaksgjennomforing.id shouldBe AFT1.id
            krav[0].periodeStart shouldBe LocalDate.of(2023, 1, 1)
        }

        test("beregning av AFT én full deltaker") {
            every { deltakerRepository.getAll(AFT1.id) } returns listOf(
                DeltakerDbo(
                    id = UUID.randomUUID(),
                    tiltaksgjennomforingId = AFT1.id,
                    status = Deltakerstatus.DELTAR,
                    startDato = LocalDate.of(2023, 1, 1),
                    sluttDato = LocalDate.of(2024, 1, 1),
                    registrertDato = LocalDateTime.now(),
                    opphav = Deltakeropphav.AMT,
                ),
            )

            val beregning = service.aftRefusjonBeregning(
                tiltaksgjennomforingId = AFT1.id,
                periodeStart = LocalDate.of(2023, 1, 1),
                periodeSlutt = LocalDate.of(2023, 1, 31),
            )

            beregning.belop shouldBe 19500 // 1 x sats
            beregning.sats shouldBe 19500
            beregning.periodeStart shouldBe LocalDate.of(2023, 1, 1)
        }
    }
})
