package no.nav.mulighetsrommet.api.tilskuddbehandling

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.mockk
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.api.tilskuddbehandling.db.TilskuddMottaker
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.Opplaeringtilskudd
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingRequest
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingStatus
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingStatusAarsak
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.VedtakResultat
import no.nav.mulighetsrommet.api.totrinnskontroll.api.TotrinnskontrollDto
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
        ).initialize(database.db)
    }

    afterEach {
        database.truncateAll()
    }

    val request = TilskuddBehandlingRequest(
        id = UUID.randomUUID(),
        gjennomforingId = GjennomforingFixtures.AFT1.id,
        soknadJournalpostId = "J-2024-001",
        soknadDato = LocalDate.of(2024, 1, 15),
        periodeStart = "2025-01-01",
        periodeSlutt = "2025-07-01",
        kostnadssted = NavEnhetNummer("0502"),
        kommentarIntern = "kommentar intern",
        tilskudd = listOf(
            TilskuddBehandlingRequest.TilskuddRequest(
                id = UUID.randomUUID(),
                tilskuddOpplaeringType = Opplaeringtilskudd.Kode.SKOLEPENGER,
                soknadBelop = ValutaBelopRequest(
                    belop = 12,
                    valuta = Valuta.SEK,
                ),
                vedtakResultat = VedtakResultat.INNVILGELSE,
                kommentarVedtaksbrev = null,
                utbetalingMottaker = TilskuddMottaker.ARRANGOR,
                kidNummer = "116",
                belop = 100,
            ),
        ),
    )

    fun createService() = TilskuddBehandlingService(
        database.db,
        mockk(relaxed = true),
    )

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
                forklaring = "fordi",
            ).shouldBeRight()

            service.getDetaljerDto(request.id, ansatt1)?.opprettelse.shouldBeTypeOf<TotrinnskontrollDto.Besluttet>() should {
                it.aarsaker shouldBe listOf(TilskuddBehandlingStatusAarsak.FEIL_VEDTAKSRESULTAT, TilskuddBehandlingStatusAarsak.ANNET).map { it.name }
                it.forklaring shouldBe "fordi"
                it.beslutning shouldBe TotrinnskontrollDto.Beslutning.RETURNERT
                it.besluttetAv.navn shouldBe "Mikke Mus"
            }
        }
    }
})
