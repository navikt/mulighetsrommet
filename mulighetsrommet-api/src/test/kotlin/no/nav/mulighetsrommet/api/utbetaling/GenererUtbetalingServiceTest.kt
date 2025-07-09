package no.nav.mulighetsrommet.api.utbetaling

import arrow.core.Either
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.mulighetsrommet.api.avtale.model.AvtaltSats
import no.nav.mulighetsrommet.api.avtale.model.Prismodell
import no.nav.mulighetsrommet.api.clients.kontoregisterOrganisasjon.KontonummerResponse
import no.nav.mulighetsrommet.api.clients.kontoregisterOrganisasjon.KontoregisterOrganisasjonClient
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.utbetaling.db.DeltakerDbo
import no.nav.mulighetsrommet.api.utbetaling.model.*
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.*
import java.time.LocalDate
import java.time.LocalDateTime

class GenererUtbetalingServiceTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))
    val kontoregisterOrganisasjonClient: KontoregisterOrganisasjonClient = mockk(relaxed = true)

    afterEach {
        database.truncateAll()
    }

    fun createUtbetalingService() = GenererUtbetalingService(
        db = database.db,
        kontoregisterOrganisasjonClient = kontoregisterOrganisasjonClient,
    )

    coEvery { kontoregisterOrganisasjonClient.getKontonummerForOrganisasjon(Organisasjonsnummer("123456789")) } returns Either.Right(
        KontonummerResponse(
            mottaker = "123456789",
            kontonr = "12345678901",
        ),
    )

    coEvery { kontoregisterOrganisasjonClient.getKontonummerForOrganisasjon(Organisasjonsnummer("976663934")) } returns Either.Right(
        KontonummerResponse(
            mottaker = "976663934",
            kontonr = "12345678901",
        ),
    )

    context("utbetalinger for forhåndsgodkjente tiltak") {
        val service = createUtbetalingService()

        val organisasjonsnummer = ArrangorFixtures.underenhet1.organisasjonsnummer

        test("genererer ikke utbetaling når deltakelser mangler") {
            MulighetsrommetTestDomain(
                arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
            ).initialize(database.db)

            service.genererUtbetalingForMonth(1)

            database.run {
                queries.utbetaling.getByArrangorIds(organisasjonsnummer).shouldHaveSize(0)
            }
        }

        test("genererer en utbetaling med riktig periode, sats og deltakere som input") {
            val domain = MulighetsrommetTestDomain(
                arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                deltakere = listOf(
                    DeltakerFixtures.createDeltaker(
                        AFT1.id,
                        startDato = LocalDate.of(2025, 1, 1),
                        sluttDato = LocalDate.of(2025, 1, 31),
                        statusType = DeltakerStatus.Type.DELTAR,
                        deltakelsesprosent = 100.0,
                    ),
                ),
            ).initialize(database.db)

            val utbetaling = service.genererUtbetalingForMonth(1)
                .shouldHaveSize(1)
                .first()

            utbetaling.gjennomforing.id shouldBe AFT1.id
            utbetaling.betalingsinformasjon.kontonummer shouldBe Kontonummer("12345678901")
            utbetaling.beregning.input shouldBe UtbetalingBeregningPrisPerManedsverk.Input(
                periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                sats = 20975,
                stengt = setOf(),
                deltakelser = setOf(
                    DeltakelsePerioder(
                        deltakelseId = domain.deltakere[0].id,
                        perioder = listOf(
                            DeltakelsesprosentPeriode(
                                periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 2, 1)),
                                deltakelsesprosent = 100.0,
                            ),
                        ),
                    ),
                ),
            )
        }

        test("genererer en utbetaling med kid-nummer fra forrige godkjente utbetaling fra arrangør") {
            MulighetsrommetTestDomain(
                gjennomforinger = listOf(AFT1),
                deltakere = listOf(
                    DeltakerFixtures.createDeltaker(
                        AFT1.id,
                        startDato = LocalDate.of(2025, 1, 1),
                        sluttDato = LocalDate.of(2025, 2, 28),
                        statusType = DeltakerStatus.Type.DELTAR,
                        deltakelsesprosent = 100.0,
                    ),
                ),
            ).initialize(database.db)

            val utbetaling = service.genererUtbetalingForMonth(1).first()
            utbetaling.gjennomforing.id shouldBe AFT1.id
            utbetaling.betalingsinformasjon.kontonummer shouldBe Kontonummer("12345678901")
            utbetaling.betalingsinformasjon.kid shouldBe null

            database.run {
                queries.utbetaling.setKid(
                    id = utbetaling.id,
                    kid = Kid.parseOrThrow("006402710013"),
                )
            }

            val sisteKrav = service.genererUtbetalingForMonth(2).first()
            sisteKrav.gjennomforing.id shouldBe AFT1.id
            sisteKrav.betalingsinformasjon.kid shouldBe Kid.parseOrThrow("006402710013")
        }

        test("genererer en utbetaling med relevante deltakelse-perioder som input") {
            val domain = MulighetsrommetTestDomain(
                gjennomforinger = listOf(AFT1),
                deltakere = listOf(
                    DeltakerFixtures.createDeltaker(
                        AFT1.id,
                        startDato = LocalDate.of(2025, 1, 1),
                        sluttDato = LocalDate.of(2025, 1, 31),
                        statusType = DeltakerStatus.Type.DELTAR,
                        deltakelsesprosent = 100.0,
                    ),
                    DeltakerFixtures.createDeltaker(
                        AFT1.id,
                        startDato = LocalDate.of(2025, 1, 1),
                        sluttDato = LocalDate.of(2025, 1, 15),
                        statusType = DeltakerStatus.Type.DELTAR,
                        deltakelsesprosent = 40.0,
                    ),
                    DeltakerFixtures.createDeltaker(
                        AFT1.id,
                        startDato = LocalDate.of(2023, 1, 1),
                        sluttDato = LocalDate.of(2025, 12, 31),
                        statusType = DeltakerStatus.Type.DELTAR,
                        deltakelsesprosent = 50.0,
                    ),
                    DeltakerFixtures.createDeltaker(
                        AFT1.id,
                        startDato = LocalDate.of(2023, 1, 1),
                        sluttDato = LocalDate.of(2023, 12, 31),
                        statusType = DeltakerStatus.Type.DELTAR,
                        deltakelsesprosent = 100.0,
                    ),
                    DeltakerFixtures.createDeltaker(
                        AFT1.id,
                        startDato = LocalDate.of(2025, 1, 1),
                        sluttDato = LocalDate.of(2025, 1, 31),
                        statusType = DeltakerStatus.Type.IKKE_AKTUELL,
                        deltakelsesprosent = 100.0,
                    ),
                    DeltakerFixtures.createDeltaker(
                        AFT1.id,
                        startDato = LocalDate.of(2023, 1, 1),
                        sluttDato = LocalDate.of(2025, 12, 31),
                        statusType = DeltakerStatus.Type.DELTAR,
                        deltakelsesprosent = 10.0,
                        deltakelsesmengder = listOf(
                            DeltakerDbo.Deltakelsesmengde(
                                gyldigFra = LocalDate.of(2023, 1, 1),
                                deltakelsesprosent = 20.0,
                                opprettetTidspunkt = LocalDateTime.now(),
                            ),
                            DeltakerDbo.Deltakelsesmengde(
                                gyldigFra = LocalDate.of(2025, 1, 10),
                                deltakelsesprosent = 15.0,
                                opprettetTidspunkt = LocalDateTime.now(),
                            ),
                            DeltakerDbo.Deltakelsesmengde(
                                gyldigFra = LocalDate.of(2025, 1, 20),
                                deltakelsesprosent = 10.0,
                                opprettetTidspunkt = LocalDateTime.now(),
                            ),
                            DeltakerDbo.Deltakelsesmengde(
                                gyldigFra = LocalDate.of(2025, 2, 1),
                                deltakelsesprosent = 5.0,
                                opprettetTidspunkt = LocalDateTime.now(),
                            ),
                        ),
                    ),
                ),
            ).initialize(database.db)

            val utbetaling = service.genererUtbetalingForMonth(1).first()

            utbetaling.beregning.input.shouldBeTypeOf<UtbetalingBeregningPrisPerManedsverk.Input>().should {
                it.deltakelser shouldBe setOf(
                    DeltakelsePerioder(
                        deltakelseId = domain.deltakere[0].id,
                        perioder = listOf(
                            DeltakelsesprosentPeriode(
                                periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 2, 1)),
                                deltakelsesprosent = 100.0,
                            ),
                        ),
                    ),
                    DeltakelsePerioder(
                        deltakelseId = domain.deltakere[1].id,
                        perioder = listOf(
                            DeltakelsesprosentPeriode(
                                periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 16)),
                                deltakelsesprosent = 40.0,
                            ),
                        ),
                    ),
                    DeltakelsePerioder(
                        deltakelseId = domain.deltakere[2].id,
                        perioder = listOf(
                            DeltakelsesprosentPeriode(
                                periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 2, 1)),
                                deltakelsesprosent = 50.0,
                            ),
                        ),
                    ),
                    DeltakelsePerioder(
                        deltakelseId = domain.deltakere[5].id,
                        perioder = listOf(
                            DeltakelsesprosentPeriode(
                                periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 10)),
                                deltakelsesprosent = 20.0,
                            ),
                            DeltakelsesprosentPeriode(
                                periode = Periode(LocalDate.of(2025, 1, 10), LocalDate.of(2025, 1, 20)),
                                deltakelsesprosent = 15.0,
                            ),
                            DeltakelsesprosentPeriode(
                                periode = Periode(LocalDate.of(2025, 1, 20), LocalDate.of(2025, 2, 1)),
                                deltakelsesprosent = 10.0,
                            ),
                        ),
                    ),
                )
            }
        }

        test("overstyrer deltakelse-perioder når det er stengt hos arrangør") {
            MulighetsrommetTestDomain(
                gjennomforinger = listOf(AFT1),
                deltakere = listOf(
                    DeltakerFixtures.createDeltaker(
                        AFT1.id,
                        startDato = LocalDate.of(2025, 1, 1),
                        sluttDato = LocalDate.of(2025, 2, 1),
                        statusType = DeltakerStatus.Type.DELTAR,
                        deltakelsesprosent = 10.0,
                        deltakelsesmengder = listOf(
                            DeltakerDbo.Deltakelsesmengde(
                                gyldigFra = LocalDate.of(2023, 1, 1),
                                deltakelsesprosent = 20.0,
                                opprettetTidspunkt = LocalDateTime.now(),
                            ),
                            DeltakerDbo.Deltakelsesmengde(
                                gyldigFra = LocalDate.of(2025, 1, 15),
                                deltakelsesprosent = 10.0,
                                opprettetTidspunkt = LocalDateTime.now(),
                            ),
                        ),
                    ),
                ),
            ) {
                queries.gjennomforing.setStengtHosArrangor(
                    AFT1.id,
                    Periode(LocalDate.of(2023, 12, 10), LocalDate.of(2025, 1, 10)),
                    "Ferie 1",
                )
                queries.gjennomforing.setStengtHosArrangor(
                    AFT1.id,
                    Periode(LocalDate.of(2025, 1, 20), LocalDate.of(2025, 2, 20)),
                    "Ferie 2",
                )
                queries.gjennomforing.setStengtHosArrangor(
                    AFT1.id,
                    Periode(LocalDate.of(2025, 2, 20), LocalDate.of(2025, 3, 20)),
                    "Fremtidig ferie",
                )
            }.initialize(database.db)

            val utbetaling = service.genererUtbetalingForMonth(1).first()

            utbetaling.beregning.input.shouldBeTypeOf<UtbetalingBeregningPrisPerManedsverk.Input>().should {
                it.stengt shouldBe setOf(
                    StengtPeriode(Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 10)), "Ferie 1"),
                    StengtPeriode(Periode(LocalDate.of(2025, 1, 20), LocalDate.of(2025, 2, 1)), "Ferie 2"),
                )
            }
        }

        test("genererer en utbetaling med beregnet belop basert på input") {
            val domain = MulighetsrommetTestDomain(
                gjennomforinger = listOf(AFT1),
                deltakere = listOf(
                    DeltakerFixtures.createDeltaker(
                        AFT1.id,
                        startDato = LocalDate.of(2025, 1, 1),
                        sluttDato = LocalDate.of(2025, 1, 31),
                        statusType = DeltakerStatus.Type.DELTAR,
                        deltakelsesprosent = 100.0,
                    ),
                ),
            ).initialize(database.db)

            val utbetaling = service.genererUtbetalingForMonth(1).first()

            utbetaling.beregning.output.shouldBeTypeOf<UtbetalingBeregningPrisPerManedsverk.Output>().should {
                it.belop shouldBe 20975
                it.deltakelser shouldBe setOf(
                    DeltakelseManedsverk(
                        deltakelseId = domain.deltakere[0].id,
                        manedsverk = 1.0,
                    ),
                )
            }
        }

        test("genererer ikke utbetaling hvis det finnes et med overlappende periode") {
            MulighetsrommetTestDomain(
                gjennomforinger = listOf(AFT1),
                deltakere = listOf(
                    DeltakerFixtures.createDeltaker(
                        AFT1.id,
                        startDato = LocalDate.of(2023, 2, 1),
                        sluttDato = LocalDate.of(2026, 6, 1),
                        statusType = DeltakerStatus.Type.DELTAR,
                        deltakelsesprosent = 100.0,
                    ),
                    DeltakerFixtures.createDeltaker(
                        AFT1.id,
                        startDato = LocalDate.of(2023, 1, 1),
                        sluttDato = LocalDate.of(2026, 2, 1),
                        statusType = DeltakerStatus.Type.DELTAR,
                        deltakelsesprosent = 100.0,
                    ),
                ),
            ).initialize(database.db)

            service.genererUtbetalingForMonth(1).shouldHaveSize(1)
            database.run { queries.utbetaling.getByArrangorIds(organisasjonsnummer).shouldHaveSize(1) }

            service.genererUtbetalingForMonth(2).shouldHaveSize(1)
            database.run { queries.utbetaling.getByArrangorIds(organisasjonsnummer).shouldHaveSize(2) }

            // Februar finnes allerede så ingen nye
            service.genererUtbetalingForMonth(2).shouldHaveSize(0)
            database.run { queries.utbetaling.getByArrangorIds(organisasjonsnummer).shouldHaveSize(2) }
        }

        test("deltaker med startDato lik periodeSlutt blir ikke med i kravet") {
            val domain = MulighetsrommetTestDomain(
                gjennomforinger = listOf(AFT1),
                deltakere = listOf(
                    DeltakerFixtures.createDeltaker(
                        AFT1.id,
                        startDato = LocalDate.of(2025, 2, 1),
                        sluttDato = LocalDate.of(2025, 6, 1),
                        statusType = DeltakerStatus.Type.DELTAR,
                        deltakelsesprosent = 100.0,
                    ),
                    DeltakerFixtures.createDeltaker(
                        AFT1.id,
                        startDato = LocalDate.of(2023, 1, 1),
                        sluttDato = LocalDate.of(2025, 2, 1),
                        statusType = DeltakerStatus.Type.DELTAR,
                        deltakelsesprosent = 100.0,
                    ),
                ),
            ).initialize(database.db)

            val utbetaling = service.genererUtbetalingForMonth(1).first()

            utbetaling.beregning.input.shouldBeTypeOf<UtbetalingBeregningPrisPerManedsverk.Input>().should {
                it.deltakelser.shouldHaveSize(1).first().deltakelseId.shouldBe(domain.deltakere[1].id)
            }
        }
    }

    context("rekalkulering av utbetalinger for forhåndsgodkjente tiltak") {
        val service = createUtbetalingService()

        val deltaker = DeltakerFixtures.createDeltaker(
            AFT1.id,
            startDato = LocalDate.of(2025, 6, 1),
            sluttDato = LocalDate.of(2025, 6, 15),
            statusType = DeltakerStatus.Type.DELTAR,
            deltakelsesprosent = 100.0,
        )

        val beregning = UtbetalingBeregningPrisPerManedsverk(
            input = UtbetalingBeregningPrisPerManedsverk.Input(
                periode = Periode.forMonthOf(LocalDate.of(2025, 6, 1)),
                sats = 20975,
                stengt = setOf(),
                deltakelser = setOf(
                    DeltakelsePerioder(
                        deltakelseId = deltaker.id,
                        perioder = listOf(
                            DeltakelsesprosentPeriode(
                                periode = Periode.forMonthOf(LocalDate.of(2025, 6, 1)),
                                deltakelsesprosent = 100.0,
                            ),
                        ),
                    ),
                ),
            ),
            output = UtbetalingBeregningPrisPerManedsverk.Output(
                belop = 20975,
                deltakelser = setOf(
                    DeltakelseManedsverk(deltakelseId = deltaker.id, manedsverk = 1.0),
                ),
            ),
        )

        test("oppdaterer beregnet utbetaling når deltakelser endres") {
            MulighetsrommetTestDomain(
                gjennomforinger = listOf(AFT1),
                utbetalinger = listOf(
                    UtbetalingFixtures.utbetaling1.copy(
                        gjennomforingId = AFT1.id,
                        periode = beregning.input.periode,
                        beregning = beregning,
                    ),
                ),
                deltakere = listOf(deltaker),
            ).initialize(database.db)

            service.oppdaterUtbetalingBeregningForGjennomforing(AFT1.id)

            database.run {
                val utbetaling = queries.utbetaling.get(UtbetalingFixtures.utbetaling1.id).shouldNotBeNull()
                utbetaling.beregning.output.shouldBeTypeOf<UtbetalingBeregningPrisPerManedsverk.Output>().should {
                    it.belop shouldBe 10488
                    it.deltakelser shouldBe setOf(
                        DeltakelseManedsverk(deltakelseId = deltaker.id, manedsverk = 0.5),
                    )
                }
            }
        }

        test("oppdaterer ikke utbetaling hvis det allerede er godkjent av arrangør") {
            MulighetsrommetTestDomain(
                gjennomforinger = listOf(AFT1),
                utbetalinger = listOf(
                    UtbetalingFixtures.utbetaling1.copy(
                        gjennomforingId = AFT1.id,
                        periode = beregning.input.periode,
                        beregning = beregning,
                    ),
                ),
                deltakere = listOf(deltaker),
            ) {
                queries.utbetaling.setGodkjentAvArrangor(UtbetalingFixtures.utbetaling1.id, LocalDateTime.now())
            }.initialize(database.db)

            service.oppdaterUtbetalingBeregningForGjennomforing(AFT1.id)

            database.run {
                val utbetaling = queries.utbetaling.get(UtbetalingFixtures.utbetaling1.id).shouldNotBeNull()
                utbetaling.beregning.output.shouldBeTypeOf<UtbetalingBeregningPrisPerManedsverk.Output>().should {
                    it.belop shouldBe 20975
                    it.deltakelser shouldBe setOf(
                        DeltakelseManedsverk(deltakelseId = deltaker.id, manedsverk = 1.0),
                    )
                }
            }
        }
    }

    context("utbetalinger for anskaffede tiltak") {
        val service = createUtbetalingService()

        val oppfolging = GjennomforingFixtures.Oppfolging1

        test("genererer en utbetaling for avtalt pris per månedsverk med riktig periode, stengt, sats og deltakere som input") {
            val avtale = AvtaleFixtures.oppfolging.copy(
                prismodell = Prismodell.AVTALT_PRIS_PER_MANEDSVERK,
                satser = listOf(
                    AvtaltSats(Periode.forMonthOf(LocalDate.of(2025, 1, 1)), 100),
                ),
            )

            val domain = MulighetsrommetTestDomain(
                arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
                avtaler = listOf(avtale),
                gjennomforinger = listOf(oppfolging),
                deltakere = listOf(
                    DeltakerFixtures.createDeltaker(
                        oppfolging.id,
                        startDato = LocalDate.of(2025, 1, 1),
                        sluttDato = LocalDate.of(2025, 1, 31),
                        statusType = DeltakerStatus.Type.DELTAR,
                    ),
                    DeltakerFixtures.createDeltaker(
                        oppfolging.id,
                        startDato = LocalDate.of(2025, 2, 1),
                        sluttDato = LocalDate.of(2025, 3, 31),
                        statusType = DeltakerStatus.Type.DELTAR,
                    ),
                ),
            ) {
                queries.gjennomforing.setStengtHosArrangor(
                    oppfolging.id,
                    Periode(LocalDate.of(2025, 1, 20), LocalDate.of(2025, 2, 20)),
                    "Ferie!",
                )
            }.initialize(database.db)

            val utbetaling = service.genererUtbetalingForMonth(1)
                .shouldHaveSize(1)
                .first()

            utbetaling.gjennomforing.id shouldBe oppfolging.id
            utbetaling.beregning.input shouldBe UtbetalingBeregningPrisPerManedsverk.Input(
                periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                sats = 100,
                stengt = setOf(
                    StengtPeriode(Periode(LocalDate.of(2025, 1, 20), LocalDate.of(2025, 2, 1)), "Ferie!"),
                ),
                deltakelser = setOf(
                    DeltakelsePerioder(
                        deltakelseId = domain.deltakere[0].id,
                        perioder = listOf(
                            DeltakelsesprosentPeriode(
                                periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 2, 1)),
                                deltakelsesprosent = 100.0,
                            ),
                        ),
                    ),
                ),
            )
        }

        test("genererer en utbetaling for avtalt pris per ukesverk med riktig periode, stengt, sats og deltakere som input") {
            val avtale = AvtaleFixtures.oppfolging.copy(
                prismodell = Prismodell.AVTALT_PRIS_PER_UKESVERK,
                satser = listOf(
                    AvtaltSats(Periode.forMonthOf(LocalDate.of(2025, 1, 1)), 100),
                ),
            )

            val domain = MulighetsrommetTestDomain(
                arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
                avtaler = listOf(avtale),
                gjennomforinger = listOf(oppfolging),
                deltakere = listOf(
                    DeltakerFixtures.createDeltaker(
                        oppfolging.id,
                        startDato = LocalDate.of(2025, 1, 1),
                        sluttDato = LocalDate.of(2025, 1, 31),
                        statusType = DeltakerStatus.Type.DELTAR,
                    ),
                    DeltakerFixtures.createDeltaker(
                        oppfolging.id,
                        startDato = LocalDate.of(2025, 2, 1),
                        sluttDato = LocalDate.of(2025, 3, 31),
                        statusType = DeltakerStatus.Type.DELTAR,
                    ),
                ),
            ) {
                queries.gjennomforing.setStengtHosArrangor(
                    oppfolging.id,
                    Periode(LocalDate.of(2025, 1, 20), LocalDate.of(2025, 2, 20)),
                    "Ferie!",
                )
            }.initialize(database.db)

            val utbetaling = service.genererUtbetalingForMonth(1)
                .shouldHaveSize(1)
                .first()

            utbetaling.gjennomforing.id shouldBe oppfolging.id
            utbetaling.beregning.input shouldBe UtbetalingBeregningPrisPerUkesverk.Input(
                periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                sats = 100,
                stengt = setOf(
                    StengtPeriode(Periode(LocalDate.of(2025, 1, 20), LocalDate.of(2025, 2, 1)), "Ferie!"),
                ),
                deltakelser = setOf(
                    DeltakelsePeriode(
                        deltakelseId = domain.deltakere[0].id,
                        periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 2, 1)),
                    ),
                ),
            )
        }

        test("utbetalinger blir oppdatert med ny beregning når avtalens prismodell endres") {
            val avtale = AvtaleFixtures.oppfolging.copy(
                prismodell = Prismodell.AVTALT_PRIS_PER_MANEDSVERK,
                satser = listOf(
                    AvtaltSats(Periode.forMonthOf(LocalDate.of(2025, 1, 1)), 100),
                ),
            )

            MulighetsrommetTestDomain(
                arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
                avtaler = listOf(avtale),
                gjennomforinger = listOf(oppfolging),
                deltakere = listOf(
                    DeltakerFixtures.createDeltaker(
                        oppfolging.id,
                        startDato = LocalDate.of(2025, 1, 1),
                        sluttDato = LocalDate.of(2025, 1, 31),
                        statusType = DeltakerStatus.Type.DELTAR,
                    ),
                ),
            ).initialize(database.db)

            val generertUtbetaling = service.genererUtbetalingForMonth(1).shouldHaveSize(1).first()
            generertUtbetaling.beregning.shouldBeTypeOf<UtbetalingBeregningPrisPerManedsverk>()

            database.run {
                queries.avtale.upsert(avtale.copy(prismodell = Prismodell.AVTALT_PRIS_PER_UKESVERK))
            }

            val oppdatertUtbetaling = service.oppdaterUtbetalingBeregningForGjennomforing(oppfolging.id)
                .shouldHaveSize(1).first()
            oppdatertUtbetaling.beregning.shouldBeTypeOf<UtbetalingBeregningPrisPerUkesverk>()
        }
    }
})
