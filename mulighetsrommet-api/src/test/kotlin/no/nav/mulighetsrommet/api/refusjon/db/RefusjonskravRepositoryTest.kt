package no.nav.mulighetsrommet.api.refusjon.db

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.http.*
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.ArrangorFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.refusjon.model.*
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.truncateAll
import no.nav.mulighetsrommet.domain.dto.Kid
import no.nav.mulighetsrommet.domain.dto.Kontonummer
import org.junit.jupiter.api.assertThrows
import java.sql.SQLException
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class RefusjonskravRepositoryTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

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
        val deltakelse1Id = UUID.randomUUID()
        val deltakelse2Id = UUID.randomUUID()
        val beregning = RefusjonKravBeregningAft(
            input = RefusjonKravBeregningAft.Input(
                sats = 20_205,
                periode = RefusjonskravPeriode.fromDayInMonth(LocalDate.of(2023, 1, 1)),
                deltakelser = setOf(
                    DeltakelsePerioder(
                        deltakelseId = deltakelse1Id,
                        perioder = listOf(
                            DeltakelsePeriode(
                                start = LocalDate.of(2023, 1, 1),
                                slutt = LocalDate.of(2023, 1, 10),
                                deltakelsesprosent = 100.0,
                            ),
                            DeltakelsePeriode(
                                start = LocalDate.of(2023, 1, 10),
                                slutt = LocalDate.of(2023, 1, 20),
                                deltakelsesprosent = 50.0,
                            ),
                            DeltakelsePeriode(
                                start = LocalDate.of(2023, 1, 20),
                                slutt = LocalDate.of(2023, 2, 1),
                                deltakelsesprosent = 50.0,
                            ),
                        ),
                    ),
                    DeltakelsePerioder(
                        deltakelseId = deltakelse2Id,
                        perioder = listOf(
                            DeltakelsePeriode(
                                start = LocalDate.of(2023, 1, 1),
                                slutt = LocalDate.of(2023, 2, 1),
                                deltakelsesprosent = 100.0,
                            ),
                        ),
                    ),
                ),
            ),
            output = RefusjonKravBeregningAft.Output(
                belop = 100_000,
                deltakelser = setOf(
                    DeltakelseManedsverk(deltakelse1Id, 1.0),
                    DeltakelseManedsverk(deltakelse2Id, 1.0),
                ),
            ),
        )

        test("upsert and get") {
            val frist = LocalDate.of(2024, 10, 1).atStartOfDay()
            val krav = RefusjonskravDbo(
                id = UUID.randomUUID(),
                gjennomforingId = AFT1.id,
                fristForGodkjenning = frist,
                beregning = beregning,
                kontonummer = Kontonummer("11111111111"),
                kid = Kid("12345"),
            )

            repository.upsert(krav)

            repository.get(krav.id) shouldBe RefusjonskravDto(
                id = krav.id,
                status = RefusjonskravStatus.KLAR_FOR_GODKJENNING,
                fristForGodkjenning = frist,
                tiltakstype = RefusjonskravDto.Tiltakstype(
                    navn = TiltakstypeFixtures.AFT.navn,
                ),
                gjennomforing = RefusjonskravDto.Gjennomforing(
                    id = AFT1.id,
                    navn = AFT1.navn,
                ),
                arrangor = RefusjonskravDto.Arrangor(
                    navn = ArrangorFixtures.underenhet1.navn,
                    id = ArrangorFixtures.underenhet1.id,
                    organisasjonsnummer = ArrangorFixtures.underenhet1.organisasjonsnummer,
                    slettet = ArrangorFixtures.underenhet1.slettetDato != null,
                ),
                beregning = beregning,
                betalingsinformasjon = RefusjonskravDto.Betalingsinformasjon(
                    kontonummer = Kontonummer("11111111111"),
                    kid = Kid("12345"),
                ),
                journalpostId = null,
            )
        }

        test("godkjenn refusjonskrav") {
            val krav = RefusjonskravDbo(
                id = UUID.randomUUID(),
                gjennomforingId = AFT1.id,
                fristForGodkjenning = LocalDate.of(2024, 10, 1).atStartOfDay(),
                beregning = beregning,
                kontonummer = null,
                kid = null,
            )

            repository.upsert(krav)

            repository.get(krav.id).shouldNotBeNull().status shouldBe RefusjonskravStatus.KLAR_FOR_GODKJENNING

            repository.setGodkjentAvArrangor(krav.id, LocalDateTime.now())

            repository.get(krav.id).shouldNotBeNull().status shouldBe RefusjonskravStatus.GODKJENT_AV_ARRANGOR
        }

        test("set journalpost id") {
            val krav = RefusjonskravDbo(
                id = UUID.randomUUID(),
                gjennomforingId = AFT1.id,
                fristForGodkjenning = LocalDate.of(2024, 10, 1).atStartOfDay(),
                beregning = beregning,
                kontonummer = null,
                kid = null,
            )
            repository.upsert(krav)

            repository.setJournalpostId(krav.id, "123")
        }

        test("tillater ikke lagring av overlappende perioder") {
            val periode = DeltakelsePeriode(
                start = LocalDate.of(2023, 1, 1),
                slutt = LocalDate.of(2023, 1, 2),
                deltakelsesprosent = 100.0,
            )
            val deltakelse = DeltakelsePerioder(
                deltakelseId = UUID.randomUUID(),
                perioder = listOf(periode, periode),
            )
            val krav = RefusjonskravDbo(
                id = UUID.randomUUID(),
                gjennomforingId = AFT1.id,
                fristForGodkjenning = LocalDate.of(2024, 10, 1).atStartOfDay(),
                beregning = RefusjonKravBeregningAft(
                    input = RefusjonKravBeregningAft.Input(
                        periode = RefusjonskravPeriode.fromDayInMonth(LocalDate.of(2023, 1, 1)),
                        sats = 20_205,
                        deltakelser = setOf(deltakelse),
                    ),
                    output = RefusjonKravBeregningAft.Output(
                        belop = 0,
                        deltakelser = setOf(),
                    ),
                ),
                kontonummer = null,
                kid = null,
            )

            assertThrows<SQLException> {
                repository.upsert(krav)
            }
        }
    }
})
