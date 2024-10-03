package no.nav.mulighetsrommet.api.okonomi.refusjon

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.fixtures.ArrangorFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.okonomi.prismodell.Prismodell
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.truncateAll
import java.time.LocalDate
import java.util.*

class RefusjonskravRepositoryTest : FunSpec({
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

    context("CRUD") {
        val repository = RefusjonskravRepository(database.db)
        val beregning = Prismodell.RefusjonskravBeregning.AFT(
            sats = 20205,
            periodeStart = LocalDate.of(2023, 1, 1),
            belop = 100_000,
            deltakere = listOf(
                Prismodell.RefusjonskravBeregning.AFT.Deltaker(
                    startDato = LocalDate.of(2023, 1, 1),
                    sluttDato = LocalDate.of(2023, 1, 10),
                    prosentPerioder = listOf(
                        Prismodell.RefusjonskravBeregning.AFT.Deltaker.ProsentPeriode(
                            startDato = LocalDate.of(2023, 1, 1),
                            sluttDato = LocalDate.of(2023, 1, 10),
                            prosent = 1.0,
                        ),
                    ),
                ),
            ),
        )

        val krav = RefusjonskravDbo(
            id = UUID.randomUUID(),
            tiltaksgjennomforingId = AFT1.id,
            periodeStart = LocalDate.of(2023, 1, 1),
            periodeSlutt = LocalDate.of(2023, 1, 31),
            beregning = beregning,
        )

        test("upsert and get") {
            repository.upsert(krav)
            repository.get(krav.id) shouldBe RefusjonskravDto(
                id = krav.id,
                tiltaksgjennomforing = RefusjonskravDto.Gjennomforing(
                    id = AFT1.id,
                    navn = AFT1.navn,
                ),
                periodeStart = LocalDate.of(2023, 1, 1),
                periodeSlutt = LocalDate.of(2023, 1, 31),
                arrangor = RefusjonskravDto.Arrangor(
                    navn = ArrangorFixtures.underenhet1.navn,
                    id = ArrangorFixtures.underenhet1.id,
                    organisasjonsnummer = ArrangorFixtures.underenhet1.organisasjonsnummer,
                    slettet = ArrangorFixtures.underenhet1.slettetDato != null,
                ),
                beregning = beregning,
            )
        }
    }
})
