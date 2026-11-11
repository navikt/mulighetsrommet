package no.nav.mulighetsrommet.api.tilskuddbehandling.kafka

import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.admin.arrangor.BetalingsinformasjonQuery
import no.nav.mulighetsrommet.api.brukerutbetaling.db.UpsertBrukerUtbetalingDbo
import no.nav.mulighetsrommet.api.contracts.helved.HelVedUtbetaling
import no.nav.mulighetsrommet.api.contracts.helved.HelVedUtbetaling.Periode
import no.nav.mulighetsrommet.api.contracts.totrinnskontroll.TotrinnskontrollAgent
import no.nav.mulighetsrommet.api.contracts.totrinnskontroll.TotrinnskontrollHendelse
import no.nav.mulighetsrommet.api.domain.opplaring.Opplaeringtilskudd
import no.nav.mulighetsrommet.api.domain.personalia.Gradering
import no.nav.mulighetsrommet.api.domain.personalia.Personalia
import no.nav.mulighetsrommet.api.domain.testing.fixture.DeltakerFixtures
import no.nav.mulighetsrommet.api.domain.testing.fixture.NavAnsattFixture
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.TotrinnskontrollType
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.persistence.testing.fixture.GjennomforingDboFixtures
import no.nav.mulighetsrommet.api.tilsagn.model.GyldigTilsagnPeriode
import no.nav.mulighetsrommet.api.tilskuddbehandling.TilskuddBehandlingService
import no.nav.mulighetsrommet.api.tilskuddbehandling.db.BrukerUtbetalingType
import no.nav.mulighetsrommet.api.tilskuddbehandling.db.TilskuddMottakerType
import no.nav.mulighetsrommet.api.tilskuddbehandling.gyldigJournalpostValidator
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingRequest
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddVedtakRequest
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.VedtakResultat
import no.nav.mulighetsrommet.api.tilskuddbehandling.task.JournalforVedtaksbrev
import no.nav.mulighetsrommet.api.utbetaling.api.ValutaBelopRequest
import no.nav.mulighetsrommet.api.utbetaling.service.PersonaliaService
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Valuta
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import no.nav.mulighetsrommet.model.Periode as ModelPeriode

