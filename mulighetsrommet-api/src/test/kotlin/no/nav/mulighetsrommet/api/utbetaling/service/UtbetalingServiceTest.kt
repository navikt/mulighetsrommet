package no.nav.mulighetsrommet.api.utbetaling.service

import arrow.core.left
import arrow.core.nel
import arrow.core.right
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.serialization.json.Json
import no.nav.common.kafka.util.KafkaUtils
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.TransactionalQueryContext
import no.nav.mulighetsrommet.api.arrangor.ArrangorService
import no.nav.mulighetsrommet.api.arrangor.model.Betalingsinformasjon
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.endringshistorikk.DocumentClass
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Innlandet
import no.nav.mulighetsrommet.api.fixtures.TilsagnFixtures.Tilsagn1
import no.nav.mulighetsrommet.api.fixtures.TilsagnFixtures.Tilsagn2
import no.nav.mulighetsrommet.api.fixtures.UtbetalingFixtures.utbetaling1
import no.nav.mulighetsrommet.api.fixtures.UtbetalingFixtures.utbetaling2
import no.nav.mulighetsrommet.api.fixtures.UtbetalingFixtures.utbetalingLinje1
import no.nav.mulighetsrommet.api.fixtures.UtbetalingFixtures.utbetalingLinje2
import no.nav.mulighetsrommet.api.fixtures.setTilsagnStatus
import no.nav.mulighetsrommet.api.fixtures.setUtbetalingLinjeStatus
import no.nav.mulighetsrommet.api.navansatt.db.NavAnsattDbo
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsattRolle
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.tilsagn.TilsagnService
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningFri
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Besluttelse
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
import no.nav.mulighetsrommet.api.utbetaling.api.OpprettUtbetalingLinjerRequest
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingLinjeRequest
import no.nav.mulighetsrommet.api.utbetaling.api.ValutaBelopRequest
import no.nav.mulighetsrommet.api.utbetaling.model.AutomatiskUtbetalingResult
import no.nav.mulighetsrommet.api.utbetaling.model.SatsPeriode
import no.nav.mulighetsrommet.api.utbetaling.model.UpsertUtbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFastSatsPerTiltaksplassPerManed
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFri
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingLinjeReturnertAarsak
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingLinjeStatus
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatusType
import no.nav.mulighetsrommet.api.utbetaling.task.JournalforUtbetaling
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.kafka.KAFKA_CONSUMER_RECORD_PROCESSOR_SCHEDULED_AT
import no.nav.mulighetsrommet.model.Arrangor
import no.nav.mulighetsrommet.model.JournalpostId
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltaksadministrasjon
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.model.ValutaBelop
import no.nav.mulighetsrommet.model.withValuta
import no.nav.tiltak.okonomi.FakturaStatusType
import no.nav.tiltak.okonomi.OkonomiBestillingMelding
import no.nav.tiltak.okonomi.Tilskuddstype
import no.nav.tiltak.okonomi.toOkonomiPart
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

class UtbetalingServiceTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    afterEach {
        database.truncateAll()
    }

    var umiddelbarUtbetaling = TidligstTidspunktForUtbetalingCalculator { _, _ -> null }
    val arrangorService: ArrangorService = mockk()

    fun createTilsagnService(): TilsagnService = TilsagnService(
        TilsagnService.Config("bestilling-topic", mapOf()),
        db = database.db,
        navAnsattService = mockk(),
        personaliaService = mockk(relaxed = true),
    )

    fun createUtbetalingService(
        tilsagnService: TilsagnService = createTilsagnService(),
        journalforUtbetaling: JournalforUtbetaling = mockk(relaxed = true),
        tidligstTidspunktForUtbetaling: TidligstTidspunktForUtbetalingCalculator = umiddelbarUtbetaling,
    ) = UtbetalingService(
        config = UtbetalingService.Config(
            bestillingTopic = "bestilling-topic",
            tidligstTidspunktForUtbetaling = tidligstTidspunktForUtbetaling,
        ),
        db = database.db,
        tilsagnService = tilsagnService,
        journalforUtbetaling = journalforUtbetaling,
        arrangorService = arrangorService,
    )

    context("opprett og rediger utbetaling") {
        val upsert = UpsertUtbetaling.Anskaffelse(
            id = UUID.randomUUID(),
            gjennomforingId = AFT1.id,
            periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
            journalpostId = JournalpostId("123123123"),
            kid = null,
            beregning = UtbetalingBeregningFri.from(10.withValuta(Valuta.NOK)),
            kommentar = "Arrangør trenger penger",
            tilskuddstype = Tilskuddstype.TILTAK_DRIFTSTILSKUDD,
            vedlegg = listOf(),
        )

        beforeEach {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                utbetalinger = listOf(utbetaling1.copy(status = UtbetalingStatusType.GENERERT)),
            ).initialize(database.db)

            coEvery { arrangorService.getBetalingsinformasjon(any()) } returns Betalingsinformasjon.BBan(
                kontonummer = Kontonummer("12345678901"),
                kid = null,
            )
        }

        test("utbetaling blir opprettet med fri-beregning") {
            val service = createUtbetalingService()

            val utbetaling = service.opprettUtbetaling(
                opprett = upsert,
                agent = NavAnsattFixture.DonaldDuck.navIdent,
            ).shouldBeRight()

            utbetaling.id.shouldNotBeNull()
            utbetaling.periode shouldBe Periode.forMonthOf(LocalDate.of(2025, 1, 1))
            utbetaling.beregning shouldBe upsert.beregning
        }

        test("samme utbetaling kan ikke opprettes to ganger") {
            val service = createUtbetalingService()

            service.opprettUtbetaling(
                opprett = upsert,
                agent = Arrangor,
            ).shouldBeRight()

            service.opprettUtbetaling(
                opprett = upsert,
                agent = Arrangor,
            ) shouldBeLeft listOf(
                FieldError.of("Utbetalingen er allerede opprettet"),
            )
        }

        test("utbetaling blir ikke journalført når den blir opprettet av Nav-ansatt") {
            val journalforUtbetaling = mockk<JournalforUtbetaling>(relaxed = true)

            val service = createUtbetalingService(journalforUtbetaling = journalforUtbetaling)

            service.opprettUtbetaling(
                opprett = upsert,
                agent = NavAnsattFixture.DonaldDuck.navIdent,
            ).shouldBeRight().status shouldBe UtbetalingStatusType.TIL_BEHANDLING

            verify(exactly = 0) { journalforUtbetaling.schedule(any(), any(), any(), any()) }
        }

        test("utbetaling blir journalført når den blir opprettet av Arrangør") {
            val journalforUtbetaling = mockk<JournalforUtbetaling>(relaxed = true)

            val service = createUtbetalingService(journalforUtbetaling = journalforUtbetaling)

            val utbetaling = service.opprettUtbetaling(
                opprett = upsert,
                agent = Arrangor,
            ).shouldBeRight()

            verify(exactly = 1) { journalforUtbetaling.schedule(utbetaling.id, any(), any(), any()) }
        }

        test("kan redigeres når den er til behandling") {
            val service = createUtbetalingService()

            service.opprettUtbetaling(
                opprett = upsert,
                agent = NavAnsattFixture.DonaldDuck.navIdent,
            ).shouldBeRight().status shouldBe UtbetalingStatusType.TIL_BEHANDLING

            val kommentar = "Arrangør trenger mer penger"
            val beregning = UtbetalingBeregningFri.from(ValutaBelop(100, Valuta.NOK))
            service.redigerUtbetaling(
                rediger = upsert.copy(kommentar = kommentar, beregning = beregning),
                agent = NavAnsattFixture.DonaldDuck.navIdent,
            ).shouldBeRight().should {
                it.kommentar shouldBe kommentar
                it.beregning shouldBe beregning
            }
        }

        test("kan ikke redigeres når den er innsendt av arrangør") {
            val service = createUtbetalingService()

            service.opprettUtbetaling(
                opprett = upsert,
                agent = Arrangor,
            ).shouldBeRight().status shouldBe UtbetalingStatusType.TIL_BEHANDLING

            service.redigerUtbetaling(
                rediger = upsert,
                agent = NavAnsattFixture.DonaldDuck.navIdent,
            ) shouldBeLeft listOf(
                FieldError.of("Utbetalingen kan ikke redigeres"),
            )
        }

        test("kan ikke redigeres når den er generert") {
            val service = createUtbetalingService()

            service.redigerUtbetaling(
                rediger = upsert.copy(id = utbetaling1.id),
                agent = NavAnsattFixture.DonaldDuck.navIdent,
            ) shouldBeLeft listOf(
                FieldError.of("Utbetalingen kan ikke redigeres"),
            )
        }
    }

    context("opprett og rediger korreksjon") {
        val upsert = UpsertUtbetaling.Korreksjon(
            id = UUID.randomUUID(),
            periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
            korreksjonGjelderUtbetalingId = utbetaling1.id,
            korreksjonBegrunnelse = "Feilutbetaling",
            kid = null,
            beregning = UtbetalingBeregningFri.from(10.withValuta(Valuta.NOK)),
            kommentar = null,
            tilskuddstype = Tilskuddstype.TILTAK_DRIFTSTILSKUDD,
        )

        beforeEach {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                utbetalinger = listOf(
                    utbetaling1.copy(status = UtbetalingStatusType.FERDIG_BEHANDLET),
                    utbetaling2.copy(status = UtbetalingStatusType.GENERERT),
                ),
            ).initialize(database.db)

            coEvery { arrangorService.getBetalingsinformasjon(any()) } returns Betalingsinformasjon.BBan(
                kontonummer = Kontonummer("12345678901"),
                kid = null,
            )
        }

        test("korreksjon må gjelde for en eksisterende utbetaling") {
            val service = createUtbetalingService()

            service.opprettUtbetaling(
                opprett = upsert.copy(korreksjonGjelderUtbetalingId = UUID.randomUUID()),
                agent = NavAnsattFixture.DonaldDuck.navIdent,
            ) shouldBeLeft listOf(
                FieldError.of("Utbetaling som skal korrigeres eksisterer ikke"),
            )
        }

        test("korreksjon kan ikke opprettes for genererte utbetalinger") {
            val service = createUtbetalingService()

            service.opprettUtbetaling(
                opprett = upsert.copy(korreksjonGjelderUtbetalingId = utbetaling2.id),
                agent = NavAnsattFixture.DonaldDuck.navIdent,
            ) shouldBeLeft listOf(
                FieldError.of("Utbetaling kan ikke korrigeres når den har status GENERERT"),
            )
        }

        test("korreksjon kan opprettes og redigeres") {
            val service = createUtbetalingService()

            service.opprettUtbetaling(
                opprett = upsert,
                agent = NavAnsattFixture.DonaldDuck.navIdent,
            ).shouldBeRight().korreksjon shouldBe Utbetaling.Korreksjon(
                gjelderUtbetalingId = utbetaling1.id,
                begrunnelse = "Feilutbetaling",
            )

            service.redigerUtbetaling(
                rediger = upsert.copy(korreksjonBegrunnelse = "Fordi"),
                agent = NavAnsattFixture.DonaldDuck.navIdent,
            ).shouldBeRight().korreksjon shouldBe Utbetaling.Korreksjon(
                gjelderUtbetalingId = utbetaling1.id,
                begrunnelse = "Fordi",
            )
        }
    }

    context("når utbetaling blir behandlet") {
        test("skal ikke kunne beslutte utbetalingslinje når ansatt mangler attestant-rolle") {
            val domain = MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
                utbetalinger = listOf(utbetaling1.copy(status = UtbetalingStatusType.TIL_ATTESTERING)),
                utbetalingLinjer = listOf(utbetalingLinje1),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT)
                setUtbetalingLinjeStatus(utbetalingLinje1, UtbetalingLinjeStatus.TIL_ATTESTERING)
            }.initialize(database.db)

            val service = createUtbetalingService()

            service.godkjennUtbetalingLinje(
                id = utbetalingLinje1.id,
                navIdent = domain.ansatte[1].navIdent,
            ) shouldBeLeft listOf(
                FieldError.of("Kan ikke attestere utbetalingen fordi du ikke er attestant ved tilsagnets kostnadssted (Nav Innlandet)"),
            )
        }

        test("kan ikke beslutte egen utbetaling") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
                utbetalinger = listOf(utbetaling1.copy(status = UtbetalingStatusType.TIL_BEHANDLING)),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT)
                setRoller(
                    NavAnsattFixture.DonaldDuck,
                    setOf(NavAnsattRolle.kontorspesifikk(Rolle.ATTESTANT_UTBETALING, setOf(Innlandet.enhetsnummer))),
                )
            }.initialize(database.db)

            val service = createUtbetalingService()
            val linje = UtbetalingLinjeRequest(
                id = UUID.randomUUID(),
                tilsagnId = Tilsagn1.id,
                gjorOppTilsagn = false,
                pris = 100.withValuta(Valuta.NOK).toRequest(),
            )
            val opprettRequest = OpprettUtbetalingLinjerRequest(
                utbetalingId = utbetaling1.id,
                utbetalingLinjer = listOf(linje),
                begrunnelseMindreBetalt = "begrunnelse",
            )
            service.opprettUtbetalingLinjer(
                request = opprettRequest,
                navIdent = NavAnsattFixture.DonaldDuck.navIdent,
            ).shouldBeRight()
            service.godkjennUtbetalingLinje(
                id = linje.id,
                navIdent = NavAnsattFixture.DonaldDuck.navIdent,
            ) shouldBeLeft listOf(
                FieldError.of("Kan ikke attestere en utbetaling du selv har opprettet"),
            )
        }

        test("kan beslutte utbetaling når man har besluttet tilsagnet") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
                utbetalinger = listOf(utbetaling1.copy(status = UtbetalingStatusType.TIL_BEHANDLING)),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT, besluttetAv = NavAnsattFixture.DonaldDuck.navIdent)
                setRoller(
                    NavAnsattFixture.DonaldDuck,
                    setOf(NavAnsattRolle.kontorspesifikk(Rolle.ATTESTANT_UTBETALING, setOf(Innlandet.enhetsnummer))),
                )
            }.initialize(database.db)

            val service = createUtbetalingService()
            val linje = UtbetalingLinjeRequest(
                id = UUID.randomUUID(),
                tilsagnId = Tilsagn1.id,
                gjorOppTilsagn = false,
                pris = 100.withValuta(Valuta.NOK).toRequest(),
            )
            val opprettRequest = OpprettUtbetalingLinjerRequest(
                utbetalingId = utbetaling1.id,
                utbetalingLinjer = listOf(linje),
                begrunnelseMindreBetalt = "begrunnelse",
            )
            service.opprettUtbetalingLinjer(
                request = opprettRequest,
                navIdent = NavAnsattFixture.MikkeMus.navIdent,
            ).shouldBeRight()
            service.godkjennUtbetalingLinje(
                id = linje.id,
                navIdent = NavAnsattFixture.DonaldDuck.navIdent,
            ).shouldBeRight().status shouldBe UtbetalingStatusType.FERDIG_BEHANDLET
        }

        test("returnering av utbetalingslinje setter den i RETURNERT status") {
            val domain = MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
                utbetalinger = listOf(utbetaling1.copy(status = UtbetalingStatusType.TIL_BEHANDLING)),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT)
                setRoller(
                    NavAnsattFixture.DonaldDuck,
                    setOf(NavAnsattRolle.kontorspesifikk(Rolle.ATTESTANT_UTBETALING, setOf(Innlandet.enhetsnummer))),
                )
            }.initialize(database.db)

            val service = createUtbetalingService()
            val linje = UtbetalingLinjeRequest(
                id = UUID.randomUUID(),
                tilsagnId = Tilsagn1.id,
                gjorOppTilsagn = false,
                pris = 100.withValuta(Valuta.NOK).toRequest(),
            )
            val opprettRequest = OpprettUtbetalingLinjerRequest(
                utbetalingId = utbetaling1.id,
                utbetalingLinjer = listOf(linje),
                begrunnelseMindreBetalt = "begrunnelse",
            )
            service.opprettUtbetalingLinjer(
                request = opprettRequest,
                navIdent = domain.ansatte[1].navIdent,
            ).shouldBeRight()
            service.returnerUtbetalingLinje(
                id = linje.id,
                aarsaker = listOf(UtbetalingLinjeReturnertAarsak.ANNET),
                forklaring = "Maksbeløp er 5",
                navIdent = domain.ansatte[0].navIdent,
            ).shouldBeRight().status shouldBe UtbetalingStatusType.RETURNERT

            database.run {
                queries.utbetalingLinje.getOrError(linje.id).status shouldBe UtbetalingLinjeStatus.RETURNERT
            }
        }

        test("sletting av utbetalingslinje skjer ikke ved valideringsfeil") {
            val domain = MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
                utbetalinger = listOf(utbetaling1.copy(status = UtbetalingStatusType.TIL_BEHANDLING)),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT)
                setRoller(
                    NavAnsattFixture.MikkeMus,
                    setOf(NavAnsattRolle.kontorspesifikk(Rolle.ATTESTANT_UTBETALING, setOf(Innlandet.enhetsnummer))),
                )
            }.initialize(database.db)

            val service = createUtbetalingService()
            val linje = UtbetalingLinjeRequest(
                id = UUID.randomUUID(),
                tilsagnId = Tilsagn1.id,
                gjorOppTilsagn = false,
                pris = 100.withValuta(Valuta.NOK).toRequest(),
            )
            val opprettRequest = OpprettUtbetalingLinjerRequest(
                utbetalingId = utbetaling1.id,
                utbetalingLinjer = listOf(linje),
                begrunnelseMindreBetalt = "begrunnelse",
            )
            service.opprettUtbetalingLinjer(
                request = opprettRequest,
                navIdent = domain.ansatte[0].navIdent,
            ).shouldBeRight()

            service.returnerUtbetalingLinje(
                id = linje.id,
                aarsaker = listOf(UtbetalingLinjeReturnertAarsak.ANNET),
                forklaring = "Maksbeløp er 5",
                navIdent = domain.ansatte[1].navIdent,
            ).shouldBeRight().status shouldBe UtbetalingStatusType.RETURNERT

            service.opprettUtbetalingLinjer(
                request = OpprettUtbetalingLinjerRequest(utbetaling1.id, emptyList(), "begrunnelse"),
                navIdent = domain.ansatte[0].navIdent,
            ) shouldBeLeft listOf(
                FieldError.of("Utbetalingslinjer mangler"),
            )

            database.run {
                queries.utbetalingLinje.getOrError(linje.id).status shouldBe UtbetalingLinjeStatus.RETURNERT
            }
        }

        test("skal ikke kunne godkjenne utbetalingslinje hvis den er allerede godkjent") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
                utbetalinger = listOf(utbetaling1.copy(status = UtbetalingStatusType.TIL_BEHANDLING)),
                utbetalingLinjer = listOf(utbetalingLinje1),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT)
                setUtbetalingLinjeStatus(utbetalingLinje1, UtbetalingLinjeStatus.GODKJENT)
            }.initialize(database.db)

            val service = createUtbetalingService()

            service.godkjennUtbetalingLinje(
                id = utbetalingLinje1.id,
                navIdent = NavAnsattFixture.MikkeMus.navIdent,
            ) shouldBeLeft listOf(
                FieldError.of("Utbetaling er ikke satt til attestering"),
                FieldError.of("Kan ikke attestere utbetalingen fordi du ikke er attestant ved tilsagnets kostnadssted (Nav Innlandet)"),
            )
        }

        test("oppdatering av returnert utbetalingslinje setter status TIL_ATTESTERING") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
                utbetalinger = listOf(utbetaling1.copy(status = UtbetalingStatusType.TIL_BEHANDLING)),
                utbetalingLinjer = listOf(utbetalingLinje1),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT)
                setUtbetalingLinjeStatus(utbetalingLinje1, UtbetalingLinjeStatus.RETURNERT)
            }.initialize(database.db)

            val service = createUtbetalingService()
            service.opprettUtbetalingLinjer(
                request = OpprettUtbetalingLinjerRequest(
                    utbetalingId = utbetaling1.id,
                    utbetalingLinjer = listOf(
                        UtbetalingLinjeRequest(
                            utbetalingLinje1.id,
                            Tilsagn1.id,
                            gjorOppTilsagn = false,
                            pris = 100.withValuta(Valuta.NOK).toRequest(),
                        ),
                    ),
                    begrunnelseMindreBetalt = "begrunnelse",
                ),
                navIdent = NavAnsattFixture.DonaldDuck.navIdent,
            ).shouldBeRight()

            database.run {
                queries.utbetalingLinje.getOrError(utbetalingLinje1.id)
                    .status shouldBe UtbetalingLinjeStatus.TIL_ATTESTERING
                queries.utbetaling.getOrError(utbetalingLinje1.utbetalingId)
                    .status shouldBe UtbetalingStatusType.TIL_ATTESTERING
            }
        }

        test("skal bare kunne opprette utbetalingslinje når utbetalingsperiode og tilsagnsperiode overlapper") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
                utbetalinger = listOf(
                    utbetaling1.copy(
                        periode = Periode.forMonthOf(LocalDate.of(2023, 4, 4)),
                        status = UtbetalingStatusType.TIL_BEHANDLING,
                    ),
                ),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT)
            }.initialize(database.db)

            val service = createUtbetalingService()

            val request = OpprettUtbetalingLinjerRequest(
                utbetalingId = utbetaling1.id,
                utbetalingLinjer = listOf(
                    UtbetalingLinjeRequest(
                        UUID.randomUUID(),
                        Tilsagn1.id,
                        gjorOppTilsagn = false,
                        pris = 100.withValuta(Valuta.NOK).toRequest(),
                    ),
                ),
                begrunnelseMindreBetalt = "begrunnelse",
            )

            shouldThrow<IllegalArgumentException> {
                service.opprettUtbetalingLinjer(
                    request,
                    NavAnsattFixture.DonaldDuck.navIdent,
                )
            }.message shouldBe "Utbetalingsperiode og tilsagnsperiode overlapper ikke"
        }

        test("ved ny innsending til godkjenning skal utbetalingLinjer som ikke er inkludert i forespørselen slettes fra databasen") {
            val tilsagn1 = Tilsagn1.copy(
                periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
            )
            val tilsagn2 = Tilsagn2.copy(
                periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
            )
            val utbetaling = utbetaling1.copy(
                periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                beregning = UtbetalingBeregningFri(
                    input = UtbetalingBeregningFri.Input(10.withValuta(Valuta.NOK)),
                    output = UtbetalingBeregningFri.Output(10.withValuta(Valuta.NOK)),
                ),
                status = UtbetalingStatusType.TIL_BEHANDLING,
            )

            val domain = MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(tilsagn1, tilsagn2),
                utbetalinger = listOf(utbetaling),
            ) {
                setTilsagnStatus(tilsagn1, TilsagnStatus.GODKJENT)
                setTilsagnStatus(tilsagn2, TilsagnStatus.GODKJENT)
                setRoller(
                    NavAnsattFixture.MikkeMus,
                    setOf(NavAnsattRolle.kontorspesifikk(Rolle.ATTESTANT_UTBETALING, setOf(Innlandet.enhetsnummer))),
                )
            }.initialize(database.db)
            val service = createUtbetalingService()

            val utbetalingLinje1 = UtbetalingLinjeRequest(
                UUID.randomUUID(),
                tilsagn1.id,
                gjorOppTilsagn = false,
                pris = 5.withValuta(Valuta.NOK).toRequest(),
            )
            val utbetalingLinje2 = UtbetalingLinjeRequest(
                UUID.randomUUID(),
                tilsagn2.id,
                gjorOppTilsagn = false,
                pris = 5.withValuta(Valuta.NOK).toRequest(),
            )
            service.opprettUtbetalingLinjer(
                OpprettUtbetalingLinjerRequest(utbetaling.id, listOf(utbetalingLinje1, utbetalingLinje2), null),
                domain.ansatte[0].navIdent,
            ).shouldBeRight()

            service.returnerUtbetalingLinje(
                id = utbetalingLinje1.id,
                aarsaker = listOf(UtbetalingLinjeReturnertAarsak.FEIL_BELOP),
                forklaring = null,
                navIdent = domain.ansatte[1].navIdent,
            ).shouldBeRight().status shouldBe UtbetalingStatusType.RETURNERT

            service.opprettUtbetalingLinjer(
                OpprettUtbetalingLinjerRequest(utbetaling.id, listOf(utbetalingLinje1), "begrunnelse"),
                domain.ansatte[0].navIdent,
            ).shouldBeRight()

            val utbetalingLinjer = database.run { queries.utbetalingLinje.getByUtbetalingId(utbetaling.id) }
            utbetalingLinjer.size shouldBe 1
            utbetalingLinjer[0].id shouldBe utbetalingLinje1.id
        }

        test("alle utbetalingLinjer (selv godkjente) blir returnert når saksbehandler avviser en utbetalingslinje") {
            val tilsagn1 = Tilsagn1.copy(
                periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
            )
            val tilsagn2 = Tilsagn2.copy(
                periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
            )
            val utbetaling = utbetaling1.copy(
                periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                beregning = UtbetalingBeregningFri(
                    input = UtbetalingBeregningFri.Input(10.withValuta(Valuta.NOK)),
                    output = UtbetalingBeregningFri.Output(10.withValuta(Valuta.NOK)),
                ),
                status = UtbetalingStatusType.TIL_BEHANDLING,
            )

            val domain = MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(tilsagn1, tilsagn2),
                utbetalinger = listOf(utbetaling),
            ) {
                setTilsagnStatus(tilsagn1, TilsagnStatus.GODKJENT)
                setTilsagnStatus(tilsagn2, TilsagnStatus.GODKJENT)
                setRoller(
                    NavAnsattFixture.DonaldDuck,
                    setOf(NavAnsattRolle.kontorspesifikk(Rolle.ATTESTANT_UTBETALING, setOf(Innlandet.enhetsnummer))),
                )
            }.initialize(database.db)
            val service = createUtbetalingService()

            val utbetalingLinje1 = UtbetalingLinjeRequest(
                UUID.randomUUID(),
                tilsagn1.id,
                gjorOppTilsagn = false,
                pris = 5.withValuta(Valuta.NOK).toRequest(),
            )
            val utbetalingLinje2 = UtbetalingLinjeRequest(
                UUID.randomUUID(),
                tilsagn2.id,
                gjorOppTilsagn = false,
                pris = 5.withValuta(Valuta.NOK).toRequest(),
            )
            service.opprettUtbetalingLinjer(
                OpprettUtbetalingLinjerRequest(utbetaling.id, listOf(utbetalingLinje1, utbetalingLinje2), null),
                domain.ansatte[1].navIdent,
            ).shouldBeRight()

            service.godkjennUtbetalingLinje(
                utbetalingLinje1.id,
                domain.ansatte[0].navIdent,
            ).shouldBeRight().status shouldBe UtbetalingStatusType.TIL_ATTESTERING

            database.run {
                queries.utbetalingLinje.getOrError(utbetalingLinje1.id).status shouldBe UtbetalingLinjeStatus.GODKJENT
            }

            service.returnerUtbetalingLinje(
                id = utbetalingLinje2.id,
                aarsaker = listOf(UtbetalingLinjeReturnertAarsak.ANNET),
                forklaring = "Maksbeløp er 5",
                navIdent = domain.ansatte[0].navIdent,
            ).shouldBeRight().status shouldBe UtbetalingStatusType.RETURNERT

            database.run {
                queries.utbetalingLinje.getOrError(utbetalingLinje1.id).status shouldBe UtbetalingLinjeStatus.RETURNERT
                queries.totrinnskontroll.getOrError(utbetalingLinje1.id, Totrinnskontroll.Type.OPPRETT).should {
                    it.besluttelse shouldBe Besluttelse.AVVIST
                    it.besluttetAv shouldBe Tiltaksadministrasjon
                }

                queries.utbetalingLinje.getOrError(utbetalingLinje1.id).status shouldBe UtbetalingLinjeStatus.RETURNERT
                queries.totrinnskontroll.getOrError(utbetalingLinje2.id, Totrinnskontroll.Type.OPPRETT).should {
                    it.besluttelse shouldBe Besluttelse.AVVIST
                    it.besluttetAv shouldBe domain.ansatte[0].navIdent
                }
            }
        }

        test("løpenummer, fakturanummer og periode blir utledet fra tilsagnet og utbetalingen") {
            val tilsagn1 = Tilsagn2.copy(
                periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                bestillingsnummer = "A-2025/1-1",
            )

            val tilsagn2 = Tilsagn1.copy(
                periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                bestillingsnummer = "A-2025/1-2",
            )

            val utbetaling1 = utbetaling1.copy(
                periode = Periode(LocalDate.of(2023, 12, 15), LocalDate.of(2025, 1, 15)),
            )

            val utbetaling2 = utbetaling2.copy(
                periode = Periode(LocalDate.of(2025, 1, 15), LocalDate.of(2025, 2, 15)),
            )

            val domain = MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(tilsagn1, tilsagn2),
                utbetalinger = listOf(
                    utbetaling1.copy(status = UtbetalingStatusType.TIL_BEHANDLING),
                    utbetaling2.copy(status = UtbetalingStatusType.TIL_BEHANDLING),
                ),
            ) {
                setTilsagnStatus(tilsagn1, TilsagnStatus.GODKJENT)
                setTilsagnStatus(tilsagn2, TilsagnStatus.GODKJENT)
            }.initialize(database.db)

            val service = createUtbetalingService()

            service.opprettUtbetalingLinjer(
                OpprettUtbetalingLinjerRequest(
                    utbetaling1.id,
                    listOf(
                        UtbetalingLinjeRequest(
                            UUID.randomUUID(),
                            tilsagn1.id,
                            gjorOppTilsagn = false,
                            pris = 50.withValuta(Valuta.NOK).toRequest(),
                        ),
                        UtbetalingLinjeRequest(
                            UUID.randomUUID(),
                            tilsagn2.id,
                            gjorOppTilsagn = false,
                            pris = 50.withValuta(Valuta.NOK).toRequest(),
                        ),
                    ),
                    begrunnelseMindreBetalt = "begrunnelse",
                ),
                domain.ansatte[0].navIdent,
            ).shouldBeRight()

            service.opprettUtbetalingLinjer(
                OpprettUtbetalingLinjerRequest(
                    utbetaling2.id,
                    listOf(
                        UtbetalingLinjeRequest(
                            UUID.randomUUID(),
                            tilsagn1.id,
                            gjorOppTilsagn = false,
                            pris = 100.withValuta(Valuta.NOK).toRequest(),
                        ),
                    ),
                    begrunnelseMindreBetalt = "begrunnelse",
                ),
                domain.ansatte[0].navIdent,
            ).shouldBeRight()

            database.run {
                queries.utbetalingLinje.getByUtbetalingId(utbetaling1.id).should { (first, second) ->
                    first.pris shouldBe 50.withValuta(Valuta.NOK)
                    first.periode shouldBe Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 15))
                    first.lopenummer shouldBe 1
                    first.faktura.fakturanummer shouldBe "A-2025/1-1-1"

                    second.pris shouldBe 50.withValuta(Valuta.NOK)
                    second.periode shouldBe Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 15))
                    second.lopenummer shouldBe 1
                    second.faktura.fakturanummer shouldBe "A-2025/1-2-1"
                }

                queries.utbetalingLinje.getByUtbetalingId(utbetaling2.id).should { (first) ->
                    first.pris shouldBe 100.withValuta(Valuta.NOK)
                    first.lopenummer shouldBe 2
                    first.faktura.fakturanummer shouldBe "A-2025/1-1-2"
                    first.periode shouldBe Periode(LocalDate.of(2025, 1, 15), LocalDate.of(2025, 2, 1))
                }
            }
        }

        test("løpenummer og fakturanummer beholdes ved returnering og godkjenning av utbetalingslinje for samme tilsagn") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
                utbetalinger = listOf(utbetaling1.copy(status = UtbetalingStatusType.TIL_BEHANDLING)),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT)
                setRoller(
                    NavAnsattFixture.MikkeMus,
                    setOf(NavAnsattRolle.kontorspesifikk(Rolle.ATTESTANT_UTBETALING, setOf(Innlandet.enhetsnummer))),
                )
            }.initialize(database.db)

            val service = createUtbetalingService()

            val utbetalingLinje1 = UtbetalingLinjeRequest(
                UUID.randomUUID(),
                Tilsagn1.id,
                gjorOppTilsagn = false,
                pris = 5.withValuta(Valuta.NOK).toRequest(),
            )
            service.opprettUtbetalingLinjer(
                OpprettUtbetalingLinjerRequest(utbetaling1.id, listOf(utbetalingLinje1), "begrunnelse"),
                NavAnsattFixture.DonaldDuck.navIdent,
            ).shouldBeRight()

            database.run {
                queries.utbetalingLinje.getOrError(utbetalingLinje1.id).should {
                    it.id shouldBe utbetalingLinje1.id
                    it.lopenummer shouldBe 1
                    it.faktura.fakturanummer shouldBe "A-2025/1-1-1"
                }
            }

            service.returnerUtbetalingLinje(
                id = utbetalingLinje1.id,
                aarsaker = listOf(UtbetalingLinjeReturnertAarsak.ANNET),
                forklaring = "Maksbeløp er 5",
                navIdent = NavAnsattFixture.MikkeMus.navIdent,
            ).shouldBeRight().status shouldBe UtbetalingStatusType.RETURNERT

            service.opprettUtbetalingLinjer(
                OpprettUtbetalingLinjerRequest(utbetaling1.id, listOf(utbetalingLinje1), "begrunnelse"),
                NavAnsattFixture.DonaldDuck.navIdent,
            ).shouldBeRight()

            database.run {
                queries.utbetalingLinje.getOrError(utbetalingLinje1.id).should {
                    it.id shouldBe utbetalingLinje1.id
                    it.lopenummer shouldBe 1
                    it.faktura.fakturanummer shouldBe "A-2025/1-1-1"
                }
            }

            service.returnerUtbetalingLinje(
                id = utbetalingLinje1.id,
                aarsaker = listOf(UtbetalingLinjeReturnertAarsak.ANNET),
                forklaring = "Maksbeløp er 5",
                navIdent = NavAnsattFixture.MikkeMus.navIdent,
            ).shouldBeRight().status shouldBe UtbetalingStatusType.RETURNERT

            val utbetalingLinje2 = UtbetalingLinjeRequest(
                UUID.randomUUID(),
                Tilsagn1.id,
                gjorOppTilsagn = false,
                pris = 5.withValuta(Valuta.NOK).toRequest(),
            )
            service.opprettUtbetalingLinjer(
                OpprettUtbetalingLinjerRequest(utbetaling1.id, listOf(utbetalingLinje2), "begrunnelse"),
                NavAnsattFixture.DonaldDuck.navIdent,
            ).shouldBeRight()

            database.run {
                queries.utbetalingLinje.getOrError(utbetalingLinje2.id).should {
                    it.id shouldBe utbetalingLinje2.id
                    it.lopenummer shouldBe 1
                    it.faktura.fakturanummer shouldBe "A-2025/1-1-1"
                }
            }
        }

        test("alle utbetalingLinjer blir returnert hvis tilsagn ikke har godkjent-status når utbetalingslinje blir forsøkt godkjent") {
            val tilsagn1 = Tilsagn1.copy(
                periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                bestillingsnummer = "A-2025/1-1",
            )

            val tilsagn2 = Tilsagn2.copy(
                periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                bestillingsnummer = "A-2025/1-2",
            )

            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(tilsagn1, tilsagn2),
                utbetalinger = listOf(utbetaling1.copy(status = UtbetalingStatusType.TIL_ATTESTERING)),
                utbetalingLinjer = listOf(utbetalingLinje1, utbetalingLinje2),
            ) {
                setTilsagnStatus(tilsagn1, TilsagnStatus.GODKJENT)
                setTilsagnStatus(tilsagn2, TilsagnStatus.OPPGJORT)
                setUtbetalingLinjeStatus(utbetalingLinje1, UtbetalingLinjeStatus.TIL_ATTESTERING)
                setUtbetalingLinjeStatus(utbetalingLinje2, UtbetalingLinjeStatus.TIL_ATTESTERING)
                setRoller(
                    NavAnsattFixture.MikkeMus,
                    setOf(NavAnsattRolle.kontorspesifikk(Rolle.ATTESTANT_UTBETALING, setOf(Innlandet.enhetsnummer))),
                )
            }.initialize(database.db)

            val service = createUtbetalingService()

            service.godkjennUtbetalingLinje(
                id = utbetalingLinje1.id,
                navIdent = NavAnsattFixture.MikkeMus.navIdent,
            ).shouldBeRight().status shouldBe UtbetalingStatusType.RETURNERT

            database.run {
                queries.utbetalingLinje.getByUtbetalingId(utbetaling1.id).should { (first, second) ->
                    first.id shouldBe utbetalingLinje2.id
                    first.status shouldBe UtbetalingLinjeStatus.RETURNERT

                    second.id shouldBe utbetalingLinje1.id
                    second.status shouldBe UtbetalingLinjeStatus.RETURNERT
                }

                queries.totrinnskontroll.getOrError(utbetalingLinje1.id, Totrinnskontroll.Type.OPPRETT).should {
                    it.besluttetAv shouldBe Tiltaksadministrasjon
                    it.besluttelse shouldBe Besluttelse.AVVIST
                }

                queries.totrinnskontroll.getOrError(utbetalingLinje2.id, Totrinnskontroll.Type.OPPRETT).should {
                    it.besluttetAv shouldBe Tiltaksadministrasjon
                    it.besluttelse shouldBe Besluttelse.AVVIST
                }

                queries.kafkaProducerRecord.getRecords(10).shouldBeEmpty()
            }
        }

        test("tilsagn blir oppgjort når utbetaling benytter resten av tilsagnsbeløpet") {
            val tilsagn = Tilsagn1.copy(
                periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 3, 1)),
                beregning = getTilsagnBeregning(pris = 10.withValuta(Valuta.NOK)),
            )

            val utbetaling = utbetaling1.copy(
                periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                beregning = UtbetalingBeregningFri(
                    input = UtbetalingBeregningFri.Input(10.withValuta(Valuta.NOK)),
                    output = UtbetalingBeregningFri.Output(10.withValuta(Valuta.NOK)),
                ),
                status = UtbetalingStatusType.TIL_BEHANDLING,
            )

            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(tilsagn),
                utbetalinger = listOf(utbetaling),
            ) {
                setTilsagnStatus(tilsagn, TilsagnStatus.GODKJENT)
                setRoller(
                    NavAnsattFixture.MikkeMus,
                    setOf(NavAnsattRolle.kontorspesifikk(Rolle.ATTESTANT_UTBETALING, setOf(Innlandet.enhetsnummer))),
                )
            }.initialize(database.db)

            val service = createUtbetalingService()

            val linje = UtbetalingLinjeRequest(
                UUID.randomUUID(),
                tilsagn.id,
                gjorOppTilsagn = false,
                pris = 10.withValuta(Valuta.NOK).toRequest(),
            )
            service.opprettUtbetalingLinjer(
                OpprettUtbetalingLinjerRequest(utbetaling.id, listOf(linje), begrunnelseMindreBetalt = null),
                NavAnsattFixture.DonaldDuck.navIdent,
            ).shouldBeRight()

            service.godkjennUtbetalingLinje(
                id = linje.id,
                navIdent = NavAnsattFixture.MikkeMus.navIdent,
            ).shouldBeRight().status shouldBe UtbetalingStatusType.FERDIG_BEHANDLET

            database.run {
                queries.utbetalingLinje.getOrError(linje.id).status shouldBe UtbetalingLinjeStatus.OVERFORT_TIL_UTBETALING
                queries.tilsagn.getOrError(Tilsagn1.id).status shouldBe TilsagnStatus.OPPGJORT
            }
        }

        test("utbetaling blir sendt som melding til økonomi når alle utbetalingLinjer er godkjent") {
            val tilsagn1 = Tilsagn1.copy(
                periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                bestillingsnummer = "A-2025/1-1",
            )

            val tilsagn2 = Tilsagn2.copy(
                periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                bestillingsnummer = "A-2025/1-2",
            )

            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(tilsagn1, tilsagn2),
                utbetalinger = listOf(utbetaling1.copy(status = UtbetalingStatusType.TIL_BEHANDLING)),
            ) {
                setTilsagnStatus(tilsagn1, TilsagnStatus.GODKJENT)
                setTilsagnStatus(tilsagn2, TilsagnStatus.GODKJENT)
                setRoller(
                    NavAnsattFixture.MikkeMus,
                    setOf(NavAnsattRolle.kontorspesifikk(Rolle.ATTESTANT_UTBETALING, setOf(Innlandet.enhetsnummer))),
                )
            }.initialize(database.db)

            val service = createUtbetalingService()

            val utbetalingLinje1 = UtbetalingLinjeRequest(
                UUID.randomUUID(),
                tilsagn1.id,
                gjorOppTilsagn = false,
                pris = 1.withValuta(Valuta.NOK).toRequest(),
            )
            val utbetalingLinje2 = UtbetalingLinjeRequest(
                UUID.randomUUID(),
                tilsagn2.id,
                gjorOppTilsagn = false,
                pris = 2.withValuta(Valuta.NOK).toRequest(),
            )
            val opprettRequest = OpprettUtbetalingLinjerRequest(
                utbetalingId = utbetaling1.id,
                utbetalingLinjer = listOf(utbetalingLinje1, utbetalingLinje2),
                begrunnelseMindreBetalt = "begrunnelse",
            )
            service.opprettUtbetalingLinjer(
                request = opprettRequest,
                navIdent = NavAnsattFixture.DonaldDuck.navIdent,
            ).shouldBeRight()

            service.godkjennUtbetalingLinje(
                id = utbetalingLinje1.id,
                navIdent = NavAnsattFixture.MikkeMus.navIdent,
            ).shouldBeRight().status shouldBe UtbetalingStatusType.TIL_ATTESTERING

            database.run {
                queries.utbetalingLinje.getOrError(utbetalingLinje1.id).status shouldBe UtbetalingLinjeStatus.GODKJENT
            }

            service.godkjennUtbetalingLinje(
                id = utbetalingLinje2.id,
                navIdent = NavAnsattFixture.MikkeMus.navIdent,
            ).shouldBeRight().status shouldBe UtbetalingStatusType.FERDIG_BEHANDLET

            database.run {
                queries.utbetalingLinje.getOrError(utbetalingLinje1.id).status shouldBe UtbetalingLinjeStatus.OVERFORT_TIL_UTBETALING
                queries.utbetalingLinje.getOrError(utbetalingLinje2.id).status shouldBe UtbetalingLinjeStatus.OVERFORT_TIL_UTBETALING

                val records = queries.kafkaProducerRecord.getRecords(10)
                records.shouldHaveSize(2)

                Json.decodeFromString<OkonomiBestillingMelding>(records[0].value.decodeToString())
                    .shouldBeTypeOf<OkonomiBestillingMelding.Faktura>()
                    .payload.should {
                        it.fakturanummer shouldBe "A-2025/1-1-1"
                        it.belop shouldBe 1
                        it.behandletAv shouldBe NavAnsattFixture.DonaldDuck.navIdent.toOkonomiPart()
                        it.besluttetAv shouldBe NavAnsattFixture.MikkeMus.navIdent.toOkonomiPart()
                        it.periode shouldBe Periode.forMonthOf(LocalDate.of(2025, 1, 1))
                    }

                Json.decodeFromString<OkonomiBestillingMelding>(records[1].value.decodeToString())
                    .shouldBeTypeOf<OkonomiBestillingMelding.Faktura>()
                    .payload.should {
                        it.fakturanummer shouldBe "A-2025/1-2-1"
                        it.belop shouldBe 2
                        it.behandletAv shouldBe NavAnsattFixture.DonaldDuck.navIdent.toOkonomiPart()
                        it.besluttetAv shouldBe NavAnsattFixture.MikkeMus.navIdent.toOkonomiPart()
                        it.periode shouldBe Periode.forMonthOf(LocalDate.of(2025, 1, 1))
                    }
            }
        }

        test("utbetaling blir konfigurert til å bli behandlet på et senere tidspunkt når utbetaling blir godkjent") {
            val tilsagn1 = Tilsagn1.copy(
                periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                bestillingsnummer = "A-2025/1-1",
            )
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(tilsagn1),
                utbetalinger = listOf(utbetaling1.copy(status = UtbetalingStatusType.TIL_ATTESTERING)),
                utbetalingLinjer = listOf(utbetalingLinje1),
            ) {
                setTilsagnStatus(tilsagn1, TilsagnStatus.GODKJENT)
                setUtbetalingLinjeStatus(utbetalingLinje1, UtbetalingLinjeStatus.TIL_ATTESTERING)
                setRoller(
                    NavAnsattFixture.MikkeMus,
                    setOf(NavAnsattRolle.kontorspesifikk(Rolle.ATTESTANT_UTBETALING, setOf(Innlandet.enhetsnummer))),
                )
            }.initialize(database.db)

            val februarNorskTid = TidligstTidspunktForUtbetalingCalculator { _, _ ->
                LocalDate.of(2025, 2, 1).atStartOfDay(ZoneId.of("Europe/Oslo")).toInstant()
            }

            val service = createUtbetalingService(tidligstTidspunktForUtbetaling = februarNorskTid)

            service.godkjennUtbetalingLinje(
                id = utbetalingLinje1.id,
                navIdent = NavAnsattFixture.MikkeMus.navIdent,
            ).shouldBeRight().status shouldBe UtbetalingStatusType.FERDIG_BEHANDLET

            database.run {
                val record = queries.kafkaProducerRecord.getRecords(10).shouldHaveSize(1).first()

                val header = KafkaUtils.jsonToHeaders(record.headersJson).shouldHaveSize(1).first()

                header.key() shouldBe KAFKA_CONSUMER_RECORD_PROCESSOR_SCHEDULED_AT
                String(header.value()) shouldBe "2025-01-31T23:00:00Z"
            }
        }

        test("utbetaling blir konfigurert til å bli behandlet på et senere tidspunkt") {
            val februarNorskTid = LocalDate.of(2025, 2, 1).atStartOfDay(ZoneId.of("Europe/Oslo")).toInstant()
            val tilsagn1 = Tilsagn1.copy(
                periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                bestillingsnummer = "A-2025/1-1",
            )
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(tilsagn1),
                utbetalinger = listOf(
                    utbetaling1.copy(
                        status = UtbetalingStatusType.TIL_ATTESTERING,
                        utbetalesTidligstTidspunkt = februarNorskTid,
                    ),
                ),
                utbetalingLinjer = listOf(utbetalingLinje1),
            ) {
                setTilsagnStatus(tilsagn1, TilsagnStatus.GODKJENT)
                setUtbetalingLinjeStatus(utbetalingLinje1, UtbetalingLinjeStatus.TIL_ATTESTERING)
                setRoller(
                    NavAnsattFixture.MikkeMus,
                    setOf(NavAnsattRolle.kontorspesifikk(Rolle.ATTESTANT_UTBETALING, setOf(Innlandet.enhetsnummer))),
                )
            }.initialize(database.db)

            val service = createUtbetalingService()

            service.godkjennUtbetalingLinje(
                id = utbetalingLinje1.id,
                navIdent = NavAnsattFixture.MikkeMus.navIdent,
            ).shouldBeRight().status shouldBe UtbetalingStatusType.FERDIG_BEHANDLET

            database.run {
                val record = queries.kafkaProducerRecord.getRecords(10).shouldHaveSize(1).first()

                val header = KafkaUtils.jsonToHeaders(record.headersJson).shouldHaveSize(1).first()

                header.key() shouldBe KAFKA_CONSUMER_RECORD_PROCESSOR_SCHEDULED_AT
                String(header.value()) shouldBe "2025-01-31T23:00:00Z"
            }
        }
    }

    context("Automatisk utbetaling når arrangør godkjenner") {
        val utbetaling1Id = utbetaling1.id

        val utbetaling1Forhandsgodkjent = utbetaling1.copy(
            periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
            beregning = getForhandsgodkjentBeregning(
                periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                pris = 1000.withValuta(Valuta.NOK),
            ),
        )

        test("utbetaling blir journalført når arrangør godkjenner") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
                utbetalinger = listOf(utbetaling1Forhandsgodkjent),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT)
            }.initialize(database.db)

            val journalforUtbetaling = mockk<JournalforUtbetaling>(relaxed = true)
            val service = createUtbetalingService(journalforUtbetaling = journalforUtbetaling)

            service.godkjentAvArrangor(utbetaling1Id, kid = null).shouldBeRight()

            database.run {
                queries.utbetaling.getOrError(utbetaling1Id).innsending.shouldNotBeNull().tidspunkt.toLocalDate() shouldBe LocalDate.now()
            }

            verify(exactly = 1) {
                journalforUtbetaling.schedule(utbetaling1Forhandsgodkjent.id, any(), any(), listOf())
            }
        }

        test("utbetaling kan ikke godkjennes flere ganger samtidig") {
            val utbetaling = utbetaling1Forhandsgodkjent.copy(
                beregning = getForhandsgodkjentBeregning(
                    periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                    pris = 1.withValuta(Valuta.NOK),
                ),
            )
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
                utbetalinger = listOf(utbetaling),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT)
            }.initialize(database.db)

            val service = createUtbetalingService()

            val job1 = async(Dispatchers.Default) {
                service.godkjentAvArrangor(utbetaling1Id, kid = null)
            }
            val job2 = async(Dispatchers.Default) {
                service.godkjentAvArrangor(utbetaling1Id, kid = null)
            }

            listOf(job1.await(), job2.await()) shouldContainExactlyInAnyOrder listOf(
                AutomatiskUtbetalingResult.GODKJENT.right(),
                FieldError.of("Utbetaling er allerede godkjent").nel().left(),
            )
        }

        test("utbetales ikke automatisk hvis det allerede finnes en utbetalingslinje når arrangør godkjenner") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
                utbetalinger = listOf(utbetaling1Forhandsgodkjent),
                utbetalingLinjer = listOf(utbetalingLinje1),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT)
                setUtbetalingLinjeStatus(utbetalingLinje1, UtbetalingLinjeStatus.GODKJENT)
            }.initialize(database.db)

            val service = createUtbetalingService()

            service.godkjentAvArrangor(utbetaling1Id, kid = null).shouldBeRight(
                AutomatiskUtbetalingResult.UTBETALINGLINJER_ALLEREDE_OPPRETTET,
            )
        }

        test("utbetales automatisk når det finnes et enkelt tilsagn med nok midler og det er godkjent") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(
                    Tilsagn1.copy(
                        beregning = getTilsagnBeregning(
                            pris = 1000.withValuta(Valuta.NOK),
                        ),
                    ),
                ),
                utbetalinger = listOf(utbetaling1Forhandsgodkjent),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT)
            }.initialize(database.db)

            val service = createUtbetalingService()

            service.godkjentAvArrangor(utbetaling1Id, kid = null).shouldBeRight(
                AutomatiskUtbetalingResult.GODKJENT,
            )

            database.run {
                val linje = queries.utbetalingLinje.getByUtbetalingId(utbetaling1Id).shouldHaveSize(1).first().also {
                    it.status shouldBe UtbetalingLinjeStatus.OVERFORT_TIL_UTBETALING
                    it.pris shouldBe 1000.withValuta(Valuta.NOK)
                }

                queries.totrinnskontroll.getOrError(linje.id, Totrinnskontroll.Type.OPPRETT).should {
                    it.behandletAv shouldBe Tiltaksadministrasjon
                    it.besluttetAv shouldBe Tiltaksadministrasjon
                }

                queries.tilsagn.getOrError(Tilsagn1.id).should {
                    it.belopBrukt shouldBe 1000.withValuta(Valuta.NOK)
                }

                val records = queries.kafkaProducerRecord.getRecords(50)
                records.shouldHaveSize(1)
                Json.decodeFromString<OkonomiBestillingMelding>(records[0].value.decodeToString())
                    .shouldBeTypeOf<OkonomiBestillingMelding.Faktura>()
                    .payload.should {
                        it.belop shouldBe 1000
                        it.behandletAv shouldBe Tiltaksadministrasjon.toOkonomiPart()
                        it.besluttetAv shouldBe Tiltaksadministrasjon.toOkonomiPart()
                        it.periode shouldBe Periode.forMonthOf(LocalDate.of(2025, 1, 1))
                        it.beskrivelse shouldBe """
                            Tiltakstype: Arbeidsforberedende trening
                            Periode: 01.01.2025 - 31.01.2025
                            Tilsagnsnummer: A-2025/1-1
                        """.trimIndent()
                    }
            }
        }

        test("valideringsfeil hvis utbetaling forsøkes godkjennes flere ganger") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
                utbetalinger = listOf(utbetaling1Forhandsgodkjent),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT)
            }.initialize(database.db)

            val service = createUtbetalingService()

            service.godkjentAvArrangor(utbetaling1Id, kid = null).shouldBeRight(
                AutomatiskUtbetalingResult.GODKJENT,
            )

            service.godkjentAvArrangor(utbetaling1Id, kid = null).shouldBeLeft(
                listOf(FieldError.of("Utbetaling er allerede godkjent")),
            )
        }

        test("ingen automatisk utbetaling hvis tilsagn ikke er godkjent") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
                utbetalinger = listOf(utbetaling1Forhandsgodkjent),
            ).initialize(database.db)

            val service = createUtbetalingService()

            service.godkjentAvArrangor(utbetaling1Id, kid = null).shouldBeRight(
                AutomatiskUtbetalingResult.FEIL_ANTALL_TILSAGN,
            )

            database.run {
                queries.utbetaling.getOrError(utbetaling1.id).status shouldBe UtbetalingStatusType.TIL_BEHANDLING
                queries.utbetalingLinje.getByUtbetalingId(utbetaling1Id).shouldBeEmpty()
            }
        }

        test("ingen automatisk utbetaling hvis ingen tilsagn") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                utbetalinger = listOf(utbetaling1Forhandsgodkjent),
            ).initialize(database.db)

            val service = createUtbetalingService()

            service.godkjentAvArrangor(utbetaling1Id, kid = null).shouldBeRight(
                AutomatiskUtbetalingResult.FEIL_ANTALL_TILSAGN,
            )

            database.run {
                queries.utbetaling.getOrError(utbetaling1.id).status shouldBe UtbetalingStatusType.TIL_BEHANDLING
                queries.utbetalingLinje.getByUtbetalingId(utbetaling1Id).shouldBeEmpty()
            }
        }

        test("ingen automatisk utbetaling hvis flere tilsagn") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1, Tilsagn2.copy(periode = Tilsagn1.periode)),
                utbetalinger = listOf(utbetaling1Forhandsgodkjent),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT)
                setTilsagnStatus(Tilsagn2, TilsagnStatus.GODKJENT)
            }.initialize(database.db)

            val service = createUtbetalingService()

            service.godkjentAvArrangor(utbetaling1Id, kid = null).shouldBeRight(
                AutomatiskUtbetalingResult.FEIL_ANTALL_TILSAGN,
            )

            database.run {
                queries.utbetaling.getOrError(utbetaling1.id).status shouldBe UtbetalingStatusType.TIL_BEHANDLING
                queries.utbetalingLinje.getByUtbetalingId(utbetaling1Id).shouldBeEmpty()
            }
        }

        test("ingen automatisk utbetaling hvis tilsagn ikke har nok penger") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(
                    Tilsagn1.copy(
                        beregning = getTilsagnBeregning(
                            pris = 1.withValuta(Valuta.NOK),
                        ),
                    ),
                ),
                utbetalinger = listOf(utbetaling1Forhandsgodkjent),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT)
            }.initialize(database.db)

            val service = createUtbetalingService()

            service.godkjentAvArrangor(utbetaling1Id, kid = null).shouldBeRight(
                AutomatiskUtbetalingResult.IKKE_NOK_PENGER,
            )

            database.run {
                queries.utbetaling.getOrError(utbetaling1.id).status shouldBe UtbetalingStatusType.TIL_BEHANDLING
                queries.utbetalingLinje.getByUtbetalingId(utbetaling1Id).shouldBeEmpty()
            }
        }

        test("ingen automatisk utbetaling når prismodell er fri") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
                utbetalinger = listOf(
                    utbetaling1Forhandsgodkjent.copy(
                        beregning = UtbetalingBeregningFri(
                            input = UtbetalingBeregningFri.Input(1.withValuta(Valuta.NOK)),
                            output = UtbetalingBeregningFri.Output(1.withValuta(Valuta.NOK)),
                        ),
                    ),
                ),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT)
            }.initialize(database.db)

            val service = createUtbetalingService()
            service.godkjentAvArrangor(utbetaling1Id, kid = null).shouldBeRight(
                AutomatiskUtbetalingResult.FEIL_PRISMODELL,
            )

            database.run {
                queries.utbetaling.getOrError(utbetaling1.id).status shouldBe UtbetalingStatusType.TIL_BEHANDLING
                queries.utbetalingLinje.getByUtbetalingId(utbetaling1Id).shouldBeEmpty()
            }
        }

        test("Tilsagn gjøres ikke opp hvis det varer lengre enn utbetalingsperioden") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(
                    Tilsagn1.copy(
                        periode = Periode(LocalDate.of(2025, 1, 4), LocalDate.of(2025, 3, 1)),
                    ),
                ),
                utbetalinger = listOf(
                    utbetaling1.copy(
                        periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                        beregning = getForhandsgodkjentBeregning(
                            periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                            pris = 100.withValuta(Valuta.NOK),
                        ),
                    ),
                ),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT)
            }.initialize(database.db)

            val service = createUtbetalingService()

            service.godkjentAvArrangor(utbetaling1Id, kid = null).shouldBeRight(
                AutomatiskUtbetalingResult.GODKJENT,
            )

            database.run {
                queries.utbetaling.getOrError(utbetaling1.id).status shouldBe UtbetalingStatusType.FERDIG_BEHANDLET
                queries.tilsagn.getOrError(Tilsagn1.id).status shouldBe TilsagnStatus.GODKJENT
                queries.utbetalingLinje.getByUtbetalingId(utbetaling1Id).first().should {
                    it.status shouldBe UtbetalingLinjeStatus.OVERFORT_TIL_UTBETALING
                    it.gjorOppTilsagn shouldBe false
                }
            }
        }

        test("Tilsagn gjøres opp automatisk når siste dato i tilsagnsperioden er inkludert i utbetalingsperioden") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(
                    Tilsagn1.copy(
                        periode = Periode(LocalDate.of(2025, 1, 4), LocalDate.of(2025, 3, 1)),
                    ),
                ),
                utbetalinger = listOf(
                    utbetaling1.copy(
                        periode = Periode.forMonthOf(LocalDate.of(2025, 2, 1)),
                        beregning = getForhandsgodkjentBeregning(
                            periode = Periode.forMonthOf(LocalDate.of(2025, 2, 1)),
                            pris = 100.withValuta(Valuta.NOK),
                        ),
                    ),
                ),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT)
            }.initialize(database.db)

            val service = createUtbetalingService()

            service.godkjentAvArrangor(utbetaling1.id, kid = null).shouldBeRight(
                AutomatiskUtbetalingResult.GODKJENT,
            )

            database.run {
                queries.utbetaling.getOrError(utbetaling1.id).status shouldBe UtbetalingStatusType.FERDIG_BEHANDLET
                queries.tilsagn.getOrError(Tilsagn1.id).status shouldBe TilsagnStatus.OPPGJORT
                queries.utbetalingLinje.getByUtbetalingId(utbetaling1.id).shouldHaveSize(1).should { (first) ->
                    first.status shouldBe UtbetalingLinjeStatus.OVERFORT_TIL_UTBETALING
                    first.gjorOppTilsagn shouldBe true
                }
            }
        }

        test("blir ikke utbetalt hvis det oppstår valideringsfeil i forbindelse med oppgjør av tilsagnet") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(
                    Tilsagn1.copy(
                        periode = Periode(LocalDate.of(2025, 1, 4), LocalDate.of(2025, 3, 1)),
                    ),
                ),
                utbetalinger = listOf(
                    utbetaling1.copy(
                        periode = Periode.forMonthOf(LocalDate.of(2025, 2, 1)),
                        beregning = getForhandsgodkjentBeregning(
                            periode = Periode.forMonthOf(LocalDate.of(2025, 2, 1)),
                            pris = 100.withValuta(Valuta.NOK),
                        ),
                    ),
                ),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT)
            }.initialize(database.db)

            val tilsagnService: TilsagnService = spyk(createTilsagnService())
            coEvery {
                with(any<TransactionalQueryContext>()) {
                    tilsagnService.gjorOppTilsagn(any(), any(), any())
                }
            } answers {
                FieldError.root("Noe feil skjedde").nel().left()
            }
            val service = createUtbetalingService(tilsagnService = tilsagnService)

            service.godkjentAvArrangor(
                utbetaling1.id,
                kid = null,
            ) shouldBeRight AutomatiskUtbetalingResult.VALIDERINGSFEIL

            database.run {
                queries.utbetaling.getOrError(utbetaling1.id).status shouldBe UtbetalingStatusType.TIL_BEHANDLING
                queries.tilsagn.getOrError(Tilsagn1.id).status shouldBe TilsagnStatus.GODKJENT
                queries.utbetalingLinje.getByUtbetalingId(utbetaling1.id).shouldBeEmpty()
            }
        }
    }

    context("sletting og avbryting") {
        test("kan ikke slette ferdig behandlet") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                utbetalinger = listOf(utbetaling1.copy(status = UtbetalingStatusType.FERDIG_BEHANDLET)),
            ).initialize(database.db)

            val service = createUtbetalingService()
            service.slettKorreksjon(utbetaling1.id) shouldBeLeft listOf(
                FieldError.root("Kan ikke slette utbetaling fordi den har status: FERDIG_BEHANDLET"),
            )
        }

        test("kan ikke slette ikke korreksjon") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                utbetalinger = listOf(utbetaling1.copy(status = UtbetalingStatusType.TIL_BEHANDLING)),
            ).initialize(database.db)

            val service = createUtbetalingService()
            service.slettKorreksjon(utbetaling1.id) shouldBeLeft listOf(
                FieldError.root("Kan kun slette korreksjoner"),
            )
        }
    }

    context("oppdaterFakturaStatus") {
        beforeEach {
            database.truncateAll()
        }

        test("skal ikke prosessere fakturastatus eldre enn sist oppdatert") {
            val lagretFakturaStatusSistOppdatert = LocalDateTime.of(2026, 1, 1, 0, 0, 0)
            val linje = utbetalingLinje1.copy(
                status = UtbetalingLinjeStatus.OVERFORT_TIL_UTBETALING,
                fakturanummer = "2025-abc-1",
                fakturaStatusEndretTidspunkt = lagretFakturaStatusSistOppdatert,
                fakturaStatus = FakturaStatusType.SENDT,
            )
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
                utbetalinger = listOf(utbetaling1.copy(status = UtbetalingStatusType.FERDIG_BEHANDLET)),
                utbetalingLinjer = listOf(linje),
            ) {
                queries.utbetalingLinje.setFakturaSendtTidspunk(linje.id, lagretFakturaStatusSistOppdatert)
            }.initialize(database.db)

            val service = createUtbetalingService()

            service.oppdaterFakturaStatus(
                linje.fakturanummer,
                FakturaStatusType.FEILET,
                lagretFakturaStatusSistOppdatert.minusMinutes(1),
            ).status shouldBe UtbetalingStatusType.FERDIG_BEHANDLET

            database.run {
                queries.utbetalingLinje.getOrError(linje.id).should {
                    it.status shouldBe UtbetalingLinjeStatus.OVERFORT_TIL_UTBETALING
                    it.faktura.statusEndretTidspunkt shouldBe lagretFakturaStatusSistOppdatert
                    it.faktura.status shouldBe FakturaStatusType.SENDT
                }

                queries.endringshistorikk.getEndringshistorikk(
                    DocumentClass.UTBETALING,
                    utbetaling1.id,
                ).entries.shouldBeEmpty()
            }
        }

        test("skal oppdatere utbetaling endringslogg når faktura status er utbetalt") {
            val lagretFakturaStatusSistOppdatert = LocalDateTime.of(2026, 1, 1, 0, 0, 0)
            val linje = utbetalingLinje1.copy(
                status = UtbetalingLinjeStatus.OVERFORT_TIL_UTBETALING,
                fakturanummer = "2025-abc-1",
                fakturaStatusEndretTidspunkt = lagretFakturaStatusSistOppdatert,
                fakturaStatus = FakturaStatusType.SENDT,
            )
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
                utbetalinger = listOf(utbetaling1.copy(status = UtbetalingStatusType.FERDIG_BEHANDLET)),
                utbetalingLinjer = listOf(linje),
            ) {
                queries.utbetalingLinje.setFakturaSendtTidspunk(linje.id, lagretFakturaStatusSistOppdatert)
            }.initialize(database.db)

            val service = createUtbetalingService()

            val fakturaStatusEndretTidspunkt = lagretFakturaStatusSistOppdatert.plusMinutes(1)
            service.oppdaterFakturaStatus(
                linje.fakturanummer,
                FakturaStatusType.FULLT_BETALT,
                fakturaStatusEndretTidspunkt,
            ).status shouldBe UtbetalingStatusType.UTBETALT

            database.run {
                queries.utbetalingLinje.getOrError(linje.id).should {
                    it.status shouldBe UtbetalingLinjeStatus.UTBETALT
                    it.faktura.statusEndretTidspunkt shouldBe fakturaStatusEndretTidspunkt
                    it.faktura.status shouldBe FakturaStatusType.FULLT_BETALT
                }

                queries.endringshistorikk.getEndringshistorikk(
                    DocumentClass.UTBETALING,
                    utbetaling1.id,
                ).entries.size shouldBe 1
            }
        }

        test("skal ikke oppdatere utbetaling endringslogg når utbetalingslinje allerede er utbetalt") {
            val lagretFakturaStatusSistOppdatert = LocalDateTime.of(2026, 1, 1, 0, 0, 0)
            val linje = utbetalingLinje1.copy(
                status = UtbetalingLinjeStatus.UTBETALT,
                fakturanummer = "2025-abc-1",
                fakturaStatusEndretTidspunkt = lagretFakturaStatusSistOppdatert,
                fakturaStatus = FakturaStatusType.DELVIS_BETALT,
            )
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
                utbetalinger = listOf(utbetaling1.copy(status = UtbetalingStatusType.FERDIG_BEHANDLET)),
                utbetalingLinjer = listOf(linje),
            ) {
                queries.utbetalingLinje.setFakturaSendtTidspunk(linje.id, lagretFakturaStatusSistOppdatert)
            }.initialize(database.db)

            val service = createUtbetalingService()

            val fakturaStatusEndretTidspunkt = lagretFakturaStatusSistOppdatert.plusMinutes(1)
            service.oppdaterFakturaStatus(
                linje.fakturanummer,
                FakturaStatusType.FULLT_BETALT,
                fakturaStatusEndretTidspunkt,
            ).status shouldBe UtbetalingStatusType.UTBETALT

            database.run {
                queries.utbetalingLinje.getOrError(linje.id).status shouldBe UtbetalingLinjeStatus.UTBETALT

                queries.endringshistorikk.getEndringshistorikk(
                    DocumentClass.UTBETALING,
                    utbetaling1.id,
                ).entries.size shouldBe 0
            }
        }

        test("skal oppdatere utbetaling status til delvis hvis minst en utbetalingslinje er utbetalt") {
            val lagretFakturaStatusSistOppdatert = LocalDateTime.of(2026, 1, 1, 0, 0, 0)
            val linje1 = utbetalingLinje1.copy(
                status = UtbetalingLinjeStatus.OVERFORT_TIL_UTBETALING,
                fakturanummer = "2025-abc-1",
                fakturaStatusEndretTidspunkt = lagretFakturaStatusSistOppdatert,
                fakturaStatus = FakturaStatusType.SENDT,
            )
            val linje2 = utbetalingLinje2.copy(
                status = UtbetalingLinjeStatus.OVERFORT_TIL_UTBETALING,
                fakturanummer = "2025-abcd-1",
                fakturaStatusEndretTidspunkt = lagretFakturaStatusSistOppdatert,
                fakturaStatus = FakturaStatusType.SENDT,
            )
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1, Tilsagn2),
                utbetalinger = listOf(utbetaling1.copy(status = UtbetalingStatusType.FERDIG_BEHANDLET)),
                utbetalingLinjer = listOf(linje1, linje2),
            ) {
                queries.utbetalingLinje.setFakturaSendtTidspunk(linje1.id, lagretFakturaStatusSistOppdatert)
                queries.utbetalingLinje.setFakturaSendtTidspunk(linje2.id, lagretFakturaStatusSistOppdatert)
            }.initialize(database.db)

            val service = createUtbetalingService()

            val fakturaStatusEndretTidspunkt = lagretFakturaStatusSistOppdatert.plusMinutes(1)
            service.oppdaterFakturaStatus(
                linje1.fakturanummer,
                FakturaStatusType.FULLT_BETALT,
                fakturaStatusEndretTidspunkt,
            ).status shouldBe UtbetalingStatusType.DELVIS_UTBETALT

            service.oppdaterFakturaStatus(
                linje2.fakturanummer,
                FakturaStatusType.FULLT_BETALT,
                fakturaStatusEndretTidspunkt,
            ).status shouldBe UtbetalingStatusType.UTBETALT
        }
    }
})

