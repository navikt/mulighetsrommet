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
import no.nav.tiltak.okonomi.Tilskuddstype
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

    val friBeregning = UtbetalingBeregningFri(
        input = UtbetalingBeregningFri.Input(belop = 137_077),
        output = UtbetalingBeregningFri.Output(belop = 137_077),
    )

    val utbetaling = UtbetalingDbo(
        id = UUID.randomUUID(),
        gjennomforingId = AFT1.id,
        beregning = friBeregning,
        kontonummer = Kontonummer("11111111111"),
        kid = Kid.parseOrThrow("006402710013"),
        periode = periode,
        innsender = NavIdent("Z123456"),
        beskrivelse = "En beskrivelse",
        tilskuddstype = Tilskuddstype.TILTAK_DRIFTSTILSKUDD,
        godkjentAvArrangorTidspunkt = null,
        status = UtbetalingStatusType.GENERERT,
    )

    test("upsert and get utbetaling med fri beregning") {
        database.runAndRollback { session ->
            domain.setup(session)

            val queries = UtbetalingQueries(session)

            queries.upsert(utbetaling)

            queries.get(utbetaling.id).shouldNotBeNull().should {
                it.id shouldBe utbetaling.id
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
                it.beregning shouldBe friBeregning
                it.betalingsinformasjon shouldBe Utbetaling.Betalingsinformasjon(
                    kontonummer = Kontonummer("11111111111"),
                    kid = Kid.parseOrThrow("006402710013"),
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

    context("utbetaling med beregning for månedsverk med deltakelsesmengder") {
        test("upsert and get beregning") {
            database.runAndRollback { session ->
                domain.setup(session)

                val queries = UtbetalingQueries(session)

                val deltakelse1Id = UUID.randomUUID()
                val deltakelse2Id = UUID.randomUUID()
                val beregning = UtbetalingBeregningFastSatsPerTiltaksplassPerManed(
                    input = UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Input(
                        sats = 20_205,
                        periode = periode,
                        stengt = setOf(
                            StengtPeriode(
                                Periode(LocalDate.of(2023, 1, 10), LocalDate.of(2023, 1, 20)),
                                "Ferie",
                            ),
                        ),
                        deltakelser = setOf(
                            DeltakelseDeltakelsesprosentPerioder(
                                deltakelseId = deltakelse1Id,
                                perioder = listOf(
                                    DeltakelsesprosentPeriode(
                                        periode = Periode(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 1, 10)),
                                        deltakelsesprosent = 100.0,
                                    ),
                                    DeltakelsesprosentPeriode(
                                        periode = Periode(LocalDate.of(2023, 1, 10), LocalDate.of(2023, 1, 20)),
                                        deltakelsesprosent = 50.0,
                                    ),
                                    DeltakelsesprosentPeriode(
                                        periode = Periode(LocalDate.of(2023, 1, 20), LocalDate.of(2023, 2, 1)),
                                        deltakelsesprosent = 50.0,
                                    ),
                                ),
                            ),
                            DeltakelseDeltakelsesprosentPerioder(
                                deltakelseId = deltakelse2Id,
                                perioder = listOf(
                                    DeltakelsesprosentPeriode(
                                        periode = Periode(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 2, 1)),
                                        deltakelsesprosent = 100.0,
                                    ),
                                ),
                            ),
                        ),
                    ),
                    output = UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Output(
                        belop = 100_000,
                        deltakelser = setOf(
                            DeltakelseManedsverk(deltakelse1Id, 1.0),
                            DeltakelseManedsverk(deltakelse2Id, 1.0),
                        ),
                    ),
                )

                queries.upsert(utbetaling.copy(beregning = beregning))

                queries.get(utbetaling.id).shouldNotBeNull().should {
                    it.beregning shouldBe beregning
                }
            }
        }

        test("tillater lagring av overlappende deltakelsesperioder") {
            database.runAndRollback { session ->
                domain.setup(session)

                val queries = UtbetalingQueries(session)

                val deltakelsePeriode = DeltakelsesprosentPeriode(
                    periode = Periode(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 1, 2)),
                    deltakelsesprosent = 100.0,
                )
                val deltakelse = DeltakelseDeltakelsesprosentPerioder(
                    deltakelseId = UUID.randomUUID(),
                    perioder = listOf(deltakelsePeriode, deltakelsePeriode),
                )
                val beregning = UtbetalingBeregningFastSatsPerTiltaksplassPerManed(
                    input = UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Input(
                        periode = Periode.forMonthOf(LocalDate.of(2023, 1, 1)),
                        sats = 20_205,
                        stengt = setOf(),
                        deltakelser = setOf(deltakelse),
                    ),
                    output = UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Output(
                        belop = 0,
                        deltakelser = setOf(),
                    ),
                )

                queries.upsert(utbetaling.copy(beregning = beregning))
            }
        }
    }

    context("utbetaling med beregning for månedsverk") {
        test("upsert and get beregning") {
            database.runAndRollback { session ->
                domain.setup(session)

                val queries = UtbetalingQueries(session)

                val deltakelse1Id = UUID.randomUUID()
                val deltakelse2Id = UUID.randomUUID()
                val beregning = UtbetalingBeregningPrisPerManedsverk(
                    input = UtbetalingBeregningPrisPerManedsverk.Input(
                        sats = 20_205,
                        periode = periode,
                        stengt = setOf(
                            StengtPeriode(
                                Periode(LocalDate.of(2023, 1, 10), LocalDate.of(2023, 1, 20)),
                                "Ferie",
                            ),
                        ),
                        deltakelser = setOf(
                            DeltakelsePeriode(
                                deltakelseId = deltakelse1Id,
                                periode = Periode(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 1, 16)),
                            ),
                            DeltakelsePeriode(
                                deltakelseId = deltakelse2Id,
                                periode = Periode(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 2, 1)),
                            ),
                        ),
                    ),
                    output = UtbetalingBeregningPrisPerManedsverk.Output(
                        belop = 100_000,
                        deltakelser = setOf(
                            DeltakelseManedsverk(deltakelse1Id, 0.5),
                            DeltakelseManedsverk(deltakelse2Id, 1.0),
                        ),
                    ),
                )

                queries.upsert(utbetaling.copy(beregning = beregning))

                queries.get(utbetaling.id).shouldNotBeNull().should {
                    it.beregning shouldBe beregning
                }
            }
        }
    }

    context("utbetaling med beregning for ukesverk") {
        test("upsert and get beregning") {
            database.runAndRollback { session ->
                domain.setup(session)

                val queries = UtbetalingQueries(session)

                val deltakelse1Id = UUID.randomUUID()
                val deltakelse2Id = UUID.randomUUID()
                val beregning = UtbetalingBeregningPrisPerUkesverk(
                    input = UtbetalingBeregningPrisPerUkesverk.Input(
                        sats = 2999,
                        periode = periode,
                        stengt = setOf(
                            StengtPeriode(
                                Periode(LocalDate.of(2023, 1, 10), LocalDate.of(2023, 1, 20)),
                                "Ferie",
                            ),
                        ),
                        deltakelser = setOf(
                            DeltakelsePeriode(
                                deltakelseId = deltakelse1Id,
                                periode = Periode(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 1, 10)),
                            ),
                            DeltakelsePeriode(
                                deltakelseId = deltakelse2Id,
                                periode = Periode(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 2, 1)),
                            ),
                        ),
                    ),
                    output = UtbetalingBeregningPrisPerUkesverk.Output(
                        belop = 5999,
                        deltakelser = setOf(
                            DeltakelseUkesverk(deltakelse1Id, 2.2),
                            DeltakelseUkesverk(deltakelse2Id, 4.2),
                        ),
                    ),
                )

                queries.upsert(utbetaling.copy(beregning = beregning))

                queries.get(utbetaling.id).shouldNotBeNull().should {
                    it.beregning shouldBe beregning
                }
            }
        }
    }
})
