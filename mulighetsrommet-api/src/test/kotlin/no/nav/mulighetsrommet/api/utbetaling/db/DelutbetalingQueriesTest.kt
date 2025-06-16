package no.nav.mulighetsrommet.api.utbetaling.db

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.totrinnskontroll.db.TotrinnskontrollQueries
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Besluttelse
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
import no.nav.mulighetsrommet.api.utbetaling.model.DelutbetalingStatus
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.model.Arena
import no.nav.mulighetsrommet.model.Tiltaksadministrasjon
import no.nav.tiltak.okonomi.FakturaStatusType
import java.time.LocalDateTime
import java.util.*

class DelutbetalingQueriesTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    val domain = MulighetsrommetTestDomain(
        ansatte = listOf(NavAnsattFixture.DonaldDuck),
        avtaler = listOf(AvtaleFixtures.AFT),
        gjennomforinger = listOf(AFT1),
        tilsagn = listOf(TilsagnFixtures.Tilsagn1),
        utbetalinger = listOf(UtbetalingFixtures.utbetaling1, UtbetalingFixtures.utbetaling2),
    )

    val delutbetaling = DelutbetalingDbo(
        id = UUID.randomUUID(),
        tilsagnId = TilsagnFixtures.Tilsagn1.id,
        utbetalingId = UtbetalingFixtures.utbetaling1.id,
        status = DelutbetalingStatus.TIL_ATTESTERING,
        fakturaStatusSistOppdatert = LocalDateTime.of(2025, 1, 1, 12, 0),
        belop = 100,
        gjorOppTilsagn = false,
        periode = UtbetalingFixtures.utbetaling1.periode,
        lopenummer = 1,
        fakturanummer = "1",
        fakturaStatus = null,
    )

    test("opprett delutbetaling") {
        database.runAndRollback { session ->
            domain.setup(session)

            val queries = DelutbetalingQueries(session)

            queries.upsert(delutbetaling)

            queries.getByUtbetalingId(UtbetalingFixtures.utbetaling1.id).first().should {
                it.tilsagnId shouldBe TilsagnFixtures.Tilsagn1.id
                it.utbetalingId shouldBe UtbetalingFixtures.utbetaling1.id
                it.status shouldBe DelutbetalingStatus.TIL_ATTESTERING
                it.belop shouldBe 100
                it.periode shouldBe UtbetalingFixtures.utbetaling1.periode
                it.lopenummer shouldBe 1
                it.faktura.fakturanummer shouldBe "1"
            }
        }
    }

    test("delete delutbetaling") {
        database.runAndRollback { session ->
            domain.setup(session)

            val queries = DelutbetalingQueries(session)

            queries.upsert(delutbetaling)
            queries.delete(delutbetaling.id)

            queries.get(delutbetaling.id) shouldBe null
        }
    }

    test("set sendt til okonomi") {
        database.runAndRollback { session ->
            domain.setup(session)

            val queries = DelutbetalingQueries(session)

            queries.upsert(delutbetaling)

            queries.getSkalSendesTilOkonomi(TilsagnFixtures.Tilsagn1.id).shouldHaveSize(1).first().should {
                it.id shouldBe delutbetaling.id
            }

            queries.setSendtTilOkonomi(
                UtbetalingFixtures.utbetaling1.id,
                TilsagnFixtures.Tilsagn1.id,
                LocalDateTime.now(),
            )

            queries.getSkalSendesTilOkonomi(TilsagnFixtures.Tilsagn1.id) shouldHaveSize 0
        }
    }

    test("set faktura_status") {
        database.runAndRollback { session ->
            domain.setup(session)

            val queries = DelutbetalingQueries(session)

            queries.upsert(delutbetaling.copy(fakturaStatus = FakturaStatusType.SENDT))

            queries.get(delutbetaling.id).shouldNotBeNull().faktura.status shouldBe FakturaStatusType.SENDT

            queries.setFakturaStatus(
                delutbetaling.fakturanummer,
                FakturaStatusType.UTBETALT,
                fakturaStatusSistOppdatert = LocalDateTime.now(),
            )

            queries.get(delutbetaling.id).shouldNotBeNull().faktura.status shouldBe FakturaStatusType.UTBETALT
        }
    }

    test("totrinnskontroll kan besluttes to ganger") {
        database.runAndRollback { session ->
            val queries = TotrinnskontrollQueries(session)
            val id = UUID.randomUUID()
            val entityId = UUID.randomUUID()
            queries.upsert(
                Totrinnskontroll(
                    id = id,
                    entityId = entityId,
                    behandletAv = Tiltaksadministrasjon,
                    aarsaker = emptyList(),
                    forklaring = null,
                    type = Totrinnskontroll.Type.OPPRETT,
                    behandletTidspunkt = LocalDateTime.now(),
                    besluttelse = Besluttelse.GODKJENT,
                    besluttetAv = Tiltaksadministrasjon,
                    besluttetTidspunkt = LocalDateTime.now(),
                ),
            )
            queries.upsert(
                Totrinnskontroll(
                    id = id,
                    entityId = entityId,
                    behandletAv = Tiltaksadministrasjon,
                    aarsaker = emptyList(),
                    forklaring = null,
                    type = Totrinnskontroll.Type.OPPRETT,
                    behandletTidspunkt = LocalDateTime.now(),
                    besluttelse = Besluttelse.AVVIST,
                    besluttetAv = Arena,
                    besluttetTidspunkt = LocalDateTime.now(),
                ),
            )
            val totrinn = queries.get(entityId, Totrinnskontroll.Type.OPPRETT)
            totrinn?.besluttetAv shouldBe Arena
            totrinn?.besluttelse shouldBe Besluttelse.AVVIST
        }
    }
})