private fun QueryContext.setRoller(ansatt: NavAnsattDbo, roller: Set<NavAnsattRolle>) {
    queries.ansatt.setRoller(
        navIdent = ansatt.navIdent,
        roller = roller,
    )
}

private fun getForhandsgodkjentBeregning(periode: Periode, pris: ValutaBelop) = UtbetalingBeregningFastSatsPerTiltaksplassPerManed(
    input = UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Input(
        satser = setOf(SatsPeriode(periode, 20205.withValuta(Valuta.NOK))),
        stengt = setOf(),
        deltakelser = setOf(),
    ),
    output = UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Output(
        pris = pris,
        deltakelser = setOf(),
    ),
)

fun getTilsagnBeregning(pris: ValutaBelop) = TilsagnBeregningFri(
    input = TilsagnBeregningFri.Input(
        linjer = listOf(
            TilsagnBeregningFri.InputLinje(
                id = UUID.randomUUID(),
                beskrivelse = "Beskrivelse",
                pris = pris,
                antall = 1,
            ),
        ),
        prisbetingelser = null,
    ),
    output = TilsagnBeregningFri.Output(pris),
).copy(
    output = TilsagnBeregningFri.Output(pris),
)

fun ValutaBelop.toRequest() = ValutaBelopRequest(
    belop = this.belop,
    valuta = this.valuta,
)
