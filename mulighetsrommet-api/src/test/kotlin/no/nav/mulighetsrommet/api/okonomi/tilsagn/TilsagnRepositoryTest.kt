package no.nav.mulighetsrommet.api.okonomi.tilsagn

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.*
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Gjovik
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.truncateAll
import no.nav.mulighetsrommet.domain.dto.*
import java.time.LocalDate
import java.util.*

class TilsagnRepositoryTest : FunSpec({
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

    context("CRUD") {
        test("upsert and get") {
            val repository = TilsagnRepository(database.db)

            val tilsagn = TilsagnDbo(
                id = UUID.randomUUID(),
                tiltaksgjennomforingId = AFT1.id,
                periodeStart = LocalDate.of(2023, 1, 1),
                periodeSlutt = LocalDate.of(2023, 2, 1),
                kostnadssted = Gjovik.enhetsnummer,
                belop = 123,
                opprettetAv = NavAnsattFixture.ansatt1.navIdent,
                arrangorId = ArrangorFixtures.underenhet1.id,
            )

            repository.upsert(tilsagn)
            repository.get(tilsagn.id) shouldBe TilsagnDto(
                id = tilsagn.id,
                tiltaksgjennomforingId = AFT1.id,
                periodeStart = LocalDate.of(2023, 1, 1),
                periodeSlutt = LocalDate.of(2023, 2, 1),
                kostnadssted = Gjovik,
                belop = 123,
                sendtTidspunkt = null,
                annullertTidspunkt = null,
                lopenummer = 1,
                arrangor = TilsagnDto.Arrangor(
                    navn = ArrangorFixtures.underenhet1.navn,
                    id = ArrangorFixtures.underenhet1.id,
                    organisasjonsnummer = ArrangorFixtures.underenhet1.organisasjonsnummer,
                    slettet = ArrangorFixtures.underenhet1.slettetDato != null,
                ),
            )
        }
    }
})
