package no.nav.mulighetsrommet.api.utbetaling

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
import io.kotest.matchers.comparables.shouldBeLessThanOrEqualTo
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.serialization.json.Json
import no.nav.common.kafka.util.KafkaUtils
import no.nav.mulighetsrommet.api.QueryContext
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
import no.nav.mulighetsrommet.api.fixtures.UtbetalingFixtures.delutbetaling1
import no.nav.mulighetsrommet.api.fixtures.UtbetalingFixtures.delutbetaling2
import no.nav.mulighetsrommet.api.fixtures.UtbetalingFixtures.utbetaling1
import no.nav.mulighetsrommet.api.fixtures.UtbetalingFixtures.utbetaling2
import no.nav.mulighetsrommet.api.fixtures.setDelutbetalingStatus
import no.nav.mulighetsrommet.api.fixtures.setTilsagnStatus
import no.nav.mulighetsrommet.api.navansatt.db.NavAnsattDbo
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsattRolle
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.tilsagn.TilsagnService
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningFri
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Besluttelse
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
import no.nav.mulighetsrommet.api.utbetaling.api.DelutbetalingRequest
import no.nav.mulighetsrommet.api.utbetaling.api.OpprettDelutbetalingerRequest
import no.nav.mulighetsrommet.api.utbetaling.model.AutomatiskUtbetalingResult
import no.nav.mulighetsrommet.api.utbetaling.model.DelutbetalingReturnertAarsak
import no.nav.mulighetsrommet.api.utbetaling.model.DelutbetalingStatus
import no.nav.mulighetsrommet.api.utbetaling.model.OpprettUtbetalingAnnenAvtaltPris
import no.nav.mulighetsrommet.api.utbetaling.model.SatsPeriode
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFastSatsPerTiltaksplassPerManed
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFri
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatusType
import no.nav.mulighetsrommet.api.utbetaling.task.JournalforUtbetaling
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.kafka.KAFKA_CONSUMER_RECORD_PROCESSOR_SCHEDULED_AT
import no.nav.mulighetsrommet.model.Arrangor
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
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

class UtbetalingServiceTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    afterEach {
        database.truncateAll()
    }

    var umiddelbarUtbetaling = TidligstTidspunktForUtbetalingCalculator { _, _ -> null }
    val arrangorService: ArrangorService = mockk()

    fun createUtbetalingService(
        tilsagnService: TilsagnService = mockk(relaxed = true),
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

    context("opprett utbetaling - annen avtalt pris") {
        val opprettAnnenAvtaltPrisUtbetaling = OpprettUtbetalingAnnenAvtaltPris(
            id = UUID.randomUUID(),
            gjennomforingId = AFT1.id,
            periodeStart = LocalDate.of(2025, 1, 1),
            periodeSlutt = LocalDate.of(2025, 1, 31),
            beskrivelse = "Arrangør trenger penger",
            kidNummer = null,
            pris = 10.withValuta(Valuta.NOK),
            tilskuddstype = Tilskuddstype.TILTAK_DRIFTSTILSKUDD,
            vedlegg = listOf(),
        )

        beforeEach {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
            ).initialize(database.db)

            coEvery { arrangorService.getBetalingsinformasjon(any()) } returns Betalingsinformasjon.BBan(
                kontonummer = Kontonummer("12345678901"),
                kid = null,
            )
        }

        test("utbetaling blir opprettet med fri-beregning") {
            val service = createUtbetalingService()

            val utbetaling = service.opprettAnnenAvtaltPrisUtbetaling(
                request = opprettAnnenAvtaltPrisUtbetaling,
                agent = NavAnsattFixture.DonaldDuck.navIdent,
            ).shouldBeRight()

            utbetaling.id.shouldNotBeNull()
            utbetaling.periode shouldBe Periode.forMonthOf(LocalDate.of(2025, 1, 1))
            utbetaling.beregning.shouldBeTypeOf<UtbetalingBeregningFri>().should {
                it.input.pris shouldBe 10.withValuta(Valuta.NOK)
                it.output.pris shouldBe 10.withValuta(Valuta.NOK)
            }
        }

        test("utbetaling kan ikke endres hvis den først har blitt opprettet") {
            val service = createUtbetalingService()

            service.opprettAnnenAvtaltPrisUtbetaling(
                request = opprettAnnenAvtaltPrisUtbetaling.copy(
                    pris = 5.withValuta(Valuta.NOK),
                ),
                agent = Arrangor,
            ).shouldBeRight()

            service.opprettAnnenAvtaltPrisUtbetaling(
                request = opprettAnnenAvtaltPrisUtbetaling.copy(
                    pris = 10.withValuta(Valuta.NOK),
                ),
                agent = Arrangor,
            ) shouldBeLeft listOf(
                FieldError.of("Utbetalingen er allerede opprettet"),
            )
        }

        test("utbetaling blir ikke journalført når den blir opprettet av Nav-ansatt") {
            val journalforUtbetaling = mockk<JournalforUtbetaling>(relaxed = true)

            val service = createUtbetalingService(journalforUtbetaling = journalforUtbetaling)

            service.opprettAnnenAvtaltPrisUtbetaling(
                request = opprettAnnenAvtaltPrisUtbetaling,
                agent = NavAnsattFixture.DonaldDuck.navIdent,
            ).shouldBeRight().status shouldBe UtbetalingStatusType.INNSENDT

            verify(exactly = 0) { journalforUtbetaling.schedule(any(), any(), any(), any()) }
        }

        test("utbetaling blir journalført når den blir opprettet av Arrangør") {
            val journalforUtbetaling = mockk<JournalforUtbetaling>(relaxed = true)

            val service = createUtbetalingService(journalforUtbetaling = journalforUtbetaling)

            val utbetaling = service.opprettAnnenAvtaltPrisUtbetaling(
                request = opprettAnnenAvtaltPrisUtbetaling,
                agent = Arrangor,
            ).shouldBeRight()

            verify(exactly = 1) { journalforUtbetaling.schedule(utbetaling.id, any(), any(), any()) }
        }
    }

    context("når utbetaling blir behandlet") {
        test("skal ikke kunne beslutte delutbetaling når ansatt mangler attestant-rolle") {
            val domain = MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
                utbetalinger = listOf(utbetaling1),
                delutbetalinger = listOf(delutbetaling1),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT)
                setDelutbetalingStatus(delutbetaling1, DelutbetalingStatus.TIL_ATTESTERING)
            }.initialize(database.db)

            val service = createUtbetalingService()

            service.godkjennDelutbetaling(
                id = delutbetaling1.id,
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
                utbetalinger = listOf(utbetaling1.copy(status = UtbetalingStatusType.INNSENDT)),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT)
                setRoller(
                    NavAnsattFixture.DonaldDuck,
                    setOf(NavAnsattRolle.kontorspesifikk(Rolle.ATTESTANT_UTBETALING, setOf(Innlandet.enhetsnummer))),
                )
            }.initialize(database.db)

            val service = createUtbetalingService()
            val delutbetaling = DelutbetalingRequest(
                id = UUID.randomUUID(),
                tilsagnId = Tilsagn1.id,
                gjorOppTilsagn = false,
                pris = 100.withValuta(Valuta.NOK),
            )
            val opprettRequest = OpprettDelutbetalingerRequest(
                utbetalingId = utbetaling1.id,
                delutbetalinger = listOf(delutbetaling),
                begrunnelseMindreBetalt = "begrunnelse",
            )
            service.opprettDelutbetalinger(
                request = opprettRequest,
                navIdent = NavAnsattFixture.DonaldDuck.navIdent,
            ).shouldBeRight()
            service.godkjennDelutbetaling(
                id = delutbetaling.id,
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
                utbetalinger = listOf(utbetaling1.copy(status = UtbetalingStatusType.INNSENDT)),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT, besluttetAv = NavAnsattFixture.DonaldDuck.navIdent)
                setRoller(
                    NavAnsattFixture.DonaldDuck,
                    setOf(NavAnsattRolle.kontorspesifikk(Rolle.ATTESTANT_UTBETALING, setOf(Innlandet.enhetsnummer))),
                )
            }.initialize(database.db)

            val service = createUtbetalingService()
            val delutbetaling = DelutbetalingRequest(
                id = UUID.randomUUID(),
                tilsagnId = Tilsagn1.id,
                gjorOppTilsagn = false,
                pris = 100.withValuta(Valuta.NOK),
            )
            val opprettRequest = OpprettDelutbetalingerRequest(
                utbetalingId = utbetaling1.id,
                delutbetalinger = listOf(delutbetaling),
                begrunnelseMindreBetalt = "begrunnelse",
            )
            service.opprettDelutbetalinger(
                request = opprettRequest,
                navIdent = NavAnsattFixture.MikkeMus.navIdent,
            ).shouldBeRight()
            service.godkjennDelutbetaling(
                id = delutbetaling.id,
                navIdent = NavAnsattFixture.DonaldDuck.navIdent,
            ).shouldBeRight()
        }

        test("returnering av delutbetaling setter den i RETURNERT status") {
            val domain = MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
                utbetalinger = listOf(utbetaling1.copy(status = UtbetalingStatusType.INNSENDT)),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT)
                setRoller(
                    NavAnsattFixture.DonaldDuck,
                    setOf(NavAnsattRolle.kontorspesifikk(Rolle.ATTESTANT_UTBETALING, setOf(Innlandet.enhetsnummer))),
                )
            }.initialize(database.db)

            val service = createUtbetalingService()
            val delutbetaling = DelutbetalingRequest(
                id = UUID.randomUUID(),
                tilsagnId = Tilsagn1.id,
                gjorOppTilsagn = false,
                pris = 100.withValuta(Valuta.NOK),
            )
            val opprettRequest = OpprettDelutbetalingerRequest(
                utbetalingId = utbetaling1.id,
                delutbetalinger = listOf(delutbetaling),
                begrunnelseMindreBetalt = "begrunnelse",
            )
            service.opprettDelutbetalinger(
                request = opprettRequest,
                navIdent = domain.ansatte[1].navIdent,
            ).shouldBeRight()
            service.returnerDelutbetaling(
                id = delutbetaling.id,
                aarsaker = listOf(DelutbetalingReturnertAarsak.ANNET),
                forklaring = "Maksbeløp er 5",
                navIdent = domain.ansatte[0].navIdent,
            ).shouldBeRight().status shouldBe DelutbetalingStatus.RETURNERT
        }

        test("sletting av delutbetaling skjer ikke ved valideringsfeil") {
            val domain = MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
                utbetalinger = listOf(utbetaling1.copy(status = UtbetalingStatusType.INNSENDT)),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT)
                setRoller(
                    NavAnsattFixture.MikkeMus,
                    setOf(NavAnsattRolle.kontorspesifikk(Rolle.ATTESTANT_UTBETALING, setOf(Innlandet.enhetsnummer))),
                )
            }.initialize(database.db)

            val service = createUtbetalingService()
            val delutbetaling = DelutbetalingRequest(
                id = UUID.randomUUID(),
                tilsagnId = Tilsagn1.id,
                gjorOppTilsagn = false,
                pris = 100.withValuta(Valuta.NOK),
            )
            val opprettRequest = OpprettDelutbetalingerRequest(
                utbetalingId = utbetaling1.id,
                delutbetalinger = listOf(delutbetaling),
                begrunnelseMindreBetalt = "begrunnelse",
            )
            service.opprettDelutbetalinger(
                request = opprettRequest,
                navIdent = domain.ansatte[0].navIdent,
            ).shouldBeRight()

            service.returnerDelutbetaling(
                id = delutbetaling.id,
                aarsaker = listOf(DelutbetalingReturnertAarsak.ANNET),
                forklaring = "Maksbeløp er 5",
                navIdent = domain.ansatte[1].navIdent,
            ).shouldBeRight().status shouldBe DelutbetalingStatus.RETURNERT

            service.opprettDelutbetalinger(
                request = OpprettDelutbetalingerRequest(utbetaling1.id, emptyList(), "begrunnelse"),
                navIdent = domain.ansatte[0].navIdent,
            ) shouldBeLeft listOf(
                FieldError.of("Utbetalingslinjer mangler"),
            )

            database.run {
                queries.delutbetaling.getOrError(delutbetaling.id)
            }
        }

        test("skal ikke kunne godkjenne delutbetaling hvis den er allerede godkjent") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
                utbetalinger = listOf(utbetaling1.copy(status = UtbetalingStatusType.INNSENDT)),
                delutbetalinger = listOf(delutbetaling1),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT)
                setDelutbetalingStatus(delutbetaling1, DelutbetalingStatus.GODKJENT)
            }.initialize(database.db)

            val service = createUtbetalingService()

            service.godkjennDelutbetaling(
                id = delutbetaling1.id,
                navIdent = NavAnsattFixture.MikkeMus.navIdent,
            ) shouldBeLeft listOf(
                FieldError.of("Utbetaling er ikke satt til attestering"),
                FieldError.of("Kan ikke attestere utbetalingen fordi du ikke er attestant ved tilsagnets kostnadssted (Nav Innlandet)"),
            )
        }

        test("oppdatering av returnert delutbetaling setter status TIL_ATTESTERING") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
                utbetalinger = listOf(utbetaling1.copy(status = UtbetalingStatusType.INNSENDT)),
                delutbetalinger = listOf(delutbetaling1),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT)
                setDelutbetalingStatus(delutbetaling1, DelutbetalingStatus.RETURNERT)
            }.initialize(database.db)

            val service = createUtbetalingService()
            service.opprettDelutbetalinger(
                request = OpprettDelutbetalingerRequest(
                    utbetalingId = utbetaling1.id,
                    delutbetalinger = listOf(
                        DelutbetalingRequest(
                            delutbetaling1.id,
                            Tilsagn1.id,
                            gjorOppTilsagn = false,
                            pris = 100.withValuta(Valuta.NOK),
                        ),
                    ),
                    begrunnelseMindreBetalt = "begrunnelse",
                ),
                navIdent = NavAnsattFixture.DonaldDuck.navIdent,
            ).shouldBeRight()

            database.run {
                queries.delutbetaling.getOrError(delutbetaling1.id)
                    .status shouldBe DelutbetalingStatus.TIL_ATTESTERING
                queries.utbetaling.getOrError(delutbetaling1.utbetalingId)
                    .status shouldBe UtbetalingStatusType.TIL_ATTESTERING
            }
        }

        test("skal bare kunne opprette delutbetaling når utbetalingsperiode og tilsagnsperiode overlapper") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
                utbetalinger = listOf(
                    utbetaling1.copy(
                        periode = Periode.forMonthOf(LocalDate.of(2023, 4, 4)),
                        status = UtbetalingStatusType.INNSENDT,
                    ),
                ),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT)
            }.initialize(database.db)

            val service = createUtbetalingService()

            val request = OpprettDelutbetalingerRequest(
                utbetalingId = utbetaling1.id,
                delutbetalinger = listOf(
                    DelutbetalingRequest(
                        UUID.randomUUID(),
                        Tilsagn1.id,
                        gjorOppTilsagn = false,
                        pris = 100.withValuta(Valuta.NOK),
                    ),
                ),
                begrunnelseMindreBetalt = "begrunnelse",
            )

            shouldThrow<IllegalArgumentException> {
                service.opprettDelutbetalinger(
                    request,
                    NavAnsattFixture.DonaldDuck.navIdent,
                )
            }.message shouldBe "Utbetalingsperiode og tilsagnsperiode overlapper ikke"
        }

        test("ved ny innsending til godkjenning skal delutbetalinger som ikke er inkludert i forespørselen slettes fra databasen") {
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
                status = UtbetalingStatusType.INNSENDT,
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

            val delutbetaling1 = DelutbetalingRequest(
                UUID.randomUUID(),
                tilsagn1.id,
                gjorOppTilsagn = false,
                pris = 5.withValuta(Valuta.NOK),
            )
            val delutbetaling2 = DelutbetalingRequest(
                UUID.randomUUID(),
                tilsagn2.id,
                gjorOppTilsagn = false,
                pris = 5.withValuta(Valuta.NOK),
            )
            service.opprettDelutbetalinger(
                OpprettDelutbetalingerRequest(utbetaling.id, listOf(delutbetaling1, delutbetaling2), null),
                domain.ansatte[0].navIdent,
            ).shouldBeRight()

            service.returnerDelutbetaling(
                id = delutbetaling1.id,
                aarsaker = listOf(DelutbetalingReturnertAarsak.FEIL_BELOP),
                forklaring = null,
                navIdent = domain.ansatte[1].navIdent,
            ).shouldBeRight().status shouldBe DelutbetalingStatus.RETURNERT

            service.opprettDelutbetalinger(
                OpprettDelutbetalingerRequest(utbetaling.id, listOf(delutbetaling1), "begrunnelse"),
                domain.ansatte[0].navIdent,
            ).shouldBeRight()

            val delutbetalinger = database.run { queries.delutbetaling.getByUtbetalingId(utbetaling.id) }
            delutbetalinger.size shouldBe 1
            delutbetalinger[0].id shouldBe delutbetaling1.id
        }

        test("alle delutbetalinger (selv godkjente) blir returnert når saksbehandler avviser en delutbetaling") {
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
                status = UtbetalingStatusType.INNSENDT,
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

            val delutbetaling1 = DelutbetalingRequest(
                UUID.randomUUID(),
                tilsagn1.id,
                gjorOppTilsagn = false,
                pris = 5.withValuta(Valuta.NOK),
            )
            val delutbetaling2 = DelutbetalingRequest(
                UUID.randomUUID(),
                tilsagn2.id,
                gjorOppTilsagn = false,
                pris = 5.withValuta(Valuta.NOK),
            )
            service.opprettDelutbetalinger(
                OpprettDelutbetalingerRequest(utbetaling.id, listOf(delutbetaling1, delutbetaling2), null),
                domain.ansatte[1].navIdent,
            ).shouldBeRight()

            service.godkjennDelutbetaling(
                delutbetaling1.id,
                domain.ansatte[0].navIdent,
            ).shouldBeRight().status shouldBe DelutbetalingStatus.GODKJENT

            service.returnerDelutbetaling(
                id = delutbetaling2.id,
                aarsaker = listOf(DelutbetalingReturnertAarsak.ANNET),
                forklaring = "Maksbeløp er 5",
                navIdent = domain.ansatte[0].navIdent,
            ).shouldBeRight().status shouldBe DelutbetalingStatus.RETURNERT

            database.run {
                queries.delutbetaling.getOrError(delutbetaling1.id).status shouldBe DelutbetalingStatus.RETURNERT
                queries.totrinnskontroll.getOrError(delutbetaling1.id, Totrinnskontroll.Type.OPPRETT).should {
                    it.besluttelse shouldBe Besluttelse.AVVIST
                    it.besluttetAv shouldBe Tiltaksadministrasjon
                }

                queries.totrinnskontroll.getOrError(delutbetaling2.id, Totrinnskontroll.Type.OPPRETT).should {
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
                    utbetaling1.copy(status = UtbetalingStatusType.INNSENDT),
                    utbetaling2.copy(status = UtbetalingStatusType.INNSENDT),
                ),
            ) {
                setTilsagnStatus(tilsagn1, TilsagnStatus.GODKJENT)
                setTilsagnStatus(tilsagn2, TilsagnStatus.GODKJENT)
            }.initialize(database.db)

            val service = createUtbetalingService()

            service.opprettDelutbetalinger(
                OpprettDelutbetalingerRequest(
                    utbetaling1.id,
                    listOf(
                        DelutbetalingRequest(
                            UUID.randomUUID(),
                            tilsagn1.id,
                            gjorOppTilsagn = false,
                            pris = 50.withValuta(Valuta.NOK),
                        ),
                        DelutbetalingRequest(
                            UUID.randomUUID(),
                            tilsagn2.id,
                            gjorOppTilsagn = false,
                            pris = 50.withValuta(Valuta.NOK),
                        ),
                    ),
                    begrunnelseMindreBetalt = "begrunnelse",
                ),
                domain.ansatte[0].navIdent,
            ).shouldBeRight()

            service.opprettDelutbetalinger(
                OpprettDelutbetalingerRequest(
                    utbetaling2.id,
                    listOf(
                        DelutbetalingRequest(
                            UUID.randomUUID(),
                            tilsagn1.id,
                            gjorOppTilsagn = false,
                            pris = 100.withValuta(Valuta.NOK),
                        ),
                    ),
                    begrunnelseMindreBetalt = "begrunnelse",
                ),
                domain.ansatte[0].navIdent,
            ).shouldBeRight()

            database.run {
                queries.delutbetaling.getByUtbetalingId(utbetaling1.id).should { (first, second) ->
                    first.pris shouldBe 50.withValuta(Valuta.NOK)
                    first.periode shouldBe Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 15))
                    first.lopenummer shouldBe 1
                    first.faktura.fakturanummer shouldBe "A-2025/1-1-1"

                    second.pris shouldBe 50.withValuta(Valuta.NOK)
                    second.periode shouldBe Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 15))
                    second.lopenummer shouldBe 1
                    second.faktura.fakturanummer shouldBe "A-2025/1-2-1"
                }

                queries.delutbetaling.getByUtbetalingId(utbetaling2.id).should { (first) ->
                    first.pris shouldBe 100.withValuta(Valuta.NOK)
                    first.lopenummer shouldBe 2
                    first.faktura.fakturanummer shouldBe "A-2025/1-1-2"
                    first.periode shouldBe Periode(LocalDate.of(2025, 1, 15), LocalDate.of(2025, 2, 1))
                }
            }
        }

        test("løpenummer og fakturanummer beholdes ved returnering og godkjenning av delutbetaling for samme tilsagn") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
                utbetalinger = listOf(utbetaling1.copy(status = UtbetalingStatusType.INNSENDT)),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT)
                setRoller(
                    NavAnsattFixture.MikkeMus,
                    setOf(NavAnsattRolle.kontorspesifikk(Rolle.ATTESTANT_UTBETALING, setOf(Innlandet.enhetsnummer))),
                )
            }.initialize(database.db)

            val service = createUtbetalingService()

            val delutbetaling1 = DelutbetalingRequest(
                UUID.randomUUID(),
                Tilsagn1.id,
                gjorOppTilsagn = false,
                pris = 5.withValuta(Valuta.NOK),
            )
            service.opprettDelutbetalinger(
                OpprettDelutbetalingerRequest(utbetaling1.id, listOf(delutbetaling1), "begrunnelse"),
                NavAnsattFixture.DonaldDuck.navIdent,
            ).shouldBeRight()

            database.run {
                queries.delutbetaling.getOrError(delutbetaling1.id).should {
                    it.id shouldBe delutbetaling1.id
                    it.lopenummer shouldBe 1
                    it.faktura.fakturanummer shouldBe "A-2025/1-1-1"
                }
            }

            service.returnerDelutbetaling(
                id = delutbetaling1.id,
                aarsaker = listOf(DelutbetalingReturnertAarsak.ANNET),
                forklaring = "Maksbeløp er 5",
                navIdent = NavAnsattFixture.MikkeMus.navIdent,
            ).shouldBeRight().status shouldBe DelutbetalingStatus.RETURNERT

            service.opprettDelutbetalinger(
                OpprettDelutbetalingerRequest(utbetaling1.id, listOf(delutbetaling1), "begrunnelse"),
                NavAnsattFixture.DonaldDuck.navIdent,
            ).shouldBeRight()

            database.run {
                queries.delutbetaling.getOrError(delutbetaling1.id).should {
                    it.id shouldBe delutbetaling1.id
                    it.lopenummer shouldBe 1
                    it.faktura.fakturanummer shouldBe "A-2025/1-1-1"
                }
            }

            service.returnerDelutbetaling(
                id = delutbetaling1.id,
                aarsaker = listOf(DelutbetalingReturnertAarsak.ANNET),
                forklaring = "Maksbeløp er 5",
                navIdent = NavAnsattFixture.MikkeMus.navIdent,
            ).shouldBeRight().status shouldBe DelutbetalingStatus.RETURNERT

            val delutbetaling2 = DelutbetalingRequest(
                UUID.randomUUID(),
                Tilsagn1.id,
                gjorOppTilsagn = false,
                pris = 5.withValuta(Valuta.NOK),
            )
            service.opprettDelutbetalinger(
                OpprettDelutbetalingerRequest(utbetaling1.id, listOf(delutbetaling2), "begrunnelse"),
                NavAnsattFixture.DonaldDuck.navIdent,
            ).shouldBeRight()

            database.run {
                queries.delutbetaling.getOrError(delutbetaling2.id).should {
                    it.id shouldBe delutbetaling2.id
                    it.lopenummer shouldBe 1
                    it.faktura.fakturanummer shouldBe "A-2025/1-1-1"
                }
            }
        }

        test("alle delutbetalinger blir returnert hvis tilsagn ikke har godkjent-status når delutbetaling blir forsøkt godkjent") {
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
                utbetalinger = listOf(utbetaling1.copy(status = UtbetalingStatusType.INNSENDT)),
                delutbetalinger = listOf(delutbetaling1, delutbetaling2),
            ) {
                setTilsagnStatus(tilsagn1, TilsagnStatus.GODKJENT)
                setTilsagnStatus(tilsagn2, TilsagnStatus.OPPGJORT)
                setDelutbetalingStatus(delutbetaling1, DelutbetalingStatus.TIL_ATTESTERING)
                setDelutbetalingStatus(delutbetaling2, DelutbetalingStatus.TIL_ATTESTERING)
                setRoller(
                    NavAnsattFixture.MikkeMus,
                    setOf(NavAnsattRolle.kontorspesifikk(Rolle.ATTESTANT_UTBETALING, setOf(Innlandet.enhetsnummer))),
                )
            }.initialize(database.db)

            val service = createUtbetalingService()

            service.godkjennDelutbetaling(
                id = delutbetaling1.id,
                navIdent = NavAnsattFixture.MikkeMus.navIdent,
            ).shouldBeRight().status shouldBe DelutbetalingStatus.RETURNERT

            database.run {
                queries.delutbetaling.getByUtbetalingId(utbetaling1.id).should { (first, second) ->
                    first.id shouldBe delutbetaling2.id
                    first.status shouldBe DelutbetalingStatus.RETURNERT

                    second.id shouldBe delutbetaling1.id
                    second.status shouldBe DelutbetalingStatus.RETURNERT
                }

                queries.totrinnskontroll.getOrError(delutbetaling1.id, Totrinnskontroll.Type.OPPRETT).should {
                    it.besluttetAv shouldBe Tiltaksadministrasjon
                    it.besluttelse shouldBe Besluttelse.AVVIST
                }

                queries.totrinnskontroll.getOrError(delutbetaling2.id, Totrinnskontroll.Type.OPPRETT).should {
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
                status = UtbetalingStatusType.INNSENDT,
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

            val tilsagnService: TilsagnService = mockk(relaxed = true)
            val service = createUtbetalingService(tilsagnService)

            val delutbetaling = DelutbetalingRequest(
                UUID.randomUUID(),
                tilsagn.id,
                gjorOppTilsagn = false,
                pris = 10.withValuta(Valuta.NOK),
            )
            service.opprettDelutbetalinger(
                OpprettDelutbetalingerRequest(utbetaling1.id, listOf(delutbetaling), begrunnelseMindreBetalt = null),
                NavAnsattFixture.DonaldDuck.navIdent,
            ).shouldBeRight()

            service.godkjennDelutbetaling(
                id = delutbetaling.id,
                navIdent = NavAnsattFixture.MikkeMus.navIdent,
            ).shouldBeRight().status shouldBe DelutbetalingStatus.OVERFORT_TIL_UTBETALING

            verify(exactly = 1) {
                tilsagnService.gjorOppTilsagnVedUtbetaling(Tilsagn1.id, any(), any(), any())
            }
        }

        test("utbetaling blir sendt som melding til økonomi når alle delutbetalinger er godkjent") {
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
                utbetalinger = listOf(utbetaling1.copy(status = UtbetalingStatusType.INNSENDT)),
            ) {
                setTilsagnStatus(tilsagn1, TilsagnStatus.GODKJENT)
                setTilsagnStatus(tilsagn2, TilsagnStatus.GODKJENT)
                setRoller(
                    NavAnsattFixture.MikkeMus,
                    setOf(NavAnsattRolle.kontorspesifikk(Rolle.ATTESTANT_UTBETALING, setOf(Innlandet.enhetsnummer))),
                )
            }.initialize(database.db)

            val service = createUtbetalingService()

            val delutbetaling1 = DelutbetalingRequest(
                UUID.randomUUID(),
                tilsagn1.id,
                gjorOppTilsagn = false,
                pris = 1.withValuta(Valuta.NOK),
            )
            val delutbetaling2 = DelutbetalingRequest(
                UUID.randomUUID(),
                tilsagn2.id,
                gjorOppTilsagn = false,
                pris = 2.withValuta(Valuta.NOK),
            )
            val opprettRequest = OpprettDelutbetalingerRequest(
                utbetalingId = utbetaling1.id,
                delutbetalinger = listOf(delutbetaling1, delutbetaling2),
                begrunnelseMindreBetalt = "begrunnelse",
            )
            service.opprettDelutbetalinger(
                request = opprettRequest,
                navIdent = NavAnsattFixture.DonaldDuck.navIdent,
            ).shouldBeRight()

            service.godkjennDelutbetaling(
                id = delutbetaling1.id,
                navIdent = NavAnsattFixture.MikkeMus.navIdent,
            ).shouldBeRight().status shouldBe DelutbetalingStatus.GODKJENT

            service.godkjennDelutbetaling(
                id = delutbetaling2.id,
                navIdent = NavAnsattFixture.MikkeMus.navIdent,
            ).shouldBeRight().status shouldBe DelutbetalingStatus.OVERFORT_TIL_UTBETALING

            database.run {
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
                utbetalinger = listOf(utbetaling1.copy(status = UtbetalingStatusType.INNSENDT)),
                delutbetalinger = listOf(delutbetaling1),
            ) {
                setTilsagnStatus(tilsagn1, TilsagnStatus.GODKJENT)
                setDelutbetalingStatus(delutbetaling1, DelutbetalingStatus.TIL_ATTESTERING)
                setRoller(
                    NavAnsattFixture.MikkeMus,
                    setOf(NavAnsattRolle.kontorspesifikk(Rolle.ATTESTANT_UTBETALING, setOf(Innlandet.enhetsnummer))),
                )
            }.initialize(database.db)

            val februarNorskTid = TidligstTidspunktForUtbetalingCalculator { _, _ ->
                LocalDate.of(2025, 2, 1).atStartOfDay(ZoneId.of("Europe/Oslo")).toInstant()
            }

            val service = createUtbetalingService(tidligstTidspunktForUtbetaling = februarNorskTid)

            service.godkjennDelutbetaling(
                id = delutbetaling1.id,
                navIdent = NavAnsattFixture.MikkeMus.navIdent,
            ).shouldBeRight().status shouldBe DelutbetalingStatus.OVERFORT_TIL_UTBETALING

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
                        status = UtbetalingStatusType.INNSENDT,
                        utbetalesTidligstTidspunkt = februarNorskTid,
                    ),
                ),
                delutbetalinger = listOf(delutbetaling1),
            ) {
                setTilsagnStatus(tilsagn1, TilsagnStatus.GODKJENT)
                setDelutbetalingStatus(delutbetaling1, DelutbetalingStatus.TIL_ATTESTERING)
                setRoller(
                    NavAnsattFixture.MikkeMus,
                    setOf(NavAnsattRolle.kontorspesifikk(Rolle.ATTESTANT_UTBETALING, setOf(Innlandet.enhetsnummer))),
                )
            }.initialize(database.db)

            val service = createUtbetalingService()

            service.godkjennDelutbetaling(
                id = delutbetaling1.id,
                navIdent = NavAnsattFixture.MikkeMus.navIdent,
            ).shouldBeRight().status shouldBe DelutbetalingStatus.OVERFORT_TIL_UTBETALING

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
                queries.utbetaling.getOrError(utbetaling1Id).innsender shouldBe Arrangor
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

        test("utbetales ikke automatisk hvis det allerede finnes en delutbetaling når arrangør godkjenner") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
                utbetalinger = listOf(utbetaling1Forhandsgodkjent),
                delutbetalinger = listOf(delutbetaling1),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT)
                setDelutbetalingStatus(delutbetaling1, DelutbetalingStatus.GODKJENT)
            }.initialize(database.db)

            val service = createUtbetalingService()

            service.godkjentAvArrangor(utbetaling1Id, kid = null).shouldBeRight(
                AutomatiskUtbetalingResult.DELUTBETALINGER_ALLEREDE_OPPRETTET,
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
                val delutbetaling = queries.delutbetaling.getByUtbetalingId(utbetaling1Id).shouldHaveSize(1).first()
                delutbetaling.status shouldBe DelutbetalingStatus.OVERFORT_TIL_UTBETALING
                delutbetaling.pris shouldBe 1000.withValuta(Valuta.NOK)

                queries.totrinnskontroll.getOrError(delutbetaling.id, Totrinnskontroll.Type.OPPRETT).should {
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
                queries.delutbetaling.getByUtbetalingId(utbetaling1Id).shouldBeEmpty()
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
                queries.delutbetaling.getByUtbetalingId(utbetaling1Id).shouldBeEmpty()
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
                queries.delutbetaling.getByUtbetalingId(utbetaling1Id).shouldBeEmpty()
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
                queries.delutbetaling.getByUtbetalingId(utbetaling1Id).shouldBeEmpty()
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
                queries.delutbetaling.getByUtbetalingId(utbetaling1Id).shouldBeEmpty()
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
                queries.delutbetaling.getByUtbetalingId(utbetaling1Id).first().should {
                    it.status shouldBe DelutbetalingStatus.OVERFORT_TIL_UTBETALING
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

            val tilsagnService: TilsagnService = mockk(relaxed = true)
            val service = createUtbetalingService(tilsagnService = tilsagnService)

            service.godkjentAvArrangor(utbetaling1.id, kid = null).shouldBeRight(
                AutomatiskUtbetalingResult.GODKJENT,
            )

            database.run {
                queries.delutbetaling.getByUtbetalingId(utbetaling1.id).first().should {
                    it.status shouldBe DelutbetalingStatus.OVERFORT_TIL_UTBETALING
                    it.gjorOppTilsagn shouldBe true
                }
            }

            verify(exactly = 1) {
                tilsagnService.gjorOppTilsagnVedUtbetaling(Tilsagn1.id, any(), any(), any())
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
                utbetalinger = listOf(utbetaling1.copy(status = UtbetalingStatusType.INNSENDT)),
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
        test("skal ikke prosessere faktura status eldre enn sist oppdatert") {
            val zoneId = ZoneId.of("Europe/Oslo")
            val lagretFakturaStatusSistOppdatert = ZonedDateTime.now(zoneId).toLocalDateTime()
            val delutbetalingMock = delutbetaling1.copy(
                id = UUID.randomUUID(),
                fakturanummer = "2025-abc-1",
                fakturaStatusSistOppdatert = lagretFakturaStatusSistOppdatert,
            )
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
                utbetalinger = listOf(
                    utbetaling1.copy(
                        status = UtbetalingStatusType.FERDIG_BEHANDLET,
                    ),
                ),
                delutbetalinger = listOf(
                    delutbetalingMock,
                ),
            ).initialize(database.db)

            val service = createUtbetalingService()

            database.run {
                service.oppdaterFakturaStatus(
                    delutbetalingMock.fakturanummer,
                    FakturaStatusType.FEILET,
                    lagretFakturaStatusSistOppdatert.minusMinutes(1),
                )

                val delutbetaling = queries.delutbetaling.getOrError(delutbetalingMock.id)
                val utbetaling = queries.utbetaling.getOrError(utbetaling1.id)
                val endringshistorikk =
                    queries.endringshistorikk.getEndringshistorikk(DocumentClass.UTBETALING, utbetaling1.id)
                utbetaling.status shouldBe UtbetalingStatusType.FERDIG_BEHANDLET
                endringshistorikk.entries.shouldBeEmpty()
                val diff =
                    Duration.between(delutbetaling.faktura.statusSistOppdatert!!, lagretFakturaStatusSistOppdatert)
                diff shouldBeLessThanOrEqualTo Duration.ofMillis(1)
            }
        }

        test("skal oppdatere utbetaling endringslogg når faktura status er utbetalt") {
            val lagretFakturaStatusSistOppdatert = ZonedDateTime.now(ZoneId.of("Europe/Oslo")).toLocalDateTime()
            val delutbetalingMock = delutbetaling1.copy(
                id = UUID.randomUUID(),
                fakturanummer = "2025-abc-1",
                fakturaStatusSistOppdatert = lagretFakturaStatusSistOppdatert,
                fakturaStatus = FakturaStatusType.SENDT,
            )
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
                utbetalinger = listOf(
                    utbetaling1.copy(
                        status = UtbetalingStatusType.FERDIG_BEHANDLET,
                    ),
                ),
                delutbetalinger = listOf(
                    delutbetalingMock,
                ),
            ).initialize(database.db)

            val service = createUtbetalingService()

            service.oppdaterFakturaStatus(
                delutbetalingMock.fakturanummer,
                FakturaStatusType.FULLT_BETALT,
                lagretFakturaStatusSistOppdatert.plusMinutes(1),
            )

            database.run {
                queries.delutbetaling.getOrError(delutbetalingMock.id).status shouldBe DelutbetalingStatus.UTBETALT
                queries.utbetaling.getOrError(utbetaling1.id).status shouldBe UtbetalingStatusType.UTBETALT
                queries.endringshistorikk.getEndringshistorikk(
                    DocumentClass.UTBETALING,
                    utbetaling1.id,
                ).entries.size shouldBe 1
            }
        }

        test("skal ikke oppdatere utbetaling endringslogg når delutbetaling allerede er utbetalt") {
            val lagretFakturaStatusSistOppdatert = ZonedDateTime.now(ZoneId.of("Europe/Oslo")).toLocalDateTime()
            val delutbetalingMock = delutbetaling1.copy(
                id = UUID.randomUUID(),
                fakturanummer = "2025-abc-1",
                fakturaStatusSistOppdatert = lagretFakturaStatusSistOppdatert,
                fakturaStatus = FakturaStatusType.DELVIS_BETALT,
            )
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
                utbetalinger = listOf(
                    utbetaling1.copy(
                        status = UtbetalingStatusType.FERDIG_BEHANDLET,
                    ),
                ),
                delutbetalinger = listOf(
                    delutbetalingMock,
                ),
            ).initialize(database.db)

            val service = createUtbetalingService()

            service.oppdaterFakturaStatus(
                delutbetalingMock.fakturanummer,
                FakturaStatusType.FULLT_BETALT,
                lagretFakturaStatusSistOppdatert.plusMinutes(1),
            )

            database.run {
                queries.delutbetaling.getOrError(delutbetalingMock.id).status shouldBe DelutbetalingStatus.UTBETALT
                queries.utbetaling.getOrError(utbetaling1.id).status shouldBe UtbetalingStatusType.UTBETALT
                queries.endringshistorikk.getEndringshistorikk(
                    DocumentClass.UTBETALING,
                    utbetaling1.id,
                ).entries.size shouldBe 0
            }
        }

        test("skal oppdatere utbetaling status til delvis hvis minst en delutbetaling er utbetalt") {
            val lagretFakturaStatusSistOppdatert = ZonedDateTime.now(ZoneId.of("Europe/Oslo")).toLocalDateTime()
            val delutbetalingMock = delutbetaling1.copy(
                id = UUID.randomUUID(),
                fakturanummer = "2025-abc-1",
                fakturaStatusSistOppdatert = lagretFakturaStatusSistOppdatert,
                fakturaStatus = FakturaStatusType.SENDT,
                lopenummer = 2,
            )
            val delutbetalingMock2 = delutbetaling1.copy(
                id = UUID.randomUUID(),
                tilsagnId = Tilsagn2.id,
                fakturanummer = "2025-abcd-1",
                fakturaStatusSistOppdatert = lagretFakturaStatusSistOppdatert,
                fakturaStatus = FakturaStatusType.SENDT,
                lopenummer = 3,
            )
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1, Tilsagn2),
                utbetalinger = listOf(
                    utbetaling1.copy(
                        status = UtbetalingStatusType.FERDIG_BEHANDLET,
                    ),
                ),
                delutbetalinger = listOf(
                    delutbetalingMock,
                    delutbetalingMock2,
                ),
            ).initialize(database.db)

            val service = createUtbetalingService()

            service.oppdaterFakturaStatus(
                delutbetalingMock.fakturanummer,
                FakturaStatusType.FULLT_BETALT,
                lagretFakturaStatusSistOppdatert.plusMinutes(1),
            )

            database.run {
                queries.utbetaling.getOrError(utbetaling1.id).status shouldBe UtbetalingStatusType.DELVIS_UTBETALT
            }
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
