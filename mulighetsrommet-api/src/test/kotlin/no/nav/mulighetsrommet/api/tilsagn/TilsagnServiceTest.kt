package no.nav.mulighetsrommet.api.tilsagn

import arrow.core.left
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Gjovik
import no.nav.mulighetsrommet.api.fixtures.TilsagnFixtures.medStatus
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.tilsagn.model.*
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnDto.TilsagnStatusDto
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.ktor.exception.BadRequest
import no.nav.mulighetsrommet.ktor.exception.Forbidden
import no.nav.mulighetsrommet.model.NavIdent
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
            id = TilsagnFixtures.Tilsagn1.id,
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
                id = TilsagnFixtures.Tilsagn1.id,
                besluttelse = BesluttTilsagnRequest.GodkjentTilsagnRequest,
                navIdent = TilsagnFixtures.Tilsagn1.status.opprettelse.behandletAv,
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
                id = TilsagnFixtures.Tilsagn1.id,
                besluttelse = BesluttTilsagnRequest.GodkjentTilsagnRequest,
                navIdent = NavAnsattFixture.ansatt2.navIdent,
            ).shouldBeRight()

            service.beslutt(
                id = TilsagnFixtures.Tilsagn1.id,
                besluttelse = BesluttTilsagnRequest.GodkjentTilsagnRequest,
                navIdent = NavAnsattFixture.ansatt2.navIdent,
            ) shouldBe BadRequest("Tilsagnet kan ikke besluttes fordi det har status Godkjent").left()
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
                id = TilsagnFixtures.Tilsagn1.id,
                besluttelse = BesluttTilsagnRequest.GodkjentTilsagnRequest,
                navIdent = NavAnsattFixture.ansatt2.navIdent,
            ).shouldBeRight()

            verify(exactly = 1) {
                okonomi.scheduleBehandleGodkjentTilsagn(TilsagnFixtures.Tilsagn1.id, any())
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
                id = TilsagnFixtures.Tilsagn1.id,
                navIdent = NavAnsattFixture.ansatt2.navIdent,
                annullering = TilAnnulleringRequest(
                    aarsaker = listOf(TilsagnStatusAarsak.FEIL_BELOP),
                    forklaring = "Velg et annet beløp",
                ),
            ) shouldBeLeft BadRequest("Kan bare annullere godkjente tilsagn")

            service.beslutt(
                id = TilsagnFixtures.Tilsagn1.id,
                besluttelse = BesluttTilsagnRequest.GodkjentTilsagnRequest,
                navIdent = NavAnsattFixture.ansatt2.navIdent,
            ).shouldBeRight()

            service.tilAnnullering(
                id = TilsagnFixtures.Tilsagn1.id,
                navIdent = NavAnsattFixture.ansatt1.navIdent,
                annullering = TilAnnulleringRequest(
                    aarsaker = listOf(TilsagnStatusAarsak.FEIL_BELOP),
                    forklaring = "Velg et annet beløp",
                ),
            ).shouldBeRight().status.shouldBeTypeOf<TilsagnStatusDto.TilAnnullering> {
                it.annullering.behandletAv shouldBe NavAnsattFixture.ansatt1.navIdent
                it.annullering.aarsaker shouldBe listOf(TilsagnStatusAarsak.FEIL_BELOP.name)
                it.annullering.forklaring shouldBe "Velg et annet beløp"
            }
        }

        test("annullering av tilsagn trigger melding til økonomi") {
            val okonomi = mockk<OkonomiBestillingService>()
            every { okonomi.scheduleBehandleGodkjentTilsagn(any(), any()) } returns Unit
            every { okonomi.scheduleBehandleAnnullertTilsagn(any(), any()) } returns Unit

            MulighetsrommetTestDomain(
                arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(TilsagnFixtures.Tilsagn1.medStatus(TilsagnStatus.TIL_ANNULLERING)),
            ).initialize(database.db)

            val service = createTilsagnService(okonomi)
            service.beslutt(
                id = TilsagnFixtures.Tilsagn1.id,
                besluttelse = BesluttTilsagnRequest.GodkjentTilsagnRequest,
                navIdent = NavAnsattFixture.ansatt2.navIdent,
            ).shouldBeRight()

            verify(exactly = 1) {
                okonomi.scheduleBehandleAnnullertTilsagn(TilsagnFixtures.Tilsagn1.id, any())
            }
        }
    }

    context("slett tilsagn") {
        test("kan slette tilsagn når det er avvist") {
            MulighetsrommetTestDomain(
                arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(TilsagnFixtures.Tilsagn1.medStatus(TilsagnStatus.RETURNERT)),
            ).initialize(database.db)

            val service = createTilsagnService()

            service.slettTilsagn(TilsagnFixtures.Tilsagn1.id).shouldBeRight()

            service.getAll() shouldHaveSize 0
        }

        test("kan ikke slette tilsagn når det er godkjent") {
            MulighetsrommetTestDomain(
                arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(TilsagnFixtures.Tilsagn1.medStatus(TilsagnStatus.GODKJENT)),
            ).initialize(database.db)

            val service = createTilsagnService()
            service.slettTilsagn(TilsagnFixtures.Tilsagn1.id) shouldBeLeft BadRequest("Kan ikke slette tilsagn som er godkjent")
        }
    }

    context("endre status på tilsagn") {
        test("send til godkjenning på nytt etter returnert kan endre hvem som har behandlet") {
            MulighetsrommetTestDomain(
                arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(TilsagnFixtures.Tilsagn1),
            ).initialize(database.db)

            val service = createTilsagnService()

            service.beslutt(
                id = TilsagnFixtures.Tilsagn1.id,
                navIdent = NavAnsattFixture.ansatt2.navIdent,
                besluttelse = BesluttTilsagnRequest.AvvistTilsagnRequest(
                    aarsaker = listOf(TilsagnStatusAarsak.FEIL_BELOP),
                    forklaring = null,
                ),
            ).shouldBeRight()

            service.upsert(
                TilsagnRequest(
                    id = TilsagnFixtures.Tilsagn1.id,
                    gjennomforingId = TilsagnFixtures.Tilsagn1.gjennomforing.id,
                    type = TilsagnFixtures.Tilsagn1.type,
                    periodeStart = TilsagnFixtures.Tilsagn1.periodeStart,
                    periodeSlutt = TilsagnFixtures.Tilsagn1.periodeSlutt,
                    kostnadssted = TilsagnFixtures.Tilsagn1.kostnadssted.enhetsnummer,
                    beregning = TilsagnFixtures.Tilsagn1.beregning.input,
                ),
                navIdent = NavIdent("T888888"),
            ).shouldBeRight()

            service.getAll()[0].status.opprettelse.behandletAv shouldBe NavIdent("T888888")
        }
    }
})
