package no.nav.mulighetsrommet.api.utbetaling.db

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.ArrangorFixtures
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.utbetaling.model.*
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.model.Kid
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.Periode
import org.junit.jupiter.api.assertThrows
import java.sql.SQLException
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class UtbetalingQueriesTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    val domain = MulighetsrommetTestDomain(
        avtaler = listOf(AvtaleFixtures.AFT),
        gjennomforinger = listOf(AFT1),
    )

    context("CRUD") {
        val deltakelse1Id = UUID.randomUUID()
        val deltakelse2Id = UUID.randomUUID()
        val beregning = UtbetalingBeregningAft(
            input = UtbetalingBeregningAft.Input(
                sats = 20_205,
                periode = Periode.forMonthOf(LocalDate.of(2023, 1, 1)),
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
            output = UtbetalingBeregningAft.Output(
                belop = 100_000,
                deltakelser = setOf(
                    DeltakelseManedsverk(deltakelse1Id, 1.0),
                    DeltakelseManedsverk(deltakelse2Id, 1.0),
                ),
            ),
        )

        test("upsert and get") {
            database.runAndRollback { session ->
                domain.setup(session)

                val queries = UtbetalingQueries(session)

                val frist = LocalDate.of(2024, 10, 1).atStartOfDay()
                val utbetaling = UtbetalingDbo(
                    id = UUID.randomUUID(),
                    gjennomforingId = AFT1.id,
                    fristForGodkjenning = frist,
                    beregning = beregning,
                    kontonummer = Kontonummer("11111111111"),
                    kid = Kid("12345"),
                    periode = beregning.input.periode,
                )

                queries.upsert(utbetaling)

                queries.get(utbetaling.id)!! should {
                    it.id shouldBe utbetaling.id
                    it.status shouldBe UtbetalingStatus.KLAR_FOR_GODKJENNING
                    it.fristForGodkjenning shouldBe frist
                    it.tiltakstype shouldBe UtbetalingDto.Tiltakstype(
                        navn = TiltakstypeFixtures.AFT.navn,
                    )
                    it.gjennomforing shouldBe UtbetalingDto.Gjennomforing(
                        id = AFT1.id,
                        navn = AFT1.navn,
                    )
                    it.arrangor shouldBe UtbetalingDto.Arrangor(
                        navn = ArrangorFixtures.underenhet1.navn,
                        id = ArrangorFixtures.underenhet1.id,
                        organisasjonsnummer = ArrangorFixtures.underenhet1.organisasjonsnummer,
                        slettet = ArrangorFixtures.underenhet1.slettetDato != null,
                    )
                    it.beregning shouldBe beregning
                    it.betalingsinformasjon shouldBe UtbetalingDto.Betalingsinformasjon(
                        kontonummer = Kontonummer("11111111111"),
                        kid = Kid("12345"),
                    )
                    it.journalpostId shouldBe null
                    it.periode shouldBe beregning.input.periode
                    it.godkjentAvArrangorTidspunkt shouldBe null
                }
            }
        }

        test("upsert fri beregning") {
            database.runAndRollback { session ->
                domain.setup(session)

                val queries = UtbetalingQueries(session)

                val frist = LocalDate.of(2024, 10, 1).atStartOfDay()
                val friberegning = UtbetalingBeregningFri(
                    input = UtbetalingBeregningFri.Input(belop = 137_077),
                    output = UtbetalingBeregningFri.Output(belop = 137_077),
                )
                val utbetaling = UtbetalingDbo(
                    id = UUID.randomUUID(),
                    gjennomforingId = AFT1.id,
                    fristForGodkjenning = frist,
                    beregning = friberegning,
                    kontonummer = Kontonummer("11111111111"),
                    kid = Kid("12345"),
                    periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 5, 5)),
                )

                queries.upsert(utbetaling)
                queries.get(utbetaling.id).shouldNotBeNull() should {
                    it.id shouldBe utbetaling.id
                    it.beregning shouldBe friberegning
                }
            }
        }

        test("godkjenn utbetaling") {
            database.runAndRollback { session ->
                domain.setup(session)

                val queries = UtbetalingQueries(session)

                val utbetaling = UtbetalingDbo(
                    id = UUID.randomUUID(),
                    gjennomforingId = AFT1.id,
                    fristForGodkjenning = LocalDate.of(2024, 10, 1).atStartOfDay(),
                    beregning = beregning,
                    kontonummer = null,
                    kid = null,
                    periode = beregning.input.periode,
                )

                queries.upsert(utbetaling)

                queries.get(utbetaling.id)
                    .shouldNotBeNull().status shouldBe UtbetalingStatus.KLAR_FOR_GODKJENNING

                queries.setGodkjentAvArrangor(utbetaling.id, LocalDateTime.now())

                queries.get(utbetaling.id)
                    .shouldNotBeNull().status shouldBe UtbetalingStatus.GODKJENT_AV_ARRANGOR
            }
        }

        test("set journalpost id") {
            database.runAndRollback { session ->
                domain.setup(session)

                val queries = UtbetalingQueries(session)

                val utbetaling = UtbetalingDbo(
                    id = UUID.randomUUID(),
                    gjennomforingId = AFT1.id,
                    fristForGodkjenning = LocalDate.of(2024, 10, 1).atStartOfDay(),
                    beregning = beregning,
                    kontonummer = null,
                    kid = null,
                    periode = beregning.input.periode,
                )
                queries.upsert(utbetaling)

                queries.setJournalpostId(utbetaling.id, "123")
            }
        }

        test("tillater ikke lagring av overlappende perioder") {
            database.runAndRollback { session ->
                domain.setup(session)

                val queries = UtbetalingQueries(session)

                val periode = DeltakelsePeriode(
                    start = LocalDate.of(2023, 1, 1),
                    slutt = LocalDate.of(2023, 1, 2),
                    deltakelsesprosent = 100.0,
                )
                val deltakelse = DeltakelsePerioder(
                    deltakelseId = UUID.randomUUID(),
                    perioder = listOf(periode, periode),
                )
                val utbetaling = UtbetalingDbo(
                    id = UUID.randomUUID(),
                    gjennomforingId = AFT1.id,
                    fristForGodkjenning = LocalDate.of(2024, 10, 1).atStartOfDay(),
                    beregning = UtbetalingBeregningAft(
                        input = UtbetalingBeregningAft.Input(
                            periode = Periode.forMonthOf(LocalDate.of(2023, 1, 1)),
                            sats = 20_205,
                            deltakelser = setOf(deltakelse),
                        ),
                        output = UtbetalingBeregningAft.Output(
                            belop = 0,
                            deltakelser = setOf(),
                        ),
                    ),
                    kontonummer = null,
                    kid = null,
                    periode = Periode.forMonthOf(LocalDate.of(2023, 1, 1)),
                )

                assertThrows<SQLException> {
                    queries.upsert(utbetaling)
                }
            }
        }
    }
})
