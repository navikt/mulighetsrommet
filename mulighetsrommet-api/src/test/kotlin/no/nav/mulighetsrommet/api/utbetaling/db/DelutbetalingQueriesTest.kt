package no.nav.mulighetsrommet.api.utbetaling.db

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.tilsagn.model.Besluttelse
import no.nav.mulighetsrommet.api.totrinnskontroll.db.TotrinnskontrollQueries
import no.nav.mulighetsrommet.api.totrinnskontroll.db.TotrinnskontrollType
import no.nav.mulighetsrommet.api.utbetaling.model.DelutbetalingDto
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.model.NavIdent
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
            TotrinnskontrollQueries(session).beslutter(
                entityId = delutbetaling.id,
                navIdent = NavIdent("Z123456"),
                besluttelse = Besluttelse.GODKJENT,
                type = TotrinnskontrollType.OPPRETT,
                aarsaker = null,
                forklaring = null,
                tidspunkt = LocalDateTime.now(),
            )

            queries.getSkalSendesTilOkonomi(TilsagnFixtures.Tilsagn1.id) shouldHaveSize 1
            queries.setSendtTilOkonomi(UtbetalingFixtures.utbetaling1.id, TilsagnFixtures.Tilsagn1.id, LocalDateTime.now())
            queries.getSkalSendesTilOkonomi(TilsagnFixtures.Tilsagn1.id) shouldHaveSize 0
        }
    }

    test("sendt til okonomi sorterer etter besluttet tidspunkt") {
        database.runAndRollback { session ->
            domain.setup(session)

            val queries = DelutbetalingQueries(session)

            val delutbetaling1 = DelutbetalingDbo(
                id = UUID.randomUUID(),
                tilsagnId = TilsagnFixtures.Tilsagn1.id,
                utbetalingId = UtbetalingFixtures.utbetaling1.id,
                belop = 100,
                periode = UtbetalingFixtures.utbetaling1.periode,
                lopenummer = 1,
                fakturanummer = "1",
                opprettetAv = NavAnsattFixture.ansatt1.navIdent,
            )
            val delutbetaling2 = DelutbetalingDbo(
                id = UUID.randomUUID(),
                tilsagnId = TilsagnFixtures.Tilsagn1.id,
                utbetalingId = UtbetalingFixtures.utbetaling2.id,
                belop = 100,
                periode = UtbetalingFixtures.utbetaling2.periode,
                lopenummer = 2,
                fakturanummer = "2",
                opprettetAv = NavAnsattFixture.ansatt1.navIdent,
            )
            // Oppretter 1 først, men godkjenner 2 først
            queries.upsert(delutbetaling1)
            queries.upsert(delutbetaling2)
            TotrinnskontrollQueries(session).beslutter(
                entityId = delutbetaling2.id,
                navIdent = NavIdent("Z123456"),
                besluttelse = Besluttelse.GODKJENT,
                type = TotrinnskontrollType.OPPRETT,
                aarsaker = null,
                forklaring = null,
                tidspunkt = LocalDateTime.of(2025, 1, 1, 10, 0, 0),
            )
            TotrinnskontrollQueries(session).beslutter(
                entityId = delutbetaling1.id,
                navIdent = NavIdent("Z123456"),
                besluttelse = Besluttelse.GODKJENT,
                type = TotrinnskontrollType.OPPRETT,
                aarsaker = null,
                forklaring = null,
                tidspunkt = LocalDateTime.of(2025, 1, 1, 11, 0, 0),
            )

            val skalSendes = queries.getSkalSendesTilOkonomi(TilsagnFixtures.Tilsagn1.id)
            skalSendes shouldHaveSize 2
            skalSendes[0].utbetalingId shouldBe UtbetalingFixtures.utbetaling2.id
            skalSendes[1].utbetalingId shouldBe UtbetalingFixtures.utbetaling1.id
        }
    }
})
