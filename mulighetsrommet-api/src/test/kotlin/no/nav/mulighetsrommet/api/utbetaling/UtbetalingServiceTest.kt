package no.nav.mulighetsrommet.api.utbetaling

import arrow.core.Either
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.clients.kontoregisterOrganisasjon.KontonummerResponse
import no.nav.mulighetsrommet.api.clients.kontoregisterOrganisasjon.KontoregisterOrganisasjonClient
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Innlandet
import no.nav.mulighetsrommet.api.fixtures.TilsagnFixtures.Tilsagn1
import no.nav.mulighetsrommet.api.fixtures.TilsagnFixtures.Tilsagn2
import no.nav.mulighetsrommet.api.fixtures.UtbetalingFixtures.delutbetaling1
import no.nav.mulighetsrommet.api.fixtures.UtbetalingFixtures.utbetaling1
import no.nav.mulighetsrommet.api.fixtures.UtbetalingFixtures.utbetaling2
import no.nav.mulighetsrommet.api.navansatt.db.NavAnsattDbo
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsattRolle
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.tilsagn.TilsagnService
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningFri
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Besluttelse
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
import no.nav.mulighetsrommet.api.utbetaling.api.BesluttDelutbetalingRequest
import no.nav.mulighetsrommet.api.utbetaling.api.DelutbetalingRequest
import no.nav.mulighetsrommet.api.utbetaling.api.OpprettDelutbetalingerRequest
import no.nav.mulighetsrommet.api.utbetaling.db.DeltakerDbo
import no.nav.mulighetsrommet.api.utbetaling.model.*
import no.nav.mulighetsrommet.api.utbetaling.task.JournalforUtbetaling
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.*
import no.nav.tiltak.okonomi.OkonomiBestillingMelding
import no.nav.tiltak.okonomi.toOkonomiPart
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class UtbetalingServiceTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))
    val kontoregisterOrganisasjonClient: KontoregisterOrganisasjonClient = mockk(relaxed = true)

    afterEach {
        database.truncateAll()
    }

    fun createUtbetalingService(
        tilsagnService: TilsagnService = mockk(relaxed = true),
        journalforUtbetaling: JournalforUtbetaling = mockk(relaxed = true),
    ) = UtbetalingService(
        config = UtbetalingService.Config(
            bestillingTopic = "topic",
        ),
        db = database.db,
        tilsagnService = tilsagnService,
        journalforUtbetaling = journalforUtbetaling,
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

    context("generering av utbetaling for AFT") {
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

        test("genererer en utbetaling med riktig periode, frist og sats som input") {
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
            utbetaling.beregning.input shouldBe UtbetalingBeregningForhandsgodkjent.Input(
                periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                sats = 20975,
                stengt = setOf(),
                deltakelser = setOf(
                    DeltakelsePerioder(
                        deltakelseId = domain.deltakere[0].id,
                        perioder = listOf(
                            DeltakelsePeriode(
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

            utbetaling.beregning.input.shouldBeTypeOf<UtbetalingBeregningForhandsgodkjent.Input>().should {
                it.deltakelser shouldBe setOf(
                    DeltakelsePerioder(
                        deltakelseId = domain.deltakere[0].id,
                        perioder = listOf(
                            DeltakelsePeriode(
                                periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 2, 1)),
                                deltakelsesprosent = 100.0,
                            ),
                        ),
                    ),
                    DeltakelsePerioder(
                        deltakelseId = domain.deltakere[1].id,
                        perioder = listOf(
                            DeltakelsePeriode(
                                periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 16)),
                                deltakelsesprosent = 40.0,
                            ),
                        ),
                    ),
                    DeltakelsePerioder(
                        deltakelseId = domain.deltakere[2].id,
                        perioder = listOf(
                            DeltakelsePeriode(
                                periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 2, 1)),
                                deltakelsesprosent = 50.0,
                            ),
                        ),
                    ),
                    DeltakelsePerioder(
                        deltakelseId = domain.deltakere[5].id,
                        perioder = listOf(
                            DeltakelsePeriode(
                                periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 10)),
                                deltakelsesprosent = 20.0,
                            ),
                            DeltakelsePeriode(
                                periode = Periode(LocalDate.of(2025, 1, 10), LocalDate.of(2025, 1, 20)),
                                deltakelsesprosent = 15.0,
                            ),
                            DeltakelsePeriode(
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

            utbetaling.beregning.input.shouldBeTypeOf<UtbetalingBeregningForhandsgodkjent.Input>().should {
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

            utbetaling.beregning.output.shouldBeTypeOf<UtbetalingBeregningForhandsgodkjent.Output>().should {
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

            utbetaling.beregning.input.shouldBeTypeOf<UtbetalingBeregningForhandsgodkjent.Input>().should {
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
                        startDato = LocalDate.of(2025, 6, 1),
                        sluttDato = LocalDate.of(2025, 6, 30),
                        statusType = DeltakerStatus.Type.DELTAR,
                        deltakelsesprosent = 100.0,
                    ),
                ),
            ).initialize(database.db)

            val utbetalingId = UUID.randomUUID()

            database.run {
                val utbetaling = service.createUtbetalingForhandsgodkjent(
                    utbetalingId = utbetalingId,
                    gjennomforingId = AFT1.id,
                    periode = Periode.forMonthOf(LocalDate.of(2025, 6, 1)),
                )
                queries.utbetaling.upsert(utbetaling)
                utbetaling.beregning.output.shouldBeTypeOf<UtbetalingBeregningForhandsgodkjent.Output>().belop shouldBe 20975

                val updatedDeltaker = domain.deltakere[0].copy(
                    sluttDato = LocalDate.of(2025, 6, 15),
                )
                queries.deltaker.upsert(updatedDeltaker)
            }

            service.oppdaterUtbetalingBeregningForGjennomforing(AFT1.id)

            database.run {
                val utbetaling = queries.utbetaling.get(utbetalingId).shouldNotBeNull()
                utbetaling.beregning.output.shouldBeTypeOf<UtbetalingBeregningForhandsgodkjent.Output>().should {
                    it.belop shouldBe 10488
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
                        startDato = LocalDate.of(2025, 6, 1),
                        sluttDato = LocalDate.of(2025, 6, 30),
                        statusType = DeltakerStatus.Type.DELTAR,
                        deltakelsesprosent = 100.0,
                    ),
                ),
            ).initialize(database.db)

            val utbetalingId = UUID.randomUUID()

            database.run {
                val utbetaling = service.createUtbetalingForhandsgodkjent(
                    utbetalingId = utbetalingId,
                    gjennomforingId = AFT1.id,
                    periode = Periode.forMonthOf(LocalDate.of(2025, 6, 1)),
                )
                queries.utbetaling.upsert(utbetaling)
                utbetaling.beregning.output.shouldBeTypeOf<UtbetalingBeregningForhandsgodkjent.Output>().belop shouldBe 20975

                val updatedDeltaker = domain.deltakere[0].copy(
                    sluttDato = LocalDate.of(2025, 6, 15),
                )
                queries.deltaker.upsert(updatedDeltaker)

                queries.utbetaling.setGodkjentAvArrangor(utbetalingId, LocalDateTime.now())
            }

            service.oppdaterUtbetalingBeregningForGjennomforing(AFT1.id)

            database.run {
                val utbetaling = queries.utbetaling.get(utbetalingId).shouldNotBeNull()
                utbetaling.beregning.output.shouldBeTypeOf<UtbetalingBeregningForhandsgodkjent.Output>().should {
                    it.belop shouldBe 20975
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
        test("skal ikke kunne beslutte delutbetaling når ansatt mangler attestant-rolle") {
            val domain = MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
                utbetalinger = listOf(utbetaling1),
                delutbetalinger = listOf(delutbetaling1),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT)
                setDelutbetalingStatus(delutbetaling1, DelutbetalingStatus.TIL_ATTESTERING)
            }.initialize(database.db)

            val service = createUtbetalingService()

            service.besluttDelutbetaling(
                id = delutbetaling1.id,
                request = BesluttDelutbetalingRequest.GodkjentDelutbetalingRequest,
                navIdent = domain.ansatte[1].navIdent,
            ) shouldBeLeft listOf(
                FieldError.of("Kan ikke attestere utbetalingen fordi du ikke er attstant ved tilsagnets kostnadssted (Nav Innlandet)"),
            )
        }

        test("skal ikke kunne opprette delutbetaling hvis utbetalingen allerede er godkjent") {
            val domain = MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
                utbetalinger = listOf(utbetaling1),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT)
                setRoller(
                    NavAnsattFixture.MikkeMus,
                    setOf(NavAnsattRolle.kontorspesifikk(Rolle.ATTESTANT_UTBETALING, setOf(Innlandet.enhetsnummer))),
                )
            }.initialize(database.db)

            val service = createUtbetalingService()

            val delutbetaling = DelutbetalingRequest(
                id = UUID.randomUUID(),
                tilsagnId = Tilsagn1.id,
                gjorOppTilsagn = false,
                belop = 100,
            )

            val opprettRequest = OpprettDelutbetalingerRequest(
                utbetalingId = utbetaling1.id,
                delutbetalinger = listOf(delutbetaling),
            )

            service.opprettDelutbetalinger(
                request = opprettRequest,
                navIdent = domain.ansatte[0].navIdent,
            ).shouldBeRight()

            service.besluttDelutbetaling(
                id = delutbetaling.id,
                request = BesluttDelutbetalingRequest.GodkjentDelutbetalingRequest,
                navIdent = domain.ansatte[1].navIdent,
            ).shouldBeRight().status shouldBe DelutbetalingStatus.OVERFORT_TIL_UTBETALING

            service.opprettDelutbetalinger(
                request = opprettRequest,
                navIdent = domain.ansatte[0].navIdent,
            ) shouldBeLeft listOf(
                FieldError("/0", "Utbetaling kan ikke endres fordi den har status: OVERFORT_TIL_UTBETALING"),
            )
        }

        test("returnering av delutbetaling setter den i RETURNERT status") {
            val domain = MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
                utbetalinger = listOf(utbetaling1),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT)
                setRoller(
                    NavAnsattFixture.DonaldDuck,
                    setOf(NavAnsattRolle.kontorspesifikk(Rolle.ATTESTANT_UTBETALING, setOf(Innlandet.enhetsnummer))),
                )
            }.initialize(database.db)

            val service = createUtbetalingService()
            val delutbetaling = DelutbetalingRequest(
                id = UUID.randomUUID(),
                tilsagnId = Tilsagn1.id,
                gjorOppTilsagn = false,
                belop = 100,
            )
            val opprettRequest = OpprettDelutbetalingerRequest(
                utbetalingId = utbetaling1.id,
                delutbetalinger = listOf(delutbetaling),
            )
            service.opprettDelutbetalinger(
                request = opprettRequest,
                navIdent = domain.ansatte[1].navIdent,
            ).shouldBeRight()
            service.besluttDelutbetaling(
                id = delutbetaling.id,
                request = BesluttDelutbetalingRequest.AvvistDelutbetalingRequest(
                    aarsaker = emptyList(),
                    forklaring = null,
                ),
                navIdent = domain.ansatte[0].navIdent,
            ).shouldBeRight().status shouldBe DelutbetalingStatus.RETURNERT
        }

        test("sletting av delutbetaling skjer ikke ved valideringsfeil") {
            val domain = MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
                utbetalinger = listOf(utbetaling1),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT)
                setRoller(
                    NavAnsattFixture.MikkeMus,
                    setOf(NavAnsattRolle.kontorspesifikk(Rolle.ATTESTANT_UTBETALING, setOf(Innlandet.enhetsnummer))),
                )
            }.initialize(database.db)

            val service = createUtbetalingService()
            val delutbetaling = DelutbetalingRequest(
                id = UUID.randomUUID(),
                tilsagnId = Tilsagn1.id,
                gjorOppTilsagn = false,
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
                request = BesluttDelutbetalingRequest.AvvistDelutbetalingRequest(
                    aarsaker = emptyList(),
                    forklaring = null,
                ),
                navIdent = domain.ansatte[1].navIdent,
            ).shouldBeRight().status shouldBe DelutbetalingStatus.RETURNERT

            service.opprettDelutbetalinger(
                request = OpprettDelutbetalingerRequest(utbetaling1.id, emptyList()),
                navIdent = domain.ansatte[0].navIdent,
            ).shouldBeLeft()
        }

        test("skal ikke kunne godkjenne delutbetaling hvis den er godkjent") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
                utbetalinger = listOf(utbetaling1),
                delutbetalinger = listOf(delutbetaling1),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT)
                setDelutbetalingStatus(delutbetaling1, DelutbetalingStatus.GODKJENT)
            }.initialize(database.db)

            val service = createUtbetalingService()

            shouldThrow<IllegalArgumentException> {
                service.besluttDelutbetaling(
                    id = delutbetaling1.id,
                    request = BesluttDelutbetalingRequest.GodkjentDelutbetalingRequest,
                    navIdent = NavAnsattFixture.MikkeMus.navIdent,
                )
            }.message shouldBe "Utbetaling er allerede besluttet"
        }

        test("oppdatering av delutbetaling etter returnert gir TIL_GODKJENNING status") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
                utbetalinger = listOf(utbetaling1),
                delutbetalinger = listOf(delutbetaling1),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT)
                setDelutbetalingStatus(delutbetaling1, DelutbetalingStatus.RETURNERT)
            }.initialize(database.db)

            val service = createUtbetalingService()
            service.opprettDelutbetalinger(
                request = OpprettDelutbetalingerRequest(
                    utbetalingId = utbetaling1.id,
                    delutbetalinger = listOf(
                        DelutbetalingRequest(
                            id = delutbetaling1.id,
                            tilsagnId = Tilsagn1.id,
                            gjorOppTilsagn = false,
                            belop = 100,
                        ),
                    ),
                ),
                navIdent = NavAnsattFixture.DonaldDuck.navIdent,
            ).shouldBeRight()

            database.run {
                queries.delutbetaling.get(delutbetaling1.id).shouldNotBeNull()
                    .status shouldBe DelutbetalingStatus.TIL_ATTESTERING
            }
        }

        test("skal ikke kunne opprette delutbetaling hvis utbetalingsperiode og tilsagnsperiode ikke overlapper") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
                utbetalinger = listOf(
                    utbetaling1.copy(
                        periode = Periode.forMonthOf(LocalDate.of(2023, 4, 4)),
                    ),
                ),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT)
            }.initialize(database.db)

            val service = createUtbetalingService()

            val request = OpprettDelutbetalingerRequest(
                utbetalingId = utbetaling1.id,
                delutbetalinger = listOf(
                    DelutbetalingRequest(
                        id = UUID.randomUUID(),
                        tilsagnId = Tilsagn1.id,
                        gjorOppTilsagn = false,
                        belop = 100,
                    ),
                ),
            )

            shouldThrow<IllegalArgumentException> {
                service.opprettDelutbetalinger(
                    request,
                    NavAnsattFixture.DonaldDuck.navIdent,
                )
            }.message shouldBe "Utbetalingsperiode og tilsagnsperiode overlapper ikke"
        }

        test("skal ikke kunne opprette delutbetaling hvis belop er for stort") {
            val tilsagn1 = Tilsagn1.copy(
                periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
            )
            val tilsagn2 = Tilsagn2.copy(
                periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
            )

            val utbetaling = utbetaling1.copy(
                periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                beregning = UtbetalingBeregningFri(
                    input = UtbetalingBeregningFri.Input(10),
                    output = UtbetalingBeregningFri.Output(10),
                ),
            )

            val domain = MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(tilsagn1, tilsagn2),
                utbetalinger = listOf(utbetaling),
            ) {
                setTilsagnStatus(tilsagn1, TilsagnStatus.GODKJENT)
                setTilsagnStatus(tilsagn2, TilsagnStatus.GODKJENT)
            }.initialize(database.db)
            val service = createUtbetalingService()

            service.opprettDelutbetalinger(
                OpprettDelutbetalingerRequest(
                    utbetaling.id,
                    listOf(DelutbetalingRequest(UUID.randomUUID(), tilsagn1.id, gjorOppTilsagn = false, belop = 100)),
                ),
                domain.ansatte[0].navIdent,
            ) shouldBeLeft listOf(
                FieldError.of("Kan ikke utbetale mer enn innsendt beløp"),
            )

            service.opprettDelutbetalinger(
                OpprettDelutbetalingerRequest(
                    utbetaling.id,
                    listOf(
                        DelutbetalingRequest(UUID.randomUUID(), tilsagn1.id, gjorOppTilsagn = false, belop = 7),
                        DelutbetalingRequest(UUID.randomUUID(), tilsagn2.id, gjorOppTilsagn = false, belop = 5),
                    ),
                ),
                domain.ansatte[0].navIdent,
            ) shouldBeLeft listOf(
                FieldError.of("Kan ikke utbetale mer enn innsendt beløp"),
            )
        }

        test("ny send til godkjenning sletter rader som ikke er med") {
            val tilsagn1 = Tilsagn1.copy(
                periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
            )
            val tilsagn2 = Tilsagn2.copy(
                periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
            )
            val utbetaling = utbetaling1.copy(
                periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                beregning = UtbetalingBeregningFri(
                    input = UtbetalingBeregningFri.Input(10),
                    output = UtbetalingBeregningFri.Output(10),
                ),
            )

            val domain = MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(tilsagn1, tilsagn2),
                utbetalinger = listOf(utbetaling),
            ) {
                setTilsagnStatus(tilsagn1, TilsagnStatus.GODKJENT)
                setTilsagnStatus(tilsagn2, TilsagnStatus.GODKJENT)
                setRoller(
                    NavAnsattFixture.MikkeMus,
                    setOf(NavAnsattRolle.kontorspesifikk(Rolle.ATTESTANT_UTBETALING, setOf(Innlandet.enhetsnummer))),
                )
            }.initialize(database.db)
            val service = createUtbetalingService()

            val delutbetalingId1 = UUID.randomUUID()
            val delutbetalingId2 = UUID.randomUUID()
            service.opprettDelutbetalinger(
                OpprettDelutbetalingerRequest(
                    utbetaling.id,
                    listOf(
                        DelutbetalingRequest(delutbetalingId1, tilsagn1.id, gjorOppTilsagn = false, belop = 5),
                        DelutbetalingRequest(delutbetalingId2, tilsagn2.id, gjorOppTilsagn = false, belop = 5),
                    ),
                ),
                domain.ansatte[0].navIdent,
            ).shouldBeRight()

            service.besluttDelutbetaling(
                delutbetalingId2,
                BesluttDelutbetalingRequest.AvvistDelutbetalingRequest(
                    aarsaker = emptyList(),
                    forklaring = null,
                ),
                domain.ansatte[1].navIdent,
            ).shouldBeRight().status shouldBe DelutbetalingStatus.RETURNERT

            service.opprettDelutbetalinger(
                OpprettDelutbetalingerRequest(
                    utbetaling.id,
                    listOf(DelutbetalingRequest(delutbetalingId1, tilsagn1.id, gjorOppTilsagn = false, belop = 5)),
                ),
                domain.ansatte[0].navIdent,
            ).shouldBeRight()

            val delutbetalinger = database.run { queries.delutbetaling.getByUtbetalingId(utbetaling.id) }
            delutbetalinger.size shouldBe 1
            delutbetalinger[0].id shouldBe delutbetalingId1
        }

        test("ny send til godkjenning med de to samme tilsagn men nye id'er fungerer") {
            val tilsagn1 = Tilsagn1.copy(
                periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
            )
            val tilsagn2 = Tilsagn2.copy(
                periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
            )
            val utbetaling = utbetaling1.copy(
                periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                beregning = UtbetalingBeregningFri(
                    input = UtbetalingBeregningFri.Input(10),
                    output = UtbetalingBeregningFri.Output(10),
                ),
            )

            val domain = MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(tilsagn1, tilsagn2),
                utbetalinger = listOf(utbetaling),
            ) {
                setTilsagnStatus(tilsagn1, TilsagnStatus.GODKJENT)
                setTilsagnStatus(tilsagn2, TilsagnStatus.GODKJENT)
                setRoller(
                    NavAnsattFixture.MikkeMus,
                    setOf(NavAnsattRolle.kontorspesifikk(Rolle.ATTESTANT_UTBETALING, setOf(Innlandet.enhetsnummer))),
                )
            }.initialize(database.db)
            val service = createUtbetalingService()

            val delutbetalingId1 = UUID.randomUUID()
            val delutbetalingId2 = UUID.randomUUID()
            service.opprettDelutbetalinger(
                OpprettDelutbetalingerRequest(
                    utbetaling.id,
                    listOf(
                        DelutbetalingRequest(delutbetalingId1, tilsagn1.id, gjorOppTilsagn = false, belop = 5),
                        DelutbetalingRequest(delutbetalingId2, tilsagn2.id, gjorOppTilsagn = false, belop = 5),
                    ),
                ),
                domain.ansatte[0].navIdent,
            ).shouldBeRight()

            service.besluttDelutbetaling(
                delutbetalingId2,
                BesluttDelutbetalingRequest.AvvistDelutbetalingRequest(
                    aarsaker = emptyList(),
                    forklaring = null,
                ),
                domain.ansatte[1].navIdent,
            ).shouldBeRight().status shouldBe DelutbetalingStatus.RETURNERT

            service.opprettDelutbetalinger(
                OpprettDelutbetalingerRequest(
                    utbetaling.id,
                    listOf(
                        DelutbetalingRequest(UUID.randomUUID(), tilsagn1.id, gjorOppTilsagn = false, belop = 5),
                        DelutbetalingRequest(UUID.randomUUID(), tilsagn2.id, gjorOppTilsagn = false, belop = 5),
                    ),
                ),
                domain.ansatte[0].navIdent,
            ).shouldBeRight()

            val delutbetalinger = database.run { queries.delutbetaling.getByUtbetalingId(utbetaling.id) }
            delutbetalinger.size shouldBe 2
        }

        test("returner returnerer alle delutbetalinger (selv godkjente)") {
            val tilsagn1 = Tilsagn1.copy(
                periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
            )
            val tilsagn2 = Tilsagn2.copy(
                periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
            )
            val utbetaling = utbetaling1.copy(
                periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                beregning = UtbetalingBeregningFri(
                    input = UtbetalingBeregningFri.Input(10),
                    output = UtbetalingBeregningFri.Output(10),
                ),
            )

            val domain = MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(tilsagn1, tilsagn2),
                utbetalinger = listOf(utbetaling),
            ) {
                setTilsagnStatus(tilsagn1, TilsagnStatus.GODKJENT)
                setTilsagnStatus(tilsagn2, TilsagnStatus.GODKJENT)
                setRoller(
                    NavAnsattFixture.DonaldDuck,
                    setOf(NavAnsattRolle.kontorspesifikk(Rolle.ATTESTANT_UTBETALING, setOf(Innlandet.enhetsnummer))),
                )
            }.initialize(database.db)
            val service = createUtbetalingService()

            val delutbetalingId1 = UUID.randomUUID()
            val delutbetalingId2 = UUID.randomUUID()
            service.opprettDelutbetalinger(
                OpprettDelutbetalingerRequest(
                    utbetaling.id,
                    listOf(
                        DelutbetalingRequest(delutbetalingId1, tilsagn1.id, gjorOppTilsagn = false, belop = 5),
                        DelutbetalingRequest(delutbetalingId2, tilsagn2.id, gjorOppTilsagn = false, belop = 5),
                    ),
                ),
                domain.ansatte[1].navIdent,
            ).shouldBeRight()

            service.besluttDelutbetaling(
                delutbetalingId1,
                BesluttDelutbetalingRequest.GodkjentDelutbetalingRequest,
                domain.ansatte[0].navIdent,
            ).shouldBeRight().status shouldBe DelutbetalingStatus.GODKJENT

            service.besluttDelutbetaling(
                delutbetalingId2,
                BesluttDelutbetalingRequest.AvvistDelutbetalingRequest(
                    aarsaker = emptyList(),
                    forklaring = null,
                ),
                domain.ansatte[0].navIdent,
            ).shouldBeRight().status shouldBe DelutbetalingStatus.RETURNERT

            database.run {
                queries.totrinnskontroll.getOrError(
                    delutbetalingId1,
                    Totrinnskontroll.Type.OPPRETT,
                )
            }.besluttetAv shouldBe Tiltaksadministrasjon
        }

        test("løpenummer, fakturanummer og periode blir utledet fra tilsagnet og utbetalingen") {
            val tilsagn1 = Tilsagn2.copy(
                periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                bestillingsnummer = "A-2025/1-1",
            )

            val tilsagn2 = Tilsagn1.copy(
                periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                bestillingsnummer = "A-2025/1-2",
            )

            val utbetaling1 = utbetaling1.copy(
                periode = Periode(LocalDate.of(2023, 12, 15), LocalDate.of(2025, 1, 15)),
            )

            val utbetaling2 = utbetaling2.copy(
                periode = Periode(LocalDate.of(2025, 1, 15), LocalDate.of(2025, 2, 15)),
            )

            val domain = MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(tilsagn1, tilsagn2),
                utbetalinger = listOf(utbetaling1, utbetaling2),
            ) {
                setTilsagnStatus(tilsagn1, TilsagnStatus.GODKJENT)
                setTilsagnStatus(tilsagn2, TilsagnStatus.GODKJENT)
            }.initialize(database.db)

            val service = createUtbetalingService()

            service.opprettDelutbetalinger(
                OpprettDelutbetalingerRequest(
                    utbetaling1.id,
                    listOf(
                        DelutbetalingRequest(
                            id = UUID.randomUUID(),
                            tilsagnId = tilsagn1.id,
                            gjorOppTilsagn = false,
                            belop = 50,
                        ),
                        DelutbetalingRequest(
                            id = UUID.randomUUID(),
                            tilsagnId = tilsagn2.id,
                            gjorOppTilsagn = false,
                            belop = 50,
                        ),
                    ),
                ),
                domain.ansatte[0].navIdent,
            ).shouldBeRight()

            service.opprettDelutbetalinger(
                OpprettDelutbetalingerRequest(
                    utbetaling2.id,
                    listOf(
                        DelutbetalingRequest(
                            id = UUID.randomUUID(),
                            tilsagnId = tilsagn1.id,
                            gjorOppTilsagn = false,
                            belop = 100,
                        ),
                    ),
                ),
                domain.ansatte[0].navIdent,
            ).shouldBeRight()

            database.run {
                queries.delutbetaling.getByUtbetalingId(utbetaling1.id).should { (first, second) ->
                    second.belop shouldBe 50
                    second.periode shouldBe Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 15))
                    second.lopenummer shouldBe 1
                    second.faktura.fakturanummer shouldBe "A-2025/1-2-1"

                    first.belop shouldBe 50
                    first.periode shouldBe Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 15))
                    first.lopenummer shouldBe 1
                    first.faktura.fakturanummer shouldBe "A-2025/1-1-1"
                }

                queries.delutbetaling.getByUtbetalingId(utbetaling2.id).should { (first) ->
                    first.belop shouldBe 100
                    first.lopenummer shouldBe 2
                    first.faktura.fakturanummer shouldBe "A-2025/1-1-2"
                    first.periode shouldBe Periode(LocalDate.of(2025, 1, 15), LocalDate.of(2025, 2, 1))
                }
            }
        }

        test("delutbetaling blir returnert hvis tilsagn har endret status") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
                utbetalinger = listOf(utbetaling1),
                delutbetalinger = listOf(delutbetaling1),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.OPPGJORT)
                setDelutbetalingStatus(delutbetaling1, DelutbetalingStatus.TIL_ATTESTERING)
                setRoller(
                    NavAnsattFixture.MikkeMus,
                    setOf(NavAnsattRolle.kontorspesifikk(Rolle.ATTESTANT_UTBETALING, setOf(Innlandet.enhetsnummer))),
                )
            }.initialize(database.db)

            val service = createUtbetalingService()

            service.besluttDelutbetaling(
                id = delutbetaling1.id,
                request = BesluttDelutbetalingRequest.GodkjentDelutbetalingRequest,
                navIdent = NavAnsattFixture.MikkeMus.navIdent,
            ).shouldBeRight().status shouldBe DelutbetalingStatus.RETURNERT

            database.run {
                val opprettelse = queries.totrinnskontroll.getOrError(delutbetaling1.id, Totrinnskontroll.Type.OPPRETT)
                opprettelse.besluttetAv shouldBe Tiltaksadministrasjon
                opprettelse.besluttelse shouldBe Besluttelse.AVVIST
            }
        }
    }

    context("Automatisk utbetaling") {
        val utbetaling1Id = utbetaling1.id

        val utbetaling1Forhandsgodkjent = utbetaling1.copy(
            periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
            beregning = getForhandsgodkjentBeregning(
                periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                belop = 1000,
            ),
        )

        test("utbetales automatisk når det finnes et enkelt tilsagn med nok midler og det er godkjent") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(
                    Tilsagn1.copy(
                        beregning = TilsagnBeregningFri(
                            input = TilsagnBeregningFri.Input(
                                linjer = listOf(
                                    TilsagnBeregningFri.InputLinje(
                                        id = UUID.randomUUID(),
                                        beskrivelse = "Beskrivelse",
                                        belop = 1500,
                                        antall = 1,
                                    ),
                                ),
                                prisbetingelser = null,
                            ),
                            output = TilsagnBeregningFri.Output(1000),
                        ),
                    ),
                ),
                utbetalinger = listOf(utbetaling1Forhandsgodkjent),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT)
            }.initialize(database.db)

            val service = createUtbetalingService()
            service.godkjentAvArrangor(utbetaling1Id, kid = null)

            database.run {
                val delutbetaling = queries.delutbetaling.getByUtbetalingId(utbetaling1Id).shouldHaveSize(1).first()
                delutbetaling.status shouldBe DelutbetalingStatus.OVERFORT_TIL_UTBETALING
                delutbetaling.belop shouldBe 1000

                queries.totrinnskontroll.getOrError(delutbetaling.id, Totrinnskontroll.Type.OPPRETT).should {
                    it.behandletAv shouldBe Tiltaksadministrasjon
                    it.besluttetAv shouldBe Tiltaksadministrasjon
                }

                queries.tilsagn.get(Tilsagn1.id).shouldNotBeNull().should {
                    it.belopBrukt shouldBe it.beregning.output.belop
                }
                Json.decodeFromString<OkonomiBestillingMelding>(
                    queries.kafkaProducerRecord.getRecords(50).first().value.decodeToString(),
                )
                    .shouldBeTypeOf<OkonomiBestillingMelding.Faktura>()
                    .payload.should {
                        it.belop shouldBe delutbetaling.belop
                        it.behandletAv shouldBe Tiltaksadministrasjon.toOkonomiPart()
                        it.besluttetAv shouldBe Tiltaksadministrasjon.toOkonomiPart()
                        it.periode shouldBe delutbetaling.periode
                        it.beskrivelse shouldBe """
                            Tiltakstype: Arbeidsforberedende trening (AFT)
                            Periode: 01.01.2025 - 31.01.2025
                            Tilsagnsnummer: A-2025/1-1
                        """.trimIndent()
                    }
            }
        }

        test("ingen automatisk utbetaling hvis tilsagn ikke er godkjent") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
                utbetalinger = listOf(utbetaling1Forhandsgodkjent),
            ).initialize(database.db)

            val service = createUtbetalingService()
            service.godkjentAvArrangor(utbetaling1Id, kid = null)

            database.run {
                queries.delutbetaling.getByUtbetalingId(utbetaling1Id).shouldBeEmpty()
            }
        }

        test("ingen automatisk utbetaling hvis ingen tilsagn") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                utbetalinger = listOf(utbetaling1Forhandsgodkjent),
            ).initialize(database.db)

            val service = createUtbetalingService()
            service.godkjentAvArrangor(utbetaling1Id, kid = null)

            database.run {
                queries.delutbetaling.getByUtbetalingId(utbetaling1Id).shouldBeEmpty()
            }
        }

        test("ingen automatisk utbetaling hvis flere tilsagn") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1, Tilsagn2.copy(periode = Tilsagn1.periode)),
                utbetalinger = listOf(utbetaling1Forhandsgodkjent),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT)
                setTilsagnStatus(Tilsagn2, TilsagnStatus.GODKJENT)
            }.initialize(database.db)

            val service = createUtbetalingService()
            service.godkjentAvArrangor(utbetaling1Id, kid = null)

            database.run {
                queries.delutbetaling.getByUtbetalingId(utbetaling1Id).shouldBeEmpty()
            }
        }

        test("ingen automatisk utbetaling hvis tilsagn ikke har nok penger") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(
                    Tilsagn1.copy(
                        beregning = TilsagnBeregningFri(
                            input = TilsagnBeregningFri.Input(
                                linjer = listOf(
                                    TilsagnBeregningFri.InputLinje(
                                        id = UUID.randomUUID(),
                                        beskrivelse = "Beskrivelse",
                                        belop = 1500,
                                        antall = 1,
                                    ),
                                ),
                                prisbetingelser = null,
                            ),
                            output = TilsagnBeregningFri.Output(belop = 1),
                        ),
                    ),
                ),
                utbetalinger = listOf(utbetaling1Forhandsgodkjent),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT)
            }.initialize(database.db)

            val service = createUtbetalingService()
            service.godkjentAvArrangor(utbetaling1Id, kid = null)

            database.run {
                queries.delutbetaling.getByUtbetalingId(utbetaling1Id).shouldBeEmpty()
            }
        }

        test("ingen automatisk utbetaling når prismodell er fri") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
                utbetalinger = listOf(
                    utbetaling1Forhandsgodkjent.copy(
                        beregning = UtbetalingBeregningFri(
                            input = UtbetalingBeregningFri.Input(1),
                            output = UtbetalingBeregningFri.Output(1),
                        ),
                    ),
                ),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT)
            }.initialize(database.db)

            val service = createUtbetalingService()
            service.godkjentAvArrangor(utbetaling1Id, kid = null)

            database.run {
                queries.delutbetaling.getByUtbetalingId(utbetaling1Id).shouldBeEmpty()
            }
        }

        test("Tilsagn skal oppgjøres automatisk når siste dato i tilsagnsperioden er inkludert i utbetalingsperioden") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(
                    Tilsagn1.copy(
                        periode = Periode(LocalDate.of(2025, 1, 4), LocalDate.of(2025, 3, 1)),
                    ),
                ),
                utbetalinger = listOf(
                    utbetaling1Forhandsgodkjent.copy(
                        periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                        beregning = getForhandsgodkjentBeregning(
                            periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                            belop = 100,
                        ),
                    ),
                    utbetaling2.copy(
                        periode = Periode.forMonthOf(LocalDate.of(2025, 2, 1)),
                        beregning = getForhandsgodkjentBeregning(
                            periode = Periode.forMonthOf(LocalDate.of(2025, 2, 1)),
                            belop = 100,
                        ),
                    ),
                ),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT)
            }.initialize(database.db)

            val tilsagnService: TilsagnService = mockk(relaxed = true)
            val service = createUtbetalingService(tilsagnService = tilsagnService)

            service.godkjentAvArrangor(utbetaling1Id, kid = null)
            database.run {
                queries.delutbetaling.getByUtbetalingId(utbetaling1Id).first().should {
                    it.status shouldBe DelutbetalingStatus.OVERFORT_TIL_UTBETALING
                    it.gjorOppTilsagn shouldBe false
                }
            }

            service.godkjentAvArrangor(utbetaling2.id, kid = null)
            database.run {
                queries.delutbetaling.getByUtbetalingId(utbetaling2.id).first().should {
                    it.status shouldBe DelutbetalingStatus.OVERFORT_TIL_UTBETALING
                    it.gjorOppTilsagn shouldBe true
                }
            }

            verify(exactly = 1) {
                tilsagnService.gjorOppAutomatisk(Tilsagn1.id, any())
            }
        }
    }
    context("totrinnskontroll regler") {
        test("kan ikke beslutte egen utbetaling") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
                utbetalinger = listOf(utbetaling1),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT)
                setRoller(
                    NavAnsattFixture.DonaldDuck,
                    setOf(NavAnsattRolle.kontorspesifikk(Rolle.ATTESTANT_UTBETALING, setOf(Innlandet.enhetsnummer))),
                )
            }.initialize(database.db)

            val service = createUtbetalingService()
            val delutbetaling = DelutbetalingRequest(
                id = UUID.randomUUID(),
                tilsagnId = Tilsagn1.id,
                gjorOppTilsagn = false,
                belop = 100,
            )
            val opprettRequest = OpprettDelutbetalingerRequest(
                utbetalingId = utbetaling1.id,
                delutbetalinger = listOf(delutbetaling),
            )
            service.opprettDelutbetalinger(
                request = opprettRequest,
                navIdent = NavAnsattFixture.DonaldDuck.navIdent,
            )
            service.besluttDelutbetaling(
                id = delutbetaling.id,
                request = BesluttDelutbetalingRequest.GodkjentDelutbetalingRequest,
                navIdent = NavAnsattFixture.DonaldDuck.navIdent,
            ) shouldBeLeft listOf(
                FieldError.of("Kan ikke attestere en utbetaling du selv har opprettet"),
            )
        }

        test("kan ikke beslutte utbetaling når man har besluttet tilsagnet") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
                utbetalinger = listOf(utbetaling1),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT, besluttetAv = NavAnsattFixture.DonaldDuck.navIdent)
                setRoller(
                    NavAnsattFixture.DonaldDuck,
                    setOf(NavAnsattRolle.kontorspesifikk(Rolle.ATTESTANT_UTBETALING, setOf(Innlandet.enhetsnummer))),
                )
            }.initialize(database.db)

            val service = createUtbetalingService()
            val delutbetaling = DelutbetalingRequest(
                id = UUID.randomUUID(),
                tilsagnId = Tilsagn1.id,
                gjorOppTilsagn = false,
                belop = 100,
            )
            val opprettRequest = OpprettDelutbetalingerRequest(
                utbetalingId = utbetaling1.id,
                delutbetalinger = listOf(delutbetaling),
            )
            service.opprettDelutbetalinger(
                request = opprettRequest,
                navIdent = NavAnsattFixture.MikkeMus.navIdent,
            )
            service.besluttDelutbetaling(
                id = delutbetaling.id,
                request = BesluttDelutbetalingRequest.GodkjentDelutbetalingRequest,
                navIdent = NavAnsattFixture.DonaldDuck.navIdent,
            ) shouldBeLeft listOf(
                FieldError.of("Kan ikke attestere en utbetaling der du selv har besluttet tilsagnet"),
            )
        }
    }
})

private fun QueryContext.setRoller(ansatt: NavAnsattDbo, roller: Set<NavAnsattRolle>) {
    queries.ansatt.setRoller(
        navIdent = ansatt.navIdent,
        roller = roller,
    )
}

private fun getForhandsgodkjentBeregning(periode: Periode, belop: Int) = UtbetalingBeregningForhandsgodkjent(
    input = UtbetalingBeregningForhandsgodkjent.Input(
        periode = periode,
        sats = 20205,
        stengt = setOf(),
        deltakelser = setOf(),
    ),
    output = UtbetalingBeregningForhandsgodkjent.Output(
        belop = belop,
        deltakelser = setOf(),
    ),
)
