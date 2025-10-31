package no.nav.mulighetsrommet.api.utbetaling.db

import io.kotest.core.spec.style.FunSpec
import io.kotest.inspectors.shouldForAll
import io.kotest.matchers.collections.shouldHaveAtMostSize
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.VTA1
import no.nav.mulighetsrommet.api.fixtures.TilsagnFixtures.Tilsagn1
import no.nav.mulighetsrommet.api.fixtures.TilsagnFixtures.Tilsagn2
import no.nav.mulighetsrommet.api.fixtures.UtbetalingFixtures.utbetaling1
import no.nav.mulighetsrommet.api.utbetaling.api.AdminInnsendingerFilter
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingStatusDto.Type
import no.nav.mulighetsrommet.api.utbetaling.model.*
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.model.*
import no.nav.tiltak.okonomi.Tilskuddstype

class UtbetalingQueriesTest : FunSpec(
    {
        val database = extension(FlywayDatabaseTestListener(databaseConfig))

        val domain = MulighetsrommetTestDomain(
            avtaler = listOf(AvtaleFixtures.AFT, AvtaleFixtures.VTA),
            gjennomforinger = listOf(AFT1, VTA1),
            tilsagn = listOf(
                Tilsagn1,
                Tilsagn2.copy(kostnadssted = NavEnhetFixtures.Gjovik.enhetsnummer),
            ),
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

        context("utbetaling med beregning for månedsverk med fast sats per tiltaksplass") {
            test("upsert and get beregning") {
                database.runAndRollback { session ->
                    domain.setup(session)

                    val queries = UtbetalingQueries(session)

                    val deltakelse1Id = UUID.randomUUID()
                    val deltakelse2Id = UUID.randomUUID()
                    val beregning = UtbetalingBeregningFastSatsPerTiltaksplassPerManed(
                        input = UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Input(
                            satser = setOf(SatsPeriode(periode, 20_205)),
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
                                UtbetalingBeregningOutputDeltakelse(
                                    deltakelse1Id,
                                    setOf(
                                        UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(periode, 1.0, 20_205),
                                    ),
                                ),
                                UtbetalingBeregningOutputDeltakelse(
                                    deltakelse2Id,
                                    setOf(
                                        UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(periode, 1.0, 20_205),
                                    ),
                                ),
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
                            satser = setOf(SatsPeriode(Periode.forMonthOf(LocalDate.of(2023, 1, 1)), 20_205)),
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
                            satser = setOf(
                                SatsPeriode(Periode(periode.start, LocalDate.of(2023, 1, 15)), 20_205),
                                SatsPeriode(Periode(LocalDate.of(2023, 1, 15), periode.slutt), 20_975),
                            ),
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
                                UtbetalingBeregningOutputDeltakelse(
                                    deltakelse1Id,
                                    setOf(
                                        UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(periode, 0.5, 20_205),
                                    ),
                                ),
                                UtbetalingBeregningOutputDeltakelse(
                                    deltakelse2Id,
                                    setOf(
                                        UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(periode, 1.0, 20_205),
                                    ),
                                ),
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
                            satser = setOf(SatsPeriode(periode, 2999)),
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
                                UtbetalingBeregningOutputDeltakelse(
                                    deltakelse1Id,
                                    setOf(
                                        UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(periode, 2.2, 20_205),
                                    ),
                                ),
                                UtbetalingBeregningOutputDeltakelse(
                                    deltakelse2Id,
                                    setOf(
                                        UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(periode, 4.2, 20_205),
                                    ),
                                ),
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

        context("utbetaling med beregning for hele ukesverk") {
            test("upsert and get beregning") {
                database.runAndRollback { session ->
                    domain.setup(session)

                    val queries = UtbetalingQueries(session)

                    val deltakelse1Id = UUID.randomUUID()
                    val deltakelse2Id = UUID.randomUUID()
                    val beregning = UtbetalingBeregningPrisPerHeleUkesverk(
                        input = UtbetalingBeregningPrisPerHeleUkesverk.Input(
                            satser = setOf(SatsPeriode(periode, 2999)),
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
                        output = UtbetalingBeregningPrisPerHeleUkesverk.Output(
                            belop = 5999,
                            deltakelser = setOf(
                                UtbetalingBeregningOutputDeltakelse(
                                    deltakelse1Id,
                                    setOf(
                                        UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(periode, 2.0, 20_205),
                                    ),
                                ),
                                UtbetalingBeregningOutputDeltakelse(
                                    deltakelse2Id,
                                    setOf(
                                        UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(periode, 4.0, 20_205),
                                    ),
                                ),
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

        context("utbetaling med beregning for pris per time oppfølging") {
            test("upsert and get beregning") {
                database.runAndRollback { session ->
                    domain.setup(session)

                    val queries = UtbetalingQueries(session)

                    val deltakelse1Id = UUID.randomUUID()
                    val deltakelse2Id = UUID.randomUUID()
                    val beregning = UtbetalingBeregningPrisPerTimeOppfolging(
                        input = UtbetalingBeregningPrisPerTimeOppfolging.Input(
                            belop = 1999,
                            satser = setOf(SatsPeriode(periode, 100), SatsPeriode(periode, 200)),
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
                        output = UtbetalingBeregningPrisPerTimeOppfolging.Output(
                            belop = 1999,
                        ),
                    )

                    queries.upsert(utbetaling.copy(beregning = beregning))

                    queries.get(utbetaling.id).shouldNotBeNull().should {
                        it.beregning shouldBe beregning
                    }
                }
            }
        }

        context("Filter for manglende innsendinger") {
            test("Henter kun innsendinger som venter på arrangor") {
                database.runAndRollback { session ->
                    domain.setup(session)

                    val queries = UtbetalingQueries(session)

                    val utbetaling1 = utbetaling1.copy(
                        status = UtbetalingStatusType.GENERERT,
                    )
                    val utbetaling2 = UtbetalingFixtures.utbetaling2.copy(status = UtbetalingStatusType.INNSENDT)
                    val utbetaling3 = UtbetalingFixtures.utbetaling3.copy(
                        status = UtbetalingStatusType.GENERERT,
                        gjennomforingId = AFT1.id,
                    )

                    queries.upsert(utbetaling1)
                    queries.upsert(utbetaling2)

                    queries.getAll(
                        filter = AdminInnsendingerFilter(
                            navEnheter = emptyList(),
                            tiltakstyper = emptyList(),
                            sortering = null,
                        ),
                    ).shouldHaveSize(1).shouldForAll { it.status.type == Type.VENTER_PA_ARRANGOR }

                    queries.upsert(utbetaling3)

                    queries.getAll(
                        filter = AdminInnsendingerFilter(
                            navEnheter = emptyList(),
                            tiltakstyper = emptyList(),
                            sortering = null,
                        ),
                    ).shouldHaveSize(2).shouldForAll { it.status.type == Type.VENTER_PA_ARRANGOR }
                }
            }

            test("Filtrer på tiltakstype") {
                database.runAndRollback { session ->
                    domain.setup(session)

                    val queries = UtbetalingQueries(session)

                    val utbetaling1 = utbetaling1.copy(
                        status = UtbetalingStatusType.GENERERT,
                        gjennomforingId = AFT1.id,
                    )
                    val utbetaling2 = UtbetalingFixtures.utbetaling2.copy(
                        status = UtbetalingStatusType.GENERERT,
                        gjennomforingId = VTA1.id,
                    )

                    queries.upsert(utbetaling1)
                    queries.upsert(utbetaling2)

                    queries.getAll(
                        filter = AdminInnsendingerFilter(
                            navEnheter = emptyList(),
                            tiltakstyper = listOf(TiltakstypeFixtures.AFT.id),
                            sortering = null,
                        ),
                    ).shouldHaveAtMostSize(1)
                        .shouldForAll { it.tiltakstype.tiltakskode == TiltakstypeFixtures.AFT.tiltakskode }
                }
            }

            test("Filtrer på kostnadssted") {
                database.runAndRollback { session ->
                    domain.setup(session)

                    val utbetalingQueries = UtbetalingQueries(session)

                    val utbetaling1 = utbetaling1.copy(
                        status = UtbetalingStatusType.GENERERT,
                        gjennomforingId = Tilsagn1.gjennomforingId,
                        periode = Tilsagn1.periode,
                    )
                    val utbetaling2 = UtbetalingFixtures.utbetaling2.copy(
                        status = UtbetalingStatusType.GENERERT,
                        gjennomforingId = Tilsagn2.gjennomforingId,
                        periode = Tilsagn2.periode,
                    )

                    utbetalingQueries.upsert(utbetaling1)
                    utbetalingQueries.upsert(utbetaling2)

                    utbetalingQueries.getAll(
                        filter = AdminInnsendingerFilter(
                            navEnheter = listOf(NavEnhetFixtures.Gjovik.enhetsnummer),
                            tiltakstyper = emptyList(),
                            sortering = null,
                        ),
                    ).shouldHaveAtMostSize(1)
                        .shouldForAll { it.kostnadssteder == listOf(NavEnhetFixtures.Gjovik.enhetsnummer) }
                }
            }
        }
    },
)
