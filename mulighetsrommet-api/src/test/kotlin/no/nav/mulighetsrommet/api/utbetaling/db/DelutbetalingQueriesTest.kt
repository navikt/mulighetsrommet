package no.nav.mulighetsrommet.api.utbetaling.db

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.api.fixtures.TilsagnFixtures
import no.nav.mulighetsrommet.api.fixtures.UtbetalingFixtures
import no.nav.mulighetsrommet.api.totrinnskontroll.db.TotrinnskontrollQueries
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Besluttelse
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
import no.nav.mulighetsrommet.api.utbetaling.model.DelutbetalingStatus
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.model.Arena
import no.nav.mulighetsrommet.model.Tiltaksadministrasjon
import no.nav.tiltak.okonomi.FakturaStatusType
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

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

            queries.getOrError(delutbetaling.id).faktura.sendtTidspunkt.shouldBeNull()

            queries.setSendtTilOkonomi(
                UtbetalingFixtures.utbetaling1.id,
                TilsagnFixtures.Tilsagn1.id,
                Instant.parse("2025-12-01T00:00:00.00Z"),
            )

            queries.getOrError(delutbetaling.id).faktura.sendtTidspunkt.shouldBe(
                LocalDateTime.of(2025, 12, 1, 1, 0, 0),
            )
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
                FakturaStatusType.FULLT_BETALT,
                fakturaStatusSistOppdatert = LocalDateTime.now(),
            )

            queries.get(delutbetaling.id).shouldNotBeNull().faktura.status shouldBe FakturaStatusType.FULLT_BETALT
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
                    besluttetAvNavn = null,
                    behandletAvNavn = null,
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
                    besluttetAvNavn = null,
                    behandletAvNavn = null,
                    besluttetTidspunkt = LocalDateTime.now(),
                ),
            )
            val totrinn = queries.get(entityId, Totrinnskontroll.Type.OPPRETT)
            totrinn?.besluttetAv shouldBe Arena
            totrinn?.besluttelse shouldBe Besluttelse.AVVIST
        }
    }
})
