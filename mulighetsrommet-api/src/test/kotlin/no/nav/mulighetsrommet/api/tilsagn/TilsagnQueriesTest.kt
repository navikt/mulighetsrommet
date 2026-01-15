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
import no.nav.mulighetsrommet.api.tilsagn.model.Tilsagn
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningFastSatsPerTiltaksplassPerManed
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningFri
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningPrisPerHeleUkesverk
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningPrisPerManedsverk
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningPrisPerTimeOppfolgingPerDeltaker
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningPrisPerUkesverk
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnType
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.database.utils.IntegrityConstraintViolation
import no.nav.mulighetsrommet.database.utils.query
import no.nav.mulighetsrommet.model.Currency
import no.nav.mulighetsrommet.model.Periode
import no.nav.tiltak.okonomi.BestillingStatusType
import java.time.LocalDate
import java.util.UUID

class TilsagnQueriesTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

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
                        valuta = Currency.NOK,
                        antall = 1,
                    ),
                ),
                prisbetingelser = "Prisbetingelser fra avtale",
            ),
            TilsagnBeregningFri.Output(
                belop = 123,
                valuta = Currency.NOK,
            ),
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
        valuta = Currency.NOK,
        beregning = beregningFri(),
        kommentar = "Kommentar",
        beskrivelse = "Beskrivelse til arrangør",
    )

    context("CRUD") {
        test("upsert, get, delete") {
            database.runAndRollback { session ->
                domain.setup(session)

                queries.tilsagn.upsert(tilsagn)

                queries.tilsagn.getOrError(tilsagn.id) should {
                    it.id shouldBe tilsagn.id
                    it.tiltakstype shouldBe Tilsagn.Tiltakstype(
                        tiltakskode = TiltakstypeFixtures.AFT.tiltakskode!!,
                        navn = TiltakstypeFixtures.AFT.navn,
                    )
                    it.gjennomforing.id shouldBe GjennomforingFixtures.AFT1.id
                    it.gjennomforing.lopenummer.shouldNotBeNull()
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
                        TilsagnBeregningFri.Output(
                            belop = 123,
                            valuta = Currency.NOK,
                        ),
                    )
                    it.type shouldBe TilsagnType.TILSAGN
                    it.status shouldBe TilsagnStatus.TIL_GODKJENNING
                    it.kommentar shouldBe "Kommentar"
                    it.beskrivelse shouldBe "Beskrivelse til arrangør"
                }

                queries.tilsagn.delete(tilsagn.id)

                queries.tilsagn.get(tilsagn.id) shouldBe null
            }
        }

        test("upsert beregning - fri") {
            database.runAndRollback { session ->
                domain.setup(session)

                val beregning = TilsagnBeregningFri(
                    input = TilsagnBeregningFri.Input(
                        prisbetingelser = "Betingelser",
                        linjer = listOf(
                            TilsagnBeregningFri.InputLinje(
                                id = UUID.randomUUID(),
                                beskrivelse = "To ting",
                                valuta = Currency.NOK,
                                belop = 500,
                                antall = 2,
                            ),
                            TilsagnBeregningFri.InputLinje(
                                id = UUID.randomUUID(),
                                beskrivelse = "En ting",
                                valuta = Currency.NOK,
                                belop = 100,
                                antall = 1,
                            ),
                        ),
                    ),
                    output = TilsagnBeregningFri.Output(
                        belop = 1100,
                        valuta = Currency.NOK,
                    ),
                )
                queries.tilsagn.upsert(tilsagn.copy(beregning = beregning))

                queries.tilsagn.getOrError(tilsagn.id).beregning shouldBe beregning
            }
        }

        test("upsert beregning - fast sats per tiltaksplass per måned") {
            database.runAndRollback { session ->
                domain.setup(session)

                val beregning = TilsagnBeregningFastSatsPerTiltaksplassPerManed(
                    input = TilsagnBeregningFastSatsPerTiltaksplassPerManed.Input(
                        periode = tilsagn.periode,
                        antallPlasser = 10,
                        sats = 100,
                        valuta = Currency.NOK,
                    ),
                    output = TilsagnBeregningFastSatsPerTiltaksplassPerManed.Output(
                        belop = 1000,
                        valuta = Currency.NOK,
                    ),
                )
                queries.tilsagn.upsert(tilsagn.copy(beregning = beregning))

                queries.tilsagn.getOrError(tilsagn.id).beregning shouldBe beregning
            }
        }

        test("upsert beregning - pris per månedsverk") {
            database.runAndRollback { session ->
                domain.setup(session)

                val beregning = TilsagnBeregningPrisPerManedsverk(
                    input = TilsagnBeregningPrisPerManedsverk.Input(
                        periode = tilsagn.periode,
                        antallPlasser = 10,
                        sats = 100,
                        valuta = Currency.NOK,
                        prisbetingelser = "Per måned",
                    ),
                    output = TilsagnBeregningPrisPerManedsverk.Output(
                        belop = 1000,
                        valuta = Currency.NOK,
                    ),
                )
                queries.tilsagn.upsert(tilsagn.copy(beregning = beregning))

                queries.tilsagn.getOrError(tilsagn.id).beregning shouldBe beregning
            }
        }

        test("upsert beregning - pris per ukesverk") {
            database.runAndRollback { session ->
                domain.setup(session)

                val beregning = TilsagnBeregningPrisPerUkesverk(
                    input = TilsagnBeregningPrisPerUkesverk.Input(
                        periode = tilsagn.periode,
                        antallPlasser = 10,
                        sats = 100,
                        valuta = Currency.NOK,
                        prisbetingelser = "Per uke",
                    ),
                    output = TilsagnBeregningPrisPerUkesverk.Output(
                        belop = 1000,
                        valuta = Currency.NOK,
                    ),
                )
                queries.tilsagn.upsert(tilsagn.copy(beregning = beregning))

                queries.tilsagn.getOrError(tilsagn.id).beregning shouldBe beregning
            }
        }

        test("upsert beregning - Avtalt pris per uke med påbegynt oppfølging per deltaker") {
            database.runAndRollback { session ->
                domain.setup(session)

                val beregning = TilsagnBeregningPrisPerHeleUkesverk(
                    input = TilsagnBeregningPrisPerHeleUkesverk.Input(
                        periode = tilsagn.periode,
                        antallPlasser = 10,
                        sats = 100,
                        valuta = Currency.NOK,
                        prisbetingelser = "Per uke",
                    ),
                    output = TilsagnBeregningPrisPerHeleUkesverk.Output(
                        1000,
                        valuta = Currency.NOK,
                    ),
                )
                queries.tilsagn.upsert(tilsagn.copy(beregning = beregning))

                queries.tilsagn.getOrError(tilsagn.id).beregning shouldBe beregning
            }
        }

        test("upsert beregning - pris per time oppfølging") {
            database.runAndRollback { session ->
                domain.setup(session)

                val beregning = TilsagnBeregningPrisPerTimeOppfolgingPerDeltaker(
                    input = TilsagnBeregningPrisPerTimeOppfolgingPerDeltaker.Input(
                        periode = tilsagn.periode,
                        sats = 100,
                        valuta = Currency.NOK,
                        antallPlasser = 10,
                        antallTimerOppfolgingPerDeltaker = 5,
                        prisbetingelser = "Betingelser",
                    ),
                    output = TilsagnBeregningPrisPerTimeOppfolgingPerDeltaker.Output(
                        belop = 5000,
                        valuta = Currency.NOK,
                    ),
                )
                queries.tilsagn.upsert(tilsagn.copy(beregning = beregning))

                queries.tilsagn.getOrError(tilsagn.id).beregning shouldBe beregning
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

                queries.tilsagn.upsert(
                    tilsagn.copy(
                        id = UUID.randomUUID(),
                        lopenummer = 1,
                        bestillingsnummer = "1",
                        gjennomforingId = GjennomforingFixtures.AFT1.id,
                        beregning = beregningFri(),
                    ),
                )

                queries.tilsagn.upsert(
                    tilsagn.copy(
                        id = UUID.randomUUID(),
                        lopenummer = 1,
                        bestillingsnummer = "2",
                        gjennomforingId = aft2.id,
                        beregning = beregningFri(),
                    ),
                )

                query {
                    queries.tilsagn.upsert(
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

                queries.tilsagn.upsert(
                    tilsagn.copy(
                        id = UUID.randomUUID(),
                        lopenummer = 1,
                        bestillingsnummer = "1",
                    ),
                )

                query {
                    queries.tilsagn.upsert(
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

                queries.tilsagn.upsert(tilsagn)

                queries.tilsagn.getAll(statuser = listOf(TilsagnStatus.TIL_GODKJENNING)).shouldHaveSize(1)
                queries.tilsagn.getAll(statuser = listOf(TilsagnStatus.TIL_ANNULLERING)).shouldHaveSize(0)

                queries.tilsagn.getAll(gjennomforingId = GjennomforingFixtures.AFT1.id).shouldHaveSize(1)
                queries.tilsagn.getAll(gjennomforingId = UUID.randomUUID()).shouldHaveSize(0)

                queries.tilsagn.getAll(typer = listOf(TilsagnType.TILSAGN)).shouldHaveSize(1)
                queries.tilsagn.getAll(typer = listOf(TilsagnType.EKSTRATILSAGN)).shouldHaveSize(0)
            }
        }

        test("set status") {
            database.runAndRollback { session ->
                domain.setup(session)

                queries.tilsagn.upsert(tilsagn)

                queries.tilsagn.getOrError(tilsagn.id).status shouldBe TilsagnStatus.TIL_GODKJENNING

                queries.tilsagn.setStatus(tilsagn.id, TilsagnStatus.TIL_ANNULLERING)

                queries.tilsagn.getOrError(tilsagn.id).status shouldBe TilsagnStatus.TIL_ANNULLERING
            }
        }

        test("set bestilling-status") {
            database.runAndRollback { session ->
                domain.setup(session)

                queries.tilsagn.upsert(tilsagn.copy(bestillingStatus = BestillingStatusType.SENDT))

                queries.tilsagn.getOrError(tilsagn.id).bestilling.status shouldBe BestillingStatusType.SENDT

                queries.tilsagn.setBestillingStatus(tilsagn.bestillingsnummer, BestillingStatusType.OPPGJORT)

                queries.tilsagn.getOrError(tilsagn.id).bestilling.status shouldBe BestillingStatusType.OPPGJORT
            }
        }
    }
})
