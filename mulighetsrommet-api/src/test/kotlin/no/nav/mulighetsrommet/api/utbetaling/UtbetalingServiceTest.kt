package no.nav.mulighetsrommet.api.utbetaling

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.mockk
import no.nav.mulighetsrommet.api.arrangorflate.GodkjennUtbetaling
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.fixtures.TilsagnFixtures.Tilsagn1
import no.nav.mulighetsrommet.api.fixtures.TilsagnFixtures.Tilsagn2
import no.nav.mulighetsrommet.api.fixtures.TilsagnFixtures.setTilsagnStatus
import no.nav.mulighetsrommet.api.fixtures.UtbetalingFixtures.delutbetaling1
import no.nav.mulighetsrommet.api.fixtures.UtbetalingFixtures.setDelutbetalingStatus
import no.nav.mulighetsrommet.api.fixtures.UtbetalingFixtures.utbetaling1
import no.nav.mulighetsrommet.api.fixtures.UtbetalingFixtures.utbetaling2
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.responses.ValidationError
import no.nav.mulighetsrommet.api.tilsagn.OkonomiBestillingService
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningFri
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.utbetaling.db.DeltakerDbo
import no.nav.mulighetsrommet.api.utbetaling.model.*
import no.nav.mulighetsrommet.api.utbetaling.task.JournalforUtbetaling
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.ktor.exception.BadRequest
import no.nav.mulighetsrommet.model.*
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
        journalforUtbetaling: JournalforUtbetaling = mockk(relaxed = true),
    ) = UtbetalingService(
        db = database.db,
        okonomi = okonomi,
        journalforUtbetaling = journalforUtbetaling,
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

        test("genererer en utbetaling med riktig periode, frist og sats som input") {
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
                stengt = setOf(),
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

        test("genererer en utbetaling med kontonummer og kid-nummer fra forrige godkjente utbetaling fra arrangør") {
            MulighetsrommetTestDomain(
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
                queries.utbetaling.setBetalingsinformasjon(
                    id = utbetaling.id,
                    kontonummer = Kontonummer("12345678901"),
                    kid = Kid("12345678901"),
                )
            }

            val sisteKrav = service.genererUtbetalingForMonth(LocalDate.of(2024, 2, 1)).first()
            sisteKrav.gjennomforing.id shouldBe AFT1.id
            sisteKrav.betalingsinformasjon.kontonummer shouldBe Kontonummer("12345678901")
            sisteKrav.betalingsinformasjon.kid shouldBe Kid("12345678901")
        }

        test("genererer en utbetaling med relevante deltakelse-perioder som input") {
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
                    DeltakerFixtures.createDeltaker(
                        AFT1.id,
                        startDato = LocalDate.of(2023, 1, 1),
                        sluttDato = LocalDate.of(2024, 12, 31),
                        statusType = DeltakerStatus.Type.DELTAR,
                        deltakelsesprosent = 10.0,
                        deltakelsesmengder = listOf(
                            DeltakerDbo.Deltakelsesmengde(
                                gyldigFra = LocalDate.of(2023, 1, 1),
                                deltakelsesprosent = 20.0,
                                opprettetTidspunkt = LocalDateTime.now(),
                            ),
                            DeltakerDbo.Deltakelsesmengde(
                                gyldigFra = LocalDate.of(2024, 1, 10),
                                deltakelsesprosent = 15.0,
                                opprettetTidspunkt = LocalDateTime.now(),
                            ),
                            DeltakerDbo.Deltakelsesmengde(
                                gyldigFra = LocalDate.of(2024, 1, 20),
                                deltakelsesprosent = 10.0,
                                opprettetTidspunkt = LocalDateTime.now(),
                            ),
                            DeltakerDbo.Deltakelsesmengde(
                                gyldigFra = LocalDate.of(2024, 2, 1),
                                deltakelsesprosent = 5.0,
                                opprettetTidspunkt = LocalDateTime.now(),
                            ),
                        ),
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
                    DeltakelsePerioder(
                        deltakelseId = domain.deltakere[5].id,
                        perioder = listOf(
                            DeltakelsePeriode(
                                start = LocalDate.of(2024, 1, 1),
                                slutt = LocalDate.of(2024, 1, 10),
                                deltakelsesprosent = 20.0,
                            ),
                            DeltakelsePeriode(
                                start = LocalDate.of(2024, 1, 10),
                                slutt = LocalDate.of(2024, 1, 20),
                                deltakelsesprosent = 15.0,
                            ),
                            DeltakelsePeriode(
                                start = LocalDate.of(2024, 1, 20),
                                slutt = LocalDate.of(2024, 2, 1),
                                deltakelsesprosent = 10.0,
                            ),
                        ),
                    ),
                )
            }
        }

        test("overstyrer deltakelse-perioder når det er stengt hos arrangør") {
            val domain = MulighetsrommetTestDomain(
                gjennomforinger = listOf(AFT1),
                deltakere = listOf(
                    DeltakerFixtures.createDeltaker(
                        AFT1.id,
                        startDato = LocalDate.of(2024, 1, 1),
                        sluttDato = LocalDate.of(2024, 2, 1),
                        statusType = DeltakerStatus.Type.DELTAR,
                        deltakelsesprosent = 10.0,
                        deltakelsesmengder = listOf(
                            DeltakerDbo.Deltakelsesmengde(
                                gyldigFra = LocalDate.of(2023, 1, 1),
                                deltakelsesprosent = 20.0,
                                opprettetTidspunkt = LocalDateTime.now(),
                            ),
                            DeltakerDbo.Deltakelsesmengde(
                                gyldigFra = LocalDate.of(2024, 1, 15),
                                deltakelsesprosent = 10.0,
                                opprettetTidspunkt = LocalDateTime.now(),
                            ),
                        ),
                    ),
                ),
            ) {
                queries.gjennomforing.setStengtHosArrangor(
                    AFT1.id,
                    Periode(LocalDate.of(2023, 12, 10), LocalDate.of(2024, 1, 10)),
                    "Ferie 1",
                )
                queries.gjennomforing.setStengtHosArrangor(
                    AFT1.id,
                    Periode(LocalDate.of(2024, 1, 20), LocalDate.of(2024, 2, 20)),
                    "Ferie 2",
                )
                queries.gjennomforing.setStengtHosArrangor(
                    AFT1.id,
                    Periode(LocalDate.of(2024, 2, 20), LocalDate.of(2024, 3, 20)),
                    "Fremtidig ferie",
                )
            }.initialize(database.db)

            val utbetaling = service.genererUtbetalingForMonth(LocalDate.of(2024, 1, 1)).first()

            utbetaling.beregning.input.shouldBeTypeOf<UtbetalingBeregningAft.Input>().should {
                it.stengt shouldBe setOf(
                    StengtPeriode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 10), "Ferie 1"),
                    StengtPeriode(LocalDate.of(2024, 1, 20), LocalDate.of(2024, 2, 1), "Ferie 2"),
                )
            }
        }

        test("genererer en utbetaling med beregnet belop basert på input") {
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
            val domain = MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.ansatt1, NavAnsattFixture.ansatt2),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
                utbetalinger = listOf(utbetaling1),
            ).initialize(database.db)

            val service = createUtbetalingService()

            val delutbetaling = DelutbetalingRequest(
                id = UUID.randomUUID(),
                tilsagnId = Tilsagn1.id,
                frigjorTilsagn = false,
                belop = 100,
            )

            val opprettRequest = OpprettDelutbetalingerRequest(
                utbetalingId = utbetaling1.id,
                delutbetalinger = listOf(delutbetaling),
            )

            service.opprettDelutbetalinger(
                request = opprettRequest,
                navIdent = domain.ansatte[0].navIdent,
            )

            service.besluttDelutbetaling(
                id = delutbetaling.id,
                request = BesluttDelutbetalingRequest.GodkjentDelutbetalingRequest,
                navIdent = domain.ansatte[1].navIdent,
            )

            service.opprettDelutbetalinger(
                request = opprettRequest,
                navIdent = domain.ansatte[0].navIdent,
            ).shouldBeLeft() shouldBe BadRequest("Utbetaling kan ikke endres")
        }

        test("skal ikke kunne godkjenne delutbetaling hvis den er godkjent") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.ansatt1, NavAnsattFixture.ansatt2),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
                utbetalinger = listOf(utbetaling1),
                delutbetalinger = listOf(delutbetaling1),
            ).initialize(database.db)

            val service = createUtbetalingService()

            service.besluttDelutbetaling(
                id = delutbetaling1.id,
                request = BesluttDelutbetalingRequest.GodkjentDelutbetalingRequest,
                navIdent = NavAnsattFixture.ansatt2.navIdent,
            )

            shouldThrow<IllegalArgumentException> {
                service.besluttDelutbetaling(
                    id = delutbetaling1.id,
                    request = BesluttDelutbetalingRequest.GodkjentDelutbetalingRequest,
                    navIdent = NavAnsattFixture.ansatt2.navIdent,
                )
            }.message shouldBe "Utbetaling er allerede besluttet"
        }

        test("oppdatering av delutbetaling etter returnert gir TIL_GODKJENNING status") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.ansatt1, NavAnsattFixture.ansatt2),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
                utbetalinger = listOf(utbetaling1),
                delutbetalinger = listOf(delutbetaling1),
            ) {
                setDelutbetalingStatus(
                    delutbetaling1,
                    UtbetalingFixtures.DelutbetalingStatus.RETURNERT,
                )
            }.initialize(database.db)

            val service = createUtbetalingService()
            service.opprettDelutbetalinger(
                request = OpprettDelutbetalingerRequest(
                    utbetalingId = utbetaling1.id,
                    delutbetalinger = listOf(
                        DelutbetalingRequest(
                            id = delutbetaling1.id,
                            tilsagnId = Tilsagn1.id,
                            frigjorTilsagn = false,
                            belop = 100,
                        ),
                    ),
                ),
                navIdent = NavAnsattFixture.ansatt1.navIdent,
            ).shouldBeRight()
            database.run { queries.delutbetaling.get(delutbetaling1.id) }.shouldNotBeNull()
                .shouldBeTypeOf<DelutbetalingDto.DelutbetalingTilGodkjenning>()
        }

        test("skal ikke kunne opprette delutbetaling hvis utbetalingsperiode og tilsagnsperiode ikke overlapper") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.ansatt1),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
                utbetalinger = listOf(
                    utbetaling1.copy(
                        periode = Periode.forMonthOf(
                            LocalDate.of(
                                2023,
                                4,
                                4,
                            ),
                        ),
                    ),
                ),
            ).initialize(database.db)

            val service = createUtbetalingService()

            val request = OpprettDelutbetalingerRequest(
                utbetalingId = utbetaling1.id,
                delutbetalinger = listOf(
                    DelutbetalingRequest(
                        id = UUID.randomUUID(),
                        tilsagnId = Tilsagn1.id,
                        frigjorTilsagn = false,
                        belop = 100,
                    ),
                ),
            )

            shouldThrow<IllegalArgumentException> {
                service.opprettDelutbetalinger(
                    request,
                    NavAnsattFixture.ansatt1.navIdent,
                )
            }.message shouldBe "Utbetalingsperiode og tilsagnsperiode overlapper ikke"
        }

        test("skal ikke kunne opprette delutbetaling hvis belop er for stort") {
            val tilsagn1 = Tilsagn1.copy(
                periode = Periode.forMonthOf(LocalDate.of(2024, 1, 1)),
            )
            val tilsagn2 = Tilsagn2.copy(
                periode = Periode.forMonthOf(LocalDate.of(2024, 1, 1)),
            )

            val utbetaling = utbetaling1.copy(
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

            service.opprettDelutbetalinger(
                OpprettDelutbetalingerRequest(
                    utbetaling.id,
                    listOf(DelutbetalingRequest(UUID.randomUUID(), tilsagn1.id, frigjorTilsagn = false, belop = 100)),
                ),
                domain.ansatte[0].navIdent,
            ).shouldBeLeft().shouldBeTypeOf<ValidationError>() should {
                it.errors shouldContainExactly listOf(FieldError("/belop", "Kan ikke betale ut mer enn det er krav på"))
            }

            service.opprettDelutbetalinger(
                OpprettDelutbetalingerRequest(
                    utbetaling.id,
                    listOf(
                        DelutbetalingRequest(UUID.randomUUID(), tilsagn1.id, frigjorTilsagn = false, belop = 7),
                    ),
                ),
                domain.ansatte[0].navIdent,
            ).shouldBeRight()

            // Siden 7 allerede er utbetalt nå
            service.opprettDelutbetalinger(
                OpprettDelutbetalingerRequest(
                    utbetaling.id,
                    listOf(
                        DelutbetalingRequest(UUID.randomUUID(), tilsagn2.id, frigjorTilsagn = false, belop = 5),
                    ),
                ),
                domain.ansatte[0].navIdent,
            ).shouldBeLeft().shouldBeTypeOf<ValidationError>() should {
                it.errors shouldContainExactly listOf(FieldError("/belop", "Kan ikke betale ut mer enn det er krav på"))
            }
        }

        test("løpenummer, fakturanummer og periode blir utledet fra tilsagnet og utbetalingen") {
            val tilsagn1 = Tilsagn2.copy(
                periode = Periode.forMonthOf(LocalDate.of(2024, 1, 1)),
                bestillingsnummer = "A-2024/1-1",
            )

            val tilsagn2 = Tilsagn1.copy(
                periode = Periode.forMonthOf(LocalDate.of(2024, 1, 1)),
                bestillingsnummer = "A-2024/1-2",
            )

            val utbetaling1 = utbetaling1.copy(
                periode = Periode(LocalDate.of(2023, 12, 15), LocalDate.of(2024, 1, 15)),
            )

            val utbetaling2 = utbetaling2.copy(
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

            service.opprettDelutbetalinger(
                OpprettDelutbetalingerRequest(
                    utbetaling1.id,
                    listOf(
                        DelutbetalingRequest(
                            id = UUID.randomUUID(),
                            tilsagnId = tilsagn1.id,
                            frigjorTilsagn = false,
                            belop = 50,
                        ),
                    ),
                ),
                domain.ansatte[0].navIdent,
            )
            service.opprettDelutbetalinger(
                OpprettDelutbetalingerRequest(
                    utbetaling1.id,
                    listOf(
                        DelutbetalingRequest(
                            id = UUID.randomUUID(),
                            tilsagnId = tilsagn2.id,
                            frigjorTilsagn = false,
                            belop = 50,
                        ),
                    ),
                ),
                domain.ansatte[0].navIdent,
            )

            service.opprettDelutbetalinger(
                OpprettDelutbetalingerRequest(
                    utbetaling2.id,
                    listOf(
                        DelutbetalingRequest(
                            id = UUID.randomUUID(),
                            tilsagnId = tilsagn1.id,
                            frigjorTilsagn = false,
                            belop = 100,
                        ),
                    ),
                ),
                domain.ansatte[0].navIdent,
            )

            database.run {
                queries.delutbetaling.getByUtbetalingId(utbetaling1.id).should { (first, second) ->
                    first.belop shouldBe 50
                    first.periode shouldBe Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 15))
                    first.lopenummer shouldBe 1
                    first.fakturanummer shouldBe "A-2024/1-2-1"

                    second.belop shouldBe 50
                    second.periode shouldBe Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 15))
                    second.lopenummer shouldBe 1
                    second.fakturanummer shouldBe "A-2024/1-1-1"
                }

                queries.delutbetaling.getByUtbetalingId(utbetaling2.id).should { (first) ->
                    first.belop shouldBe 100
                    first.lopenummer shouldBe 2
                    first.fakturanummer shouldBe "A-2024/1-1-2"
                    first.periode shouldBe Periode(LocalDate.of(2024, 1, 15), LocalDate.of(2024, 2, 1))
                }
            }
        }
    }

    context("Automatisk utbetaling") {
        val godkjennUtbetaling = GodkjennUtbetaling(
            betalingsinformasjon = GodkjennUtbetaling.Betalingsinformasjon(
                kontonummer = Kontonummer("12312312312"),
                kid = null,
            ),
            digest = "digest",
        )

        val utbetalingId = utbetaling1.id

        test("happy case") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.ansatt1, NavAnsattFixture.ansatt2),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
                utbetalinger = listOf(utbetaling1),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT)
            }.initialize(database.db)

            val service = createUtbetalingService()
            service.godkjentAvArrangor(utbetalingId, godkjennUtbetaling)

            val delutbetalinger = database.run { queries.delutbetaling.getByUtbetalingId(utbetalingId) }
            delutbetalinger.shouldHaveSize(1).first().should {
                it.shouldBeTypeOf<DelutbetalingDto.DelutbetalingOverfortTilUtbetaling>()
                it.belop shouldBe utbetaling1.beregning.output.belop
                it.opprettelse.behandletAv shouldBe Tiltaksadministrasjon
                it.opprettelse.besluttetAv shouldBe Tiltaksadministrasjon
            }
        }

        test("ingen automatisk utbetaling hvis tilsagn ikke er godkjent") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.ansatt1, NavAnsattFixture.ansatt2),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
                utbetalinger = listOf(utbetaling1),
            ).initialize(database.db)

            val service = createUtbetalingService()
            service.godkjentAvArrangor(utbetalingId, godkjennUtbetaling)

            val delutbetalinger = database.run { queries.delutbetaling.getByUtbetalingId(utbetalingId) }
            delutbetalinger shouldHaveSize 0
        }

        test("ingen automatisk utbetaling hvis ingen tilsagn") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.ansatt1, NavAnsattFixture.ansatt2),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                utbetalinger = listOf(utbetaling1),
            ).initialize(database.db)

            val service = createUtbetalingService()
            service.godkjentAvArrangor(utbetalingId, godkjennUtbetaling)

            val delutbetalinger = database.run { queries.delutbetaling.getByUtbetalingId(utbetalingId) }
            delutbetalinger shouldHaveSize 0
        }

        test("ingen automatisk utbetaling hvis flere tilsagn") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.ansatt1, NavAnsattFixture.ansatt2),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1, Tilsagn2.copy(periode = Tilsagn1.periode)),
                utbetalinger = listOf(utbetaling1),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT)
                setTilsagnStatus(Tilsagn2, TilsagnStatus.GODKJENT)
            }.initialize(database.db)

            val service = createUtbetalingService()
            service.godkjentAvArrangor(utbetalingId, godkjennUtbetaling)

            val delutbetalinger = database.run { queries.delutbetaling.getByUtbetalingId(utbetalingId) }
            delutbetalinger shouldHaveSize 0
        }

        test("ingen automatisk utbetaling hvis tilsagn ikke har nok penger") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.ansatt1, NavAnsattFixture.ansatt2),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(
                    Tilsagn1.copy(
                        beregning = TilsagnBeregningFri(
                            input = TilsagnBeregningFri.Input(belop = 1),
                            output = TilsagnBeregningFri.Output(belop = 1),
                        ),
                    ),
                ),
                utbetalinger = listOf(utbetaling1),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT)
            }.initialize(database.db)

            val service = createUtbetalingService()
            service.godkjentAvArrangor(utbetalingId, godkjennUtbetaling)

            val delutbetalinger = database.run { queries.delutbetaling.getByUtbetalingId(utbetalingId) }
            delutbetalinger shouldHaveSize 0
        }

        test("ingen automatisk utbetaling hvis feil tiltakskode") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.ansatt1, NavAnsattFixture.ansatt2),
                avtaler = listOf(AvtaleFixtures.gruppeAmo),
                gjennomforinger = listOf(GjennomforingFixtures.GruppeAmo1),
                tilsagn = listOf(Tilsagn1.copy(gjennomforingId = GjennomforingFixtures.GruppeAmo1.id)),
                utbetalinger = listOf(utbetaling1.copy(gjennomforingId = GjennomforingFixtures.GruppeAmo1.id)),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT)
            }.initialize(database.db)

            val service = createUtbetalingService()
            service.godkjentAvArrangor(utbetalingId, godkjennUtbetaling)

            val delutbetalinger = database.run { queries.delutbetaling.getByUtbetalingId(utbetalingId) }
            delutbetalinger shouldHaveSize 0
        }

        test("Tilsagn skal frigjøres automatisk når siste dato i tilsagnsperioden er inkludert i utbetalingsperioden") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.ansatt1, NavAnsattFixture.ansatt2),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(
                    Tilsagn1.copy(
                        periode = Periode(LocalDate.of(2025, 1, 4), LocalDate.of(2025, 3, 1)),
                    ),
                ),
                utbetalinger = listOf(
                    utbetaling1.copy(
                        periode = Periode.forMonthOf(LocalDate.of(2025, 1, 4)),
                    ),
                    utbetaling2.copy(
                        periode = Periode.forMonthOf(LocalDate.of(2025, 2, 4)),
                    ),
                ),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT)
            }.initialize(database.db)

            val service = createUtbetalingService()

            val utbetalingId1 = utbetaling1.id
            service.godkjentAvArrangor(utbetalingId1, godkjennUtbetaling)
            val delutbetaling1 = database.run { queries.delutbetaling.getByUtbetalingId(utbetalingId1) }
            delutbetaling1[0].frigjorTilsagn shouldBe false

            val utbetalingId2 = utbetaling2.id
            service.godkjentAvArrangor(utbetalingId2, godkjennUtbetaling)
            val delutbetaling2 = database.run { queries.delutbetaling.getByUtbetalingId(utbetalingId2) }
            delutbetaling2[0].frigjorTilsagn shouldBe true
        }
    }
})
