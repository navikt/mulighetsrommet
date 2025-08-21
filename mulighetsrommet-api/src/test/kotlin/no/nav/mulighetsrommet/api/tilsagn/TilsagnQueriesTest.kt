package no.nav.mulighetsrommet.api.tilsagn

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.ArrangorFixtures
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Gjovik
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.tilsagn.db.TilsagnDbo
import no.nav.mulighetsrommet.api.tilsagn.db.TilsagnQueries
import no.nav.mulighetsrommet.api.tilsagn.model.*
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.utils.IntegrityConstraintViolation
import no.nav.mulighetsrommet.database.utils.query
import no.nav.mulighetsrommet.model.Periode
import no.nav.tiltak.okonomi.BestillingStatusType
import java.time.LocalDate
import java.util.*

class TilsagnQueriesTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    val domain = MulighetsrommetTestDomain(
        avtaler = listOf(AvtaleFixtures.AFT, AvtaleFixtures.ARR),
        gjennomforinger = listOf(GjennomforingFixtures.AFT1, GjennomforingFixtures.ArbeidsrettetRehabilitering),
    )

    val beregningFri = {
        TilsagnBeregningFri(
            TilsagnBeregningFri.Input(
                listOf(
                    TilsagnBeregningFri.InputLinje(
                        id = UUID.randomUUID(),
                        beskrivelse = "Beskrivelse",
                        belop = 123,
                        antall = 1,
                    ),
                ),
                prisbetingelser = "Prisbetingelser fra avtale",
            ),
            TilsagnBeregningFri.Output(123),
        )
    }

    val tilsagn = TilsagnDbo(
        id = UUID.randomUUID(),
        gjennomforingId = GjennomforingFixtures.AFT1.id,
        type = TilsagnType.TILSAGN,
        periode = Periode.forMonthOf(LocalDate.of(2023, 1, 1)),
        lopenummer = 1,
        kostnadssted = Gjovik.enhetsnummer,
        bestillingsnummer = "1",
        bestillingStatus = null,
        belopBrukt = 0,
        beregning = beregningFri(),
        kommentar = null,
    )

    context("CRUD") {
        test("upsert, get, delete") {
            database.runAndRollback { session ->
                domain.setup(session)

                val queries = TilsagnQueries(session)

                queries.upsert(tilsagn)

                queries.get(tilsagn.id).shouldNotBeNull() should {
                    it.id shouldBe tilsagn.id
                    it.tiltakstype shouldBe Tilsagn.Tiltakstype(
                        tiltakskode = TiltakstypeFixtures.AFT.tiltakskode!!,
                        navn = TiltakstypeFixtures.AFT.navn,
                    )
                    it.gjennomforing shouldBe Tilsagn.Gjennomforing(
                        id = GjennomforingFixtures.AFT1.id,
                        navn = GjennomforingFixtures.AFT1.navn,
                    )
                    it.periode shouldBe Periode(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 2, 1))
                    it.kostnadssted shouldBe Gjovik
                    it.lopenummer shouldBe 1
                    it.bestilling shouldBe Tilsagn.Bestilling(
                        bestillingsnummer = "1",
                        status = null,
                    )
                    it.arrangor shouldBe Tilsagn.Arrangor(
                        navn = ArrangorFixtures.underenhet1.navn,
                        id = ArrangorFixtures.underenhet1.id,
                        organisasjonsnummer = ArrangorFixtures.underenhet1.organisasjonsnummer,
                        slettet = false,
                    )
                    it.beregning shouldBe TilsagnBeregningFri(
                        TilsagnBeregningFri.Input(
                            linjer = (tilsagn.beregning as TilsagnBeregningFri).input.linjer,
                            prisbetingelser = "Prisbetingelser fra avtale",
                        ),
                        TilsagnBeregningFri.Output(123),
                    )
                    it.type shouldBe TilsagnType.TILSAGN
                    it.status shouldBe TilsagnStatus.TIL_GODKJENNING
                }

                queries.delete(tilsagn.id)

                queries.get(tilsagn.id) shouldBe null
            }
        }

        test("upsert forhåndsgodkjent beregning") {
            database.runAndRollback { session ->
                domain.setup(session)

                val queries = TilsagnQueries(session)

                val beregning = TilsagnBeregningPrisPerManedsverk(
                    input = TilsagnBeregningPrisPerManedsverk.Input(
                        periode = tilsagn.periode,
                        antallPlasser = 10,
                        sats = 100,
                        prisbetingelser = null,
                    ),
                    output = TilsagnBeregningPrisPerManedsverk.Output(1000),
                )
                queries.upsert(tilsagn.copy(beregning = beregning))

                queries.get(tilsagn.id).shouldNotBeNull().should {
                    it.beregning shouldBe beregning
                }
            }
        }

        test("upsert fri beregning") {
            database.runAndRollback { session ->
                domain.setup(session)

                val queries = TilsagnQueries(session)

                val beregning = TilsagnBeregningFri(
                    input = TilsagnBeregningFri.Input(
                        prisbetingelser = AvtaleFixtures.ARR.prisbetingelser,
                        linjer = listOf(
                            TilsagnBeregningFri.InputLinje(
                                id = UUID.randomUUID(),
                                beskrivelse = "Beskrivelse",
                                belop = 500,
                                antall = 2,
                            ),
                        ),
                    ),
                    output = TilsagnBeregningFri.Output(1000),
                )
                queries.upsert(tilsagn.copy(beregning = beregning, gjennomforingId = GjennomforingFixtures.ArbeidsrettetRehabilitering.id))
                queries.get(tilsagn.id).shouldNotBeNull().should {
                    it.beregning shouldBe beregning
                }
            }
        }

        test("løpenummer er unikt per gjennomføring") {
            val aft2 = GjennomforingFixtures.AFT1.copy(id = UUID.randomUUID())

            val domain2 = MulighetsrommetTestDomain(
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(GjennomforingFixtures.AFT1, aft2),
            )

            database.runAndRollback { session ->
                domain2.setup(session)

                val queries = TilsagnQueries(session)

                queries.upsert(
                    tilsagn.copy(
                        id = UUID.randomUUID(),
                        lopenummer = 1,
                        bestillingsnummer = "1",
                        gjennomforingId = GjennomforingFixtures.AFT1.id,
                        beregning = beregningFri(),
                    ),
                )

                queries.upsert(
                    tilsagn.copy(
                        id = UUID.randomUUID(),
                        lopenummer = 1,
                        bestillingsnummer = "2",
                        gjennomforingId = aft2.id,
                        beregning = beregningFri(),
                    ),
                )

                query {
                    queries.upsert(
                        tilsagn.copy(
                            id = UUID.randomUUID(),
                            lopenummer = 1,
                            bestillingsnummer = "3",
                            gjennomforingId = GjennomforingFixtures.AFT1.id,
                            beregning = beregningFri(),
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

                queries.getAll(gjennomforingId = GjennomforingFixtures.AFT1.id).shouldHaveSize(1)
                queries.getAll(gjennomforingId = UUID.randomUUID()).shouldHaveSize(0)

                queries.getAll(typer = listOf(TilsagnType.TILSAGN)).shouldHaveSize(1)
                queries.getAll(typer = listOf(TilsagnType.EKSTRATILSAGN)).shouldHaveSize(0)
            }
        }

        test("set status") {
            database.runAndRollback { session ->
                domain.setup(session)

                val queries = TilsagnQueries(session)

                queries.upsert(tilsagn)

                queries.get(tilsagn.id).shouldNotBeNull().status shouldBe TilsagnStatus.TIL_GODKJENNING

                queries.setStatus(tilsagn.id, TilsagnStatus.TIL_ANNULLERING)

                queries.get(tilsagn.id).shouldNotBeNull().status shouldBe TilsagnStatus.TIL_ANNULLERING
            }
        }

        test("set bestilling-status") {
            database.runAndRollback { session ->
                domain.setup(session)

                val queries = TilsagnQueries(session)

                queries.upsert(tilsagn.copy(bestillingStatus = BestillingStatusType.SENDT))

                queries.get(tilsagn.id).shouldNotBeNull().bestilling.status shouldBe BestillingStatusType.SENDT

                queries.setBestillingStatus(tilsagn.bestillingsnummer, BestillingStatusType.OPPGJORT)

                queries.get(tilsagn.id).shouldNotBeNull().bestilling.status shouldBe BestillingStatusType.OPPGJORT
            }
        }
    }
})
