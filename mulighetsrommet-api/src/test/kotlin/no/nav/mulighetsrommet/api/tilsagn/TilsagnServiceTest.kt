package no.nav.mulighetsrommet.api.tilsagn

import arrow.core.left
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Gjovik
import no.nav.mulighetsrommet.api.responses.BadRequest
import no.nav.mulighetsrommet.api.responses.Forbidden
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningFri
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatusAarsak
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnType
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import java.time.LocalDate
import java.util.*

class TilsagnServiceTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    val domain = MulighetsrommetTestDomain(
        gjennomforinger = listOf(AFT1),
    )

    beforeEach {
        domain.initialize(database.db)
    }

    afterEach {
        database.truncateAll()
    }

    fun createTilsagnService() = TilsagnService(
        db = database.db,
    )

    context("beslutt") {
        val service = createTilsagnService()

        val tilsagn = TilsagnRequest(
            id = UUID.randomUUID(),
            gjennomforingId = AFT1.id,
            type = TilsagnType.TILSAGN,
            periodeStart = LocalDate.of(2023, 1, 1),
            periodeSlutt = LocalDate.of(2023, 2, 1),
            kostnadssted = Gjovik.enhetsnummer,
            beregning = TilsagnBeregningFri.Input(belop = 0),
        )

        test("kan ikke beslutte egne") {
            service.upsert(tilsagn, NavAnsattFixture.ansatt1.navIdent).shouldBeRight()

            service.beslutt(
                id = tilsagn.id,
                besluttelse = BesluttTilsagnRequest.GodkjentTilsagnRequest,
                navIdent = NavAnsattFixture.ansatt1.navIdent,
            ) shouldBe Forbidden("Kan ikke beslutte eget tilsagn").left()
        }

        test("kan ikke beslutte to ganger") {
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
            beregning = TilsagnBeregningFri.Input(belop = 0),
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
