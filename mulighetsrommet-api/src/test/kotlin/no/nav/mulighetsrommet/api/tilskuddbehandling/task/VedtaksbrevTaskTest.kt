package no.nav.mulighetsrommet.api.tilskuddbehandling.task

import arrow.core.left
import arrow.core.right
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.clients.teamdokumenthandtering.DokarkClient
import no.nav.mulighetsrommet.api.clients.teamdokumenthandtering.DokarkResponse
import no.nav.mulighetsrommet.api.clients.teamdokumenthandtering.DokdistClient
import no.nav.mulighetsrommet.api.clients.teamdokumenthandtering.DokdistRequest
import no.nav.mulighetsrommet.api.clients.teamdokumenthandtering.DokdistResponse
import no.nav.mulighetsrommet.api.fixtures.DeltakerFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.api.pdfgen.PdfGenClient
import no.nav.mulighetsrommet.api.pdfgen.PdfGenError
import no.nav.mulighetsrommet.api.tilskuddbehandling.TilskuddBehandlingService
import no.nav.mulighetsrommet.api.tilskuddbehandling.db.TilskuddMottaker
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.Opplaeringtilskudd
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingRequest
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.VedtakResultat
import no.nav.mulighetsrommet.api.utbetaling.api.ValutaBelopRequest
import no.nav.mulighetsrommet.api.utbetaling.service.Gradering
import no.nav.mulighetsrommet.api.utbetaling.service.Personalia
import no.nav.mulighetsrommet.api.utbetaling.service.PersonaliaService
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.tokenprovider.AccessType
import java.time.LocalDate
import java.util.UUID

