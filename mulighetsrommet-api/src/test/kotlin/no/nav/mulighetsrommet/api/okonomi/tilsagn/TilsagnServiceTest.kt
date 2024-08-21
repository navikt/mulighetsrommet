package no.nav.mulighetsrommet.api.okonomi.tilsagn

import arrow.core.left
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Gjovik
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.okonomi.prismodell.Prismodell
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.responses.BadRequest
import no.nav.mulighetsrommet.api.responses.Forbidden
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.truncateAll
import java.time.LocalDate
import java.util.*

class TilsagnServiceTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

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

    context("beslutt") {
        val tiltaksgjennomforingRepository = TiltaksgjennomforingRepository(database.db)
        val service = TilsagnService(
            tilsagnRepository = TilsagnRepository(database.db),
            tiltaksgjennomforingRepository,
            validator = TilsagnValidator(tiltaksgjennomforingRepository),
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
        )

        test("kan ikke beslutte egne") {
            service.upsert(tilsagn, NavAnsattFixture.ansatt1.navIdent).shouldBeRight()

            service.beslutt(tilsagn.id, TilsagnBesluttelse.GODKJENT, NavAnsattFixture.ansatt1.navIdent) shouldBe
                Forbidden("Kan ikke beslutte eget tilsagn").left()
        }

        test("kan ikke beslutte annullerte") {
            service.upsert(tilsagn, NavAnsattFixture.ansatt1.navIdent).shouldBeRight()

            service.annuller(tilsagn.id).shouldBeRight()

            service.beslutt(tilsagn.id, TilsagnBesluttelse.GODKJENT, NavAnsattFixture.ansatt2.navIdent) shouldBe
                BadRequest("Tilsagn er annullert").left()
        }

        test("kan ikke beslutte to ganger") {
            service.upsert(tilsagn, NavAnsattFixture.ansatt1.navIdent).shouldBeRight()

            service.beslutt(tilsagn.id, TilsagnBesluttelse.GODKJENT, NavAnsattFixture.ansatt2.navIdent).shouldBeRight()

            service.beslutt(tilsagn.id, TilsagnBesluttelse.GODKJENT, NavAnsattFixture.ansatt2.navIdent) shouldBe
                BadRequest("Tilsagn allerede besluttet").left()
        }
    }
})
