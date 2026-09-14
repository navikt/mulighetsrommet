package no.nav.mulighetsrommet.api.tilsagn.task

import arrow.core.left
import arrow.core.right
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotliquery.queryOf
import no.nav.mulighetsrommet.admin.arrangor.ArrangorMeldingSender
import no.nav.mulighetsrommet.admin.arrangor.KontoregisterGateway
import no.nav.mulighetsrommet.admin.arrangor.MeldingError
import no.nav.mulighetsrommet.admin.arrangor.MeldingId
import no.nav.mulighetsrommet.api.clients.teamdokumenthandtering.DokarkClient
import no.nav.mulighetsrommet.api.clients.teamdokumenthandtering.DokarkError
import no.nav.mulighetsrommet.api.clients.teamdokumenthandtering.DokarkResponse
import no.nav.mulighetsrommet.api.domain.testing.fixture.DeltakerFixtures
import no.nav.mulighetsrommet.api.domain.testing.fixture.NavAnsattFixture
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TilsagnFixtures
import no.nav.mulighetsrommet.api.fixtures.setTilsagnStatus
import no.nav.mulighetsrommet.api.pdfgen.PdfGenClient
import no.nav.mulighetsrommet.api.pdfgen.PdfGenError
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.utbetaling.service.Gradering
import no.nav.mulighetsrommet.api.utbetaling.service.Personalia
import no.nav.mulighetsrommet.api.utbetaling.service.PersonaliaService
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import java.time.LocalDateTime
import java.util.Base64
import java.util.UUID

