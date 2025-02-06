package no.nav.mulighetsrommet.api.tilsagn

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Gjovik
import no.nav.mulighetsrommet.api.tilsagn.db.TilsagnDbo
import no.nav.mulighetsrommet.api.tilsagn.db.TilsagnQueries
import no.nav.mulighetsrommet.api.tilsagn.model.*
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
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
        periodeStart = LocalDate.of(2023, 1, 1),
        periodeSlutt = LocalDate.of(2023, 2, 1),
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

                val tilsagnDto = queries.get(tilsagn.id)
                tilsagnDto should {
                    it!! shouldNotBe null
                    it.id shouldBe tilsagn.id
                    it.gjennomforing shouldBe TilsagnDto.Gjennomforing(
                        id = AFT1.id,
                        tiltakskode = TiltakstypeFixtures.AFT.tiltakskode!!,
                    )
                    it.periodeStart shouldBe LocalDate.of(2023, 1, 1)
                    it.periodeSlutt shouldBe LocalDate.of(2023, 2, 1)
                    it.kostnadssted shouldBe Gjovik
                    it.lopenummer shouldBe 1
                    it.arrangor shouldBe TilsagnDto.Arrangor(
                        navn = ArrangorFixtures.underenhet1.navn,
                        id = ArrangorFixtures.underenhet1.id,
                        organisasjonsnummer = ArrangorFixtures.underenhet1.organisasjonsnummer,
                        slettet = ArrangorFixtures.underenhet1.slettetDato != null,
                    )
                    it.beregning shouldBe TilsagnBeregningFri(
                        TilsagnBeregningFri.Input(123),
                        TilsagnBeregningFri.Output(123),
                    )
                    it.type shouldBe TilsagnType.TILSAGN
                    it.status shouldBe TilsagnStatus.TIL_GODKJENNING
                }
                tilsagnDto!!.godkjennelse.opprett.navIdent shouldBe NavAnsattFixture.ansatt1.navIdent
                tilsagnDto.godkjennelse.opprett.navn shouldBe
                    "${NavAnsattFixture.ansatt1.fornavn} ${NavAnsattFixture.ansatt1.etternavn}"
                tilsagnDto.godkjennelse.aarsaker shouldHaveSize 0
                tilsagnDto.godkjennelse.forklaring shouldBe null

                queries.delete(tilsagn.id)
                queries.get(tilsagn.id) shouldBe null
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

                queries.godkjenn(
                    tilsagn.id,
                    NavIdent("B123456"),
                    endretTidspunkt,
                )

                // Send til annullering
                queries.tilAnnullering(
                    tilsagn.id,
                    tilsagn.endretAv,
                    endretTidspunkt,
                    aarsaker = listOf(TilsagnStatusAarsak.FEIL_ANNET),
                    forklaring = "Min forklaring",
                )

                queries.get(tilsagn.id).shouldNotBeNull() should {
                    it.status shouldBe TilsagnStatus.TIL_ANNULLERING
                    it.annullering.shouldNotBeNull() should { annullering ->
                        annullering.opprett.navIdent shouldBe tilsagn.endretAv
                        annullering.opprett.navn shouldBe "${NavAnsattFixture.ansatt1.fornavn} ${NavAnsattFixture.ansatt1.etternavn}"
                        annullering.aarsaker shouldBe listOf(TilsagnStatusAarsak.FEIL_ANNET)
                        annullering.forklaring shouldBe "Min forklaring"
                    }
                }

                // Beslutt annullering
                queries.godkjennAnnullering(
                    tilsagn.id,
                    NavIdent("B123456"),
                    endretTidspunkt,
                )

                queries.get(tilsagn.id).shouldNotBeNull() should {
                    it.status shouldBe TilsagnStatus.ANNULLERT
                    it.annullering.shouldNotBeNull() should { annullering ->
                        annullering.opprett.navIdent shouldBe tilsagn.endretAv
                        annullering.beslutt?.navIdent shouldBe NavIdent("B123456")
                        annullering.aarsaker shouldBe listOf(TilsagnStatusAarsak.FEIL_ANNET)
                        annullering.forklaring shouldBe "Min forklaring"
                    }
                }
            }
        }

        test("avbryt annullering") {
            database.runAndRollback { session ->
                domain.setup(session)

                val queries = TilsagnQueries(session)
                queries.upsert(tilsagn)

                val endretTidspunkt = LocalDateTime.now()
                queries.godkjenn(
                    tilsagn.id,
                    NavIdent("B123456"),
                    endretTidspunkt,
                )
                // Send til annullering
                queries.tilAnnullering(
                    tilsagn.id,
                    tilsagn.endretAv,
                    endretTidspunkt,
                    aarsaker = listOf(TilsagnStatusAarsak.FEIL_ANNET),
                    forklaring = "Min forklaring",
                )

                // Avbryt annullering
                queries.avvisAnnullering(
                    tilsagn.id,
                    NavIdent("B123456"),
                    endretTidspunkt,
                )

                queries.get(tilsagn.id).shouldNotBeNull().status shouldBe TilsagnStatus.GODKJENT
            }
        }

        test("godkjenn") {
            database.runAndRollback { session ->
                domain.setup(session)

                val queries = TilsagnQueries(session)
                queries.upsert(tilsagn)

                val besluttetTidspunkt = LocalDateTime.of(2024, 12, 12, 0, 0)

                queries.godkjenn(
                    tilsagn.id,
                    NavIdent("B123456"),
                    besluttetTidspunkt,
                )

                queries.get(tilsagn.id).shouldNotBeNull().status shouldBe TilsagnStatus.GODKJENT
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
                queries.get(tilsagn.id).shouldNotBeNull() should {
                    it.status shouldBe TilsagnStatus.RETURNERT
                    it.godkjennelse.opprett.navIdent shouldBe tilsagn.endretAv
                    it.godkjennelse.beslutt?.navn shouldBe "${NavAnsattFixture.ansatt2.fornavn} ${NavAnsattFixture.ansatt2.etternavn}"
                    it.godkjennelse.aarsaker shouldBe listOf(TilsagnStatusAarsak.FEIL_ANNET)
                    it.godkjennelse.forklaring shouldBe "Min forklaring"
                }
            }
        }

        test("Skal få status TIL_GODKJENNING etter upsert") {
            database.runAndRollback { session ->
                domain.setup(session)

                val queries = TilsagnQueries(session)
                queries.upsert(tilsagn)

                queries.get(tilsagn.id).shouldNotBeNull() should {
                    it.status shouldBe TilsagnStatus.TIL_GODKJENNING
                    it.godkjennelse.opprett.navIdent shouldBe tilsagn.endretAv
                }
            }
        }
    }

    context("tilsagn for arrangører") {
        test("get by arrangor_ids") {
            database.runAndRollback { session ->
                domain.setup(session)

                val queries = TilsagnQueries(session)
                queries.upsert(tilsagn)
                queries.godkjenn(tilsagn.id, NavIdent("B123456"), LocalDateTime.now())

                queries.getAllArrangorflateTilsagn(ArrangorFixtures.underenhet1.organisasjonsnummer) shouldBe listOf(
                    ArrangorflateTilsagn(
                        id = tilsagn.id,
                        gjennomforing = ArrangorflateTilsagn.Gjennomforing(
                            navn = AFT1.navn,
                        ),
                        tiltakstype = ArrangorflateTilsagn.Tiltakstype(
                            navn = TiltakstypeFixtures.AFT.navn,
                        ),
                        periodeStart = LocalDate.of(2023, 1, 1),
                        periodeSlutt = LocalDate.of(2023, 2, 1),
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
                    ),
                )

                queries.getArrangorflateTilsagn(tilsagn.id).shouldNotBeNull().id shouldBe tilsagn.id
            }
        }

        test("get til refusjon") {
            database.runAndRollback { session ->
                domain.setup(session)

                val queries = TilsagnQueries(session)
                queries.upsert(tilsagn)
                queries.godkjenn(tilsagn.id, NavIdent("B123456"), LocalDateTime.now())

                queries.getArrangorflateTilsagnTilRefusjon(
                    tilsagn.gjennomforingId,
                    Periode.forMonthOf(LocalDate.of(2023, 1, 1)),
                ) shouldBe listOf(
                    ArrangorflateTilsagn(
                        id = tilsagn.id,
                        gjennomforing = ArrangorflateTilsagn.Gjennomforing(
                            navn = AFT1.navn,
                        ),
                        tiltakstype = ArrangorflateTilsagn.Tiltakstype(
                            navn = TiltakstypeFixtures.AFT.navn,
                        ),
                        periodeStart = LocalDate.of(2023, 1, 1),
                        periodeSlutt = LocalDate.of(2023, 2, 1),
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
                    ),
                )
            }
        }
    }
})
