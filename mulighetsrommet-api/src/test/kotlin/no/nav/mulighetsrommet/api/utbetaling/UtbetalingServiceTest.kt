package no.nav.mulighetsrommet.api.utbetaling

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.mockk
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.tilsagn.OkonomiBestillingService
import no.nav.mulighetsrommet.api.utbetaling.model.*
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.DeltakerStatus
import no.nav.mulighetsrommet.model.Kid
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.Periode
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class UtbetalingServiceTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    afterEach {
        database.truncateAll()
    }

    fun createUtbetalingService(
        okonomi: OkonomiBestillingService = mockk(relaxed = true),
    ) = UtbetalingService(
        db = database.db,
        okonomi = okonomi,
    )

    context("generering av utbetaling for AFT") {
        val service = createUtbetalingService()

        val organisasjonsnummer = ArrangorFixtures.underenhet1.organisasjonsnummer

        test("genererer ikke utbetaling når deltakelser mangler") {
            MulighetsrommetTestDomain(
                arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
            ).initialize(database.db)

            service.genererUtbetalingForMonth(LocalDate.of(2024, 1, 1))

            database.run {
                queries.utbetaling.getByArrangorIds(organisasjonsnummer).shouldHaveSize(0)
            }
        }

        test("genererer et utbetaling med riktig periode, frist og sats som input") {
            val domain = MulighetsrommetTestDomain(
                arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                deltakere = listOf(
                    DeltakerFixtures.createDeltaker(
                        AFT1.id,
                        startDato = LocalDate.of(2024, 1, 1),
                        sluttDato = LocalDate.of(2024, 1, 31),
                        statusType = DeltakerStatus.Type.DELTAR,
                        deltakelsesprosent = 100.0,
                    ),
                ),
            ).initialize(database.db)

            val utbetaling = service.genererUtbetalingForMonth(LocalDate.of(2024, 1, 1))
                .shouldHaveSize(1)
                .first()

            utbetaling.gjennomforing.id shouldBe AFT1.id
            utbetaling.fristForGodkjenning shouldBe LocalDateTime.of(2024, 4, 1, 0, 0, 0)
            utbetaling.beregning.input shouldBe UtbetalingBeregningAft.Input(
                periode = Periode.forMonthOf(LocalDate.of(2024, 1, 1)),
                sats = 20205,
                deltakelser = setOf(
                    DeltakelsePerioder(
                        deltakelseId = domain.deltakere[0].id,
                        perioder = listOf(
                            DeltakelsePeriode(
                                start = LocalDate.of(2024, 1, 1),
                                slutt = LocalDate.of(2024, 2, 1),
                                deltakelsesprosent = 100.0,
                            ),
                        ),
                    ),
                ),
            )
        }

        test("genererer et utbetaling med kontonummer og kid-nummer fra forrige godkjente utbetaling fra arrangør") {
            val domain = MulighetsrommetTestDomain(
                gjennomforinger = listOf(AFT1),
                deltakere = listOf(
                    DeltakerFixtures.createDeltaker(
                        AFT1.id,
                        startDato = LocalDate.of(2024, 1, 1),
                        sluttDato = LocalDate.of(2024, 2, 28),
                        statusType = DeltakerStatus.Type.DELTAR,
                        deltakelsesprosent = 100.0,
                    ),
                ),
            ).initialize(database.db)

            val utbetaling = service.genererUtbetalingForMonth(LocalDate.of(2024, 1, 1)).first()
            utbetaling.gjennomforing.id shouldBe AFT1.id
            utbetaling.betalingsinformasjon.kontonummer shouldBe null
            utbetaling.betalingsinformasjon.kid shouldBe null

            database.run {
                queries.utbetaling.setBetalingsInformasjon(
                    id = utbetaling.id,
                    kontonummer = Kontonummer("12345678901"),
                    kid = Kid("12345678901"),
                )
                queries.utbetaling.setGodkjentAvArrangor(utbetaling.id, LocalDateTime.now())
            }

            val sisteKrav = service.genererUtbetalingForMonth(LocalDate.of(2024, 2, 1)).first()
            sisteKrav.gjennomforing.id shouldBe AFT1.id
            sisteKrav.betalingsinformasjon.kontonummer shouldBe Kontonummer("12345678901")
            sisteKrav.betalingsinformasjon.kid shouldBe Kid("12345678901")
        }

        test("genererer et utbetaling med relevante deltakelser som input") {
            val domain = MulighetsrommetTestDomain(
                gjennomforinger = listOf(AFT1),
                deltakere = listOf(
                    DeltakerFixtures.createDeltaker(
                        AFT1.id,
                        startDato = LocalDate.of(2024, 1, 1),
                        sluttDato = LocalDate.of(2024, 1, 31),
                        statusType = DeltakerStatus.Type.DELTAR,
                        deltakelsesprosent = 100.0,
                    ),
                    DeltakerFixtures.createDeltaker(
                        AFT1.id,
                        startDato = LocalDate.of(2024, 1, 1),
                        sluttDato = LocalDate.of(2024, 1, 15),
                        statusType = DeltakerStatus.Type.DELTAR,
                        deltakelsesprosent = 40.0,
                    ),
                    DeltakerFixtures.createDeltaker(
                        AFT1.id,
                        startDato = LocalDate.of(2023, 1, 1),
                        sluttDato = LocalDate.of(2024, 12, 31),
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
                        startDato = LocalDate.of(2024, 1, 1),
                        sluttDato = LocalDate.of(2024, 1, 31),
                        statusType = DeltakerStatus.Type.IKKE_AKTUELL,
                        deltakelsesprosent = 100.0,
                    ),
                ),
            ).initialize(database.db)

            val utbetaling = service.genererUtbetalingForMonth(LocalDate.of(2024, 1, 1)).first()

            utbetaling.beregning.input.shouldBeTypeOf<UtbetalingBeregningAft.Input>().should {
                it.deltakelser shouldBe setOf(
                    DeltakelsePerioder(
                        deltakelseId = domain.deltakere[0].id,
                        perioder = listOf(
                            DeltakelsePeriode(
                                start = LocalDate.of(2024, 1, 1),
                                slutt = LocalDate.of(2024, 2, 1),
                                deltakelsesprosent = 100.0,
                            ),
                        ),
                    ),
                    DeltakelsePerioder(
                        deltakelseId = domain.deltakere[1].id,
                        perioder = listOf(
                            DeltakelsePeriode(
                                start = LocalDate.of(2024, 1, 1),
                                slutt = LocalDate.of(2024, 1, 16),
                                deltakelsesprosent = 40.0,
                            ),
                        ),
                    ),
                    DeltakelsePerioder(
                        deltakelseId = domain.deltakere[2].id,
                        perioder = listOf(
                            DeltakelsePeriode(
                                start = LocalDate.of(2024, 1, 1),
                                slutt = LocalDate.of(2024, 2, 1),
                                deltakelsesprosent = 50.0,
                            ),
                        ),
                    ),
                )
            }
        }

        test("genererer et utbetaling med beregnet belop basert på input") {
            val domain = MulighetsrommetTestDomain(
                gjennomforinger = listOf(AFT1),
                deltakere = listOf(
                    DeltakerFixtures.createDeltaker(
                        AFT1.id,
                        startDato = LocalDate.of(2024, 1, 1),
                        sluttDato = LocalDate.of(2024, 1, 31),
                        statusType = DeltakerStatus.Type.DELTAR,
                        deltakelsesprosent = 100.0,
                    ),
                ),
            ).initialize(database.db)

            val utbetaling = service.genererUtbetalingForMonth(LocalDate.of(2024, 1, 1)).first()

            utbetaling.beregning.output.shouldBeTypeOf<UtbetalingBeregningAft.Output>().should {
                it.belop shouldBe 20205
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

            service.genererUtbetalingForMonth(LocalDate.of(2024, 1, 1)).shouldHaveSize(1)
            database.run { queries.utbetaling.getByArrangorIds(organisasjonsnummer).shouldHaveSize(1) }

            service.genererUtbetalingForMonth(LocalDate.of(2024, 2, 1)).shouldHaveSize(1)
            database.run { queries.utbetaling.getByArrangorIds(organisasjonsnummer).shouldHaveSize(2) }

            // Februar finnes allerede så ingen nye
            service.genererUtbetalingForMonth(LocalDate.of(2024, 2, 1)).shouldHaveSize(0)
            database.run { queries.utbetaling.getByArrangorIds(organisasjonsnummer).shouldHaveSize(2) }
        }

        test("deltaker med startDato lik periodeSlutt blir ikke med i kravet") {
            val domain = MulighetsrommetTestDomain(
                gjennomforinger = listOf(AFT1),
                deltakere = listOf(
                    DeltakerFixtures.createDeltaker(
                        AFT1.id,
                        startDato = LocalDate.of(2024, 2, 1),
                        sluttDato = LocalDate.of(2024, 6, 1),
                        statusType = DeltakerStatus.Type.DELTAR,
                        deltakelsesprosent = 100.0,
                    ),
                    DeltakerFixtures.createDeltaker(
                        AFT1.id,
                        startDato = LocalDate.of(2023, 1, 1),
                        sluttDato = LocalDate.of(2024, 2, 1),
                        statusType = DeltakerStatus.Type.DELTAR,
                        deltakelsesprosent = 100.0,
                    ),
                ),
            ).initialize(database.db)

            val utbetaling = service.genererUtbetalingForMonth(LocalDate.of(2024, 1, 1)).first()

            utbetaling.beregning.input.shouldBeTypeOf<UtbetalingBeregningAft.Input>().should {
                it.deltakelser.shouldHaveSize(1).first().deltakelseId.shouldBe(domain.deltakere[1].id)
            }
        }
    }

    context("rekalkulering av utbetaling for AFT") {
        val service = createUtbetalingService()

        test("oppdaterer beregnet utbetaling når deltakelser endres") {
            val domain = MulighetsrommetTestDomain(
                gjennomforinger = listOf(AFT1),
                deltakere = listOf(
                    DeltakerFixtures.createDeltaker(
                        AFT1.id,
                        startDato = LocalDate.of(2024, 6, 1),
                        sluttDato = LocalDate.of(2024, 6, 30),
                        statusType = DeltakerStatus.Type.DELTAR,
                        deltakelsesprosent = 100.0,
                    ),
                ),
            ).initialize(database.db)

            val utbetalingId = UUID.randomUUID()

            database.run {
                val utbetaling = service.createUtbetalingAft(
                    utbetalingId = utbetalingId,
                    gjennomforingId = AFT1.id,
                    periode = Periode.forMonthOf(LocalDate.of(2024, 6, 1)),
                )
                queries.utbetaling.upsert(utbetaling)
                utbetaling.beregning.output.shouldBeTypeOf<UtbetalingBeregningAft.Output>().belop shouldBe 20205

                val updatedDeltaker = domain.deltakere[0].copy(
                    sluttDato = LocalDate.of(2024, 6, 15),
                )
                queries.deltaker.upsert(updatedDeltaker)
            }

            service.recalculateUtbetalingForGjennomforing(AFT1.id)

            database.run {
                val utbetaling = queries.utbetaling.get(utbetalingId).shouldNotBeNull()
                utbetaling.beregning.output.shouldBeTypeOf<UtbetalingBeregningAft.Output>().should {
                    it.belop shouldBe 10102
                    it.deltakelser shouldBe setOf(
                        DeltakelseManedsverk(
                            deltakelseId = domain.deltakere[0].id,
                            manedsverk = 0.5,
                        ),
                    )
                }
            }
        }

        test("oppdaterer ikke utbetaling hvis det allerede er godkjent av arrangør") {
            val domain = MulighetsrommetTestDomain(
                gjennomforinger = listOf(AFT1),
                deltakere = listOf(
                    DeltakerFixtures.createDeltaker(
                        AFT1.id,
                        startDato = LocalDate.of(2024, 6, 1),
                        sluttDato = LocalDate.of(2024, 6, 30),
                        statusType = DeltakerStatus.Type.DELTAR,
                        deltakelsesprosent = 100.0,
                    ),
                ),
            ).initialize(database.db)

            val utbetalingId = UUID.randomUUID()

            database.run {
                val utbetaling = service.createUtbetalingAft(
                    utbetalingId = utbetalingId,
                    gjennomforingId = AFT1.id,
                    periode = Periode.forMonthOf(LocalDate.of(2024, 6, 1)),
                )
                queries.utbetaling.upsert(utbetaling)
                utbetaling.beregning.output.shouldBeTypeOf<UtbetalingBeregningAft.Output>().belop shouldBe 20205

                val updatedDeltaker = domain.deltakere[0].copy(
                    sluttDato = LocalDate.of(2024, 6, 15),
                )
                queries.deltaker.upsert(updatedDeltaker)

                queries.utbetaling.setGodkjentAvArrangor(utbetalingId, LocalDateTime.now())
            }

            service.recalculateUtbetalingForGjennomforing(AFT1.id)

            database.run {
                val utbetaling = queries.utbetaling.get(utbetalingId).shouldNotBeNull()
                utbetaling.beregning.output.shouldBeTypeOf<UtbetalingBeregningAft.Output>().should {
                    it.belop shouldBe 20205
                    it.deltakelser shouldBe setOf(
                        DeltakelseManedsverk(
                            deltakelseId = domain.deltakere[0].id,
                            manedsverk = 1.0,
                        ),
                    )
                }
            }
        }
    }

    context("når utbetaling blir behandlet") {
        test("skal ikke kunne opprette del utbetaling hvis den er godkjent") {
            val tilsagn = TilsagnFixtures.Tilsagn1.copy(
                periode = Periode.forMonthOf(LocalDate.of(2024, 1, 1)),
                bestillingsnummer = "2024/1",
            )

            val utbetaling = UtbetalingFixtures.utbetaling1.copy(
                periode = Periode.forMonthOf(LocalDate.of(2024, 1, 1)),
            )

            val domain = MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.ansatt1, NavAnsattFixture.ansatt2),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(tilsagn),
                utbetalinger = listOf(utbetaling),
            ).initialize(database.db)

            val service = createUtbetalingService()

            val opprettRequest = DelutbetalingRequest(tilsagnId = tilsagn.id, belop = 100)
            service.upsertDelutbetaling(
                utbetalingId = utbetaling.id,
                request = opprettRequest,
                opprettetAv = domain.ansatte[0].navIdent,
            )
            service.besluttDelutbetaling(
                utbetalingId = utbetaling.id,
                request = BesluttDelutbetalingRequest.GodkjentDelutbetalingRequest(
                    tilsagnId = tilsagn.id,
                ),
                navIdent = domain.ansatte[1].navIdent,
            )

            service.upsertDelutbetaling(
                utbetalingId = utbetaling.id,
                request = opprettRequest,
                opprettetAv = domain.ansatte[0].navIdent,
            ).shouldBeLeft().shouldContainExactly(
                listOf(FieldError("/", "Utbetaling kan ikke endres")),
            )
        }

        test("skal ikke kunne opprette delutbetaling hvis utbetalingsperiode og tilsagnsperiode ikke overlapper") {
            val tilsagn = TilsagnFixtures.Tilsagn1.copy(
                periode = Periode.forMonthOf(LocalDate.of(2024, 1, 1)),
            )

            val utbetaling = UtbetalingFixtures.utbetaling1.copy(
                periode = Periode.forMonthOf(LocalDate.of(2024, 2, 1)),
            )

            val domain = MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.ansatt1),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(tilsagn),
                utbetalinger = listOf(utbetaling),
            ).initialize(database.db)

            val service = createUtbetalingService()

            val request = DelutbetalingRequest(tilsagnId = tilsagn.id, belop = 100)

            service.upsertDelutbetaling(utbetaling.id, request, domain.ansatte[0].navIdent).shouldBeLeft().shouldContainExactly(
                listOf(FieldError("/", "Utbetalingsperiode og tilsagnsperiode overlapper ikke")),
            )
        }

        test("skal ikke kunne opprette delutbetaling hvis belop er for stort") {
            val tilsagn1 = TilsagnFixtures.Tilsagn1.copy(
                periode = Periode.forMonthOf(LocalDate.of(2024, 1, 1)),
            )
            val tilsagn2 = TilsagnFixtures.Tilsagn2.copy(
                periode = Periode.forMonthOf(LocalDate.of(2024, 1, 1)),
            )

            val utbetaling = UtbetalingFixtures.utbetaling1.copy(
                periode = Periode.forMonthOf(LocalDate.of(2024, 1, 1)),
                beregning = UtbetalingBeregningFri(
                    input = UtbetalingBeregningFri.Input(10),
                    output = UtbetalingBeregningFri.Output(10),
                ),
            )

            val domain = MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.ansatt1),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(tilsagn1, tilsagn2),
                utbetalinger = listOf(utbetaling),
            ).initialize(database.db)
            val service = createUtbetalingService()

            service.upsertDelutbetaling(
                utbetaling.id,
                DelutbetalingRequest(tilsagnId = tilsagn1.id, belop = 100),
                domain.ansatte[0].navIdent,
            ).shouldBeLeft().shouldContainExactly(
                listOf(FieldError("/belop", "Kan ikke betale ut mer enn det er krav på")),
            )

            service.upsertDelutbetaling(
                utbetaling.id,
                DelutbetalingRequest(tilsagnId = tilsagn1.id, belop = 7),
                domain.ansatte[0].navIdent,
            ).shouldBeRight()

            // Siden 7 allerede er utbetalt nå
            service.upsertDelutbetaling(
                utbetaling.id,
                DelutbetalingRequest(tilsagnId = tilsagn2.id, belop = 5),
                domain.ansatte[0].navIdent,
            ).shouldBeLeft().shouldContainExactly(
                listOf(FieldError("/belop", "Kan ikke betale ut mer enn det er krav på")),
            )
        }

        test("løpenummer, fakturanummer og periode blir utledet fra tilsagnet og utbetalingen") {
            val tilsagn1 = TilsagnFixtures.Tilsagn2.copy(
                periode = Periode.forMonthOf(LocalDate.of(2024, 1, 1)),
                bestillingsnummer = "2024/1",
            )

            val tilsagn2 = TilsagnFixtures.Tilsagn1.copy(
                periode = Periode.forMonthOf(LocalDate.of(2024, 1, 1)),
                bestillingsnummer = "2024/2",
            )

            val utbetaling1 = UtbetalingFixtures.utbetaling1.copy(
                periode = Periode(LocalDate.of(2023, 12, 15), LocalDate.of(2024, 1, 15)),
            )

            val utbetaling2 = UtbetalingFixtures.utbetaling2.copy(
                periode = Periode(LocalDate.of(2024, 1, 15), LocalDate.of(2024, 2, 15)),
            )

            val domain = MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.ansatt1),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(tilsagn1, tilsagn2),
                utbetalinger = listOf(utbetaling1, utbetaling2),
            ).initialize(database.db)

            val service = createUtbetalingService()

            service.upsertDelutbetaling(
                utbetaling1.id,
                DelutbetalingRequest(tilsagnId = tilsagn1.id, belop = 50),
                domain.ansatte[0].navIdent,
            )
            service.upsertDelutbetaling(
                utbetaling1.id,
                DelutbetalingRequest(tilsagnId = tilsagn2.id, belop = 50),
                domain.ansatte[0].navIdent,
            )

            service.upsertDelutbetaling(
                utbetaling2.id,
                DelutbetalingRequest(tilsagnId = tilsagn1.id, belop = 100),
                domain.ansatte[0].navIdent,
            )

            database.run {
                queries.delutbetaling.getByUtbetalingId(utbetaling1.id).should { (first, second) ->
                    first.belop shouldBe 50
                    first.periode shouldBe Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 15))
                    first.lopenummer shouldBe 1
                    first.fakturanummer shouldBe "2024/2/1"

                    second.belop shouldBe 50
                    second.periode shouldBe Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 15))
                    second.lopenummer shouldBe 1
                    second.fakturanummer shouldBe "2024/1/1"
                }

                queries.delutbetaling.getByUtbetalingId(utbetaling2.id).should { (first) ->
                    first.belop shouldBe 100
                    first.lopenummer shouldBe 2
                    first.fakturanummer shouldBe "2024/1/2"
                    first.periode shouldBe Periode(LocalDate.of(2024, 1, 15), LocalDate.of(2024, 2, 1))
                }
            }
        }
    }
})
