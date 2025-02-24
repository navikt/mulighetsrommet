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
import no.nav.mulighetsrommet.api.fixtures.ArrangorFixtures
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Gjovik
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningFri
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnDto.TilsagnStatus
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatusAarsak
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnType
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.ktor.exception.BadRequest
import no.nav.mulighetsrommet.ktor.exception.Forbidden
import java.time.LocalDate
import java.util.*

class TilsagnServiceTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    val domain = MulighetsrommetTestDomain(
        arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
        avtaler = listOf(AvtaleFixtures.AFT),
        gjennomforinger = listOf(AFT1),
    )

    beforeEach {
        domain.initialize(database.db)
    }

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
            id = UUID.randomUUID(),
            gjennomforingId = AFT1.id,
            type = TilsagnType.TILSAGN,
            periodeStart = LocalDate.of(2023, 1, 1),
            periodeSlutt = LocalDate.of(2023, 1, 31),
            kostnadssted = Gjovik.enhetsnummer,
            beregning = TilsagnBeregningFri.Input(belop = 0),
        )

        test("tilsagn kan ikke vare over årsskiftet") {
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

        // TODO rydd i test når det er enklere å styre status på tilsagn
        test("overskriver ikke eksisterende løpenummer") {
            domain.initialize(database.db)

            service.upsert(
                tilsagn,
                NavAnsattFixture.ansatt1.navIdent,
            ).shouldBeRight()

            database.run {
                queries.tilsagn.returner(
                    tilsagn.id,
                    NavAnsattFixture.ansatt1.navIdent,
                    tidspunkt = LocalDate.now().atStartOfDay(),
                    aarsaker = emptyList(),
                    forklaring = null,
                )
            }

            service.upsert(
                tilsagn,
                NavAnsattFixture.ansatt1.navIdent,
            ).shouldBeRight()

            database.run {
                val aft1 = queries.gjennomforing.get(domain.gjennomforinger[0].id).shouldNotBeNull()
                queries.tilsagn.get(tilsagn.id).shouldNotBeNull().should {
                    it.lopenummer shouldBe 1
                    it.bestillingsnummer shouldBe "A-${aft1.lopenummer}-1"
                }
            }
        }
    }

    context("beslutt tilsagn") {
        val tilsagn = TilsagnRequest(
            id = UUID.randomUUID(),
            gjennomforingId = AFT1.id,
            type = TilsagnType.TILSAGN,
            periodeStart = LocalDate.of(2023, 1, 1),
            periodeSlutt = LocalDate.of(2023, 2, 1),
            kostnadssted = Gjovik.enhetsnummer,
            beregning = TilsagnBeregningFri.Input(belop = 1),
        )

        test("kan ikke beslutte egne") {
            val service = createTilsagnService()

            service.upsert(tilsagn, NavAnsattFixture.ansatt1.navIdent).shouldBeRight()

            service.beslutt(
                id = tilsagn.id,
                besluttelse = BesluttTilsagnRequest.GodkjentTilsagnRequest,
                navIdent = NavAnsattFixture.ansatt1.navIdent,
            ) shouldBe Forbidden("Kan ikke beslutte eget tilsagn").left()
        }

        test("kan ikke beslutte to ganger") {
            val service = createTilsagnService()

            service.upsert(tilsagn, NavAnsattFixture.ansatt1.navIdent).shouldBeRight()

            service.beslutt(
                id = tilsagn.id,
                besluttelse = BesluttTilsagnRequest.GodkjentTilsagnRequest,
                navIdent = NavAnsattFixture.ansatt2.navIdent,
            ).shouldBeRight()

            service.beslutt(
                id = tilsagn.id,
                besluttelse = BesluttTilsagnRequest.GodkjentTilsagnRequest,
                navIdent = NavAnsattFixture.ansatt2.navIdent,
            ) shouldBe BadRequest("Tilsagnet kan ikke besluttes fordi det har status Godkjent").left()
        }

        test("godkjent tilsagn trigger melding til økonomi") {
            val okonomi = mockk<OkonomiBestillingService>()
            every { okonomi.scheduleBehandleGodkjentTilsagn(any(), any()) } returns Unit

            val service = createTilsagnService(okonomi)

            service.upsert(tilsagn, NavAnsattFixture.ansatt1.navIdent).shouldBeRight()

            service.beslutt(
                id = tilsagn.id,
                besluttelse = BesluttTilsagnRequest.GodkjentTilsagnRequest,
                navIdent = NavAnsattFixture.ansatt2.navIdent,
            ).shouldBeRight().status.shouldBeTypeOf<TilsagnStatus.Godkjent>()

            verify(exactly = 1) {
                okonomi.scheduleBehandleGodkjentTilsagn(tilsagn.id, any())
            }
        }
    }

    context("annuller tilsagn") {
        val tilsagn = TilsagnRequest(
            id = UUID.randomUUID(),
            gjennomforingId = AFT1.id,
            type = TilsagnType.TILSAGN,
            periodeStart = LocalDate.of(2023, 1, 1),
            periodeSlutt = LocalDate.of(2023, 2, 1),
            kostnadssted = Gjovik.enhetsnummer,
            beregning = TilsagnBeregningFri.Input(belop = 1),
        )

        test("tilsagn må være godkjent for å kunne settes til annullering") {
            val service = createTilsagnService()

            service.upsert(tilsagn, NavAnsattFixture.ansatt1.navIdent).shouldBeRight()

            val annullering = TilAnnulleringRequest(
                aarsaker = listOf(TilsagnStatusAarsak.FEIL_BELOP),
                forklaring = "Velg et annet beløp",
            )

            service.tilAnnullering(
                id = tilsagn.id,
                navIdent = NavAnsattFixture.ansatt2.navIdent,
                annullering = annullering,
            ) shouldBeLeft BadRequest("Kan bare annullere godkjente tilsagn")

            service.beslutt(
                id = tilsagn.id,
                besluttelse = BesluttTilsagnRequest.GodkjentTilsagnRequest,
                navIdent = NavAnsattFixture.ansatt2.navIdent,
            ).shouldBeRight()

            service.tilAnnullering(
                id = tilsagn.id,
                navIdent = NavAnsattFixture.ansatt1.navIdent,
                annullering = annullering,
            ).shouldBeRight().status.shouldBeTypeOf<TilsagnStatus.TilAnnullering> {
                it.endretAv shouldBe NavAnsattFixture.ansatt1.navIdent
                it.aarsaker shouldBe listOf(TilsagnStatusAarsak.FEIL_BELOP)
                it.forklaring shouldBe "Velg et annet beløp"
            }
        }

        test("annullering av tilsagn trigger melding til økonomi") {
            val okonomi = mockk<OkonomiBestillingService>()
            every { okonomi.scheduleBehandleGodkjentTilsagn(any(), any()) } returns Unit
            every { okonomi.scheduleBehandleAnnullertTilsagn(any(), any()) } returns Unit

            val service = createTilsagnService(okonomi)

            service.upsert(tilsagn, NavAnsattFixture.ansatt1.navIdent).shouldBeRight()

            service.beslutt(
                id = tilsagn.id,
                besluttelse = BesluttTilsagnRequest.GodkjentTilsagnRequest,
                navIdent = NavAnsattFixture.ansatt2.navIdent,
            ).shouldBeRight()

            verify(exactly = 1) {
                okonomi.scheduleBehandleGodkjentTilsagn(tilsagn.id, any())
            }

            service.tilAnnullering(
                id = tilsagn.id,
                navIdent = NavAnsattFixture.ansatt1.navIdent,
                annullering = TilAnnulleringRequest(
                    aarsaker = listOf(TilsagnStatusAarsak.FEIL_PERIODE),
                    forklaring = "Velg en annen periode",
                ),
            ).shouldBeRight()

            service.beslutt(
                id = tilsagn.id,
                besluttelse = BesluttTilsagnRequest.GodkjentTilsagnRequest,
                navIdent = NavAnsattFixture.ansatt2.navIdent,
            ).shouldBeRight().status.shouldBeTypeOf<TilsagnStatus.Annullert> {
                it.endretAv shouldBe NavAnsattFixture.ansatt1.navIdent
                it.aarsaker shouldBe listOf(TilsagnStatusAarsak.FEIL_PERIODE)
                it.forklaring shouldBe "Velg en annen periode"
                it.godkjentAv shouldBe NavAnsattFixture.ansatt2.navIdent
            }

            verify(exactly = 1) {
                okonomi.scheduleBehandleAnnullertTilsagn(tilsagn.id, any())
            }
        }
    }

    context("slett tilsagn") {
        val service = createTilsagnService()

        val tilsagn = TilsagnRequest(
            id = UUID.randomUUID(),
            gjennomforingId = AFT1.id,
            type = TilsagnType.TILSAGN,
            periodeStart = LocalDate.of(2023, 1, 1),
            periodeSlutt = LocalDate.of(2023, 2, 1),
            kostnadssted = Gjovik.enhetsnummer,
            beregning = TilsagnBeregningFri.Input(belop = 1),
        )

        test("kan slette tilsagn når det er avvist") {
            service.upsert(tilsagn, NavAnsattFixture.ansatt1.navIdent).shouldBeRight()

            service.beslutt(
                id = tilsagn.id,
                besluttelse = BesluttTilsagnRequest.AvvistTilsagnRequest(
                    aarsaker = listOf(TilsagnStatusAarsak.FEIL_PERIODE),
                    forklaring = null,
                ),
                navIdent = NavAnsattFixture.ansatt2.navIdent,
            ).shouldBeRight()

            service.slettTilsagn(tilsagn.id).shouldBeRight()

            database.run {
                queries.tilsagn.get(tilsagn.id) shouldBe null
            }
        }

        test("kan ikke slette tilsagn når det er godkjent") {
            service.upsert(tilsagn, NavAnsattFixture.ansatt1.navIdent).shouldBeRight()

            service.beslutt(
                id = tilsagn.id,
                besluttelse = BesluttTilsagnRequest.GodkjentTilsagnRequest,
                navIdent = NavAnsattFixture.ansatt2.navIdent,
            ).shouldBeRight()

            service.slettTilsagn(tilsagn.id) shouldBeLeft BadRequest("Kan ikke slette tilsagn som er godkjent")
        }
    }
})
