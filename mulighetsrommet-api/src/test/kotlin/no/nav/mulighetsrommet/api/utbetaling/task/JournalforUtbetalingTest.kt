package no.nav.mulighetsrommet.api.utbetaling.task

import arrow.core.left
import arrow.core.right
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.mulighetsrommet.api.arrangor.model.Betalingsinformasjon
import no.nav.mulighetsrommet.api.clients.teamdokumenthandtering.DokarkClient
import no.nav.mulighetsrommet.api.clients.teamdokumenthandtering.DokarkError
import no.nav.mulighetsrommet.api.clients.teamdokumenthandtering.DokarkResponse
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.ArrangorFixtures
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.fixtures.UtbetalingFixtures
import no.nav.mulighetsrommet.api.pdfgen.PdfGenClient
import no.nav.mulighetsrommet.api.pdfgen.PdfGenError
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatusType
import no.nav.mulighetsrommet.api.utbetaling.service.PersonaliaService
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.JournalpostId
import no.nav.mulighetsrommet.model.Kontonummer
import java.time.Instant

class JournalforUtbetalingTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    val hovedenhet = ArrangorFixtures.hovedenhet
    val underenhet = ArrangorFixtures.underenhet1

    val utbetaling = UtbetalingFixtures.utbetaling1.copy(
        status = UtbetalingStatusType.TIL_BEHANDLING,
        betalingsinformasjon = Betalingsinformasjon.BBan(Kontonummer("12312312312"), null),
    )

    val domain = MulighetsrommetTestDomain(
        navEnheter = listOf(NavEnhetFixtures.Innlandet, NavEnhetFixtures.Gjovik),
        ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
        tiltakstyper = listOf(TiltakstypeFixtures.AFT),
        avtaler = listOf(
            AvtaleFixtures.AFT,
        ),
        gjennomforinger = listOf(GjennomforingFixtures.AFT1.copy(arrangorId = underenhet.id)),
        deltakere = emptyList(),
        arrangorer = listOf(hovedenhet, underenhet),
        utbetalinger = listOf(utbetaling),
    )

    beforeSpec {
        domain.initialize(database.db)
    }

    val pdfGenClient = mockk<PdfGenClient>()
    val dokarkClient = mockk<DokarkClient>()
    val personaliaService = mockk<PersonaliaService>()

    coEvery { personaliaService.getPersonalia(any(), any()) } returns emptyMap()

    fun createTask() = JournalforUtbetaling(
        db = database.db,
        dokarkClient = dokarkClient,
        personaliaService = personaliaService,
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
            queries.utbetaling.getOrError(utbetaling.id).journalpostId shouldBe JournalpostId("123")
        }
    }

    test("task scheduleres ikke hvis transaction rulles tilbake") {
        val task = createTask()

        val exception = shouldThrowExactly<Exception> {
            database.db.transaction {
                task.schedule(utbetaling.id, Instant.now(), session, emptyList())
                throw Exception("Test")
            }
        }
        exception.message shouldBe "Test"

        database.assertTable("scheduled_tasks").hasNumberOfRows(0)
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
