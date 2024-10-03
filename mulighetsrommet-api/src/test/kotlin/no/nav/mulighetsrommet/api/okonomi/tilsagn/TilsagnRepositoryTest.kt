package no.nav.mulighetsrommet.api.okonomi.tilsagn

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.fixtures.ArrangorFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Gjovik
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.truncateAll
import no.nav.mulighetsrommet.domain.dto.NavIdent
import java.time.LocalDate
import java.time.LocalDateTime
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
        val repository = TilsagnRepository(database.db)

        val tilsagn = TilsagnDbo(
            id = UUID.randomUUID(),
            tiltaksgjennomforingId = AFT1.id,
            periodeStart = LocalDate.of(2023, 1, 1),
            periodeSlutt = LocalDate.of(2023, 2, 1),
            kostnadssted = Gjovik.enhetsnummer,
            opprettetAv = NavAnsattFixture.ansatt1.navIdent,
            arrangorId = ArrangorFixtures.underenhet1.id,
            belop = 123,
        )

        test("upsert and get") {
            repository.upsert(tilsagn)
            repository.get(tilsagn.id) shouldBe TilsagnDto(
                id = tilsagn.id,
                tiltaksgjennomforingId = AFT1.id,
                periodeStart = LocalDate.of(2023, 1, 1),
                periodeSlutt = LocalDate.of(2023, 2, 1),
                kostnadssted = Gjovik,
                besluttelse = null,
                annullertTidspunkt = null,
                lopenummer = 1,
                opprettetAv = NavAnsattFixture.ansatt1.navIdent,
                arrangor = TilsagnDto.Arrangor(
                    navn = ArrangorFixtures.underenhet1.navn,
                    id = ArrangorFixtures.underenhet1.id,
                    organisasjonsnummer = ArrangorFixtures.underenhet1.organisasjonsnummer,
                    slettet = ArrangorFixtures.underenhet1.slettetDato != null,
                ),
                belop = 123,
            )
        }

        test("besluttelse set and get") {
            repository.upsert(tilsagn)
            repository.setBesluttelse(
                tilsagn.id,
                TilsagnBesluttelse.AVVIST,
                NavIdent("Z123456"),
                LocalDateTime.of(2023, 2, 2, 0, 0, 0),
            )

            repository.get(tilsagn.id)?.besluttelse shouldBe TilsagnDto.Besluttelse(
                navIdent = NavIdent("Z123456"),
                tidspunkt = LocalDateTime.of(2023, 2, 2, 0, 0, 0),
                utfall = TilsagnBesluttelse.AVVIST,
            )
        }

        test("upsert nuller ut besluttelse") {
            repository.upsert(tilsagn)
            repository.setBesluttelse(
                tilsagn.id,
                TilsagnBesluttelse.AVVIST,
                NavIdent("Z123456"),
                LocalDateTime.of(2023, 2, 2, 0, 0, 0),
            )
            repository.upsert(tilsagn)
            repository.get(tilsagn.id)?.besluttelse shouldBe null
        }
    }
})
