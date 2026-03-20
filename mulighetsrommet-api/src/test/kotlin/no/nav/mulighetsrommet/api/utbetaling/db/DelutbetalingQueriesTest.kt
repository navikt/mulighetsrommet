package no.nav.mulighetsrommet.api.utbetaling.db

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.api.fixtures.TilsagnFixtures
import no.nav.mulighetsrommet.api.fixtures.UtbetalingFixtures
import no.nav.mulighetsrommet.api.utbetaling.model.DelutbetalingStatus
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.model.withValuta
import no.nav.tiltak.okonomi.FakturaStatusType
import java.time.LocalDateTime
import java.util.UUID

class DelutbetalingQueriesTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

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
        fakturaStatusEndretTidspunkt = LocalDateTime.of(2025, 1, 1, 12, 0),
        pris = 100.withValuta(Valuta.NOK),
        gjorOppTilsagn = false,
        periode = UtbetalingFixtures.utbetaling1.periode,
        lopenummer = 1,
        fakturanummer = "1",
        fakturaStatus = null,
    )

    test("opprett delutbetaling") {
        database.runAndRollback { session ->
            domain.setup(session)

            queries.delutbetaling.upsert(delutbetaling)

            queries.delutbetaling.getByUtbetalingId(UtbetalingFixtures.utbetaling1.id).first().should {
                it.tilsagnId shouldBe TilsagnFixtures.Tilsagn1.id
                it.utbetalingId shouldBe UtbetalingFixtures.utbetaling1.id
                it.status shouldBe DelutbetalingStatus.TIL_ATTESTERING
                it.pris shouldBe 100.withValuta(Valuta.NOK)
                it.periode shouldBe UtbetalingFixtures.utbetaling1.periode
                it.lopenummer shouldBe 1
                it.faktura.fakturanummer shouldBe "1"
            }
        }
    }

    test("delete delutbetaling") {
        database.runAndRollback { session ->
            domain.setup(session)

            queries.delutbetaling.upsert(delutbetaling)
            queries.delutbetaling.delete(delutbetaling.id)

            queries.delutbetaling.get(delutbetaling.id) shouldBe null
        }
    }

    test("set sendt til okonomi") {
        database.runAndRollback { session ->
            domain.setup(session)

            queries.delutbetaling.upsert(delutbetaling)

            queries.delutbetaling.getOrError(delutbetaling.id).faktura.sendtTidspunkt.shouldBeNull()

            val tidspunkt = LocalDateTime.of(2025, 12, 1, 0, 0, 0)
            queries.delutbetaling.setFakturaSendtTidspunk(delutbetaling.id, tidspunkt)

            queries.delutbetaling.getOrError(delutbetaling.id).faktura.sendtTidspunkt.shouldBe(tidspunkt)
        }
    }

    test("set faktura_status") {
        database.runAndRollback { session ->
            domain.setup(session)

            queries.delutbetaling.upsert(delutbetaling)
            val sendtTidspunkt = LocalDateTime.of(2025, 12, 1, 0, 0, 0)
            queries.delutbetaling.setFakturaSendtTidspunk(delutbetaling.id, sendtTidspunkt)

            queries.delutbetaling.getOrError(delutbetaling.id).faktura.should {
                it.sendtTidspunkt shouldBe sendtTidspunkt
                it.statusEndretTidspunkt.shouldBeNull()
                it.status.shouldBeNull()
            }

            val endretTidspunkt = LocalDateTime.of(2025, 12, 1, 1, 1, 1)
            queries.delutbetaling.setFakturaStatus(
                delutbetaling.fakturanummer,
                FakturaStatusType.FULLT_BETALT,
                endretTidspunkt,
            )

            queries.delutbetaling.getOrError(delutbetaling.id).faktura.should {
                it.sendtTidspunkt shouldBe sendtTidspunkt
                it.statusEndretTidspunkt shouldBe endretTidspunkt
                it.status shouldBe FakturaStatusType.FULLT_BETALT
            }
        }
    }
})
