package no.nav.mulighetsrommet.api.tilsagn

import arrow.core.left
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Gjovik
import no.nav.mulighetsrommet.api.fixtures.TilsagnFixtures.Tilsagn1
import no.nav.mulighetsrommet.api.fixtures.TilsagnFixtures.setTilsagnStatus
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.tilsagn.model.*
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.ktor.exception.BadRequest
import no.nav.mulighetsrommet.ktor.exception.Forbidden
import no.nav.mulighetsrommet.model.NavIdent
import org.intellij.lang.annotations.Language
import java.time.LocalDate
import java.util.*

class TilsagnServiceTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    afterEach {
        database.truncateAll()
    }

    fun createTilsagnService(
        okonomi: OkonomiBestillingService = mockk(relaxed = true),
    ) = TilsagnService(
        db = database.db,
        okonomi = okonomi,
    )

    context("opprett tilsagn") {
        val service = createTilsagnService()

        val tilsagn = TilsagnRequest(
            id = Tilsagn1.id,
            gjennomforingId = AFT1.id,
            type = TilsagnType.TILSAGN,
            periodeStart = LocalDate.of(2023, 1, 1),
            periodeSlutt = LocalDate.of(2023, 1, 31),
            kostnadssted = Gjovik.enhetsnummer,
            beregning = TilsagnBeregningFri.Input(belop = 0),
        )

        test("tilsagn kan ikke vare over årsskiftet") {
            MulighetsrommetTestDomain(
                arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
            ).initialize(database.db)

            val request = tilsagn.copy(
                periodeStart = LocalDate.of(2022, 12, 1),
                periodeSlutt = LocalDate.of(2023, 1, 1),
            )

            service.upsert(request, NavAnsattFixture.ansatt1.navIdent).shouldBeLeft() shouldBe listOf(
                FieldError(
                    pointer = "/periodeSlutt",
                    detail = "Tilsagnsperioden kan ikke vare utover årsskiftet",
                ),
            )
        }

        test("oppretter tilsagn med rikitg periode") {
            MulighetsrommetTestDomain(
                arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
            ).initialize(database.db)

            service.upsert(tilsagn, NavAnsattFixture.ansatt1.navIdent).shouldBeRight()

            service.getAll().shouldHaveSize(1).first().should {
                it.periodeStart shouldBe LocalDate.of(2023, 1, 1)
                it.periodeSlutt shouldBe LocalDate.of(2023, 1, 31)
            }
        }

        test("genererer løpenummer og bestillingsnummer") {
            val domain2 = MulighetsrommetTestDomain(
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1, AFT1.copy(id = UUID.randomUUID())),
            ).initialize(database.db)

            val tilsagn2 = UUID.randomUUID()
            val tilsagn3 = UUID.randomUUID()

            service.upsert(
                tilsagn,
                NavAnsattFixture.ansatt1.navIdent,
            ).shouldBeRight()
            service.upsert(
                tilsagn.copy(id = tilsagn2),
                NavAnsattFixture.ansatt1.navIdent,
            ).shouldBeRight()
            service.upsert(
                tilsagn.copy(id = tilsagn3, gjennomforingId = domain2.gjennomforinger[1].id),
                NavAnsattFixture.ansatt1.navIdent,
            ).shouldBeRight()

            database.run {
                val aft1 = queries.gjennomforing.get(domain2.gjennomforinger[0].id).shouldNotBeNull()
                queries.tilsagn.get(tilsagn.id).shouldNotBeNull().should {
                    it.lopenummer shouldBe 1
                    it.bestillingsnummer shouldBe "A-${aft1.lopenummer}-1"
                }

                queries.tilsagn.get(tilsagn2).shouldNotBeNull().should {
                    it.lopenummer shouldBe 2
                    it.bestillingsnummer shouldBe "A-${aft1.lopenummer}-2"
                }

                val aft2 = queries.gjennomforing.get(domain2.gjennomforinger[1].id).shouldNotBeNull()
                queries.tilsagn.get(tilsagn3).shouldNotBeNull().should {
                    it.lopenummer shouldBe 1
                    it.bestillingsnummer shouldBe "A-${aft2.lopenummer}-1"
                }
            }
        }

        test("overskriver ikke eksisterende løpenummer") {
            MulighetsrommetTestDomain(
                arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
            ).initialize(database.db)

            service.upsert(
                tilsagn,
                NavAnsattFixture.ansatt1.navIdent,
            ).shouldBeRight()

            database.run {
                val aft1 = queries.gjennomforing.get(AFT1.id).shouldNotBeNull()
                queries.tilsagn.get(tilsagn.id).shouldNotBeNull().should {
                    it.lopenummer shouldBe 1
                    it.bestillingsnummer shouldBe "A-${aft1.lopenummer}-1"
                }
            }
        }
    }

    context("beslutt tilsagn") {
        test("kan ikke beslutte egne") {
            MulighetsrommetTestDomain(
                arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(TilsagnFixtures.Tilsagn1),
            ).initialize(database.db)
            val service = createTilsagnService()

            service.beslutt(
                id = Tilsagn1.id,
                besluttelse = BesluttTilsagnRequest.GodkjentTilsagnRequest,
                navIdent = NavAnsattFixture.ansatt1.navIdent,
            ) shouldBe Forbidden("Kan ikke beslutte eget tilsagn").left()
        }

        test("kan ikke beslutte to ganger") {
            MulighetsrommetTestDomain(
                arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(TilsagnFixtures.Tilsagn1),
            ).initialize(database.db)

            val service = createTilsagnService()

            service.beslutt(
                id = Tilsagn1.id,
                besluttelse = BesluttTilsagnRequest.GodkjentTilsagnRequest,
                navIdent = NavAnsattFixture.ansatt2.navIdent,
            ).shouldBeRight()

            service.beslutt(
                id = Tilsagn1.id,
                besluttelse = BesluttTilsagnRequest.GodkjentTilsagnRequest,
                navIdent = NavAnsattFixture.ansatt2.navIdent,
            ) shouldBe BadRequest("Tilsagnet kan ikke besluttes fordi det har status GODKJENT").left()
        }

        test("godkjent tilsagn trigger melding til økonomi") {
            MulighetsrommetTestDomain(
                arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(TilsagnFixtures.Tilsagn1),
            ).initialize(database.db)

            val okonomi = mockk<OkonomiBestillingService>()
            every { okonomi.scheduleBehandleGodkjentTilsagn(any(), any()) } returns Unit

            val service = createTilsagnService(okonomi)
            service.beslutt(
                id = Tilsagn1.id,
                besluttelse = BesluttTilsagnRequest.GodkjentTilsagnRequest,
                navIdent = NavAnsattFixture.ansatt2.navIdent,
            ).shouldBeRight()

            verify(exactly = 1) {
                okonomi.scheduleBehandleGodkjentTilsagn(Tilsagn1.id, any())
            }
        }
    }

    context("annuller tilsagn") {
        test("tilsagn må være godkjent for å kunne settes til annullering") {
            MulighetsrommetTestDomain(
                arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(TilsagnFixtures.Tilsagn1),
            ).initialize(database.db)

            val service = createTilsagnService()

            service.tilAnnullering(
                id = Tilsagn1.id,
                navIdent = NavAnsattFixture.ansatt2.navIdent,
                annullering = TilAnnulleringRequest(
                    aarsaker = listOf(TilsagnStatusAarsak.FEIL_BELOP),
                    forklaring = "Velg et annet beløp",
                ),
            ) shouldBeLeft BadRequest("Kan bare annullere godkjente tilsagn")

            service.beslutt(
                id = Tilsagn1.id,
                besluttelse = BesluttTilsagnRequest.GodkjentTilsagnRequest,
                navIdent = NavAnsattFixture.ansatt2.navIdent,
            ).shouldBeRight()

            val dto = service.tilAnnullering(
                id = Tilsagn1.id,
                navIdent = NavAnsattFixture.ansatt1.navIdent,
                annullering = TilAnnulleringRequest(
                    aarsaker = listOf(TilsagnStatusAarsak.FEIL_BELOP),
                    forklaring = "Velg et annet beløp",
                ),
            ).shouldBeRight()
            dto.status shouldBe TilsagnStatus.TIL_ANNULLERING
            dto.annullering shouldNotBe null
            dto.annullering!!.behandletAv shouldBe NavAnsattFixture.ansatt1.navIdent
            dto.annullering!!.aarsaker shouldBe listOf(TilsagnStatusAarsak.FEIL_BELOP.name)
            dto.annullering!!.forklaring shouldBe "Velg et annet beløp"
        }

        test("godkjenn annullering av tilsagn") {
            MulighetsrommetTestDomain(
                arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.TIL_ANNULLERING)
            }.initialize(database.db)

            val service = createTilsagnService()
            val dto1 = database.run { queries.tilsagn.get(Tilsagn1.id) }

            val dto = service.beslutt(
                Tilsagn1.id,
                BesluttTilsagnRequest.GodkjentTilsagnRequest,
                NavAnsattFixture.ansatt2.navIdent,
            ).shouldBeRight()
            dto.status shouldBe TilsagnStatus.ANNULLERT
            dto.annullering shouldNotBe null
            dto.annullering!!.behandletAv shouldBe NavAnsattFixture.ansatt1.navIdent
            dto.annullering!!.besluttetAv shouldBe NavAnsattFixture.ansatt2.navIdent
            dto.annullering!!.besluttelse shouldBe Besluttelse.GODKJENT
            dto.annullering!!.aarsaker shouldBe listOf(TilsagnStatusAarsak.FEIL_BELOP.name)
            dto.annullering!!.forklaring shouldBe "Velg et annet beløp"
        }

        test("annullering av tilsagn trigger melding til økonomi") {
            val okonomi = mockk<OkonomiBestillingService>()
            every { okonomi.scheduleBehandleGodkjentTilsagn(any(), any()) } returns Unit
            every { okonomi.scheduleBehandleAnnullertTilsagn(any(), any()) } returns Unit

            MulighetsrommetTestDomain(
                arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.TIL_ANNULLERING)
            }.initialize(database.db)

            val service = createTilsagnService(okonomi)
            service.beslutt(
                id = Tilsagn1.id,
                besluttelse = BesluttTilsagnRequest.GodkjentTilsagnRequest,
                navIdent = NavAnsattFixture.ansatt2.navIdent,
            ).shouldBeRight()

            verify(exactly = 1) {
                okonomi.scheduleBehandleAnnullertTilsagn(Tilsagn1.id, any())
            }
        }
    }

    context("slett tilsagn") {
        test("kan slette tilsagn når det er avvist") {
            MulighetsrommetTestDomain(
                arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.RETURNERT)
            }.initialize(database.db)

            val service = createTilsagnService()

            service.slettTilsagn(Tilsagn1.id).shouldBeRight()

            service.getAll() shouldHaveSize 0
        }

        test("kan ikke slette tilsagn når det er godkjent") {
            MulighetsrommetTestDomain(
                arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(TilsagnFixtures.Tilsagn1),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT)
            }.initialize(database.db)

            val service = createTilsagnService()
            service.slettTilsagn(Tilsagn1.id) shouldBeLeft BadRequest("Kan ikke slette tilsagn som er godkjent")
        }
    }

    context("endre status på tilsagn") {
        test("send til godkjenning på nytt etter returnert kan endre hvem som har behandlet") {
            MulighetsrommetTestDomain(
                arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
            ).initialize(database.db)

            val service = createTilsagnService()
            service.getAll()[0].status shouldBe TilsagnStatus.TIL_GODKJENNING

            service.beslutt(
                id = Tilsagn1.id,
                navIdent = NavAnsattFixture.ansatt2.navIdent,
                besluttelse = BesluttTilsagnRequest.AvvistTilsagnRequest(
                    aarsaker = listOf(TilsagnStatusAarsak.FEIL_BELOP),
                    forklaring = null,
                ),
            ).shouldBeRight()
            service.getAll()[0].status shouldBe TilsagnStatus.RETURNERT

            service.upsert(
                TilsagnRequest(
                    id = Tilsagn1.id,
                    gjennomforingId = Tilsagn1.gjennomforingId,
                    type = Tilsagn1.type,
                    periodeStart = Tilsagn1.periode.start,
                    periodeSlutt = Tilsagn1.periode.getLastInclusiveDate(),
                    kostnadssted = Tilsagn1.kostnadssted,
                    beregning = Tilsagn1.beregning.input,
                ),
                navIdent = NavIdent("T888888"),
            ).shouldBeRight()
            service.getAll()[0].status shouldBe TilsagnStatus.TIL_GODKJENNING

            service.getAll()[0].opprettelse.behandletAv shouldBe NavIdent("T888888")
        }

        test("Hver upsert genererer ny rad i totrinnskontroll") {
            MulighetsrommetTestDomain(
                arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
            ).initialize(database.db)

            val service = createTilsagnService()

            service.beslutt(
                id = Tilsagn1.id,
                navIdent = NavAnsattFixture.ansatt2.navIdent,
                besluttelse = BesluttTilsagnRequest.AvvistTilsagnRequest(
                    aarsaker = listOf(TilsagnStatusAarsak.FEIL_BELOP),
                    forklaring = null,
                ),
            ).shouldBeRight()

            service.upsert(
                TilsagnRequest(
                    id = Tilsagn1.id,
                    gjennomforingId = Tilsagn1.gjennomforingId,
                    type = Tilsagn1.type,
                    periodeStart = Tilsagn1.periode.start,
                    periodeSlutt = Tilsagn1.periode.getLastInclusiveDate(),
                    kostnadssted = Tilsagn1.kostnadssted,
                    beregning = Tilsagn1.beregning.input,
                ),
                navIdent = NavIdent("T888888"),
            ).shouldBeRight()

            service.beslutt(
                id = Tilsagn1.id,
                navIdent = NavAnsattFixture.ansatt2.navIdent,
                besluttelse = BesluttTilsagnRequest.AvvistTilsagnRequest(
                    aarsaker = listOf(TilsagnStatusAarsak.FEIL_BELOP),
                    forklaring = null,
                ),
            ).shouldBeRight()
            service.getAll()[0].status shouldBe TilsagnStatus.RETURNERT

            service.upsert(
                TilsagnRequest(
                    id = Tilsagn1.id,
                    gjennomforingId = Tilsagn1.gjennomforingId,
                    type = Tilsagn1.type,
                    periodeStart = Tilsagn1.periode.start,
                    periodeSlutt = Tilsagn1.periode.getLastInclusiveDate(),
                    kostnadssted = Tilsagn1.kostnadssted,
                    beregning = TilsagnBeregningFri.Input(belop = 7),
                ),
                navIdent = NavIdent("Z777777"),
            ).shouldBeRight()
            service.getAll()[0].opprettelse.behandletAv shouldBe NavIdent("Z777777")

            @Language("PostgreSQL")
            val query = """
                select * from totrinnskontroll where entity_id = '${Tilsagn1.id}' and type = 'OPPRETT'
            """.trimIndent()
            database.run {
                session.list(queryOf(query)) { it.localDateTime("behandlet_tidspunkt") to it.string("behandlet_av") }
            } shouldHaveSize 3
        }
    }
})
