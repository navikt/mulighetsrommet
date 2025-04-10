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
import no.nav.mulighetsrommet.model.*
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

    val periode = Periode.forMonthOf(LocalDate.of(2023, 1, 1))

    val beregning = UtbetalingBeregningFri(
        input = UtbetalingBeregningFri.Input(belop = 137_077),
        output = UtbetalingBeregningFri.Output(belop = 137_077),
    )

    val utbetaling = UtbetalingDbo(
        id = UUID.randomUUID(),
        gjennomforingId = AFT1.id,
        fristForGodkjenning = LocalDate.of(2024, 10, 1).atStartOfDay(),
        beregning = beregning,
        kontonummer = Kontonummer("11111111111"),
        kid = Kid("12345"),
        periode = periode,
        innsender = NavIdent("Z123456"),
        beskrivelse = "En beskrivelse",
    )

    test("upsert and get utbetaling med fri beregning") {
        database.runAndRollback { session ->
            domain.setup(session)

            val queries = UtbetalingQueries(session)

            queries.upsert(utbetaling)

            queries.get(utbetaling.id).shouldNotBeNull().should {
                it.id shouldBe utbetaling.id
                it.fristForGodkjenning shouldBe LocalDate.of(2024, 10, 1).atStartOfDay()
                it.tiltakstype shouldBe Utbetaling.Tiltakstype(
                    navn = TiltakstypeFixtures.AFT.navn,
                    tiltakskode = TiltakstypeFixtures.AFT.tiltakskode!!,
                )
                it.gjennomforing shouldBe Utbetaling.Gjennomforing(
                    id = AFT1.id,
                    navn = AFT1.navn,
                )
                it.arrangor shouldBe Utbetaling.Arrangor(
                    navn = ArrangorFixtures.underenhet1.navn,
                    id = ArrangorFixtures.underenhet1.id,
                    organisasjonsnummer = ArrangorFixtures.underenhet1.organisasjonsnummer,
                    slettet = ArrangorFixtures.underenhet1.slettetDato != null,
                )
                it.beregning shouldBe beregning
                it.betalingsinformasjon shouldBe Utbetaling.Betalingsinformasjon(
                    kontonummer = Kontonummer("11111111111"),
                    kid = Kid("12345"),
                )
                it.journalpostId shouldBe null
                it.periode shouldBe periode
                it.godkjentAvArrangorTidspunkt shouldBe null
                it.innsender shouldBe NavIdent("Z123456")
                it.beskrivelse shouldBe "En beskrivelse"
            }
        }
    }

    test("set godkjent av arrangør") {
        database.runAndRollback { session ->
            domain.setup(session)

            val queries = UtbetalingQueries(session)

            queries.upsert(utbetaling.copy(innsender = null))

            queries.get(utbetaling.id)
                .shouldNotBeNull().innsender shouldBe null

            queries.setGodkjentAvArrangor(utbetaling.id, LocalDateTime.now())

            queries.get(utbetaling.id)
                .shouldNotBeNull().innsender shouldBe Arrangor
        }
    }

    test("set journalpost id") {
        database.runAndRollback { session ->
            domain.setup(session)

            val queries = UtbetalingQueries(session)

            queries.upsert(utbetaling)

            queries.setJournalpostId(utbetaling.id, "123")

            queries.get(utbetaling.id).shouldNotBeNull().journalpostId shouldBe "123"
        }
    }

    context("utbetaling med forhåndsgodkjent beregning") {
        test("upsert and get forhåndsgodkjent beregning") {
            database.runAndRollback { session ->
                domain.setup(session)

                val queries = UtbetalingQueries(session)

                val deltakelse1Id = UUID.randomUUID()
                val deltakelse2Id = UUID.randomUUID()
                val beregningForhandsgodkjent = UtbetalingBeregningForhandsgodkjent(
                    input = UtbetalingBeregningForhandsgodkjent.Input(
                        sats = 20_205,
                        periode = periode,
                        stengt = setOf(StengtPeriode(Periode(LocalDate.of(2023, 1, 10), LocalDate.of(2023, 1, 20)), "Ferie")),
                        deltakelser = setOf(
                            DeltakelsePerioder(
                                deltakelseId = deltakelse1Id,
                                perioder = listOf(
                                    DeltakelsePeriode(
                                        periode = Periode(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 1, 10)),
                                        deltakelsesprosent = 100.0,
                                    ),
                                    DeltakelsePeriode(
                                        periode = Periode(LocalDate.of(2023, 1, 10), LocalDate.of(2023, 1, 20)),
                                        deltakelsesprosent = 50.0,
                                    ),
                                    DeltakelsePeriode(
                                        periode = Periode(LocalDate.of(2023, 1, 20), LocalDate.of(2023, 2, 1)),
                                        deltakelsesprosent = 50.0,
                                    ),
                                ),
                            ),
                            DeltakelsePerioder(
                                deltakelseId = deltakelse2Id,
                                perioder = listOf(
                                    DeltakelsePeriode(
                                        periode = Periode(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 2, 1)),
                                        deltakelsesprosent = 100.0,
                                    ),
                                ),
                            ),
                        ),
                    ),
                    output = UtbetalingBeregningForhandsgodkjent.Output(
                        belop = 100_000,
                        deltakelser = setOf(
                            DeltakelseManedsverk(deltakelse1Id, 1.0),
                            DeltakelseManedsverk(deltakelse2Id, 1.0),
                        ),
                    ),
                )
                val tubetalingForhandsgodkjent = utbetaling.copy(
                    beregning = beregningForhandsgodkjent,
                )

                queries.upsert(tubetalingForhandsgodkjent)

                queries.get(tubetalingForhandsgodkjent.id).shouldNotBeNull().should {
                    it.beregning shouldBe beregningForhandsgodkjent
                }
            }
        }

        test("tillater ikke lagring av overlappende deltakelsesperioder") {
            database.runAndRollback { session ->
                domain.setup(session)

                val queries = UtbetalingQueries(session)

                val deltakelsePeriode = DeltakelsePeriode(
                    periode = Periode(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 1, 2)),
                    deltakelsesprosent = 100.0,
                )
                val deltakelse = DeltakelsePerioder(
                    deltakelseId = UUID.randomUUID(),
                    perioder = listOf(deltakelsePeriode, deltakelsePeriode),
                )
                val utbetalingForhandsgodkjent = UtbetalingDbo(
                    id = UUID.randomUUID(),
                    gjennomforingId = AFT1.id,
                    fristForGodkjenning = LocalDate.of(2024, 10, 1).atStartOfDay(),
                    beregning = UtbetalingBeregningForhandsgodkjent(
                        input = UtbetalingBeregningForhandsgodkjent.Input(
                            periode = Periode.forMonthOf(LocalDate.of(2023, 1, 1)),
                            sats = 20_205,
                            stengt = setOf(),
                            deltakelser = setOf(deltakelse),
                        ),
                        output = UtbetalingBeregningForhandsgodkjent.Output(
                            belop = 0,
                            deltakelser = setOf(),
                        ),
                    ),
                    kontonummer = null,
                    kid = null,
                    periode = Periode.forMonthOf(LocalDate.of(2023, 1, 1)),
                    innsender = null,
                    beskrivelse = null,
                )

                assertThrows<SQLException> {
                    queries.upsert(utbetalingForhandsgodkjent)
                }
            }
        }
    }
})
