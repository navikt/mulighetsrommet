package no.nav.mulighetsrommet.api.tilskuddbehandling

import arrow.core.left
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.mulighetsrommet.admin.journalpost.JournalpostValidator
import no.nav.mulighetsrommet.admin.totrinnskontroll.TotrinnskontrollDto
import no.nav.mulighetsrommet.api.clients.saf.SafClient
import no.nav.mulighetsrommet.api.clients.saf.SafError
import no.nav.mulighetsrommet.api.domain.opplaring.Opplaeringtilskudd
import no.nav.mulighetsrommet.api.domain.testing.fixture.AvtaleFixtures
import no.nav.mulighetsrommet.api.domain.testing.fixture.NavAnsattFixture
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.tilskuddbehandling.db.TilskuddMottaker
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingRequest
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingStatus
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingStatusAarsak
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.VedtakResultat
import no.nav.mulighetsrommet.api.utbetaling.api.ValutaBelopRequest
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.Valuta
import java.time.LocalDate
import java.util.UUID

class TilskuddBehandlingServiceTest : FunSpec({
    val database = extension(ApiDatabaseTestListener())

    val ansatt1 = NavAnsattFixture.DonaldDuck.navIdent
    val ansatt2 = NavAnsattFixture.MikkeMus.navIdent

    beforeEach {
        MulighetsrommetTestDomain(
            ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
            avtaler = listOf(AvtaleFixtures.AFT),
            gjennomforinger = listOf(GjennomforingFixtures.AFT1),
        ).initialize(database.api)
    }

    afterEach {
        database.truncateAll()
    }

    val request = TilskuddBehandlingRequest(
        id = UUID.randomUUID(),
        gjennomforingId = GjennomforingFixtures.AFT1.id,
        tilskudd = listOf(
            TilskuddBehandlingRequest.TilskuddRequest(
                id = UUID.randomUUID(),
                tilskuddId = UUID.randomUUID(),
                tilskuddOpplaeringType = Opplaeringtilskudd.Kode.SKOLEPENGER,
                soknadJournalpostId = "J-2024-001",
                soknadDato = LocalDate.of(2024, 1, 15),
                soknadBelop = ValutaBelopRequest(
                    belop = 12,
                    valuta = Valuta.SEK,
                ),
                vedtakResultat = VedtakResultat.INNVILGELSE,
                kommentarVedtaksbrev = null,
                utbetalingMottaker = TilskuddMottaker.ARRANGOR,
                kidNummer = "116",
                belop = 100,
                periodeStart = "2025-01-01",
                periodeSlutt = "2025-07-01",
                kostnadssted = NavEnhetNummer("0502"),
                kommentarIntern = "kommentar intern",
            ),
        ),
    )

    fun createService() = TilskuddBehandlingService(
        db = database.api,
        journalforVedtaksbrev = mockk(relaxed = true),
        pdf = mockk(relaxed = true),
        journalpostValidator = gyldigJournalpostValidator(),
        personaliaService = mockk(relaxed = true),
    )

    context("validering av journalpost") {
        test("upsert feiler når journalpost ikke er gyldig") {
            val saf = mockk<SafClient> {
                coEvery { hentJournalpost("J-2024-001", any()) } returns SafError.NotFound.left()
            }
            val service = TilskuddBehandlingService(
                db = database.api,
                journalforVedtaksbrev = mockk(relaxed = true),
                pdf = mockk(relaxed = true),
                journalpostValidator = JournalpostValidator(saf),
                personaliaService = mockk(relaxed = true),
            )

            service.upsert(request, ansatt1).shouldBeLeft().should {
                it shouldHaveSize 1
                it.first().pointer shouldBe "/tilskudd/0/soknadJournalpostId"
            }
        }
    }

    context("attester og returner") {
        test("kan ikke attestere sin egen behandling") {
            val service = createService()

            service.upsert(request, ansatt1).shouldBeRight()

            service.attester(request.id, ansatt1).shouldBeLeft().shouldHaveSize(1).first().should {
                it.detail shouldBe "Du kan ikke beslutte noe du selv har behandlet"
            }
        }

        test("annen ansatt kan attestere behandling") {
            val service = createService()

            service.upsert(request, ansatt1).shouldBeRight()

            service.attester(request.id, ansatt2).shouldBeRight()

            val detaljer = service.getDetaljerDto(request.id, ansatt1)
            detaljer?.behandling?.status?.type shouldBe TilskuddBehandlingStatus.FERDIG_BEHANDLET
        }

        test("happy case returner") {
            val service = createService()

            service.upsert(request, ansatt1).shouldBeRight()

            service.returner(
                request.id,
                ansatt2,
                listOf(TilskuddBehandlingStatusAarsak.FEIL_VEDTAKSRESULTAT, TilskuddBehandlingStatusAarsak.ANNET),
                begrunnelse = "fordi",
            ).shouldBeRight()

            service.getDetaljerDto(
                request.id,
                ansatt1,
            )?.opprettelse.shouldBeTypeOf<TotrinnskontrollDto.Besluttet>() should {
                it.besluttetAarsaker shouldBe listOf(
                    TilskuddBehandlingStatusAarsak.FEIL_VEDTAKSRESULTAT.name,
                    TilskuddBehandlingStatusAarsak.ANNET.name,
                )
                it.besluttetBegrunnelse shouldBe "fordi"
                it.beslutning shouldBe TotrinnskontrollDto.Beslutning.RETURNERT
                it.besluttetAv.navn shouldBe "Mikke Mus"
            }
        }
    }
})
