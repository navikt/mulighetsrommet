package no.nav.mulighetsrommet.api.tilsagn

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Gjovik
import no.nav.mulighetsrommet.api.tilsagn.db.TilsagnDbo
import no.nav.mulighetsrommet.api.tilsagn.db.TilsagnQueries
import no.nav.mulighetsrommet.api.tilsagn.model.*
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.utils.IntegrityConstraintViolation
import no.nav.mulighetsrommet.database.utils.query
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Periode
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class TilsagnQueriesTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    val domain = MulighetsrommetTestDomain(
        avtaler = listOf(AvtaleFixtures.AFT),
        gjennomforinger = listOf(AFT1),
    )

    val tilsagn = TilsagnDbo(
        id = UUID.randomUUID(),
        gjennomforingId = AFT1.id,
        periode = Periode.forMonthOf(LocalDate.of(2023, 1, 1)),
        lopenummer = 1,
        bestillingsnummer = "1",
        kostnadssted = Gjovik.enhetsnummer,
        arrangorId = ArrangorFixtures.underenhet1.id,
        beregning = TilsagnBeregningFri(TilsagnBeregningFri.Input(123), TilsagnBeregningFri.Output(123)),
        endretAv = NavAnsattFixture.ansatt1.navIdent,
        endretTidspunkt = LocalDateTime.of(2023, 1, 1, 0, 0, 0),
        type = TilsagnType.TILSAGN,
    )

    context("CRUD") {
        test("upsert, get, delete") {
            database.runAndRollback { session ->
                domain.setup(session)

                val queries = TilsagnQueries(session)

                queries.upsert(tilsagn)

                queries.get(tilsagn.id) shouldBe TilsagnDto(
                    id = tilsagn.id,
                    gjennomforing = TilsagnDto.Gjennomforing(
                        id = AFT1.id,
                        tiltakskode = TiltakstypeFixtures.AFT.tiltakskode!!,
                    ),
                    periodeStart = LocalDate.of(2023, 1, 1),
                    periodeSlutt = LocalDate.of(2023, 1, 31),
                    kostnadssted = Gjovik,
                    lopenummer = 1,
                    bestillingsnummer = "1",
                    arrangor = TilsagnDto.Arrangor(
                        navn = ArrangorFixtures.underenhet1.navn,
                        id = ArrangorFixtures.underenhet1.id,
                        organisasjonsnummer = ArrangorFixtures.underenhet1.organisasjonsnummer,
                        slettet = false,
                    ),
                    beregning = TilsagnBeregningFri(TilsagnBeregningFri.Input(123), TilsagnBeregningFri.Output(123)),
                    status = TilsagnDto.TilsagnStatus.TilGodkjenning(
                        endretAv = NavAnsattFixture.ansatt1.navIdent,
                        endretTidspunkt = LocalDateTime.of(2023, 1, 1, 0, 0, 0),
                    ),
                    type = TilsagnType.TILSAGN,
                )

                queries.delete(tilsagn.id)

                queries.get(tilsagn.id) shouldBe null
            }
        }

        test("løpenummer er unikt per gjennomføring") {
            val aft2 = AFT1.copy(id = UUID.randomUUID())

            val domain2 = MulighetsrommetTestDomain(
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1, aft2),
            )

            database.runAndRollback { session ->
                domain2.setup(session)

                val queries = TilsagnQueries(session)

                queries.upsert(
                    tilsagn.copy(
                        id = UUID.randomUUID(),
                        lopenummer = 1,
                        bestillingsnummer = "1",
                        gjennomforingId = AFT1.id,
                    ),
                )

                queries.upsert(
                    tilsagn.copy(
                        id = UUID.randomUUID(),
                        lopenummer = 1,
                        bestillingsnummer = "2",
                        gjennomforingId = aft2.id,
                    ),
                )

                query {
                    queries.upsert(
                        tilsagn.copy(
                            id = UUID.randomUUID(),
                            lopenummer = 1,
                            bestillingsnummer = "3",
                            gjennomforingId = AFT1.id,
                        ),
                    )
                }.shouldBeLeft().shouldBeTypeOf<IntegrityConstraintViolation.UniqueViolation>()
            }
        }

        test("bestillingsnummer er unikt per tilsagn") {
            database.runAndRollback { session ->
                domain.setup(session)

                val queries = TilsagnQueries(session)

                queries.upsert(
                    tilsagn.copy(
                        id = UUID.randomUUID(),
                        lopenummer = 1,
                        bestillingsnummer = "1",
                    ),
                )

                query {
                    queries.upsert(
                        tilsagn.copy(
                            id = UUID.randomUUID(),
                            lopenummer = 2,
                            bestillingsnummer = "1",
                        ),
                    )
                }.shouldBeLeft().shouldBeTypeOf<IntegrityConstraintViolation.UniqueViolation>()
            }
        }

        test("get all") {
            database.runAndRollback { session ->
                domain.setup(session)

                val queries = TilsagnQueries(session)

                queries.upsert(tilsagn)

                queries.getAll(statuser = listOf(TilsagnStatus.TIL_GODKJENNING)).shouldHaveSize(1)
                queries.getAll(statuser = listOf(TilsagnStatus.TIL_ANNULLERING)).shouldHaveSize(0)

                queries.getAll(gjennomforingId = AFT1.id).shouldHaveSize(1)
                queries.getAll(gjennomforingId = UUID.randomUUID()).shouldHaveSize(0)

                queries.getAll(type = TilsagnType.TILSAGN).shouldHaveSize(1)
                queries.getAll(type = TilsagnType.EKSTRATILSAGN).shouldHaveSize(0)
            }
        }
    }

    context("endre status på tilsagn") {
        test("annuller") {
            database.runAndRollback { session ->
                domain.setup(session)

                val queries = TilsagnQueries(session)
                queries.upsert(tilsagn)

                val endretTidspunkt = LocalDateTime.now()

                // Send til annullering
                queries.tilAnnullering(
                    tilsagn.id,
                    tilsagn.endretAv,
                    endretTidspunkt,
                    aarsaker = listOf(TilsagnStatusAarsak.FEIL_ANNET),
                    forklaring = "Min forklaring",
                )

                queries.get(tilsagn.id).shouldNotBeNull()
                    .status.shouldBeTypeOf<TilsagnDto.TilsagnStatus.TilAnnullering>().should { status ->
                        status.endretAv shouldBe tilsagn.endretAv
                        status.endretAvNavn shouldBe "${NavAnsattFixture.ansatt1.fornavn} ${NavAnsattFixture.ansatt1.etternavn}"
                        status.aarsaker shouldBe listOf(TilsagnStatusAarsak.FEIL_ANNET)
                        status.forklaring shouldBe "Min forklaring"
                    }

                // Beslutt annullering
                queries.besluttAnnullering(
                    tilsagn.id,
                    NavIdent("B123456"),
                    endretTidspunkt,
                )

                queries.get(tilsagn.id).shouldNotBeNull()
                    .status.shouldBeTypeOf<TilsagnDto.TilsagnStatus.Annullert>().should { status ->
                        status.endretAv shouldBe tilsagn.endretAv
                        status.godkjentAv shouldBe NavIdent("B123456")
                        status.aarsaker shouldBe listOf(TilsagnStatusAarsak.FEIL_ANNET)
                        status.forklaring shouldBe "Min forklaring"
                    }
            }
        }

        test("avbryt annullering") {
            database.runAndRollback { session ->
                domain.setup(session)

                val queries = TilsagnQueries(session)
                queries.upsert(tilsagn)

                val endretTidspunkt = LocalDateTime.now()

                // Send til annullering
                queries.tilAnnullering(
                    tilsagn.id,
                    tilsagn.endretAv,
                    endretTidspunkt,
                    aarsaker = listOf(TilsagnStatusAarsak.FEIL_ANNET),
                    forklaring = "Min forklaring",
                )

                // Avbryt annullering
                queries.avbrytAnnullering(
                    tilsagn.id,
                    NavIdent("B123456"),
                    endretTidspunkt,
                )

                queries.get(tilsagn.id).shouldNotBeNull().status shouldBe TilsagnDto.TilsagnStatus.Godkjent
            }
        }

        test("godkjenn") {
            database.runAndRollback { session ->
                domain.setup(session)

                val queries = TilsagnQueries(session)
                queries.upsert(tilsagn)

                val besluttetTidspunkt = LocalDateTime.of(2024, 12, 12, 0, 0)

                queries.besluttGodkjennelse(
                    tilsagn.id,
                    NavIdent("B123456"),
                    besluttetTidspunkt,
                )

                queries.get(tilsagn.id).shouldNotBeNull().status shouldBe TilsagnDto.TilsagnStatus.Godkjent
            }
        }

        test("returner") {
            database.runAndRollback { session ->
                domain.setup(session)

                val queries = TilsagnQueries(session)
                queries.upsert(tilsagn)

                val returnertTidspunkt = LocalDateTime.of(2024, 12, 12, 0, 0)

                queries.returner(
                    tilsagn.id,
                    NavAnsattFixture.ansatt2.navIdent,
                    returnertTidspunkt,
                    aarsaker = listOf(TilsagnStatusAarsak.FEIL_ANNET),
                    forklaring = "Min forklaring",
                )
                queries.get(tilsagn.id).shouldNotBeNull()
                    .status.shouldBeTypeOf<TilsagnDto.TilsagnStatus.Returnert>().should { status ->
                        status.endretAv shouldBe tilsagn.endretAv
                        status.returnertAvNavn shouldBe "${NavAnsattFixture.ansatt2.fornavn} ${NavAnsattFixture.ansatt2.etternavn}"
                        status.aarsaker shouldBe listOf(TilsagnStatusAarsak.FEIL_ANNET)
                        status.forklaring shouldBe "Min forklaring"
                    }
            }
        }

        test("Skal få status TIL_GODKJENNING etter upsert") {
            database.runAndRollback { session ->
                domain.setup(session)

                val queries = TilsagnQueries(session)
                queries.upsert(tilsagn)

                queries.get(tilsagn.id).shouldNotBeNull()
                    .status.shouldBeTypeOf<TilsagnDto.TilsagnStatus.TilGodkjenning>().should { status ->
                        status.endretAv shouldBe tilsagn.endretAv
                    }
            }
        }
    }

    context("tilsagn for arrangører") {
        val periodeMedTilsagn = Periode.forMonthOf(LocalDate.of(2023, 1, 1))
        val periodeUtenTilsagn = Periode.forMonthOf(LocalDate.of(2023, 2, 1))

        test("blir tilgjengelig for arrangør når tilsagnet er godkjent") {
            database.runAndRollback { session ->
                domain.setup(session)

                val queries = TilsagnQueries(session)
                queries.upsert(tilsagn)

                queries.getArrangorflateTilsagn(tilsagn.id).shouldBeNull()
                queries.getAllArrangorflateTilsagn(ArrangorFixtures.underenhet1.organisasjonsnummer).shouldBeEmpty()

                queries.besluttGodkjennelse(tilsagn.id, NavIdent("B123456"), LocalDateTime.now())

                queries.getArrangorflateTilsagn(tilsagn.id) shouldBe ArrangorflateTilsagn(
                    id = tilsagn.id,
                    gjennomforing = ArrangorflateTilsagn.Gjennomforing(
                        navn = AFT1.navn,
                    ),
                    tiltakstype = ArrangorflateTilsagn.Tiltakstype(
                        navn = TiltakstypeFixtures.AFT.navn,
                    ),
                    periodeStart = periodeMedTilsagn.start,
                    periodeSlutt = periodeMedTilsagn.getLastDate(),
                    arrangor = ArrangorflateTilsagn.Arrangor(
                        navn = ArrangorFixtures.underenhet1.navn,
                        id = ArrangorFixtures.underenhet1.id,
                        organisasjonsnummer = ArrangorFixtures.underenhet1.organisasjonsnummer,
                    ),
                    beregning = TilsagnBeregningFri(
                        TilsagnBeregningFri.Input(123),
                        TilsagnBeregningFri.Output(123),
                    ),
                    status = ArrangorflateTilsagn.StatusOgAarsaker(
                        status = TilsagnStatus.GODKJENT,
                        aarsaker = emptyList(),
                    ),
                    type = TilsagnType.TILSAGN,
                )

                queries.getAllArrangorflateTilsagn(ArrangorFixtures.underenhet1.organisasjonsnummer).shouldHaveSize(1)
            }
        }

        test("henter relevante tilsagn basert på periode") {
            database.runAndRollback { session ->
                domain.setup(session)

                val queries = TilsagnQueries(session)
                queries.upsert(tilsagn)

                queries.getArrangorflateTilsagnTilUtbetaling(tilsagn.gjennomforingId, periodeMedTilsagn)
                    .shouldBeEmpty()
                queries.getArrangorflateTilsagnTilUtbetaling(tilsagn.gjennomforingId, periodeUtenTilsagn)
                    .shouldBeEmpty()

                queries.besluttGodkjennelse(tilsagn.id, NavIdent("B123456"), LocalDateTime.now())

                queries.getArrangorflateTilsagnTilUtbetaling(tilsagn.gjennomforingId, periodeMedTilsagn)
                    .shouldHaveSize(1)
                queries.getArrangorflateTilsagnTilUtbetaling(tilsagn.gjennomforingId, periodeUtenTilsagn)
                    .shouldBeEmpty()
            }
        }
    }
})
