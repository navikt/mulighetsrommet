package no.nav.mulighetsrommet.api.tilskuddbehandling.kafka

import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.mulighetsrommet.api.brukerutbetaling.BrukerUtbetalingService
import no.nav.mulighetsrommet.api.clients.helved.HelVedUtbetaling
import no.nav.mulighetsrommet.api.fixtures.DeltakerFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.api.tilskuddbehandling.TilskuddBehandlingService
import no.nav.mulighetsrommet.api.tilskuddbehandling.db.TilskuddMottaker
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.Opplaeringtilskudd
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingRequest
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.VedtakResultat
import no.nav.mulighetsrommet.api.tilskuddbehandling.task.JournalforVedtaksbrev
import no.nav.mulighetsrommet.api.totrinnskontroll.model.TotrinnskontrollAgent
import no.nav.mulighetsrommet.api.totrinnskontroll.model.TotrinnskontrollHendelse
import no.nav.mulighetsrommet.api.totrinnskontroll.model.TotrinnskontrollType
import no.nav.mulighetsrommet.api.utbetaling.api.ValutaBelopRequest
import no.nav.mulighetsrommet.api.utbetaling.service.Gradering
import no.nav.mulighetsrommet.api.utbetaling.service.Personalia
import no.nav.mulighetsrommet.api.utbetaling.service.PersonaliaService
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Valuta
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class TilskuddBrukerUtbetalingConsumerTest : FunSpec({
    val database = extension(ApiDatabaseTestListener())

    val personaliaService = mockk<PersonaliaService>()
    val brukerUtbetalingService = mockk<BrukerUtbetalingService>(relaxed = true)
    val journalforVedtaksbrev = mockk<JournalforVedtaksbrev>(relaxed = true)

    val behandlingId = UUID.randomUUID()
    val tilskuddId = UUID.randomUUID()
    val deltakerId = UUID.randomUUID()

    beforeEach {
        clearMocks(brukerUtbetalingService)

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
    }

    afterEach {
        database.truncateAll()
    }

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

    val godkjentHendelse = TotrinnskontrollHendelse(
        id = UUID.randomUUID(),
        entityId = behandlingId,
        type = TotrinnskontrollType.TILSKUDD_OPPRETTELSE,
        behandletAv = TotrinnskontrollAgent.NavAnsatt(NavAnsattFixture.DonaldDuck.navIdent.value),
        behandletTidspunkt = Instant.now(),
        besluttetAv = TotrinnskontrollAgent.NavAnsatt(NavAnsattFixture.MikkeMus.navIdent.value),
        besluttetTidspunkt = Instant.now(),
        besluttelse = TotrinnskontrollHendelse.Besluttelse.GODKJENT,
        aarsaker = emptyList(),
        forklaring = null,
    )

    fun createConsumer() = TilskuddBrukerUtbetalingConsumer(
        db = database.db,
        personaliaService = personaliaService,
        brukerUtbetalingService = brukerUtbetalingService,
    )

    test("oppretter hel ved utbetaling for innvilget tilskudd til bruker") {
        val service = TilskuddBehandlingService(
            database.db,
            journalforVedtaksbrev,
            mockk(relaxed = true),
            mockk(relaxed = true),
        )
        service.upsert(request, NavAnsattFixture.DonaldDuck.navIdent).shouldBeRight()

        createConsumer().consume(behandlingId, Json.encodeToJsonElement(godkjentHendelse))

        val result = database.db.session { queries.brukerUtbetaling.getByTilskudd(tilskuddId) }

        result.shouldNotBeNull()
        result.belop shouldBe 5000
        result.tilskuddstype shouldBe HelVedUtbetaling.Tilskuddstype.SKOLEPENGER
        result.tiltakskode shouldBe HelVedUtbetaling.Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING
        result.saksbehandler shouldBe NavAnsattFixture.DonaldDuck.navIdent
        result.beslutter shouldBe NavAnsattFixture.MikkeMus.navIdent

        verify(exactly = 1) { brukerUtbetalingService.produceTilskuddUtbetaling(any()) }
    }

    test("behandler ikke tilskudd to ganger hvis utbetaling allerede eksisterer") {
        val service = TilskuddBehandlingService(
            database.db,
            journalforVedtaksbrev,
            mockk(relaxed = true),
            mockk(relaxed = true),
        )
        service.upsert(request, NavAnsattFixture.DonaldDuck.navIdent).shouldBeRight()

        val consumer = createConsumer()
        val hendelse = Json.encodeToJsonElement(godkjentHendelse)
        consumer.consume(behandlingId, hendelse)
        consumer.consume(behandlingId, hendelse)

        verify(exactly = 1) { brukerUtbetalingService.produceTilskuddUtbetaling(any()) }
    }
})
