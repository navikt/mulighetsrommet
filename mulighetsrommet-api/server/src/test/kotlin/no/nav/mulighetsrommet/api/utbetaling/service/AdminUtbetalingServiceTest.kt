package no.nav.mulighetsrommet.api.utbetaling.service

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.serialization.json.Json
import no.nav.common.kafka.util.KafkaUtils
import no.nav.mulighetsrommet.admin.endringshistorikk.EndringshistorikkType
import no.nav.mulighetsrommet.api.ApplicationConfigTest
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.arrangor.ArrangorService
import no.nav.mulighetsrommet.api.arrangor.model.Betalingsinformasjon
import no.nav.mulighetsrommet.api.domain.navansatt.NavAnsatt
import no.nav.mulighetsrommet.api.domain.navansatt.NavAnsattRolle
import no.nav.mulighetsrommet.api.domain.navansatt.Rolle
import no.nav.mulighetsrommet.api.fixtures.ArrangorFixtures
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
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.tilsagn.TilsagnService
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningFri
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.totrinnskontroll.model.TotrinnskontrollStatus
import no.nav.mulighetsrommet.api.totrinnskontroll.model.TotrinnskontrollType
import no.nav.mulighetsrommet.api.utbetaling.model.OpprettUtbetalingLinje
import no.nav.mulighetsrommet.api.utbetaling.model.OpprettUtbetalingLinjer
import no.nav.mulighetsrommet.api.utbetaling.model.UpsertUtbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFri
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingLinjeReturnertAarsak
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingLinjeStatus
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatusType
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.kafka.KAFKA_CONSUMER_RECORD_PROCESSOR_SCHEDULED_AT
import no.nav.mulighetsrommet.model.JournalpostId
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.NOK
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltaksadministrasjon
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.model.ValutaBelop
import no.nav.tiltak.okonomi.FakturaStatusType
import no.nav.tiltak.okonomi.OkonomiBestillingMelding
import no.nav.tiltak.okonomi.Tilskuddstype
import no.nav.tiltak.okonomi.toOkonomiPart
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

private val BESTILLING_TOPIC = ApplicationConfigTest.kafka.topics.okonomiBestillingTopic

