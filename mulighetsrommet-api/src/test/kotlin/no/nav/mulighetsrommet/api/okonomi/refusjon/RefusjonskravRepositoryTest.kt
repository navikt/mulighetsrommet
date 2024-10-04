package no.nav.mulighetsrommet.api.okonomi.refusjon

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.fixtures.ArrangorFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.okonomi.models.DeltakelsePeriode
import no.nav.mulighetsrommet.api.okonomi.models.RefusjonskravDeltakelsePerioder
import no.nav.mulighetsrommet.api.okonomi.prismodell.Prismodell
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.truncateAll
import org.junit.jupiter.api.assertThrows
import java.sql.SQLException
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class RefusjonskravRepositoryTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    val domain = MulighetsrommetTestDomain(
        gjennomforinger = listOf(AFT1),
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
            belop = 100_000,
            sats = 20_205,
            deltakere = setOf(
                RefusjonskravDeltakelsePerioder(
                    deltakelseId = UUID.randomUUID(),
                    perioder = listOf(
                        DeltakelsePeriode(
                            start = LocalDateTime.of(2023, 1, 1, 0, 0, 0),
                            slutt = LocalDateTime.of(2023, 1, 10, 0, 0, 0),
                            stillingsprosent = 100.0,
                        ),
                        DeltakelsePeriode(
                            start = LocalDateTime.of(2023, 1, 10, 0, 0, 0),
                            slutt = LocalDateTime.of(2023, 1, 20, 0, 0, 0),
                            stillingsprosent = 50.0,
                        ),
                        DeltakelsePeriode(
                            start = LocalDateTime.of(2023, 1, 20, 0, 0, 0),
                            slutt = LocalDateTime.of(2023, 1, 30, 0, 0, 0),
                            stillingsprosent = 50.0,
                        ),
                    ),
                ),
                RefusjonskravDeltakelsePerioder(
                    deltakelseId = UUID.randomUUID(),
                    perioder = listOf(
                        DeltakelsePeriode(
                            start = LocalDateTime.of(2023, 1, 1, 0, 0, 0),
                            slutt = LocalDateTime.of(2023, 1, 30, 0, 0, 0),
                            stillingsprosent = 100.0,
                        ),
                    ),
                ),
            ),
        )

        test("upsert and get") {
            val krav = RefusjonskravDbo(
                id = UUID.randomUUID(),
                tiltaksgjennomforingId = AFT1.id,
                periodeStart = LocalDate.of(2023, 1, 1),
                periodeSlutt = LocalDate.of(2023, 1, 31),
                beregning = beregning,
            )

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
                tiltakstype = RefusjonskravDto.Tiltakstype(
                    navn = TiltakstypeFixtures.AFT.navn,
                ),
            )
        }

        test("tillater ikke lagring av overlappende perioder") {
            val periode = DeltakelsePeriode(
                start = LocalDateTime.of(2023, 1, 1, 0, 0, 0),
                slutt = LocalDateTime.of(2023, 1, 2, 0, 0, 0),
                stillingsprosent = 100.0,
            )
            val deltakelse = RefusjonskravDeltakelsePerioder(
                deltakelseId = UUID.randomUUID(),
                perioder = listOf(periode, periode),
            )
            val krav = RefusjonskravDbo(
                id = UUID.randomUUID(),
                tiltaksgjennomforingId = AFT1.id,
                periodeStart = LocalDate.of(2023, 1, 1),
                periodeSlutt = LocalDate.of(2023, 1, 31),
                beregning = Prismodell.RefusjonskravBeregning.AFT(
                    belop = 100_000,
                    sats = 20_205,
                    deltakere = setOf(deltakelse),
                ),
            )

            assertThrows<SQLException> {
                repository.upsert(krav)
            }
        }
    }
})