class TilskuddBrukerUtbetalingConsumerTest : FunSpec({
    val database = extension(ApiDatabaseTestListener())

    val personaliaService = mockk<PersonaliaService>()
    val journalforVedtaksbrev = mockk<JournalforVedtaksbrev>(relaxed = true)
    val betalingsinformasjon = mockk<BetalingsinformasjonQuery>(relaxed = true)

    val behandlingId = UUID.randomUUID()
    val tilskuddVedtakId = UUID.randomUUID()
    val tilskuddId = UUID.randomUUID()
    val deltakerId = UUID.randomUUID()
    val deltakerNorskIdent = NorskIdent("12345678901")
    val utbetalingTopic = "test.tilskudd.utbetaling-v1"

    val gyldigTilsagnPeriode = GyldigTilsagnPeriode(
        Tiltakskode.entries.associateWith {
            ModelPeriode(LocalDate.of(2025, 1, 1), LocalDate.of(2027, 1, 1))
        },
    )

    beforeEach {
        MulighetsrommetTestDomain(
            ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
            gjennomforinger = listOf(GjennomforingDboFixtures.EnkelAmo),
            deltakere = listOf(
                DeltakerFixtures.createDeltaker(deltakerId, GjennomforingDboFixtures.EnkelAmo.id),
            ),
        ).initialize(database.api)

        coEvery { personaliaService.getPersonalia(deltakerId, any()) } returns Personalia(
            deltakerId = deltakerId,
            norskIdent = deltakerNorskIdent,
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
        gjennomforingId = GjennomforingDboFixtures.EnkelAmo.id,
        tilskuddVedtak = listOf(
            TilskuddVedtakRequest(
                id = tilskuddVedtakId,
                tilskuddId = tilskuddId,
                tilskuddOpplaeringType = Opplaeringtilskudd.Kode.SKOLEPENGER,
                soknadJournalpostId = "12345",
                soknadDato = LocalDate.of(2024, 1, 15),
                periodeStart = "2025-01-01",
                periodeSlutt = "2025-07-01",
                kostnadssted = NavEnhetNummer("0502"),
                soknadBelop = ValutaBelopRequest(belop = 5000, valuta = Valuta.NOK),
                vedtakResultat = VedtakResultat.INNVILGELSE,
                kommentarVedtaksbrev = null,
                utbetalingMottaker = TilskuddMottakerType.BRUKER,
                brukerUtbetalingType = BrukerUtbetalingType.REFUSJON,
                arrangorReferanse = null,
                kidNummer = null,
                fakturanummer = null,
                belop = 5000,
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
        behandletTidspunkt = Instant.parse("2025-03-14T10:00:00Z"),
        behandletBegrunnelse = null,
        behandletAarsaker = emptyList(),
        besluttetAv = TotrinnskontrollAgent.NavAnsatt(NavAnsattFixture.MikkeMus.navIdent),
        besluttetTidspunkt = Instant.parse("2025-03-15T10:00:00Z"),
        besluttetBegrunnelse = null,
        besluttetAarsaker = emptyList(),
    )

    fun createConsumer() = TilskuddBrukerUtbetalingConsumer(
        db = database.api,
        personaliaService = personaliaService,
    )

    fun createService() = TilskuddBehandlingService(
        gyldigTilsagnPeriode = gyldigTilsagnPeriode,
        database.api,
        journalforVedtaksbrev,
        mockk(relaxed = true),
        journalpostValidator = gyldigJournalpostValidator(),
        personaliaService = mockk(relaxed = true),
        betalingsinformasjon = betalingsinformasjon,
    )

    fun getHelVedUtbetalinger() = database.api.session {
        queries.kafkaProducerRecord.getRecords(10, listOf(utbetalingTopic))
    }

    fun seedBrukerUtbetaling(
        tilskuddVedtakId: UUID,
        belop: Int = 5000,
    ) = UpsertBrukerUtbetalingDbo(
        id = UUID.randomUUID(),
        sakId = "SAK-2025-001",
        belop = belop,
        transaksjonsDato = LocalDate.of(2025, 3, 15),
        tilskuddstype = HelVedUtbetaling.Tilskuddstype.SKOLEPENGER,
        tiltakskode = HelVedUtbetaling.Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING,
        saksbehandler = NavAnsattFixture.DonaldDuck.navIdent,
        beslutter = NavAnsattFixture.MikkeMus.navIdent,
        besluttetTidspunkt = Instant.parse("2025-03-15T10:00:00Z"),
        tilskuddVedtakId = tilskuddVedtakId,
    )

    test("oppretter hel ved utbetaling for innvilget tilskudd til bruker") {
        val service = createService()
        service.upsert(request, NavAnsattFixture.DonaldDuck.navIdent).shouldBeRight()

        createConsumer().consume(behandlingId, godkjentHendelse)

        val result = database.api.session { queries.brukerUtbetaling.getByTilskuddVedtak(tilskuddVedtakId) }

        result.shouldNotBeNull()
        result.belop shouldBe 5000
        result.tilskuddstype shouldBe HelVedUtbetaling.Tilskuddstype.SKOLEPENGER
        result.tiltakskode shouldBe HelVedUtbetaling.Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING
        result.saksbehandler shouldBe NavAnsattFixture.DonaldDuck.navIdent
        result.beslutter shouldBe NavAnsattFixture.MikkeMus.navIdent

        val record = getHelVedUtbetalinger().single()
        record.key.decodeToString() shouldBe result.id.toString()
        Json.decodeFromString<HelVedUtbetaling>(record.value!!.decodeToString()) shouldBe HelVedUtbetaling(
            id = result.id,
            sakId = result.sakId,
            behandlingId = "1",
            personIdent = deltakerNorskIdent,
            periode = Periode(LocalDate.of(2025, 3, 15), LocalDate.of(2025, 3, 15)),
            belop = 5000,
            kostnadssted = NavEnhetNummer("0502"),
            tilskuddstype = HelVedUtbetaling.Tilskuddstype.SKOLEPENGER,
            tiltakskode = HelVedUtbetaling.Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING,
            saksbehandler = NavAnsattFixture.DonaldDuck.navIdent,
            beslutter = NavAnsattFixture.MikkeMus.navIdent,
            besluttetTidspunkt = Instant.parse("2025-03-15T10:00:00Z"),
            dryrun = false,
        )
    }

    test("oppretter ikke utbetaling når norsk ident mangler") {
        createService().upsert(request, NavAnsattFixture.DonaldDuck.navIdent).shouldBeRight()
        coEvery { personaliaService.getPersonalia(deltakerId, any()) } returns mockk {
            every { norskIdent() } returns null
        }

        val error = shouldThrow<IllegalStateException> { createConsumer().consume(behandlingId, godkjentHendelse) }

        error.message shouldBe "Fant ikke norsk ident for deltaker med id=$deltakerId, behandlingId=$behandlingId"
        database.api.session { queries.brukerUtbetaling.getByTilskuddVedtak(tilskuddVedtakId) }.shouldBeNull()
        getHelVedUtbetalinger().shouldBeEmpty()
    }

    test("behandler ikke tilskudd to ganger hvis utbetaling allerede eksisterer") {
        val service = createService()
        service.upsert(request, NavAnsattFixture.DonaldDuck.navIdent).shouldBeRight()

        val consumer = createConsumer()
        consumer.consume(behandlingId, godkjentHendelse)
        val record = getHelVedUtbetalinger().single()
        database.api.session { queries.kafkaProducerRecord.deleteRecords(listOf(record.id)) }
        consumer.consume(behandlingId, godkjentHendelse)

        getHelVedUtbetalinger().shouldBeEmpty()
        database.api.session { queries.brukerUtbetaling.getByTilskuddVedtak(tilskuddVedtakId) }
            .shouldNotBeNull().id.toString() shouldBe record.key.decodeToString()
    }

    test("hopper over brukerutbetaling når utbetaling allerede finnes") {
        val service = createService()
        service.upsert(request, NavAnsattFixture.DonaldDuck.navIdent).shouldBeRight()

        database.api.transaction {
            queries.brukerUtbetaling.insert(seedBrukerUtbetaling(tilskuddVedtakId))
        }

        createConsumer().consume(behandlingId, godkjentHendelse)

        val result = database.api.session { queries.brukerUtbetaling.getByTilskuddVedtak(tilskuddVedtakId) }
        result.shouldNotBeNull()
        getHelVedUtbetalinger().shouldBeEmpty()
    }

    test("hopper over brukerutbetaling når revurdering allerede finnes") {
        val service = createService()
        service.upsert(request, NavAnsattFixture.DonaldDuck.navIdent).shouldBeRight()

        val revurderingBehandlingId = service
            .revurderingOpphor(tilskuddVedtakId, behandlingId, NavAnsattFixture.DonaldDuck.navIdent)
            .shouldBeRight()

        val revurderingBehandling = database.api.session {
            queries.tilskuddBehandling.get(revurderingBehandlingId)
        }.shouldNotBeNull()
        val revurderingTilskuddVedtakId = revurderingBehandling.tilskuddVedtak.single().id

        database.api.transaction {
            queries.brukerUtbetaling.insert(
                seedBrukerUtbetaling(revurderingTilskuddVedtakId, belop = 0),
            )
        }

        createConsumer().consume(
            revurderingBehandlingId,
            godkjentHendelse.copy(
                entityId = revurderingBehandlingId,
                type = TotrinnskontrollType.TILSKUDD_OPPHOR,
            ),
        )

        val result = database.api.session { queries.brukerUtbetaling.getByTilskuddVedtak(revurderingTilskuddVedtakId) }
        result.shouldNotBeNull()
        getHelVedUtbetalinger().shouldBeEmpty()
    }

    test("bruker besluttet tidspunkt som periode for utbetaling") {
        val service = createService()
        service.upsert(request, NavAnsattFixture.DonaldDuck.navIdent).shouldBeRight()

        val besluttetTidspunkt = Instant.parse("2025-03-15T10:00:00Z")
        val hendelse = godkjentHendelse.copy(besluttetTidspunkt = besluttetTidspunkt)
        createConsumer().consume(behandlingId, hendelse)

        val utbetaling =
            Json.decodeFromString<HelVedUtbetaling>(getHelVedUtbetalinger().single().value!!.decodeToString())
        utbetaling.periode shouldBe Periode(LocalDate.of(2025, 3, 15), LocalDate.of(2025, 3, 15))
        utbetaling.besluttetTidspunkt shouldBe Instant.parse("2025-03-15T10:00:00Z")
    }

    test("håndterer opphørsrevurdering av tilskudd lagres som ny utbetaling og sender hel ved utbetaling") {
        val service = createService()
        val consumer = createConsumer()
        service.upsert(request, NavAnsattFixture.DonaldDuck.navIdent).shouldBeRight()

        val besluttetTidspunkt = Instant.parse("2025-03-15T10:00:00Z")
        consumer.consume(behandlingId, godkjentHendelse.copy(besluttetTidspunkt = besluttetTidspunkt))

        val forsteUtbetaling = database.api.session {
            queries.brukerUtbetaling.getByTilskuddVedtak(tilskuddVedtakId)
        }.shouldNotBeNull()

        val revurderingBehandlingId = service
            .revurderingOpphor(tilskuddVedtakId, behandlingId, NavAnsattFixture.DonaldDuck.navIdent)
            .shouldBeRight()

        val revurderingHendelse = godkjentHendelse.copy(
            entityId = revurderingBehandlingId,
            type = TotrinnskontrollType.TILSKUDD_OPPHOR,
            besluttetTidspunkt = besluttetTidspunkt,
        )

        consumer.consume(revurderingBehandlingId, revurderingHendelse)
        consumer.consume(revurderingBehandlingId, revurderingHendelse)

        val result = database.api.session { queries.brukerUtbetaling.getLastFromTilskudd(tilskuddId) }
        result.shouldNotBeNull()
        result.id shouldBe forsteUtbetaling.id
        result.transaksjonsDato shouldBe forsteUtbetaling.transaksjonsDato

        result.belop shouldBe 0
        result.behandlingId shouldNotBe forsteUtbetaling.behandlingId

        val records = getHelVedUtbetalinger().shouldHaveSize(2)
        val utbetaling = Json.decodeFromString<HelVedUtbetaling>(records.last().value!!.decodeToString())
        records.last().key.decodeToString() shouldBe forsteUtbetaling.id.toString()
        utbetaling.id shouldBe forsteUtbetaling.id
        utbetaling.behandlingId shouldBe "2"
        utbetaling.belop shouldBe 0
        utbetaling.periode shouldBe Periode(LocalDate.of(2025, 3, 15), LocalDate.of(2025, 3, 15))
    }
})
