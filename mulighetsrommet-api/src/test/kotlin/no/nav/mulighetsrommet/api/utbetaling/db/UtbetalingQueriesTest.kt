package no.nav.mulighetsrommet.api.utbetaling.db

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import no.nav.mulighetsrommet.api.arrangor.model.Betalingsinformasjon
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.ArrangorFixtures
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.VTA1
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures
import no.nav.mulighetsrommet.api.fixtures.TilsagnFixtures.Tilsagn1
import no.nav.mulighetsrommet.api.fixtures.TilsagnFixtures.Tilsagn2
import no.nav.mulighetsrommet.api.fixtures.TilsagnFixtures.Tilsagn3
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.fixtures.UtbetalingFixtures
import no.nav.mulighetsrommet.api.fixtures.UtbetalingFixtures.utbetaling1
import no.nav.mulighetsrommet.api.tilsagn.api.KostnadsstedDto
import no.nav.mulighetsrommet.api.utbetaling.api.AdminInnsendingerFilter
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingStatusDto.Type
import no.nav.mulighetsrommet.api.utbetaling.model.DeltakelseDeltakelsesprosentPerioder
import no.nav.mulighetsrommet.api.utbetaling.model.DeltakelsePeriode
import no.nav.mulighetsrommet.api.utbetaling.model.DeltakelsesprosentPeriode
import no.nav.mulighetsrommet.api.utbetaling.model.SatsPeriode
import no.nav.mulighetsrommet.api.utbetaling.model.StengtPeriode
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFastSatsPerTiltaksplassPerManed
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFri
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningOutputDeltakelse
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningPrisPerHeleUkesverk
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningPrisPerManedsverk
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningPrisPerTimeOppfolging
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningPrisPerUkesverk
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatusType
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.Arrangor
import no.nav.mulighetsrommet.model.Kid
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Periode
import no.nav.tiltak.okonomi.Tilskuddstype
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

class UtbetalingQueriesTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    val domain = MulighetsrommetTestDomain(
        avtaler = listOf(AvtaleFixtures.AFT, AvtaleFixtures.VTA),
        gjennomforinger = listOf(AFT1, VTA1),
        tilsagn = listOf(
            Tilsagn1.copy(kostnadssted = NavEnhetFixtures.Innlandet.enhetsnummer),
            Tilsagn2.copy(kostnadssted = NavEnhetFixtures.Gjovik.enhetsnummer),
        ),
    )

    val periode = Periode.forMonthOf(LocalDate.of(2023, 1, 1))

    val friBeregning = UtbetalingBeregningFri(
        input = UtbetalingBeregningFri.Input(belop = 137_077),
        output = UtbetalingBeregningFri.Output(belop = 137_077),
    )

    val utbetalesTidligstTidspunkt = LocalDate.of(2025, 12, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
    val utbetaling = UtbetalingDbo(
        id = UUID.randomUUID(),
        gjennomforingId = AFT1.id,
        status = UtbetalingStatusType.GENERERT,
        beregning = friBeregning,
        betalingsinformasjon = Betalingsinformasjon.BBan(Kontonummer("11111111111"), kid = Kid.parseOrThrow("006402710013")),
        periode = periode,
        innsender = NavIdent("Z123456"),
        beskrivelse = "En beskrivelse",
        tilskuddstype = Tilskuddstype.TILTAK_DRIFTSTILSKUDD,
        godkjentAvArrangorTidspunkt = null,
        utbetalesTidligstTidspunkt = utbetalesTidligstTidspunkt,
    )

    test("upsert and get utbetaling med fri beregning") {
        database.runAndRollback { session ->
            domain.setup(session)

            queries.utbetaling.upsert(utbetaling)

            queries.utbetaling.getOrError(utbetaling.id).should {
                it.id shouldBe utbetaling.id
                it.tiltakstype shouldBe Utbetaling.Tiltakstype(
                    navn = TiltakstypeFixtures.AFT.navn,
                    tiltakskode = TiltakstypeFixtures.AFT.tiltakskode!!,
                )
                it.gjennomforing.id shouldBe AFT1.id
                it.gjennomforing.lopenummer.shouldNotBeNull()
                it.arrangor shouldBe Utbetaling.Arrangor(
                    navn = ArrangorFixtures.underenhet1.navn,
                    id = ArrangorFixtures.underenhet1.id,
                    organisasjonsnummer = ArrangorFixtures.underenhet1.organisasjonsnummer,
                    slettet = ArrangorFixtures.underenhet1.slettetDato != null,
                )
                it.beregning shouldBe friBeregning
                it.betalingsinformasjon.shouldBeTypeOf<Betalingsinformasjon.BBan>() should {
                    it.kontonummer shouldBe Kontonummer("11111111111")
                    it.kid shouldBe Kid.parseOrThrow("006402710013")
                }
                it.journalpostId shouldBe null
                it.periode shouldBe periode
                it.godkjentAvArrangorTidspunkt shouldBe null
                it.utbetalesTidligstTidspunkt shouldBe utbetalesTidligstTidspunkt
                it.innsender shouldBe NavIdent("Z123456")
                it.beskrivelse shouldBe "En beskrivelse"
            }
        }
    }

    test("set godkjent av arrangør") {
        database.runAndRollback { session ->
            domain.setup(session)

            queries.utbetaling.upsert(utbetaling.copy(innsender = null))

            queries.utbetaling.getOrError(utbetaling.id).innsender shouldBe null

            queries.utbetaling.setGodkjentAvArrangor(utbetaling.id, LocalDateTime.now())

            queries.utbetaling.getOrError(utbetaling.id).innsender shouldBe Arrangor
        }
    }

    test("set journalpost id") {
        database.runAndRollback { session ->
            domain.setup(session)

            queries.utbetaling.upsert(utbetaling)

            queries.utbetaling.setJournalpostId(utbetaling.id, "123")

            queries.utbetaling.getOrError(utbetaling.id).journalpostId shouldBe "123"
        }
    }

    context("utbetaling med beregning for månedsverk med fast sats per tiltaksplass") {
        test("upsert and get beregning") {
            database.runAndRollback { session ->
                domain.setup(session)

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

                queries.utbetaling.upsert(utbetaling.copy(beregning = beregning))

                queries.utbetaling.getOrError(utbetaling.id).beregning shouldBe beregning
            }
        }

        test("tillater lagring av overlappende deltakelsesperioder") {
            database.runAndRollback { session ->
                domain.setup(session)

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
                        satser = setOf(
                            SatsPeriode(
                                Periode.forMonthOf(LocalDate.of(2023, 1, 1)),
                                20_205,
                            ),
                        ),
                        stengt = setOf(),
                        deltakelser = setOf(deltakelse),
                    ),
                    output = UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Output(
                        belop = 0,
                        deltakelser = setOf(),
                    ),
                )

                queries.utbetaling.upsert(utbetaling.copy(beregning = beregning))
            }
        }
    }

    context("utbetaling med beregning for månedsverk") {
        test("upsert and get beregning") {
            database.runAndRollback { session ->
                domain.setup(session)

                val deltakelse1Id = UUID.randomUUID()
                val deltakelse2Id = UUID.randomUUID()
                val beregning = UtbetalingBeregningPrisPerManedsverk(
                    input = UtbetalingBeregningPrisPerManedsverk.Input(
                        satser = setOf(
                            SatsPeriode(
                                Periode(periode.start, LocalDate.of(2023, 1, 15)),
                                20_205,
                            ),
                            SatsPeriode(
                                Periode(LocalDate.of(2023, 1, 15), periode.slutt),
                                20_975,
                            ),
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
                                    UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                                        periode,
                                        0.5,
                                        20_205,
                                    ),
                                ),
                            ),
                            UtbetalingBeregningOutputDeltakelse(
                                deltakelse2Id,
                                setOf(
                                    UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                                        periode,
                                        1.0,
                                        20_205,
                                    ),
                                ),
                            ),
                        ),
                    ),
                )

                queries.utbetaling.upsert(utbetaling.copy(beregning = beregning))

                queries.utbetaling.getOrError(utbetaling.id).beregning shouldBe beregning
            }
        }
    }

    context("utbetaling med beregning for ukesverk") {
        test("upsert and get beregning") {
            database.runAndRollback { session ->
                domain.setup(session)

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
                                    UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                                        periode,
                                        2.2,
                                        20_205,
                                    ),
                                ),
                            ),
                            UtbetalingBeregningOutputDeltakelse(
                                deltakelse2Id,
                                setOf(
                                    UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                                        periode,
                                        4.2,
                                        20_205,
                                    ),
                                ),
                            ),
                        ),
                    ),
                )

                queries.utbetaling.upsert(utbetaling.copy(beregning = beregning))

                queries.utbetaling.getOrError(utbetaling.id).beregning shouldBe beregning
            }
        }
    }

    context("utbetaling med beregning for hele ukesverk") {
        test("upsert and get beregning") {
            database.runAndRollback { session ->
                domain.setup(session)

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
                                    UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                                        periode,
                                        2.0,
                                        20_205,
                                    ),
                                ),
                            ),
                            UtbetalingBeregningOutputDeltakelse(
                                deltakelse2Id,
                                setOf(
                                    UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                                        periode,
                                        4.0,
                                        20_205,
                                    ),
                                ),
                            ),
                        ),
                    ),
                )

                queries.utbetaling.upsert(utbetaling.copy(beregning = beregning))

                queries.utbetaling.getOrError(utbetaling.id).beregning shouldBe beregning
            }
        }
    }

    context("utbetaling med beregning for pris per time oppfølging") {
        test("upsert and get beregning") {
            database.runAndRollback { session ->
                domain.setup(session)

                val deltakelse1Id = UUID.randomUUID()
                val deltakelse2Id = UUID.randomUUID()
                val beregning = UtbetalingBeregningPrisPerTimeOppfolging(
                    input = UtbetalingBeregningPrisPerTimeOppfolging.Input(
                        belop = 1999,
                        satser = setOf(
                            SatsPeriode(periode, 100),
                            SatsPeriode(periode, 200),
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

                queries.utbetaling.upsert(utbetaling.copy(beregning = beregning))

                queries.utbetaling.getOrError(utbetaling.id).beregning shouldBe beregning
            }
        }
    }

    context("Filter for manglende innsendinger") {
        test("Henter kun innsendinger som venter på arrangor") {
            database.runAndRollback { session ->
                domain.setup(session)

                val utbetaling1 = utbetaling1.copy(
                    status = UtbetalingStatusType.GENERERT,
                )
                val utbetaling2 = UtbetalingFixtures.utbetaling2.copy(status = UtbetalingStatusType.INNSENDT)
                val utbetaling3 = UtbetalingFixtures.utbetaling3.copy(
                    status = UtbetalingStatusType.GENERERT,
                    gjennomforingId = AFT1.id,
                )

                queries.utbetaling.upsert(utbetaling1)
                queries.utbetaling.upsert(utbetaling2)

                queries.utbetaling.getAll(
                    filter = AdminInnsendingerFilter(
                        navEnheter = emptyList(),
                        tiltakstyper = emptyList(),
                        sortering = null,
                    ),
                ).shouldHaveSize(1).should { (first) ->
                    first.id shouldBe utbetaling1.id
                    first.status.type shouldBe Type.VENTER_PA_ARRANGOR
                }

                queries.utbetaling.upsert(utbetaling3)

                queries.utbetaling.getAll(
                    filter = AdminInnsendingerFilter(
                        navEnheter = emptyList(),
                        tiltakstyper = emptyList(),
                        sortering = null,
                    ),
                ).shouldHaveSize(2).should { (first, second) ->
                    first.id shouldBe utbetaling1.id
                    first.status.type shouldBe Type.VENTER_PA_ARRANGOR

                    second.id shouldBe utbetaling3.id
                    second.status.type shouldBe Type.VENTER_PA_ARRANGOR
                }
            }
        }

        test("Utleder kostnadssteder fra tilsagn") {
            var periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1))

            database.runAndRollback { session ->
                domain.copy(
                    tilsagn = listOf(
                        Tilsagn1.copy(
                            periode = periode,
                            kostnadssted = NavEnhetFixtures.Innlandet.enhetsnummer,
                        ),
                        Tilsagn2.copy(
                            periode = periode,
                            kostnadssted = NavEnhetFixtures.Gjovik.enhetsnummer,
                        ),
                        Tilsagn3.copy(
                            periode = periode,
                            kostnadssted = NavEnhetFixtures.Innlandet.enhetsnummer,
                        ),
                    ),
                ).setup(session)

                val utbetalingQueries = UtbetalingQueries(session)

                val utbetaling1 = utbetaling1.copy(
                    status = UtbetalingStatusType.GENERERT,
                    gjennomforingId = AFT1.id,
                    periode = periode,
                )

                utbetalingQueries.upsert(utbetaling1)

                utbetalingQueries.getAll(
                    filter = AdminInnsendingerFilter(
                        navEnheter = listOf(),
                        tiltakstyper = emptyList(),
                        sortering = null,
                    ),
                ).shouldHaveSize(1).should { (utbetaling) ->
                    utbetaling.kostnadssteder shouldContainExactlyInAnyOrder listOf(
                        KostnadsstedDto(
                            NavEnhetFixtures.Innlandet.navn,
                            NavEnhetFixtures.Innlandet.enhetsnummer,
                        ),
                        KostnadsstedDto(
                            NavEnhetFixtures.Gjovik.navn,
                            NavEnhetFixtures.Gjovik.enhetsnummer,
                        ),
                    )
                }
            }
        }

        test("Filtrer på tiltakstype") {
            database.runAndRollback { session ->
                domain.setup(session)

                val utbetaling1 = utbetaling1.copy(
                    status = UtbetalingStatusType.GENERERT,
                    gjennomforingId = AFT1.id,
                )
                val utbetaling2 = UtbetalingFixtures.utbetaling2.copy(
                    status = UtbetalingStatusType.GENERERT,
                    gjennomforingId = VTA1.id,
                )

                queries.utbetaling.upsert(utbetaling1)
                queries.utbetaling.upsert(utbetaling2)

                queries.utbetaling.getAll(
                    filter = AdminInnsendingerFilter(
                        navEnheter = emptyList(),
                        tiltakstyper = listOf(TiltakstypeFixtures.AFT.id),
                        sortering = null,
                    ),
                ).shouldHaveSize(1).should { (first) ->
                    first.id shouldBe utbetaling1.id
                    first.tiltakstype.tiltakskode shouldBe TiltakstypeFixtures.AFT.tiltakskode
                }
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
                ).shouldHaveSize(1).should { (first) ->
                    first.id shouldBe utbetaling2.id
                    first.kostnadssteder shouldBe listOf(
                        KostnadsstedDto(
                            NavEnhetFixtures.Gjovik.navn,
                            NavEnhetFixtures.Gjovik.enhetsnummer,
                        ),
                    )
                }
            }
        }
    }

    test("avbryt utbetaling") {
        database.runAndRollback { session ->
            domain.setup(session)

            queries.utbetaling.upsert(utbetaling)
            queries.utbetaling.avbrytUtbetaling(utbetaling.id, "min begrunnelse", Instant.now())

            queries.utbetaling.getOrError(utbetaling.id).status shouldBe UtbetalingStatusType.AVBRUTT
        }
    }
})