class VedtaksbrevTaskTest : FunSpec({
    val database = extension(ApiDatabaseTestListener())

    val behandlingId = UUID.randomUUID()
    val tilskuddId = UUID.randomUUID()
    val deltakerId = UUID.randomUUID()

    val personaliaService = mockk<PersonaliaService>()

    beforeEach {
        MulighetsrommetTestDomain(
            ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
            gjennomforinger = listOf(GjennomforingFixtures.EnkelAmo),
            deltakere = listOf(
                DeltakerFixtures.createDeltakerDbo(GjennomforingFixtures.EnkelAmo.id).copy(id = deltakerId),
            ),
        ).initialize(database.db)

        coEvery { personaliaService.getPersonalia(deltakerId, any()) } returns Personalia(
            deltakerId = deltakerId,
            norskIdent = NorskIdent("12345678901"),
            navn = "Test Testesen",
            oppfolgingEnhet = null,
            geografiskEnhet = null,
            region = null,
            gradering = Gradering.UGRADERT,
            avvistGrunn = null,
        )

        opprettOgAttesterTilskudd(database.db, behandlingId, tilskuddId)
    }

    afterEach {
        database.truncateAll()
    }

    test("journalforing setter journalpostid") {
        val pdfGenClient = mockk<PdfGenClient>()
        val dokarkClient = mockk<DokarkClient>()
        val distribuerVedtaksbrev = mockk<DistribuerVedtaksbrev>(relaxed = true)
        val pdfPayload = byteArrayOf(1)

        coEvery { pdfGenClient.getPdfVedtaksbrev(any()) } returns pdfPayload.right()
        coEvery { dokarkClient.opprettJournalpost(any(), any()) } returns DokarkResponse(
            journalpostId = "121212",
            journalstatus = "ok",
            melding = null,
            journalpostferdigstilt = true,
            dokumenter = emptyList(),
        ).right()

        val task = JournalforVedtaksbrev(
            db = database.db,
            dokarkClient = dokarkClient,
            personaliaService = personaliaService,
            pdf = pdfGenClient,
            distribuerVedtaksbrev = distribuerVedtaksbrev,
        )

        task.journalfor(behandlingId).shouldBeRight()

        database.run {
            queries.tilskuddBehandling.getOrError(behandlingId).vedtakJournalpostId shouldBe "121212"
        }
    }

    test("journalforing feiler nar pdfgen feiler") {
        val pdfGenClient = mockk<PdfGenClient>()
        val dokarkClient = mockk<DokarkClient>()

        coEvery { pdfGenClient.getPdfVedtaksbrev(any()) } returns PdfGenError(500, "").left()

        val task = JournalforVedtaksbrev(
            db = database.db,
            dokarkClient = dokarkClient,
            personaliaService = personaliaService,
            pdf = pdfGenClient,
            distribuerVedtaksbrev = mockk(relaxed = true),
        )

        task.journalfor(behandlingId).shouldBeLeft("Feil fra pdfgen: PdfGenError(statusCode=500, message=)")
    }

    test("distribuering sender journalpost til dokdist og lagrer bestillingsId") {
        val dokdistClient = mockk<DokdistClient>()

        database.db.transaction {
            queries.tilskuddBehandling.setJournalpostId(behandlingId, "121212")
        }

        coEvery {
            dokdistClient.distribuerJournalpost(
                journalpostId = "121212",
                accessType = AccessType.M2M,
                distribusjonstype = DokdistRequest.DistribusjonsType.VEDTAK,
                adresse = null,
                batchId = null,
            )
        } returns DokdistResponse(bestillingsId = "BEST-1").right()

        val task = DistribuerVedtaksbrev(
            db = database.db,
            dokdistClient = dokdistClient,
        )

        task.distribuerDok(behandlingId).shouldBeRight().bestillingsId shouldBe "BEST-1"

        database.assertTable("tilskudd_behandling")
            .row()
            .value("vedtak_journalpost_distribuering_id").isEqualTo("BEST-1")

        coVerify(exactly = 1) {
            dokdistClient.distribuerJournalpost(
                journalpostId = "121212",
                accessType = AccessType.M2M,
                distribusjonstype = DokdistRequest.DistribusjonsType.VEDTAK,
                adresse = null,
                batchId = null,
            )
        }
    }

    test("distribuering feiler nar vedtak mangler journalpost") {
        val task = DistribuerVedtaksbrev(
            db = database.db,
            dokdistClient = mockk(),
        )

        val error = kotlin.runCatching {
            task.distribuerDok(behandlingId)
        }.exceptionOrNull()

        error?.message shouldBe "Vedtak med id=$behandlingId har ingen journalpostId, distribuering ikke mulig"
    }
})

private fun opprettOgAttesterTilskudd(
    db: ApiDatabase,
    behandlingId: UUID,
    tilskuddId: UUID,
) {
    val request = TilskuddBehandlingRequest(
        id = behandlingId,
        gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
        soknadJournalpostId = "J-2024-001",
        soknadDato = LocalDate.of(2024, 1, 15),
        periodeStart = "2025-01-01",
        periodeSlutt = "2025-07-01",
        kostnadssted = NavEnhetNummer("0502"),
        kommentarIntern = null,
        tilskudd = listOf(
            TilskuddBehandlingRequest.TilskuddRequest(
                id = tilskuddId,
                tilskuddOpplaeringType = Opplaeringtilskudd.Kode.SKOLEPENGER,
                soknadBelop = ValutaBelopRequest(belop = 5000, valuta = Valuta.NOK),
                vedtakResultat = VedtakResultat.INNVILGELSE,
                kommentarVedtaksbrev = null,
                utbetalingMottaker = TilskuddMottaker.BRUKER,
                kidNummer = null,
                belop = 5000,
            ),
        ),
    )

    val service = TilskuddBehandlingService(
        db = db,
        journalforVedtaksbrev = mockk(relaxed = true),
        mockk(relaxed = true),
        mockk(relaxed = true),
    )

    service.upsert(request, NavAnsattFixture.DonaldDuck.navIdent).shouldBeRight()
    service.attester(request.id, NavAnsattFixture.MikkeMus.navIdent).shouldBeRight()
}
