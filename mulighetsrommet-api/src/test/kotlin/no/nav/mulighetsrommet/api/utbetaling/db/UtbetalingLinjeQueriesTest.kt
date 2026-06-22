package no.nav.mulighetsrommet.api.utbetaling.db

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.api.fixtures.TilsagnFixtures
import no.nav.mulighetsrommet.api.fixtures.UtbetalingFixtures
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingLinjeStatus
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.NOK
import no.nav.tiltak.okonomi.FakturaStatusType
import java.time.Instant
import java.util.UUID

class UtbetalingLinjeQueriesTest : FunSpec({
    val database = extension(ApiDatabaseTestListener())

    val domain = MulighetsrommetTestDomain(
        ansatte = listOf(NavAnsattFixture.DonaldDuck),
        avtaler = listOf(AvtaleFixtures.AFT),
        gjennomforinger = listOf(AFT1),
        tilsagn = listOf(TilsagnFixtures.Tilsagn1),
        utbetalinger = listOf(UtbetalingFixtures.utbetaling1, UtbetalingFixtures.utbetaling2),
    )

    val linje = UtbetalingLinjeDbo(
        id = UUID.randomUUID(),
        tilsagnId = TilsagnFixtures.Tilsagn1.id,
        utbetalingId = UtbetalingFixtures.utbetaling1.id,
        status = UtbetalingLinjeStatus.TIL_ATTESTERING,
        fakturaStatusEndretTidspunkt = Instant.parse("2025-01-01T12:00:00Z"),
        pris = 100.NOK,
        gjorOppTilsagn = false,
        periode = UtbetalingFixtures.utbetaling1.periode,
        lopenummer = 1,
        fakturanummer = "1",
        fakturaStatus = null,
    )

    test("opprett utbetalingslinje") {
        database.runAndRollback { session ->
            domain.setup(session)

            queries.utbetalingLinje.upsert(linje)

            queries.utbetalingLinje.getByUtbetalingId(UtbetalingFixtures.utbetaling1.id).first().should {
                it.tilsagnId shouldBe TilsagnFixtures.Tilsagn1.id
                it.utbetalingId shouldBe UtbetalingFixtures.utbetaling1.id
                it.status shouldBe UtbetalingLinjeStatus.TIL_ATTESTERING
                it.pris shouldBe 100.NOK
                it.periode shouldBe UtbetalingFixtures.utbetaling1.periode
                it.lopenummer shouldBe 1
                it.faktura.fakturanummer shouldBe "1"
            }
        }
    }

    test("delete utbetalingslinje") {
        database.runAndRollback { session ->
            domain.setup(session)

            queries.utbetalingLinje.upsert(linje)
            queries.utbetalingLinje.delete(linje.id)

            queries.utbetalingLinje.get(linje.id) shouldBe null
        }
    }

    test("set sendt til okonomi") {
        database.runAndRollback { session ->
            domain.setup(session)

            queries.utbetalingLinje.upsert(linje)

            queries.utbetalingLinje.getOrError(linje.id).faktura.sendtTidspunkt.shouldBeNull()

            val tidspunkt = Instant.parse("2025-12-01T00:00:00Z")
            queries.utbetalingLinje.setFakturaSendtTidspunk(linje.id, tidspunkt)

            queries.utbetalingLinje.getOrError(linje.id).faktura.sendtTidspunkt.shouldBe(tidspunkt)
        }
    }

    test("set faktura_status") {
        database.runAndRollback { session ->
            domain.setup(session)

            queries.utbetalingLinje.upsert(linje)
            val sendtTidspunkt = Instant.parse("2025-12-01T00:00:00Z")
            queries.utbetalingLinje.setFakturaSendtTidspunk(linje.id, sendtTidspunkt)

            queries.utbetalingLinje.getOrError(linje.id).faktura.should {
                it.sendtTidspunkt shouldBe sendtTidspunkt
                it.statusEndretTidspunkt.shouldBeNull()
                it.status.shouldBeNull()
            }

            val endretTidspunkt = Instant.parse("2025-12-01T01:01:01Z")
            queries.utbetalingLinje.setFakturaStatus(
                linje.fakturanummer,
                FakturaStatusType.FULLT_BETALT,
                endretTidspunkt,
            )

            queries.utbetalingLinje.getOrError(linje.id).faktura.should {
                it.sendtTidspunkt shouldBe sendtTidspunkt
                it.statusEndretTidspunkt shouldBe endretTidspunkt
                it.status shouldBe FakturaStatusType.FULLT_BETALT
            }
        }
    }
})
