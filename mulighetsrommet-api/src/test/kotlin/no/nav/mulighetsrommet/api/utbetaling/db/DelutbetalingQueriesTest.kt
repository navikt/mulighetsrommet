package no.nav.mulighetsrommet.api.utbetaling.db

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.fixtures.UtbetalingFixtures.setDelutbetalingStatus
import no.nav.mulighetsrommet.api.utbetaling.model.DelutbetalingDto
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import java.time.LocalDateTime
import java.util.*

class DelutbetalingQueriesTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    val domain = MulighetsrommetTestDomain(
        ansatte = listOf(NavAnsattFixture.ansatt1),
        avtaler = listOf(AvtaleFixtures.AFT),
        gjennomforinger = listOf(AFT1),
        tilsagn = listOf(TilsagnFixtures.Tilsagn1),
        utbetalinger = listOf(UtbetalingFixtures.utbetaling1, UtbetalingFixtures.utbetaling2),
    )

    test("opprett delutbetaling") {
        database.runAndRollback { session ->
            domain.setup(session)

            val queries = DelutbetalingQueries(session)

            val delutbetaling = DelutbetalingDbo(
                id = UUID.randomUUID(),
                tilsagnId = TilsagnFixtures.Tilsagn1.id,
                utbetalingId = UtbetalingFixtures.utbetaling1.id,
                belop = 100,
                periode = UtbetalingFixtures.utbetaling1.periode,
                lopenummer = 1,
                fakturanummer = "1",
                opprettetAv = NavAnsattFixture.ansatt1.navIdent,
            )
            queries.upsert(delutbetaling)

            queries.getByUtbetalingId(UtbetalingFixtures.utbetaling1.id).first()
                .shouldBeTypeOf<DelutbetalingDto.DelutbetalingTilGodkjenning>() should {
                it.tilsagnId shouldBe TilsagnFixtures.Tilsagn1.id
                it.utbetalingId shouldBe UtbetalingFixtures.utbetaling1.id
                it.belop shouldBe 100
                it.periode shouldBe UtbetalingFixtures.utbetaling1.periode
                it.opprettelse.behandletAv shouldBe NavAnsattFixture.ansatt1.navIdent
                it.lopenummer shouldBe 1
                it.fakturanummer shouldBe "1"
            }
        }
    }

    test("set sendt til okonomi") {
        database.runAndRollback { session ->
            domain.setup(session)

            val queries = DelutbetalingQueries(session)

            val delutbetaling = DelutbetalingDbo(
                id = UUID.randomUUID(),
                tilsagnId = TilsagnFixtures.Tilsagn1.id,
                utbetalingId = UtbetalingFixtures.utbetaling1.id,
                belop = 100,
                periode = UtbetalingFixtures.utbetaling1.periode,
                lopenummer = 1,
                fakturanummer = "1",
                opprettetAv = NavAnsattFixture.ansatt1.navIdent,
            )
            queries.upsert(delutbetaling)
            QueryContext(session).setDelutbetalingStatus(delutbetaling, UtbetalingFixtures.DelutbetalingStatus.GODKJENT)

            queries.getSkalSendesTilOkonomi(TilsagnFixtures.Tilsagn1.id) shouldHaveSize 1
            queries.setSendtTilOkonomi(UtbetalingFixtures.utbetaling1.id, TilsagnFixtures.Tilsagn1.id, LocalDateTime.now())
            queries.getSkalSendesTilOkonomi(TilsagnFixtures.Tilsagn1.id) shouldHaveSize 0
        }
    }
})
