package no.nav.mulighetsrommet.api.utbetaling.task

import arrow.core.left
import arrow.core.right
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.arrangorflate.ArrangorFlateService
import no.nav.mulighetsrommet.api.clients.dokark.DokarkClient
import no.nav.mulighetsrommet.api.clients.dokark.DokarkError
import no.nav.mulighetsrommet.api.clients.dokark.DokarkResponse
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.pdfgen.PdfGenClient
import no.nav.mulighetsrommet.api.pdfgen.PdfGenError
import no.nav.mulighetsrommet.api.utbetaling.db.UtbetalingDbo
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningPrisPerManedsverkMedDeltakelsesmengder
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.Arrangor
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.tiltak.okonomi.Tilskuddstype
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class JournalforUtbetalingTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    val hovedenhet = ArrangorFixtures.hovedenhet
    val underenhet = ArrangorFixtures.underenhet1

    val utbetaling = UtbetalingDbo(
        id = UUID.randomUUID(),
        gjennomforingId = GjennomforingFixtures.AFT1.id,
        beregning = UtbetalingBeregningPrisPerManedsverkMedDeltakelsesmengder(
            input = UtbetalingBeregningPrisPerManedsverkMedDeltakelsesmengder.Input(
                periode = Periode.forMonthOf(LocalDate.of(2024, 8, 1)),
                sats = 20205,
                stengt = setOf(),
                deltakelser = emptySet(),
            ),
            output = UtbetalingBeregningPrisPerManedsverkMedDeltakelsesmengder.Output(
                belop = 0,
                deltakelser = emptySet(),
            ),
        ),
        kontonummer = Kontonummer("12312312312"),
        kid = null,
        periode = Periode.forMonthOf(LocalDate.of(2024, 8, 1)),
        innsender = Arrangor,
        beskrivelse = null,
        tilskuddstype = Tilskuddstype.TILTAK_DRIFTSTILSKUDD,
        godkjentAvArrangorTidspunkt = LocalDateTime.now(),
        status = Utbetaling.UtbetalingStatus.INNSENDT,
    )

    val domain = MulighetsrommetTestDomain(
        navEnheter = listOf(NavEnhetFixtures.Innlandet, NavEnhetFixtures.Gjovik),
        ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
        tiltakstyper = listOf(TiltakstypeFixtures.AFT),
        avtaler = listOf(
            AvtaleFixtures.AFT.copy(
                arrangor = AvtaleFixtures.AFT.arrangor?.copy(
                    hovedenhet = hovedenhet.id,
                    underenheter = listOf(underenhet.id),
                ),
            ),
        ),
        gjennomforinger = listOf(GjennomforingFixtures.AFT1.copy(arrangorId = underenhet.id)),
        deltakere = emptyList(),
        arrangorer = listOf(hovedenhet, underenhet),
        utbetalinger = listOf(utbetaling),
    )

    beforeSpec {
        domain.initialize(database.db)
    }

    val arrangorFlateSerivce = { db: ApiDatabase ->
        ArrangorFlateService(
            db = db,
            personService = mockk(relaxed = true),
            kontoregisterOrganisasjonClient = mockk(relaxed = true),
        )
    }

    val pdfGenClient = mockk<PdfGenClient>(relaxed = true)
    val dokarkClient = mockk<DokarkClient>(relaxed = true)

    fun createTask() = JournalforUtbetaling(
        db = database.db,
        dokarkClient = dokarkClient,
        arrangorFlateService = arrangorFlateSerivce(database.db),
        pdf = pdfGenClient,
    )

    test("blir ikke journalført når pdfgen feiler") {
        coEvery { pdfGenClient.getPdfDocument(any()) } returns PdfGenError(500, "Generering feilet").left()

        val task = createTask()

        task.journalfor(utbetaling.id, emptyList())
            .shouldBeLeft("Feil fra pdfgen: PdfGenError(statusCode=500, message=Generering feilet)")
    }

    test("blir ikke journalført når dokark feiler") {
        coEvery { pdfGenClient.getPdfDocument(any()) } returns ":)".toByteArray().right()
        coEvery { dokarkClient.opprettJournalpost(any(), any()) } returns DokarkError(
            "Feilet å laste opp til joark",
        ).left()

        val task = createTask()

        task.journalfor(utbetaling.id, emptyList()).shouldBeLeft("Feil fra dokark: Feilet å laste opp til joark")
    }

    test("vellykket journalføring setter journalpost_id") {
        coEvery { pdfGenClient.getPdfDocument(any()) } returns ":)".toByteArray().right()
        coEvery { dokarkClient.opprettJournalpost(any(), any()) } returns DokarkResponse(
            journalpostId = "123",
            journalstatus = "ok",
            melding = null,
            journalpostferdigstilt = true,
            dokumenter = emptyList(),
        ).right()

        val task = createTask()

        task.journalfor(utbetaling.id, emptyList()).shouldBeRight().should {
            it.journalpostId shouldBe "123"
        }

        database.run {
            queries.utbetaling.get(utbetaling.id).shouldNotBeNull().journalpostId shouldBe "123"
        }
    }

    test("task scheduleres ikke hvis transaction rulles tilbake") {
        val task = createTask()

        assertThrows<Exception>("Test") {
            database.run { tx ->
                task.schedule(utbetaling.id, Instant.now(), tx, emptyList())
                throw Exception("Test")
            }
        }

        database.assertTable("scheduled_tasks")
            .hasNumberOfRows(0)
    }

    test("task scheduleres hvis transaction går bra") {
        val task = createTask()

        database.run { tx ->
            task.schedule(utbetaling.id, Instant.now(), tx, emptyList())
        }

        database.assertTable("scheduled_tasks")
            .row()
            .value("task_name").isEqualTo("JournalforUtbetaling")
    }
})
