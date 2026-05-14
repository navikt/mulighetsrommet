package no.nav.mulighetsrommet.api.tilskuddbehandling

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.spyk
import no.nav.mulighetsrommet.api.TransactionalQueryContext
import no.nav.mulighetsrommet.api.arrangor.ArrangorService
import no.nav.mulighetsrommet.api.arrangor.model.Betalingsinformasjon
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.api.navansatt.service.NavAnsattService
import no.nav.mulighetsrommet.api.tilsagn.TilsagnService
import no.nav.mulighetsrommet.api.tilskuddbehandling.db.TilskuddMottaker
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingRequest
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingStatus
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingStatusAarsak
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddOpplaeringType
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.VedtakResultat
import no.nav.mulighetsrommet.api.totrinnskontroll.TotrinnskontrollService
import no.nav.mulighetsrommet.api.totrinnskontroll.api.TotrinnskontrollDto
import no.nav.mulighetsrommet.api.totrinnskontroll.model.TotrinnskontrollBesluttelse
import no.nav.mulighetsrommet.api.utbetaling.api.ValutaBelopRequest
import no.nav.mulighetsrommet.api.utbetaling.model.AutomatiskUtbetalingResult
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingLinjeStatus
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatusType
import no.nav.mulighetsrommet.api.utbetaling.service.UtbetalingService
import no.nav.mulighetsrommet.api.utbetaling.task.JournalforUtbetaling
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Valuta
import java.time.LocalDate
import java.util.UUID

private const val BESTILLING_TOPIC = "bestilling-topic"
private const val TOTRINNSKONTROLL_TOPIC = "totrinnskontroll-topic"

class TilskuddBehandlingServiceTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

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

    val requestTilArrangor = TilskuddBehandlingRequest(
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
                tilskuddOpplaeringType = TilskuddOpplaeringType.SKOLEPENGER,
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

    val requestTilBruker = TilskuddBehandlingRequest(
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
                tilskuddOpplaeringType = TilskuddOpplaeringType.SKOLEPENGER,
                soknadBelop = ValutaBelopRequest(
                    belop = 12,
                    valuta = Valuta.SEK,
                ),
                vedtakResultat = VedtakResultat.INNVILGELSE,
                kommentarVedtaksbrev = null,
                utbetalingMottaker = TilskuddMottaker.BRUKER,
                kidNummer = null,
                belop = 100,
            ),
        ),
    )

    val arrangorService = mockk<ArrangorService>()

    val gyldigTilsagnPeriode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2026, 1, 1))

    fun createTilsagnService(
        navAnsattService: NavAnsattService = mockk(relaxed = true),
    ): TilsagnService {
        return TilsagnService(
            db = database.db,
            config = TilsagnService.Config(
                bestillingTopic = BESTILLING_TOPIC,
                gyldigTilsagnPeriode = mapOf(
                    Tiltakskode.ARBEIDSFORBEREDENDE_TRENING to gyldigTilsagnPeriode,
                    Tiltakskode.ARBEIDSRETTET_REHABILITERING to gyldigTilsagnPeriode,
                    Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING to gyldigTilsagnPeriode,
                ),
            ),
            navAnsattService = navAnsattService,
            totrinnskontroll = TotrinnskontrollService(TOTRINNSKONTROLL_TOPIC),
        )
    }

    fun createUtbetalingService(
        tilsagnService: TilsagnService,
        journalforUtbetaling: JournalforUtbetaling = mockk(relaxed = true),
    ) = UtbetalingService(
        config = UtbetalingService.Config(
            bestillingTopic = BESTILLING_TOPIC,
            tidligstTidspunktForUtbetaling = { _, _ -> null },
        ),
        db = database.db,
        tilsagnService = tilsagnService,
        journalforUtbetaling = journalforUtbetaling,
        arrangorService = arrangorService,
        totrinnskontroll = TotrinnskontrollService(TOTRINNSKONTROLL_TOPIC),
    )

    fun createService(
        tilsagnService: TilsagnService? = null,
        utbetalingService: UtbetalingService? = null,
    ) = run {
        val tilsagnService = tilsagnService ?: createTilsagnService()
        TilskuddBehandlingService(
            database.db,
            TotrinnskontrollService(""),
            tilsagnService = tilsagnService,
            utbetalingService = utbetalingService ?: createUtbetalingService(tilsagnService),
        )
    }

    context("attester og returner") {
        coEvery { arrangorService.getBetalingsinformasjon(any()) } returns Betalingsinformasjon.BBan(
            Kontonummer("12345678901"),
            null,
        )

        test("kan ikke attestere sin egen behandling") {
            val service = createService()

            service.upsert(requestTilArrangor, ansatt1).shouldBeRight()

            service.godkjenn(requestTilArrangor.id, ansatt1).shouldBeLeft().shouldHaveSize(1).first().should {
                it.detail shouldBe "Du kan ikke beslutte noe du selv har behandlet"
            }
        }

        test("annen ansatt kan attestere behandling") {
            val service = createService()

            service.upsert(requestTilArrangor, ansatt1).shouldBeRight()

            service.godkjenn(requestTilArrangor.id, ansatt2).shouldBeRight()

            val detaljer = service.getDetaljerDto(requestTilArrangor.id, ansatt1)
            detaljer?.behandling?.status?.type shouldBe TilskuddBehandlingStatus.FERDIG_BEHANDLET
        }

        test("happy case returner") {
            val service = createService()

            service.upsert(requestTilArrangor, ansatt1).shouldBeRight()

            service.returner(
                requestTilArrangor.id,
                ansatt2,
                listOf(TilskuddBehandlingStatusAarsak.FEIL_VEDTAKSRESULTAT, TilskuddBehandlingStatusAarsak.ANNET),
                forklaring = "fordi",
            ).shouldBeRight()

            service.getDetaljerDto(requestTilArrangor.id, ansatt1)?.opprettelse.shouldBeTypeOf<TotrinnskontrollDto.Besluttet>() should {
                it.aarsaker shouldBe listOf(TilskuddBehandlingStatusAarsak.FEIL_VEDTAKSRESULTAT, TilskuddBehandlingStatusAarsak.ANNET).map { it.name }
                it.forklaring shouldBe "fordi"
                it.besluttelse shouldBe TotrinnskontrollBesluttelse.AVVIST
                it.besluttetAv.navn shouldBe "Mikke Mus"
            }
        }
    }

    context("automatisk generering av utbetaling") {
        coEvery { arrangorService.getBetalingsinformasjon(any()) } returns Betalingsinformasjon.BBan(
            Kontonummer("12345678901"),
            null,
        )

        test("attestering av utbetaling til arrangør oppretter tilsagn og faktura") {
            val service = createService()

            service.upsert(requestTilArrangor, ansatt1).shouldBeRight()

            service.godkjenn(requestTilArrangor.id, ansatt2).shouldBeRight()

            database.run {
                val utbetaling = queries.utbetaling.getByGjennomforing(requestTilArrangor.gjennomforingId)
                    .shouldHaveSize(1).first()
                utbetaling.status shouldBe UtbetalingStatusType.FERDIG_BEHANDLET

                val linje = queries.utbetalingLinje.getByUtbetalingId(utbetaling.id)
                    .shouldHaveSize(1).first()

                linje.gjorOppTilsagn shouldBe true
                linje.status shouldBe UtbetalingLinjeStatus.OVERFORT_TIL_UTBETALING

                queries.kafkaProducerRecord.getRecords(10, listOf(BESTILLING_TOPIC))
                    .shouldHaveSize(2)
            }
        }

        test("attestering av utbetaling til bruker er ikke implementert ennå") {
            val service = createService()

            service.upsert(requestTilBruker, ansatt1).shouldBeRight()

            service.godkjenn(requestTilBruker.id, ansatt2).shouldBeLeft()

            service.getDetaljerDto(requestTilBruker.id, ansatt1)
                ?.behandling?.status?.type shouldBe TilskuddBehandlingStatus.TIL_ATTESTERING
        }

        test("hvis automatisk utbetaling ikke er feilfri lages det ikke tilsagn og attestering feiler") {
            val tilsagnService = createTilsagnService()
            val utbetalingService = spyk(createUtbetalingService(tilsagnService))
            coEvery {
                with(any<TransactionalQueryContext>()) {
                    utbetalingService.automatiskUtbetaling(any())
                }
            } answers {
                AutomatiskUtbetalingResult.IKKE_NOK_PENGER
            }

            val service = createService(tilsagnService, utbetalingService)

            service.upsert(requestTilArrangor, ansatt1).shouldBeRight()

            service.godkjenn(requestTilArrangor.id, ansatt2).shouldBeLeft()

            service.getDetaljerDto(requestTilArrangor.id, ansatt1)
                ?.behandling?.status?.type shouldBe TilskuddBehandlingStatus.TIL_ATTESTERING

            database.run {
                queries.tilsagn.getAll().shouldHaveSize(0)

                queries.kafkaProducerRecord.getRecords(10, listOf(BESTILLING_TOPIC))
                    .shouldHaveSize(0)
            }
        }
    }
})