class SendTilsagnsbrevSagaTest : FunSpec({
    val database = extension(ApiDatabaseTestListener())

    val deltaker = DeltakerFixtures.createDeltaker(
        gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
    )
    val tilsagn = TilsagnFixtures.createTilsagn(
        gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
        lopenummer = 1,
    )

    val domain = MulighetsrommetTestDomain(
        ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus, NavAnsattFixture.FetterAnton),
        gjennomforinger = listOf(GjennomforingFixtures.EnkelAmo),
        deltakere = listOf(deltaker),
        tilsagn = listOf(tilsagn),
    ) {
        setTilsagnStatus(tilsagn, TilsagnStatus.GODKJENT)
    }

    val personaliaService = mockk<PersonaliaService>()
    val pdfGenClient = mockk<PdfGenClient>()
    val dokarkClient = mockk<DokarkClient>()
    val arrangorMeldingSender = mockk<ArrangorMeldingSender>()
    val kontoregister = mockk<KontoregisterGateway>()

    beforeEach {
        domain.initialize(database.api)

        clearMocks(
            personaliaService,
            pdfGenClient,
            dokarkClient,
            arrangorMeldingSender,
            kontoregister,
        )

        coEvery {
            personaliaService.getPersonalia(
                deltaker.id,
                PersonaliaService.OnBehalfOf.System,
            )
        } returns Personalia(
            deltakerId = deltaker.id,
            norskIdent = NorskIdent("12345678901"),
            navn = "Test Testesen",
            oppfolgingEnhet = null,
            geografiskEnhet = null,
            region = null,
            gradering = Gradering.UGRADERT,
            avvistGrunn = null,
        )
        coEvery {
            kontoregister.hentKontonummer(Organisasjonsnummer("976663934"))
        } returns Kontonummer("12345678910").right()
    }

    afterEach {
        database.truncateAll()
    }

    fun createSaga() = SendTilsagnsbrevSaga(
        db = database.api,
        dokarkClient = dokarkClient,
        personaliaService = personaliaService,
        pdf = pdfGenClient,
        arrangorMeldingSender = arrangorMeldingSender,
        kontoregister = kontoregister,
    )

    fun scheduledTaskNames(): List<String> = database.run {
        session.list(queryOf("select task_name from scheduled_tasks order by task_name")) { it.string("task_name") }
    }

    context("opprettInnhold") {
        test("genererer to pdf-varianter og skedulerer arkivering i joark og sending til altinn") {
            coEvery { pdfGenClient.getPdfDocument(any()) } returns "pdf".toByteArray().right()

            val saga = createSaga()

            saga.opprettInnhold(tilsagn.id).shouldBeRight()

            coVerify(exactly = 2) { pdfGenClient.getPdfDocument(any()) }
            scheduledTaskNames() shouldBe listOf(
                "SendTilsagnsbrevSaga-ArkiverIDokark",
                "SendTilsagnsbrevSaga-SendTilAltinn",
            )
        }

        test("er idempotent nar tilsagnet allerede er journalfort og sendt til altinn") {
            database.api.transaction {
                queries.tilsagn.setJournalpostId(tilsagn.id, "121212")
                queries.tilsagn.setAltinnCorrespondenceId(tilsagn.id, UUID.randomUUID().toString())
            }

            val saga = createSaga()

            saga.opprettInnhold(tilsagn.id).shouldBeRight()

            coVerify(exactly = 0) { pdfGenClient.getPdfDocument(any()) }
            scheduledTaskNames().shouldBeEmpty()
        }

        test("ruller tilbake og skedulerer ingenting nar pdf-generering feiler") {
            coEvery { pdfGenClient.getPdfDocument(any()) } returns PdfGenError(500, "Generering feilet").left()

            val saga = createSaga()

            saga.opprettInnhold(tilsagn.id).shouldBeLeft()

            scheduledTaskNames().shouldBeEmpty()
        }
    }

    context("arkiverIDokark") {
        fun taskData() = SendTilsagnsbrevSaga.ArkiverIDokarkTaskData(
            tilsagnId = tilsagn.id,
            pdfBase64 = Base64.getEncoder().encodeToString("pdf".toByteArray()),
            deltaker = NorskIdent("12345678901"),
            arrangorOrganisasjonsnummer = "976663934",
            arrangorNavn = "Underenhet 1 AS",
            fagsakId = "2025/11457",
            besluttetTidspunkt = LocalDateTime.of(2026, 3, 1, 12, 0, 0),
        )

        test("journalforer og setter journalpost_id") {
            coEvery { dokarkClient.opprettJournalpost(any(), any()) } returns DokarkResponse(
                journalpostId = "121212",
                journalstatus = "ok",
                melding = null,
                journalpostferdigstilt = true,
                dokumenter = emptyList(),
            ).right()

            val saga = createSaga()

            saga.arkiverIDokark(taskData()).shouldBeRight()

            database.run {
                queries.tilsagn.getOrError(tilsagn.id).tilsagnsbrev?.journalpostId shouldBe "121212"
            }
        }

        test("er idempotent nar tilsagnet allerede er journalfort") {
            database.api.transaction {
                queries.tilsagn.setJournalpostId(tilsagn.id, "121212")
            }

            val saga = createSaga()

            saga.arkiverIDokark(taskData()).shouldBeRight()

            coVerify(exactly = 0) { dokarkClient.opprettJournalpost(any(), any()) }
        }

        test("feiler nar dokark feiler") {
            coEvery { dokarkClient.opprettJournalpost(any(), any()) } returns DokarkError("Feilet").left()

            val saga = createSaga()

            saga.arkiverIDokark(taskData()).shouldBeLeft()

            database.run {
                queries.tilsagn.getOrError(tilsagn.id).tilsagnsbrev shouldBe null
            }
        }
    }

    context("sendTilAltinn") {
        fun taskData() = SendTilsagnsbrevSaga.SendTilAltinnTaskData(
            tilsagnId = tilsagn.id,
            pdfBase64 = Base64.getEncoder().encodeToString("pdf".toByteArray()),
            tiltakstypeNavn = "Enkel Amo",
            bestillingsnummer = tilsagn.bestillingsnummer,
            arrangorOrganisasjonsnummer = "976663934",
            arrangorNavn = "Underenhet 1 AS",
        )

        val correspondenceId = UUID.randomUUID()

        test("sender melding og lagrer altinn-referanse") {
            coEvery { arrangorMeldingSender.send(any()) } returns MeldingId(correspondenceId).right()

            val saga = createSaga()

            saga.sendTilAltinn(taskData()).shouldBeRight()

            database.run { queries.tilsagn.getOrError(tilsagn.id).tilsagnsbrev?.altinnCorrespondenceId } shouldBe correspondenceId.toString()
        }

        test("er idempotent nar tilsagnet allerede er sendt til altinn") {
            database.api.transaction {
                queries.tilsagn.setJournalpostId(tilsagn.id, "121212")
                queries.tilsagn.setAltinnCorrespondenceId(tilsagn.id, correspondenceId.toString())
            }

            val saga = createSaga()

            saga.sendTilAltinn(taskData()).shouldBeRight()

            coVerify(exactly = 0) { arrangorMeldingSender.send(any()) }
        }

        test("feiler nar sending av melding feiler") {
            coEvery { arrangorMeldingSender.send(any()) } returns MeldingError("Feil").left()

            val saga = createSaga()

            saga.sendTilAltinn(taskData()).shouldBeLeft()

            database.run { queries.tilsagn.getOrError(tilsagn.id).tilsagnsbrev?.altinnCorrespondenceId } shouldBe null
        }
    }

    context("schedule") {
        test("skedulerer opprett-innhold-steget nøyaktig én gang") {
            val saga = createSaga()

            saga.schedule(tilsagn.id)
            saga.schedule(tilsagn.id)

            database.run {
                session.list(queryOf("select task_name, task_instance from scheduled_tasks")) {
                    it.string("task_name") to it.string("task_instance")
                } shouldBe listOf("SendTilsagnsbrevSaga-OpprettInnhold" to tilsagn.id.toString())
            }
        }
    }
})