class AdminUtbetalingServiceTest : FunSpec({
    val database = extension(ApiDatabaseTestListener())

    afterEach {
        database.truncateAll()
    }

    var umiddelbarUtbetaling = TidligstTidspunktForUtbetalingCalculator { _, _ -> null }
    val arrangorService: ArrangorService = mockk()

    coEvery { arrangorService.getBetalingsinformasjon(any()) } returns Betalingsinformasjon.BBan(
        kontonummer = Kontonummer("12345678901"),
        kid = null,
    )

    fun createTilsagnService(): TilsagnService = TilsagnService(
        TilsagnService.Config(gyldigTilsagnPeriode = mapOf()),
        db = database.db,
        navAnsattService = mockk(),
    )

    fun createUtbetalingService(
        tilsagnService: TilsagnService = createTilsagnService(),
        tidligstTidspunktForUtbetaling: TidligstTidspunktForUtbetalingCalculator = umiddelbarUtbetaling,
    ): AdminUtbetalingService {
        val utbetalingService = UtbetalingService(
            config = UtbetalingService.Config(
                tidligstTidspunktForUtbetaling = tidligstTidspunktForUtbetaling,
            ),
            tilsagnService = tilsagnService,
            arrangorService = arrangorService,
        )
        return AdminUtbetalingService(
            db = database.db,
            utbetalingService = utbetalingService,
            personaliaService = mockk(),
        )
    }

    val navIdent = NavAnsattFixture.DonaldDuck.navIdent

    context("opprett og rediger utbetaling") {
        val upsert = UpsertUtbetaling.Anskaffelse(
            id = UUID.randomUUID(),
            gjennomforingId = AFT1.id,
            periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
            journalpostId = JournalpostId("123123123"),
            kid = null,
            beregning = UtbetalingBeregningFri.from(10.NOK),
            kommentar = "Arrangør trenger penger",
            tilskuddstype = Tilskuddstype.TILTAK_DRIFTSTILSKUDD,
        )

        beforeEach {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                utbetalinger = listOf(utbetaling1.copy(status = UtbetalingStatusType.GENERERT)),
            ).initialize(database.db)
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

            service.opprettUtbetaling(upsert, navIdent).shouldBeRight()

            service.opprettUtbetaling(upsert, navIdent) shouldBeLeft listOf(
                FieldError.of("Utbetalingen er allerede opprettet"),
            )
        }

        test("journalpostId er påkrevd for norsk arrangør") {
            val service = createUtbetalingService()

            service.opprettUtbetaling(
                upsert.copy(journalpostId = null),
                navIdent,
            ) shouldBeLeft listOf(
                FieldError("/journalpostId", "Journalpost-ID er påkrevd"),
            )
        }

        test("journalpostId er ikke påkrevd for utenlandsk arrangør") {
            val utenlandskArrangor = ArrangorFixtures.Utenlandsk.hovedenhet
            val gjennomforingMedUtenlandskArrangor = AFT1.copy(arrangorId = utenlandskArrangor.id)
            MulighetsrommetTestDomain(
                arrangorer = listOf(utenlandskArrangor),
                gjennomforinger = listOf(gjennomforingMedUtenlandskArrangor),
            ).initialize(database.db)

            val service = createUtbetalingService()

            service.opprettUtbetaling(
                upsert.copy(gjennomforingId = gjennomforingMedUtenlandskArrangor.id, journalpostId = null),
                navIdent,
            ).shouldBeRight()
        }

        test("kan redigeres når den er til behandling") {
            val service = createUtbetalingService()

            service.opprettUtbetaling(
                upsert,
                navIdent,
            ).shouldBeRight().status shouldBe UtbetalingStatusType.TIL_BEHANDLING

            val kommentar = "Arrangør trenger mer penger"
            val beregning = UtbetalingBeregningFri.from(ValutaBelop(100, Valuta.NOK))
            service.redigerUtbetaling(
                upsert.copy(kommentar = kommentar, beregning = beregning),
                navIdent,
            ).shouldBeRight().should {
                it.kommentar shouldBe kommentar
                it.beregning shouldBe beregning
            }
        }

        test("kan ikke redigeres når utbetalingen er en innsending") {
            val service = createUtbetalingService()

            val innsending = UpsertUtbetaling.Innsending(
                id = upsert.id,
                gjennomforingId = upsert.gjennomforingId,
                periode = upsert.periode,
                beregning = upsert.beregning,
                tilskuddstype = upsert.tilskuddstype,
                kid = null,
            )
            service.opprettUtbetaling(
                innsending,
                navIdent,
            ).shouldBeRight().status shouldBe UtbetalingStatusType.TIL_BEHANDLING

            service.redigerUtbetaling(
                upsert,
                navIdent,
            ) shouldBeLeft listOf(
                FieldError.of("Utbetalingen kan ikke redigeres"),
            )
        }

        test("kan ikke redigeres når den er generert") {
            val service = createUtbetalingService()

            service.redigerUtbetaling(
                upsert.copy(id = utbetaling1.id),
                navIdent,
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
            beregning = UtbetalingBeregningFri.from(10.NOK),
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
        }

        test("korreksjon må gjelde for en eksisterende utbetaling") {
            val service = createUtbetalingService()

            service.opprettUtbetaling(
                opprett = upsert.copy(korreksjonGjelderUtbetalingId = UUID.randomUUID()),
                agent = navIdent,
            ) shouldBeLeft listOf(
                FieldError.of("Utbetaling som skal korrigeres eksisterer ikke"),
            )
        }

        test("korreksjon kan ikke opprettes for genererte utbetalinger") {
            val service = createUtbetalingService()

            service.opprettUtbetaling(
                opprett = upsert.copy(korreksjonGjelderUtbetalingId = utbetaling2.id),
                agent = navIdent,
            ) shouldBeLeft listOf(
                FieldError.of("Utbetaling kan ikke korrigeres når den har status GENERERT"),
            )
        }

        test("korreksjon kan opprettes og redigeres") {
            val service = createUtbetalingService()

            service.opprettUtbetaling(
                opprett = upsert,
                agent = navIdent,
            ).shouldBeRight().korreksjon shouldBe Utbetaling.Korreksjon(
                gjelderUtbetalingId = utbetaling1.id,
                begrunnelse = "Feilutbetaling",
            )

            service.redigerUtbetaling(
                rediger = upsert.copy(korreksjonBegrunnelse = "Fordi"),
                agent = navIdent,
            ).shouldBeRight().korreksjon shouldBe Utbetaling.Korreksjon(
                gjelderUtbetalingId = utbetaling1.id,
                begrunnelse = "Fordi",
            )
        }
    }

    context("behandling av utbetaling") {
        test("skal ikke kunne beslutte utbetalingslinje når ansatt mangler attestant-rolle") {
            MulighetsrommetTestDomain(
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
                navIdent = NavAnsattFixture.MikkeMus.navIdent,
            ) shouldBeLeft listOf(
                FieldError.of("Du kan ikke attestere utbetalingen fordi du ikke er attestant ved tilsagnets kostnadssted (Nav Innlandet)"),
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
                    setOf(
                        NavAnsattRolle.generell(Rolle.SAKSBEHANDLER_OKONOMI),
                        NavAnsattRolle.kontorspesifikk(Rolle.ATTESTANT_UTBETALING, setOf(Innlandet.enhetsnummer)),
                    ),
                )
            }.initialize(database.db)

            val service = createUtbetalingService()

            val linje = createUtbetalingLinje(Tilsagn1.id)
            val opprett = createOpprettUtbetalingLinjer(utbetaling1.id, listOf(linje))
            service.sendTilAttestering(opprett, navIdent).shouldBeRight()

            service.godkjennUtbetalingLinje(linje.id, navIdent) shouldBeLeft listOf(
                FieldError.of("Du kan ikke beslutte noe du selv har behandlet"),
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
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT, besluttetAv = navIdent)
                setRoller(
                    NavAnsattFixture.DonaldDuck,
                    setOf(NavAnsattRolle.kontorspesifikk(Rolle.ATTESTANT_UTBETALING, setOf(Innlandet.enhetsnummer))),
                )
                setRoller(
                    NavAnsattFixture.MikkeMus,
                    setOf(NavAnsattRolle.generell(Rolle.SAKSBEHANDLER_OKONOMI)),
                )
            }.initialize(database.db)

            val service = createUtbetalingService()

            val linje = createUtbetalingLinje(Tilsagn1.id)
            val opprett = createOpprettUtbetalingLinjer(utbetaling1.id, listOf(linje))
            service.sendTilAttestering(opprett, NavAnsattFixture.MikkeMus.navIdent).shouldBeRight()

            service.godkjennUtbetalingLinje(
                id = linje.id,
                navIdent = navIdent,
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
                setRoller(
                    NavAnsattFixture.MikkeMus,
                    setOf(NavAnsattRolle.generell(Rolle.SAKSBEHANDLER_OKONOMI)),
                )
            }.initialize(database.db)

            val service = createUtbetalingService()

            val linje = createUtbetalingLinje(Tilsagn1.id)
            val opprett = createOpprettUtbetalingLinjer(utbetaling1.id, listOf(linje))
            service.sendTilAttestering(opprett, NavAnsattFixture.MikkeMus.navIdent).shouldBeRight()

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
                    NavAnsattFixture.DonaldDuck,
                    setOf(NavAnsattRolle.generell(Rolle.SAKSBEHANDLER_OKONOMI)),
                )
                setRoller(
                    NavAnsattFixture.MikkeMus,
                    setOf(NavAnsattRolle.kontorspesifikk(Rolle.ATTESTANT_UTBETALING, setOf(Innlandet.enhetsnummer))),
                )
            }.initialize(database.db)

            val service = createUtbetalingService()

            val linje1 = createUtbetalingLinje(Tilsagn1.id, 1000.NOK)
            val opprett1 = createOpprettUtbetalingLinjer(utbetaling1.id, listOf(linje1))
            service.sendTilAttestering(opprett1, domain.ansatte[0].navIdent).shouldBeRight()

            service.returnerUtbetalingLinje(
                id = linje1.id,
                aarsaker = listOf(UtbetalingLinjeReturnertAarsak.ANNET),
                forklaring = "Maksbeløp er 5",
                navIdent = NavAnsattFixture.MikkeMus.navIdent,
            ).shouldBeRight().status shouldBe UtbetalingStatusType.RETURNERT

            val linje2 = createUtbetalingLinje(Tilsagn1.id, 0.NOK)
            val opprett2 = createOpprettUtbetalingLinjer(utbetaling1.id, listOf(linje2))
            service.sendTilAttestering(opprett2, domain.ansatte[0].navIdent) shouldBeLeft listOf(
                FieldError.of("Totalt beløp må være større enn 0"),
                FieldError.of("Begrunnelse er påkrevd ved utbetaling av mindre enn innsendt beløp"),
            )

            database.run {
                queries.utbetalingLinje.getOrError(linje1.id).status shouldBe UtbetalingLinjeStatus.RETURNERT
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
                FieldError.of("Utbetalingen kan ikke attesteres"),
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
                setRoller(
                    NavAnsattFixture.DonaldDuck,
                    setOf(NavAnsattRolle.generell(Rolle.SAKSBEHANDLER_OKONOMI)),
                )
            }.initialize(database.db)

            val service = createUtbetalingService()

            val linje = createUtbetalingLinje(id = utbetalingLinje1.id, tilsagnId = Tilsagn1.id)
            val opprett = createOpprettUtbetalingLinjer(utbetaling1.id, listOf(linje))
            service.sendTilAttestering(opprett, navIdent).shouldBeRight()

            database.run {
                queries.utbetaling.getOrError(utbetaling1.id).status shouldBe UtbetalingStatusType.TIL_ATTESTERING
                queries.utbetalingLinje.getOrError(utbetalingLinje1.id).status shouldBe UtbetalingLinjeStatus.TIL_ATTESTERING
            }
        }

        test("skal ikke kunne opprette utbetalingslinjer når utbetaling har status GENERERT") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
                utbetalinger = listOf(utbetaling1.copy(status = UtbetalingStatusType.GENERERT)),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT)
            }.initialize(database.db)

            val service = createUtbetalingService()

            val linje = createUtbetalingLinje(Tilsagn1.id)
            val opprett = createOpprettUtbetalingLinjer(utbetaling1.id, listOf(linje))
            service.sendTilAttestering(opprett, navIdent) shouldBeLeft listOf(
                FieldError.of("Utbetaling kan bare endres når den er til behandling"),
            )
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
                setRoller(
                    NavAnsattFixture.DonaldDuck,
                    setOf(NavAnsattRolle.generell(Rolle.SAKSBEHANDLER_OKONOMI)),
                )
            }.initialize(database.db)

            val service = createUtbetalingService()

            shouldThrow<IllegalArgumentException> {
                val linje = createUtbetalingLinje(Tilsagn1.id)
                val opprett = createOpprettUtbetalingLinjer(utbetaling1.id, listOf(linje))
                service.sendTilAttestering(opprett, navIdent)
            }.message shouldBe "Utbetalingsperiode og tilsagnsperiode overlapper ikke"
        }

        test("ved ny innsending til godkjenning skal utbetalingslinjer som ikke er inkludert i forespørselen slettes fra databasen") {
            val tilsagn1 = Tilsagn1.copy(
                periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
            )
            val tilsagn2 = Tilsagn2.copy(
                periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
            )
            val utbetaling = utbetaling1.copy(
                periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                beregning = UtbetalingBeregningFri(
                    input = UtbetalingBeregningFri.Input(10.NOK),
                    output = UtbetalingBeregningFri.Output(10.NOK),
                ),
                status = UtbetalingStatusType.TIL_BEHANDLING,
            )

            MulighetsrommetTestDomain(
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
                    setOf(NavAnsattRolle.generell(Rolle.SAKSBEHANDLER_OKONOMI)),
                )
                setRoller(
                    NavAnsattFixture.MikkeMus,
                    setOf(NavAnsattRolle.kontorspesifikk(Rolle.ATTESTANT_UTBETALING, setOf(Innlandet.enhetsnummer))),
                )
            }.initialize(database.db)
            val service = createUtbetalingService()

            val utbetalingLinje1 = createUtbetalingLinje(tilsagn1.id, 5.NOK)
            val utbetalingLinje2 = createUtbetalingLinje(tilsagn2.id, 5.NOK)
            val opprett1 = createOpprettUtbetalingLinjer(utbetaling.id, listOf(utbetalingLinje1, utbetalingLinje2))
            service.sendTilAttestering(opprett1, NavAnsattFixture.DonaldDuck.navIdent).shouldBeRight()

            service.returnerUtbetalingLinje(
                id = utbetalingLinje1.id,
                aarsaker = listOf(UtbetalingLinjeReturnertAarsak.FEIL_BELOP),
                forklaring = null,
                navIdent = NavAnsattFixture.MikkeMus.navIdent,
            ).shouldBeRight().status shouldBe UtbetalingStatusType.RETURNERT

            val opprett2 = createOpprettUtbetalingLinjer(utbetaling1.id, listOf(utbetalingLinje1), "begrunnelse")
            service.sendTilAttestering(opprett2, NavAnsattFixture.DonaldDuck.navIdent).shouldBeRight()

            val utbetalingLinjer = database.run { queries.utbetalingLinje.getByUtbetalingId(utbetaling.id) }
            utbetalingLinjer.size shouldBe 1
            utbetalingLinjer[0].id shouldBe utbetalingLinje1.id
        }

        test("alle utbetalingLinjer (selv godkjente) blir returnert når saksbehandler returnerer en utbetalingslinje") {
            val tilsagn1 = Tilsagn1.copy(
                periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
            )
            val tilsagn2 = Tilsagn2.copy(
                periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
            )
            val utbetaling = utbetaling1.copy(
                periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                beregning = UtbetalingBeregningFri(
                    input = UtbetalingBeregningFri.Input(10.NOK),
                    output = UtbetalingBeregningFri.Output(10.NOK),
                ),
                status = UtbetalingStatusType.TIL_BEHANDLING,
            )

            MulighetsrommetTestDomain(
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
                setRoller(
                    NavAnsattFixture.MikkeMus,
                    setOf(NavAnsattRolle.generell(Rolle.SAKSBEHANDLER_OKONOMI)),
                )
            }.initialize(database.db)
            val service = createUtbetalingService()

            val utbetalingLinje1 = createUtbetalingLinje(tilsagn1.id, 5.NOK)
            val utbetalingLinje2 = createUtbetalingLinje(tilsagn2.id, 5.NOK)
            val opprett = createOpprettUtbetalingLinjer(utbetaling.id, listOf(utbetalingLinje1, utbetalingLinje2))
            service.sendTilAttestering(opprett, NavAnsattFixture.MikkeMus.navIdent).shouldBeRight()

            service.godkjennUtbetalingLinje(
                utbetalingLinje1.id,
                NavAnsattFixture.DonaldDuck.navIdent,
            ).shouldBeRight().status shouldBe UtbetalingStatusType.TIL_ATTESTERING

            database.run {
                queries.utbetalingLinje.getOrError(utbetalingLinje1.id).status shouldBe UtbetalingLinjeStatus.GODKJENT
            }

            service.returnerUtbetalingLinje(
                id = utbetalingLinje2.id,
                aarsaker = listOf(UtbetalingLinjeReturnertAarsak.ANNET),
                forklaring = "Maksbeløp er 5",
                navIdent = NavAnsattFixture.DonaldDuck.navIdent,
            ).shouldBeRight().status shouldBe UtbetalingStatusType.RETURNERT

            database.run {
                queries.utbetalingLinje.getOrError(utbetalingLinje1.id).status shouldBe UtbetalingLinjeStatus.RETURNERT
                queries.totrinnskontroll.getOrError(
                    utbetalingLinje1.id,
                    TotrinnskontrollType.UTBETALING_LINJE_OPPRETTELSE,
                ).should {
                    it.status shouldBe TotrinnskontrollStatus.RETURNERT
                    it.besluttetAv shouldBe Tiltaksadministrasjon
                }

                queries.utbetalingLinje.getOrError(utbetalingLinje1.id).status shouldBe UtbetalingLinjeStatus.RETURNERT
                queries.totrinnskontroll.getOrError(
                    utbetalingLinje2.id,
                    TotrinnskontrollType.UTBETALING_LINJE_OPPRETTELSE,
                ).should {
                    it.status shouldBe TotrinnskontrollStatus.RETURNERT
                    it.besluttetAv shouldBe NavAnsattFixture.DonaldDuck.navIdent
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

            MulighetsrommetTestDomain(
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
                setRoller(
                    NavAnsattFixture.DonaldDuck,
                    setOf(NavAnsattRolle.generell(Rolle.SAKSBEHANDLER_OKONOMI)),
                )
            }.initialize(database.db)

            val service = createUtbetalingService()

            val linje1 = createUtbetalingLinje(tilsagn1.id, 100.NOK)
            val linje2 = createUtbetalingLinje(tilsagn2.id, 900.NOK)
            val opprett1 = createOpprettUtbetalingLinjer(utbetaling1.id, listOf(linje1, linje2))
            service.sendTilAttestering(opprett1, NavAnsattFixture.DonaldDuck.navIdent).shouldBeRight()

            val linje3 = createUtbetalingLinje(tilsagn1.id, 500.NOK)
            val opprett2 = createOpprettUtbetalingLinjer(utbetaling2.id, listOf(linje3))
            service.sendTilAttestering(opprett2, NavAnsattFixture.DonaldDuck.navIdent).shouldBeRight()

            database.run {
                queries.utbetalingLinje.getOrError(linje1.id).should {
                    it.pris shouldBe 100.NOK
                    it.periode shouldBe Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 15))
                    it.lopenummer shouldBe 1
                    it.faktura.fakturanummer shouldBe "A-2025/1-1-1"
                }

                queries.utbetalingLinje.getOrError(linje2.id).should {
                    it.pris shouldBe 900.NOK
                    it.periode shouldBe Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 15))
                    it.lopenummer shouldBe 1
                    it.faktura.fakturanummer shouldBe "A-2025/1-2-1"
                }

                queries.utbetalingLinje.getOrError(linje3.id).should {
                    it.pris shouldBe 500.NOK
                    it.lopenummer shouldBe 2
                    it.faktura.fakturanummer shouldBe "A-2025/1-1-2"
                    it.periode shouldBe Periode(LocalDate.of(2025, 1, 15), LocalDate.of(2025, 2, 1))
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
                    NavAnsattFixture.DonaldDuck,
                    setOf(NavAnsattRolle.generell(Rolle.SAKSBEHANDLER_OKONOMI)),
                )
                setRoller(
                    NavAnsattFixture.MikkeMus,
                    setOf(NavAnsattRolle.kontorspesifikk(Rolle.ATTESTANT_UTBETALING, setOf(Innlandet.enhetsnummer))),
                )
            }.initialize(database.db)

            val service = createUtbetalingService()

            val linje1 = createUtbetalingLinje(Tilsagn1.id)
            val opprett1 = createOpprettUtbetalingLinjer(utbetaling1.id, listOf(linje1))
            service.sendTilAttestering(opprett1, navIdent).shouldBeRight()

            database.run {
                queries.utbetalingLinje.getOrError(linje1.id).should {
                    it.lopenummer shouldBe 1
                    it.faktura.fakturanummer shouldBe "A-2025/1-1-1"
                }
            }

            service.returnerUtbetalingLinje(
                id = linje1.id,
                aarsaker = listOf(UtbetalingLinjeReturnertAarsak.ANNET),
                forklaring = "Maksbeløp er 5",
                navIdent = NavAnsattFixture.MikkeMus.navIdent,
            ).shouldBeRight().status shouldBe UtbetalingStatusType.RETURNERT

            service.sendTilAttestering(opprett1, navIdent).shouldBeRight()

            database.run {
                queries.utbetalingLinje.getOrError(linje1.id).should {
                    it.lopenummer shouldBe 1
                    it.faktura.fakturanummer shouldBe "A-2025/1-1-1"
                }
            }

            service.returnerUtbetalingLinje(
                id = linje1.id,
                aarsaker = listOf(UtbetalingLinjeReturnertAarsak.ANNET),
                forklaring = "Maksbeløp er 5",
                navIdent = NavAnsattFixture.MikkeMus.navIdent,
            ).shouldBeRight().status shouldBe UtbetalingStatusType.RETURNERT

            val linje2 = createUtbetalingLinje(Tilsagn1.id)
            val opprett2 = createOpprettUtbetalingLinjer(utbetaling1.id, listOf(linje2))
            service.sendTilAttestering(opprett2, navIdent).shouldBeRight()

            database.run {
                queries.utbetalingLinje.getOrError(linje2.id).should {
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

                queries.totrinnskontroll.getOrError(
                    utbetalingLinje1.id,
                    TotrinnskontrollType.UTBETALING_LINJE_OPPRETTELSE,
                ).should {
                    it.besluttetAv shouldBe Tiltaksadministrasjon
                    it.status shouldBe TotrinnskontrollStatus.RETURNERT
                }

                queries.totrinnskontroll.getOrError(
                    utbetalingLinje2.id,
                    TotrinnskontrollType.UTBETALING_LINJE_OPPRETTELSE,
                ).should {
                    it.besluttetAv shouldBe Tiltaksadministrasjon
                    it.status shouldBe TotrinnskontrollStatus.RETURNERT
                }

                queries.kafkaProducerRecord.getRecords(10, listOf(BESTILLING_TOPIC)).shouldBeEmpty()
            }
        }

        test("tilsagn blir oppgjort når utbetaling benytter resten av tilsagnsbeløpet") {
            val tilsagn = Tilsagn1.copy(
                periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 3, 1)),
                beregning = getTilsagnBeregning(pris = 10.NOK),
            )

            val utbetaling = utbetaling1.copy(
                periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                beregning = UtbetalingBeregningFri(
                    input = UtbetalingBeregningFri.Input(10.NOK),
                    output = UtbetalingBeregningFri.Output(10.NOK),
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
                    NavAnsattFixture.DonaldDuck,
                    setOf(NavAnsattRolle.generell(Rolle.SAKSBEHANDLER_OKONOMI)),
                )
                setRoller(
                    NavAnsattFixture.MikkeMus,
                    setOf(NavAnsattRolle.kontorspesifikk(Rolle.ATTESTANT_UTBETALING, setOf(Innlandet.enhetsnummer))),
                )
            }.initialize(database.db)

            val service = createUtbetalingService()

            val linje = createUtbetalingLinje(tilsagn.id, 10.NOK)
            val opprett = createOpprettUtbetalingLinjer(utbetaling1.id, listOf(linje))
            service.sendTilAttestering(opprett, navIdent).shouldBeRight()

            service.godkjennUtbetalingLinje(
                id = opprett.linjer[0].id,
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
                    NavAnsattFixture.DonaldDuck,
                    setOf(NavAnsattRolle.generell(Rolle.SAKSBEHANDLER_OKONOMI)),
                )
                setRoller(
                    NavAnsattFixture.MikkeMus,
                    setOf(NavAnsattRolle.kontorspesifikk(Rolle.ATTESTANT_UTBETALING, setOf(Innlandet.enhetsnummer))),
                )
            }.initialize(database.db)

            val service = createUtbetalingService()

            val utbetalingLinje1 = createUtbetalingLinje(tilsagn1.id, 1.NOK)
            val utbetalingLinje2 = createUtbetalingLinje(tilsagn2.id, 2.NOK)
            val opprett = createOpprettUtbetalingLinjer(
                utbetalingId = utbetaling1.id,
                linjer = listOf(utbetalingLinje1, utbetalingLinje2),
                begrunnelse = "begrunnelse",
            )
            service.sendTilAttestering(opprett, navIdent).shouldBeRight()

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

                val records = queries.kafkaProducerRecord.getRecords(10, listOf(BESTILLING_TOPIC))
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
                val record =
                    queries.kafkaProducerRecord.getRecords(10, listOf(BESTILLING_TOPIC)).shouldHaveSize(1).first()

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
                val records = queries.kafkaProducerRecord.getRecords(10, listOf(BESTILLING_TOPIC)).shouldHaveSize(1)

                val header = KafkaUtils.jsonToHeaders(records.first().headersJson).shouldHaveSize(1).first()
                header.key() shouldBe KAFKA_CONSUMER_RECORD_PROCESSOR_SCHEDULED_AT
                String(header.value()) shouldBe "2025-01-31T23:00:00Z"
            }
        }

        test("saksbehandler som sendte til attestering kan returnere utbetalingslinje") {
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
                    setOf(NavAnsattRolle.generell(Rolle.SAKSBEHANDLER_OKONOMI)),
                )
            }.initialize(database.db)

            val service = createUtbetalingService()

            val linje = createUtbetalingLinje(Tilsagn1.id)
            val opprett = createOpprettUtbetalingLinjer(utbetaling1.id, listOf(linje))
            service.sendTilAttestering(opprett, NavAnsattFixture.DonaldDuck.navIdent).shouldBeRight()

            service.returnerUtbetalingLinje(
                id = linje.id,
                aarsaker = listOf(UtbetalingLinjeReturnertAarsak.ANNET),
                forklaring = "Fordi",
                navIdent = NavAnsattFixture.DonaldDuck.navIdent,
            ).shouldBeRight().status shouldBe UtbetalingStatusType.RETURNERT
        }

        test("annen saksbehandler enn den som sendte utbetaling til attestering kan returnere utbetalingslinje") {
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
                    setOf(NavAnsattRolle.generell(Rolle.SAKSBEHANDLER_OKONOMI)),
                )
                setRoller(
                    NavAnsattFixture.MikkeMus,
                    setOf(NavAnsattRolle.generell(Rolle.SAKSBEHANDLER_OKONOMI)),
                )
            }.initialize(database.db)

            val service = createUtbetalingService()

            val linje = createUtbetalingLinje(Tilsagn1.id)
            val opprett = createOpprettUtbetalingLinjer(utbetaling1.id, listOf(linje))
            service.sendTilAttestering(opprett, NavAnsattFixture.MikkeMus.navIdent).shouldBeRight()

            service.returnerUtbetalingLinje(
                id = linje.id,
                aarsaker = listOf(UtbetalingLinjeReturnertAarsak.FEIL_BELOP),
                forklaring = null,
                navIdent = NavAnsattFixture.DonaldDuck.navIdent,
            ).shouldBeRight().status shouldBe UtbetalingStatusType.RETURNERT
        }

        test("kan ikke returnere utbetalingslinjer hvis man mangler roller") {
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
                    setOf(NavAnsattRolle.generell(Rolle.SAKSBEHANDLER_OKONOMI)),
                )
            }.initialize(database.db)

            val service = createUtbetalingService()

            val linje = createUtbetalingLinje(Tilsagn1.id)
            val opprett = createOpprettUtbetalingLinjer(utbetaling1.id, listOf(linje))
            service.sendTilAttestering(opprett, NavAnsattFixture.DonaldDuck.navIdent).shouldBeRight()

            service.returnerUtbetalingLinje(
                id = linje.id,
                aarsaker = listOf(UtbetalingLinjeReturnertAarsak.FEIL_BELOP),
                forklaring = null,
                navIdent = NavAnsattFixture.MikkeMus.navIdent,
            ) shouldBeLeft listOf(
                FieldError.of("Du kan ikke returnere utbetalingen fordi du mangler tilgang"),
            )
        }
    }

    context("validering av utbetalingslinjer") {
        test("totalt beløp kan ikke overstige innsendt beløp") {
            val tilsagnMedHoytBelop = Tilsagn1.copy(
                beregning = (Tilsagn1.beregning as TilsagnBeregningFri).copy(
                    output = TilsagnBeregningFri.Output(pris = 2000.NOK),
                ),
            )
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(tilsagnMedHoytBelop),
                utbetalinger = listOf(utbetaling1.copy(status = UtbetalingStatusType.TIL_BEHANDLING)),
            ) {
                setTilsagnStatus(tilsagnMedHoytBelop, TilsagnStatus.GODKJENT)
            }.initialize(database.db)

            val service = createUtbetalingService()

            val linje = createUtbetalingLinje(Tilsagn1.id, 1001.NOK)
            val opprett = createOpprettUtbetalingLinjer(utbetaling1.id, listOf(linje))
            service.sendTilAttestering(opprett, navIdent) shouldBeLeft listOf(
                FieldError.of("Kan ikke utbetale mer enn innsendt beløp"),
            )
        }

        test("begrunnelse er påkrevd når totalt beløp er mindre enn innsendt beløp") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
                utbetalinger = listOf(utbetaling1.copy(status = UtbetalingStatusType.TIL_BEHANDLING)),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT)
            }.initialize(database.db)

            val service = createUtbetalingService()

            val linje = createUtbetalingLinje(Tilsagn1.id, 50.NOK)
            val opprett = createOpprettUtbetalingLinjer(utbetaling1.id, listOf(linje))

            service.sendTilAttestering(opprett, navIdent) shouldBeLeft listOf(
                FieldError.of("Begrunnelse er påkrevd ved utbetaling av mindre enn innsendt beløp"),
            )
        }

        test("beløp kan ikke overstige gjenstående beløp på tilsagn") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
                utbetalinger = listOf(utbetaling1.copy(status = UtbetalingStatusType.TIL_BEHANDLING)),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT)
            }.initialize(database.db)

            val service = createUtbetalingService()

            val linje = createUtbetalingLinje(Tilsagn1.id, 1001.NOK)
            val opprett = createOpprettUtbetalingLinjer(utbetaling1.id, listOf(linje))
            service.sendTilAttestering(opprett, navIdent) shouldBeLeft listOf(
                FieldError.of("Kan ikke utbetale mer enn innsendt beløp"),
                FieldError(
                    "/utbetalingLinjer/0/tilsagnId",
                    "Beløp overstiger gjenstående beløp på tilsagn. For å utbetale hele beløpet må dere først opprette og godkjenne et ekstratilsagn",
                ),
            )
        }

        test("tilsagn som ikke status OPPGJORT kan ikke benyttes") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
                utbetalinger = listOf(utbetaling1.copy(status = UtbetalingStatusType.TIL_BEHANDLING)),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.OPPGJORT)
            }.initialize(database.db)

            val service = createUtbetalingService()

            val linje = createUtbetalingLinje(Tilsagn1.id)
            val opprett = createOpprettUtbetalingLinjer(utbetaling1.id, listOf(linje))
            service.sendTilAttestering(opprett, navIdent) shouldBeLeft listOf(
                FieldError(
                    "/utbetalingLinjer/0/tilsagnId",
                    "Beløp overstiger gjenstående beløp på tilsagn. For å utbetale hele beløpet må dere først opprette og godkjenne et ekstratilsagn",
                ),
                FieldError(
                    "/utbetalingLinjer/0/tilsagnId",
                    "Tilsagnet har status Oppgjort og kan ikke benyttes, linjen må fjernes",
                ),
            )
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
                FieldError.of("Kan ikke slette utbetaling fordi den har status: FERDIG_BEHANDLET"),
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
                FieldError.of("Kan kun slette korreksjoner"),
            )
        }

        test("kan slette korreksjon med status TIL_BEHANDLING uten utbetalingslinjer") {
            val original = utbetaling1.copy(status = UtbetalingStatusType.FERDIG_BEHANDLET)
            val korreksjon = utbetaling2.copy(
                status = UtbetalingStatusType.TIL_BEHANDLING,
                korreksjonGjelderUtbetalingId = original.id,
                korreksjonBegrunnelse = "Feilutbetaling",
            )

            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                utbetalinger = listOf(original, korreksjon),
            ).initialize(database.db)

            val service = createUtbetalingService()

            service.slettKorreksjon(korreksjon.id).shouldBeRight()

            database.run {
                queries.utbetaling.get(korreksjon.id) shouldBe null
            }
        }

        test("kan slette korreksjon med status RETURNERT og returnerte utbetalingslinjer") {
            val original = utbetaling1.copy(status = UtbetalingStatusType.FERDIG_BEHANDLET)
            val korreksjon = utbetaling2.copy(
                status = UtbetalingStatusType.RETURNERT,
                korreksjonGjelderUtbetalingId = original.id,
                korreksjonBegrunnelse = "Feilutbetaling",
            )
            val linje = utbetalingLinje1.copy(
                utbetalingId = korreksjon.id,
            )

            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
                utbetalinger = listOf(original, korreksjon),
                utbetalingLinjer = listOf(linje),
            ) {
                setUtbetalingLinjeStatus(linje, UtbetalingLinjeStatus.RETURNERT)
            }.initialize(database.db)

            val service = createUtbetalingService()

            service.slettKorreksjon(korreksjon.id).shouldBeRight()

            database.run {
                queries.utbetaling.get(korreksjon.id) shouldBe null
                queries.utbetalingLinje.getByUtbetalingId(korreksjon.id).shouldBeEmpty()
            }
        }

        test("kan ikke slette korreksjon hvis utbetalingslinje ikke er i RETURNERT status") {
            val original = utbetaling1.copy(status = UtbetalingStatusType.FERDIG_BEHANDLET)
            val korreksjon = utbetaling2.copy(
                status = UtbetalingStatusType.TIL_BEHANDLING,
                korreksjonGjelderUtbetalingId = original.id,
                korreksjonBegrunnelse = "Feilutbetaling",
            )
            val linje = utbetalingLinje1.copy(
                utbetalingId = korreksjon.id,
            )

            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
                utbetalinger = listOf(original, korreksjon),
                utbetalingLinjer = listOf(linje),
            ) {
                setUtbetalingLinjeStatus(linje, UtbetalingLinjeStatus.TIL_ATTESTERING)
            }.initialize(database.db)

            val service = createUtbetalingService()

            service.slettKorreksjon(korreksjon.id) shouldBeLeft listOf(
                FieldError.of("UtbetalingLinje var i feil status"),
            )
        }
    }

    context("oppdaterFakturaStatus") {
        beforeEach {
            database.truncateAll()
        }

        test("skal ikke prosessere fakturastatus eldre enn sist oppdatert") {
            val lagretFakturaStatusSistOppdatert = Instant.parse("2026-01-01T00:00:00Z")
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
                lagretFakturaStatusSistOppdatert.minusSeconds(60),
            ).status shouldBe UtbetalingStatusType.FERDIG_BEHANDLET

            database.run {
                queries.utbetalingLinje.getOrError(linje.id).should {
                    it.status shouldBe UtbetalingLinjeStatus.OVERFORT_TIL_UTBETALING
                    it.faktura.statusEndretTidspunkt shouldBe lagretFakturaStatusSistOppdatert
                    it.faktura.status shouldBe FakturaStatusType.SENDT
                }

                queries.endringshistorikk.getEndringshistorikk(
                    EndringshistorikkType.UTBETALING,
                    utbetaling1.id,
                ).entries.shouldBeEmpty()
            }
        }

        test("skal oppdatere utbetaling endringslogg når faktura status er utbetalt") {
            val lagretFakturaStatusSistOppdatert = Instant.parse("2026-01-01T00:00:00Z")
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

            val fakturaStatusEndretTidspunkt = lagretFakturaStatusSistOppdatert.plusSeconds(60)
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
                    EndringshistorikkType.UTBETALING,
                    utbetaling1.id,
                ).entries.size shouldBe 1
            }
        }

        test("skal ikke oppdatere utbetaling endringslogg når utbetalingslinje allerede er utbetalt") {
            val lagretFakturaStatusSistOppdatert = Instant.parse("2026-01-01T00:00:00Z")
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

            val fakturaStatusEndretTidspunkt = lagretFakturaStatusSistOppdatert.plusSeconds(60)
            service.oppdaterFakturaStatus(
                linje.fakturanummer,
                FakturaStatusType.FULLT_BETALT,
                fakturaStatusEndretTidspunkt,
            ).status shouldBe UtbetalingStatusType.UTBETALT

            database.run {
                queries.utbetalingLinje.getOrError(linje.id).status shouldBe UtbetalingLinjeStatus.UTBETALT

                queries.endringshistorikk.getEndringshistorikk(
                    EndringshistorikkType.UTBETALING,
                    utbetaling1.id,
                ).entries.size shouldBe 0
            }
        }

        test("skal oppdatere utbetaling status til delvis hvis minst en utbetalingslinje er utbetalt") {
            val lagretFakturaStatusSistOppdatert = Instant.parse("2026-01-01T00:00:00Z")
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

            val fakturaStatusEndretTidspunkt = lagretFakturaStatusSistOppdatert.plusSeconds(60)
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

private fun createOpprettUtbetalingLinjer(
    utbetalingId: UUID,
    linjer: List<OpprettUtbetalingLinje>,
    begrunnelse: String? = null,
): OpprettUtbetalingLinjer = OpprettUtbetalingLinjer(utbetalingId, linjer, begrunnelse)

private fun createUtbetalingLinje(
    tilsagnId: UUID,
    pris: ValutaBelop = 1000.NOK,
    gjorOppTilsagn: Boolean = false,
    id: UUID = UUID.randomUUID(),
) = OpprettUtbetalingLinje(
    id = id,
    tilsagnId = tilsagnId,
    pris = pris,
    gjorOppTilsagn = gjorOppTilsagn,
)

private fun QueryContext.setRoller(ansatt: NavAnsatt, roller: Set<NavAnsattRolle>) {
    queries.ansatt.save(ansatt.copy(roller = roller))
}

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
)
