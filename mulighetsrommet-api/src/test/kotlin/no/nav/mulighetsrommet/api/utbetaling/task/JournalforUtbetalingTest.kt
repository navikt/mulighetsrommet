package no.nav.mulighetsrommet.api.utbetaling.task

import arrow.core.right
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.arrangorflate.ArrangorFlateService
import no.nav.mulighetsrommet.api.clients.dokark.DokarkClient
import no.nav.mulighetsrommet.api.clients.dokark.DokarkResponse
import no.nav.mulighetsrommet.api.clients.kontoregisterOrganisasjon.KontoregisterOrganisasjonClient
import no.nav.mulighetsrommet.api.clients.pdl.PdlClient
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.pdfgen.PdfGenClient
import no.nav.mulighetsrommet.api.utbetaling.db.UtbetalingDbo
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningForhandsgodkjent
import no.nav.mulighetsrommet.api.utbetaling.pdl.HentAdressebeskyttetPersonBolkPdlQuery
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.ktor.createMockEngine
import no.nav.mulighetsrommet.model.Arrangor
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.Periode
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
        fristForGodkjenning = LocalDateTime.now(),
        beregning = UtbetalingBeregningForhandsgodkjent(
            input = UtbetalingBeregningForhandsgodkjent.Input(
                periode = Periode.forMonthOf(LocalDate.of(2024, 8, 1)),
                sats = 20205,
                stengt = setOf(),
                deltakelser = emptySet(),
            ),
            output = UtbetalingBeregningForhandsgodkjent.Output(
                belop = 0,
                deltakelser = emptySet(),
            ),
        ),
        kontonummer = Kontonummer("12312312312"),
        kid = null,
        periode = Periode.forMonthOf(LocalDate.of(2024, 8, 1)),
        innsender = Arrangor,
        beskrivelse = null,
    )

    val domain = MulighetsrommetTestDomain(
        navEnheter = listOf(NavEnhetFixtures.IT, NavEnhetFixtures.Innlandet, NavEnhetFixtures.Gjovik),
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

    beforeEach {
        domain.initialize(database.db)
    }

    val pdl: PdlClient = mockk(relaxed = true)
    val pdf = PdfGenClient(
        clientEngine = createMockEngine {
            post("http://pdfgen/api/v1/genpdf/utbetaling/journalpost") {
                respond(":)".toByteArray(), HttpStatusCode.OK)
            }
        },
        baseUrl = "http://pdfgen",
    )
    val dokarkClient: DokarkClient = mockk()
    val kontoregisterClient: KontoregisterOrganisasjonClient = mockk(relaxed = true)
    val arrangorFlateSerivce = { db: ApiDatabase ->
        ArrangorFlateService(
            pdl = HentAdressebeskyttetPersonBolkPdlQuery(pdl),
            db = db,
            kontoregisterOrganisasjonClient = kontoregisterClient,
        )
    }

    fun createTask() = JournalforUtbetaling(
        db = database.db,
        dokarkClient = dokarkClient,
        arrangorFlateService = arrangorFlateSerivce(database.db),
        pdf = pdf,
    )

    test("utbetaling må være godkjent") {
        val task = createTask()

        shouldThrow<Throwable> {
            task.journalfor(utbetaling.id, emptyList())
        }
    }

    test("vellykket journalføring setter journalpost_id") {
        val task = createTask()

        database.run {
            queries.utbetaling.setGodkjentAvArrangor(utbetaling.id, LocalDateTime.now())
        }

        coEvery { dokarkClient.opprettJournalpost(any(), any()) } returns DokarkResponse(
            journalpostId = "123",
            journalstatus = "ok",
            melding = null,
            journalpostferdigstilt = true,
            dokumenter = emptyList(),
        ).right()

        task.journalfor(utbetaling.id, emptyList())

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
