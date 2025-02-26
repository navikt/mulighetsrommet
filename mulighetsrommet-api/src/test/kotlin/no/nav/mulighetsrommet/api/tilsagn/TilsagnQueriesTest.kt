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
import no.nav.mulighetsrommet.api.totrinnskontroll.db.TotrinnskontrollQueries
import no.nav.mulighetsrommet.api.totrinnskontroll.db.TotrinnskontrollType
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
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

                queries.get(tilsagn.id).shouldNotBeNull() should {
                    it.id shouldBe tilsagn.id
                    it.gjennomforing shouldBe TilsagnDto.Gjennomforing(
                        id = AFT1.id,
                        navn = AFT1.navn,
                        tiltakskode = TiltakstypeFixtures.AFT.tiltakskode!!,
                    )
                    it.periodeStart shouldBe LocalDate.of(2023, 1, 1)
                    it.periodeSlutt shouldBe LocalDate.of(2023, 1, 31)
                    it.kostnadssted shouldBe Gjovik
                    it.lopenummer shouldBe 1
                    it.bestillingsnummer shouldBe "1"
                    it.arrangor shouldBe TilsagnDto.Arrangor(
                        navn = ArrangorFixtures.underenhet1.navn,
                        id = ArrangorFixtures.underenhet1.id,
                        organisasjonsnummer = ArrangorFixtures.underenhet1.organisasjonsnummer,
                        slettet = false,
                    )
                    it.beregning shouldBe TilsagnBeregningFri(
                        TilsagnBeregningFri.Input(123),
                        TilsagnBeregningFri.Output(123),
                    )
                    it.type shouldBe TilsagnType.TILSAGN
                    it.status shouldBe TilsagnStatus.TIL_GODKJENNING
                    it.opprettelse.shouldBeTypeOf<Totrinnskontroll.Ubesluttet>() should { tt ->
                        tt.behandletAv shouldBe NavAnsattFixture.ansatt1.navIdent
                        tt.aarsaker shouldBe emptyList()
                        tt.forklaring shouldBe null
                    }
                }

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

                TotrinnskontrollQueries(session).beslutter(
                    entityId = tilsagn.id,
                    navIdent = NavIdent("B123456"),
                    besluttelse = Besluttelse.GODKJENT,
                    aarsaker = null,
                    forklaring = null,
                    type = TotrinnskontrollType.OPPRETT,
                    tidspunkt = LocalDateTime.now(),
                )

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

                TotrinnskontrollQueries(session).beslutter(
                    entityId = tilsagn.id,
                    navIdent = NavIdent("B123456"),
                    besluttelse = Besluttelse.GODKJENT,
                    aarsaker = null,
                    forklaring = null,
                    type = TotrinnskontrollType.OPPRETT,
                    tidspunkt = LocalDateTime.now(),
                )

                queries.getArrangorflateTilsagnTilUtbetaling(tilsagn.gjennomforingId, periodeMedTilsagn)
                    .shouldHaveSize(1)
                queries.getArrangorflateTilsagnTilUtbetaling(tilsagn.gjennomforingId, periodeUtenTilsagn)
                    .shouldBeEmpty()
            }
        }
    }
})
