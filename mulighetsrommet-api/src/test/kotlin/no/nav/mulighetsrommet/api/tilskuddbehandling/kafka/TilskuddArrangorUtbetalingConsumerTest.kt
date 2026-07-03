package no.nav.mulighetsrommet.api.tilskuddbehandling.kafka

import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.mulighetsrommet.api.arrangor.ArrangorService
import no.nav.mulighetsrommet.api.arrangor.model.Betalingsinformasjon
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.api.tilsagn.TilsagnService
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
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingLinjeStatus
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatusType
import no.nav.mulighetsrommet.api.utbetaling.service.UtbetalingService
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.NavEnhetNummer
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
    val arrangorService = mockk<ArrangorService>()

    beforeEach {
        MulighetsrommetTestDomain(
            ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
            gjennomforinger = listOf(GjennomforingFixtures.EnkelAmo),
        ).initialize(database.db)

        coEvery { arrangorService.getBetalingsinformasjon(any()) } returns Betalingsinformasjon.BBan(
            Kontonummer("12345678901"),
            null,
        )
    }

    afterEach {
        database.truncateAll()
    }

    val behandlingId = UUID.randomUUID()
    val tilskuddId = UUID.randomUUID()

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
                soknadBelop = ValutaBelopRequest(belop = 100, valuta = Valuta.NOK),
                vedtakResultat = VedtakResultat.INNVILGELSE,
                kommentarVedtaksbrev = null,
                utbetalingMottaker = TilskuddMottaker.ARRANGOR,
                kidNummer = "116",
                belop = 100,
            ),
        ),
    )

    val godkjentHendelse = TotrinnskontrollHendelse(
        id = UUID.randomUUID(),
        entityId = behandlingId,
        type = TotrinnskontrollType.TILSKUDD_OPPRETTELSE,
        status = TotrinnskontrollHendelse.Status.GODKJENT,
        behandletAv = TotrinnskontrollAgent.NavAnsatt(NavAnsattFixture.DonaldDuck.navIdent.value),
        behandletTidspunkt = Instant.now(),
        besluttetAv = TotrinnskontrollAgent.NavAnsatt(NavAnsattFixture.MikkeMus.navIdent.value),
        besluttetTidspunkt = Instant.now(),
        besluttelse = TotrinnskontrollHendelse.Besluttelse.GODKJENT,
        aarsaker = emptyList(),
        forklaring = null,
    )

    val gyldigTilsagnPeriode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2026, 1, 1))

    fun createConsumer(): TilskuddArrangorUtbetalingConsumer {
        val tilsagnService = TilsagnService(
            db = database.db,
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
            arrangorService = arrangorService,
        )
        return TilskuddArrangorUtbetalingConsumer(
            db = database.db,
            utbetalingService = utbetalingService,
            tilsagnService = tilsagnService,
        )
    }

    test("oppretter utbetaling for innvilget tilskudd til arrangør") {
        val service = TilskuddBehandlingService(
            database.db,
            journalforVedtaksbrev,
            mockk(relaxed = true),
            mockk(relaxed = true),
        )
        service.upsert(request, NavAnsattFixture.DonaldDuck.navIdent).shouldBeRight()

        val consumer = createConsumer()
        consumer.consume(behandlingId, Json.encodeToJsonElement(godkjentHendelse))

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

        database.run {
            queries.utbetaling.getByGjennomforing(request.gjennomforingId).shouldHaveSize(1)
        }
    }
})
