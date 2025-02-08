package no.nav.mulighetsrommet.api.utbetaling.db

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.utbetaling.model.DelutbetalingDto
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import java.util.*

class DelutbetalingQueriesTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    val tilsagn = TilsagnFixtures.Tilsagn1

    val utbetaling = UtbetalingFixtures.utbetaling1

    val domain = MulighetsrommetTestDomain(
        ansatte = listOf(NavAnsattFixture.ansatt1),
        avtaler = listOf(AvtaleFixtures.AFT),
        gjennomforinger = listOf(AFT1),
        tilsagn = listOf(tilsagn),
        utbetalinger = listOf(utbetaling, utbetaling.copy(id = UUID.randomUUID())),
    )

    test("opprett delutbetaling") {
        database.runAndRollback { session ->
            domain.setup(session)

            val queries = DelutbetalingQueries(session)

            val delutbetaling = DelutbetalingDbo(
                tilsagnId = tilsagn.id,
                utbetalingId = utbetaling.id,
                belop = 100,
                periode = utbetaling.periode,
                lopenummer = 1,
                fakturanummer = "1",
                opprettetAv = NavAnsattFixture.ansatt1.navIdent,
            )
            queries.opprettDelutbetalinger(listOf(delutbetaling))

            queries.getByUtbetalingId(utbetaling.id).first() shouldBe DelutbetalingDto.DelutbetalingTilGodkjenning(
                tilsagnId = tilsagn.id,
                utbetalingId = utbetaling.id,
                belop = 100,
                periode = utbetaling.periode,
                opprettetAv = NavAnsattFixture.ansatt1.navIdent,
                lopenummer = 1,
                fakturanummer = "1",
            )
        }
    }
})
