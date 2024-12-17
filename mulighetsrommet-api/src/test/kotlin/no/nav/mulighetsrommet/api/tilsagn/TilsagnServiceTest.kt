package no.nav.mulighetsrommet.api.tilsagn

import arrow.core.left
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Gjovik
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.gjennomforing.db.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.okonomi.Prismodell
import no.nav.mulighetsrommet.api.responses.BadRequest
import no.nav.mulighetsrommet.api.responses.Forbidden
import no.nav.mulighetsrommet.api.services.EndringshistorikkService
import no.nav.mulighetsrommet.api.tilsagn.db.TilsagnRepository
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatusAarsak
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnType
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.truncateAll
import java.time.LocalDate
import java.util.*

class TilsagnServiceTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    val domain = MulighetsrommetTestDomain(
        gjennomforinger = listOf(
            AFT1,
        ),
    )

    beforeEach {
        domain.initialize(database.db)
    }

    afterEach {
        database.db.truncateAll()
    }
    val endringshistorikkService: EndringshistorikkService = mockk(relaxed = true)

    context("beslutt") {
        val tiltaksgjennomforingRepository = TiltaksgjennomforingRepository(database.db)
        val service = TilsagnService(
            tilsagnRepository = TilsagnRepository(database.db),
            tiltaksgjennomforingRepository,
            validator = TilsagnValidator(tiltaksgjennomforingRepository),
            endringshistorikkService = endringshistorikkService,
            db = database.db,
        )
        val tilsagn = TilsagnRequest(
            id = UUID.randomUUID(),
            tiltaksgjennomforingId = AFT1.id,
            periodeStart = LocalDate.of(2023, 1, 1),
            periodeSlutt = LocalDate.of(2023, 2, 1),
            kostnadssted = Gjovik.enhetsnummer,
            beregning = Prismodell.TilsagnBeregning.AFT(
                belop = 123,
                periodeStart = LocalDate.of(2023, 1, 1),
                periodeSlutt = LocalDate.of(2023, 2, 1),
                antallPlasser = 2,
                sats = 4,
            ),
            type = TilsagnType.TILSAGN,
        )

        test("kan ikke beslutte egne") {
            service.upsert(tilsagn, NavAnsattFixture.ansatt1.navIdent).shouldBeRight()

            service.beslutt(
                id = tilsagn.id,
                besluttelse = BesluttTilsagnRequest.GodkjentTilsagnRequest,
                navIdent = NavAnsattFixture.ansatt1.navIdent,
            ) shouldBe
                Forbidden("Kan ikke beslutte eget tilsagn").left()
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
            ) shouldBe
                BadRequest("Tilsagnet kan ikke besluttes fordi det har status Godkjent").left()
        }
    }

    context("slett tilsagn") {
        val tiltaksgjennomforingRepository = TiltaksgjennomforingRepository(database.db)
        val tilsagnRepository = TilsagnRepository(database.db)
        val service = TilsagnService(
            tilsagnRepository = tilsagnRepository,
            tiltaksgjennomforingRepository,
            validator = TilsagnValidator(tiltaksgjennomforingRepository),
            endringshistorikkService = endringshistorikkService,
            db = database.db,
        )
        val tilsagn = TilsagnRequest(
            id = UUID.randomUUID(),
            tiltaksgjennomforingId = AFT1.id,
            periodeStart = LocalDate.of(2023, 1, 1),
            periodeSlutt = LocalDate.of(2023, 2, 1),
            kostnadssted = Gjovik.enhetsnummer,
            beregning = Prismodell.TilsagnBeregning.AFT(
                belop = 123,
                periodeStart = LocalDate.of(2023, 1, 1),
                periodeSlutt = LocalDate.of(2023, 2, 1),
                antallPlasser = 2,
                sats = 4,
            ),
            type = TilsagnType.TILSAGN,
        )

        test("kan bare slette tilsagn når det er avvist") {
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
            tilsagnRepository.get(tilsagn.id) shouldBe null
        }

        test("kan ikke slette tilsagn når det er godkjent") {
            service.upsert(tilsagn, NavAnsattFixture.ansatt1.navIdent).shouldBeRight()

            service.beslutt(
                id = tilsagn.id,
                besluttelse = BesluttTilsagnRequest.GodkjentTilsagnRequest,
                navIdent = NavAnsattFixture.ansatt2.navIdent,
            ).shouldBeRight()

            service.slettTilsagn(tilsagn.id).shouldBeLeft()
        }
    }
})
