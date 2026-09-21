package no.nav.mulighetsrommet.api.tilskuddbehandling.kafka

import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.mulighetsrommet.admin.arrangor.BetalingsinformasjonQuery
import no.nav.mulighetsrommet.api.contracts.totrinnskontroll.TotrinnskontrollAgent
import no.nav.mulighetsrommet.api.contracts.totrinnskontroll.TotrinnskontrollHendelse
import no.nav.mulighetsrommet.api.domain.arrangor.Betalingsinformasjon
import no.nav.mulighetsrommet.api.domain.opplaring.Opplaeringtilskudd
import no.nav.mulighetsrommet.api.domain.testing.fixture.NavAnsattFixture
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.TotrinnskontrollType
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.UtbetalingFixtures
import no.nav.mulighetsrommet.api.navansatt.service.NavAnsattService
import no.nav.mulighetsrommet.api.tilsagn.TilsagnService
import no.nav.mulighetsrommet.api.tilskuddbehandling.TilskuddBehandlingService
import no.nav.mulighetsrommet.api.tilskuddbehandling.db.TilskuddMottaker
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingRequest
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.VedtakResultat
import no.nav.mulighetsrommet.api.tilskuddbehandling.task.JournalforVedtaksbrev
import no.nav.mulighetsrommet.api.utbetaling.api.ValutaBelopRequest
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingLinjeStatus
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatusType
import no.nav.mulighetsrommet.api.utbetaling.service.UtbetalingService
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Valuta
import no.nav.tiltak.okonomi.Tilskuddstype
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class TilskuddArrangorUtbetalingConsumerTest : FunSpec({
    val database = extension(ApiDatabaseTestListener())

    val journalforVedtaksbrev = mockk<JournalforVedtaksbrev>()
    val betalingsinformasjon = mockk<BetalingsinformasjonQuery>()
    val navAnsattService = mockk<NavAnsattService>()

    beforeEach {
        MulighetsrommetTestDomain(
            ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
            gjennomforinger = listOf(GjennomforingFixtures.EnkelAmo),
        ).initialize(database.api)

        coEvery { betalingsinformasjon.execute(any()) } returns Betalingsinformasjon.BBan(
            Kontonummer("12345678901"),
            null,
        )
        coEvery { navAnsattService.getNavAnsattEnhet(any<NavIdent>(), any()) } returns NavEnhetNummer("0400")
    }

    afterEach {
        database.truncateAll()
    }

    val behandlingId = UUID.randomUUID()
    val tilskuddVedtakId = UUID.randomUUID()
    val tilskuddId = UUID.randomUUID()

    val request = TilskuddBehandlingRequest(
        id = behandlingId,
        gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
        tilskudd = listOf(
            TilskuddBehandlingRequest.TilskuddRequest(
                id = tilskuddVedtakId,
                tilskuddId = tilskuddId,
                tilskuddOpplaeringType = Opplaeringtilskudd.Kode.SKOLEPENGER,
                soknadJournalpostId = "J-2024-001",
                soknadDato = LocalDate.of(2024, 1, 15),
                periodeStart = "2025-01-01",
                periodeSlutt = "2025-07-01",
                kostnadssted = NavEnhetNummer("0502"),
                soknadBelop = ValutaBelopRequest(belop = 100, valuta = Valuta.NOK),
                vedtakResultat = VedtakResultat.INNVILGELSE,
                kommentarVedtaksbrev = null,
                utbetalingMottaker = TilskuddMottaker.ARRANGOR,
                kidNummer = "116",
                belop = 100,
                kommentarIntern = null,
            ),
        ),
    )

    val godkjentHendelse = TotrinnskontrollHendelse(
        id = UUID.randomUUID(),
        entityId = behandlingId,
        type = TotrinnskontrollType.TILSKUDD_OPPRETTELSE,
        status = TotrinnskontrollHendelse.Status.GODKJENT,
        behandletAv = TotrinnskontrollAgent.NavAnsatt(NavAnsattFixture.DonaldDuck.navIdent),
        behandletTidspunkt = Instant.now(),
        besluttetAv = TotrinnskontrollAgent.NavAnsatt(NavAnsattFixture.MikkeMus.navIdent),
        besluttetTidspunkt = Instant.now(),
        aarsaker = emptyList(),
        forklaring = null,
    )

    val gyldigTilsagnPeriode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2026, 1, 1))

    fun createConsumer(): TilskuddArrangorUtbetalingConsumer {
        val tilsagnService = TilsagnService(
            db = database.api,
            config = TilsagnService.Config(
                gyldigTilsagnPeriode = mapOf(Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING to gyldigTilsagnPeriode),
            ),
            navAnsattService = mockk(relaxed = true),
        )
        val utbetalingService = UtbetalingService(
            config = UtbetalingService.Config(
                tidligstTidspunktForUtbetaling = { _, _ -> null },
            ),
            tilsagnService = tilsagnService,
            betalingsinformasjon = betalingsinformasjon,
        )
        return TilskuddArrangorUtbetalingConsumer(
            db = database.api,
            utbetalingService = utbetalingService,
            tilsagnService = tilsagnService,
        )
    }

    test("oppretter utbetaling for innvilget tilskudd til arrangør") {
        val service = TilskuddBehandlingService(
            database.api,
            journalforVedtaksbrev,
            mockk(relaxed = true),
            navAnsattService,
        )

        service.upsert(request, NavAnsattFixture.DonaldDuck.navIdent).shouldBeRight()

        val consumer = createConsumer()
        consumer.consume(behandlingId, godkjentHendelse)

        database.run {
            val utbetaling = queries.utbetaling.getByGjennomforing(request.gjennomforingId).shouldHaveSize(1)[0]
            utbetaling.status shouldBe UtbetalingStatusType.FERDIG_BEHANDLET
            utbetaling.tilskuddstype shouldBe Tilskuddstype.TILTAK_OPPLAERING_TILSKUDD
            val linje = queries.utbetalingLinje.getByUtbetalingId(utbetaling.id).shouldHaveSize(1)[0]
            linje.status shouldBe UtbetalingLinjeStatus.OVERFORT_TIL_UTBETALING
        }
    }

    test("behandler ikke tilskudd to ganger hvis utbetaling allerede eksisterer") {
        val service = TilskuddBehandlingService(
            database.api,
            journalforVedtaksbrev,
            mockk(relaxed = true),
            navAnsattService,
        )
        service.upsert(request, NavAnsattFixture.DonaldDuck.navIdent).shouldBeRight()

        val consumer = createConsumer()
        consumer.consume(behandlingId, godkjentHendelse)
        consumer.consume(behandlingId, godkjentHendelse)

        database.run {
            queries.utbetaling.getByGjennomforing(request.gjennomforingId).shouldHaveSize(1)
        }
    }

    test("hopper over arrangorutbetaling når utbetaling allerede finnes") {
        val service = TilskuddBehandlingService(
            database.api,
            journalforVedtaksbrev,
            mockk(relaxed = true),
        )
        service.upsert(request, NavAnsattFixture.DonaldDuck.navIdent).shouldBeRight()

        val existingUtbetaling = UtbetalingFixtures.utbetaling1.copy(
            gjennomforingId = request.gjennomforingId,
        )
        database.api.transaction {
            queries.utbetaling.upsert(existingUtbetaling)
            queries.tilskuddBehandling.setUtbetalingTilskuddVedtak(tilskuddVedtakId, existingUtbetaling.id)
        }

        createConsumer().consume(behandlingId, godkjentHendelse)

        database.run {
            val utbetaling = queries.utbetaling.getByTilskuddVedtak(tilskuddVedtakId).shouldNotBeNull()
            utbetaling.id shouldBe existingUtbetaling.id
            queries.utbetaling.getByGjennomforing(request.gjennomforingId).shouldHaveSize(1)
        }
    }
})
