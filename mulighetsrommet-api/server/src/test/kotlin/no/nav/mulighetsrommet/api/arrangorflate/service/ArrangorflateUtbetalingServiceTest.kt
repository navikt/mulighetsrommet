package no.nav.mulighetsrommet.api.arrangorflate.service

import arrow.core.left
import arrow.core.nel
import arrow.core.right
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.forAll
import io.kotest.data.row
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
import no.nav.mulighetsrommet.admin.arrangor.BetalingsinformasjonQuery
import no.nav.mulighetsrommet.api.ApplicationConfigTest
import no.nav.mulighetsrommet.api.TransactionalQueryContext
import no.nav.mulighetsrommet.api.arrangorflate.model.ArrangorflateOpprettUtbetaling
import no.nav.mulighetsrommet.api.arrangorflate.model.ArrangorflateUtbetaling
import no.nav.mulighetsrommet.api.avtale.model.PrismodellType
import no.nav.mulighetsrommet.api.domain.arrangor.Betalingsinformasjon
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.TotrinnskontrollType
import no.nav.mulighetsrommet.api.fixtures.ArrangorFixtures
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.Oppfolging1
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.api.fixtures.PrismodellFixtures
import no.nav.mulighetsrommet.api.fixtures.TilsagnFixtures.Tilsagn1
import no.nav.mulighetsrommet.api.fixtures.TilsagnFixtures.Tilsagn2
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.fixtures.UtbetalingFixtures.arrangorflateUtbetalingDto1
import no.nav.mulighetsrommet.api.fixtures.UtbetalingFixtures.utbetaling1
import no.nav.mulighetsrommet.api.fixtures.UtbetalingFixtures.utbetalingDto1
import no.nav.mulighetsrommet.api.fixtures.UtbetalingFixtures.utbetalingLinje1
import no.nav.mulighetsrommet.api.fixtures.setTilsagnStatus
import no.nav.mulighetsrommet.api.fixtures.setUtbetalingLinjeStatus
import no.nav.mulighetsrommet.api.tilsagn.TilsagnService
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningFastSatsPerTiltaksplassPerManed
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningFri
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.utbetaling.model.AutomatisertUtbetalingResult
import no.nav.mulighetsrommet.api.utbetaling.model.SatsPeriode
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFastSatsPerAvtaltTiltaksplassPerManed
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFastSatsPerTiltaksplassPerManed
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFri
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningPrisPerTimeOppfolging
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingLinjeStatus
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatusType
import no.nav.mulighetsrommet.api.utbetaling.service.GenererUtbetalingService
import no.nav.mulighetsrommet.api.utbetaling.service.TidligstTidspunktForUtbetalingCalculator
import no.nav.mulighetsrommet.api.utbetaling.service.UtbetalingService
import no.nav.mulighetsrommet.api.utbetaling.task.JournalforUtbetaling
import no.nav.mulighetsrommet.clamav.Content
import no.nav.mulighetsrommet.clamav.Vedlegg
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.FieldError
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.NOK
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltaksadministrasjon
import no.nav.mulighetsrommet.model.ValutaBelop
import no.nav.tiltak.okonomi.OkonomiBestillingMelding
import no.nav.tiltak.okonomi.Tilskuddstype
import no.nav.tiltak.okonomi.toOkonomiPart
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

private val BESTILLING_TOPIC = ApplicationConfigTest.kafka.topics.okonomiBestillingTopic

class ArrangorflateUtbetalingServiceTest : FunSpec({
    val database = extension(ApiDatabaseTestListener())

    afterEach {
        database.truncateAll()
    }

    var umiddelbarUtbetaling = TidligstTidspunktForUtbetalingCalculator { _, _ -> null }
    val betalingsinformasjon: BetalingsinformasjonQuery = mockk()

    fun createTilsagnService(): TilsagnService = TilsagnService(
        TilsagnService.Config(gyldigTilsagnPeriode = mapOf()),
        db = database.api,
        navAnsattService = mockk(),
    )

    fun createUtbetalingService(
        tilsagnService: TilsagnService = createTilsagnService(),
        genererUtbetalingService: GenererUtbetalingService = mockk(),
        journalforUtbetaling: JournalforUtbetaling = mockk(relaxed = true),
        tidligstTidspunktForUtbetaling: TidligstTidspunktForUtbetalingCalculator = umiddelbarUtbetaling,
    ): ArrangorflateUtbetalingService {
        val utbetalingService = UtbetalingService(
            config = UtbetalingService.Config(
                tidligstTidspunktForUtbetaling = tidligstTidspunktForUtbetaling,
            ),
            tilsagnService = tilsagnService,
            betalingsinformasjon = betalingsinformasjon,
        )
        return ArrangorflateUtbetalingService(
            db = database.api,
            utbetalingService = utbetalingService,
            genererUtbetalingService = genererUtbetalingService,
            journalforUtbetaling = journalforUtbetaling,
        )
    }

    context("opprettUtbetaling") {
        val periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1))

        beforeEach {
            coEvery { betalingsinformasjon.execute(any()) } returns Betalingsinformasjon.BBan(
                kontonummer = Kontonummer("12345678901"),
                kid = null,
            )
        }

        test("utbetaling for ForhandsgodkjentPrisPerManedsverk blir opprettet med tilskuddstype for investeringer") {
            MulighetsrommetTestDomain(
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
            ).initialize(database.api)

            val service = createUtbetalingService()

            val utbetaling = service.opprettUtbetaling(
                ArrangorflateOpprettUtbetaling(
                    gjennomforingId = AFT1.id,
                    periode = periode,
                    kidNummer = null,
                    pris = 1000.NOK,
                    vedlegg = emptyList(),
                ),
            ).shouldBeRight()

            utbetaling.tilskuddstype shouldBe Tilskuddstype.TILTAK_INVESTERINGER
            utbetaling.status shouldBe UtbetalingStatusType.TIL_BEHANDLING
        }

        test("utbetaling for AnnenAvtaltPris blir opprettet med tilskuddstype for driftstilskudd") {
            MulighetsrommetTestDomain(
                avtaler = listOf(AvtaleFixtures.gruppeAmo),
                gjennomforinger = listOf(GjennomforingFixtures.GruppeAmo1),
            ).initialize(database.api)

            val service = createUtbetalingService()

            val utbetaling = service.opprettUtbetaling(
                ArrangorflateOpprettUtbetaling(
                    gjennomforingId = GjennomforingFixtures.GruppeAmo1.id,
                    periode = periode,
                    kidNummer = null,
                    pris = 500.NOK,
                    vedlegg = emptyList(),
                ),
            ).shouldBeRight()

            utbetaling.tilskuddstype shouldBe Tilskuddstype.TILTAK_DRIFTSTILSKUDD
            utbetaling.status shouldBe UtbetalingStatusType.TIL_BEHANDLING
        }

        test("utbetaling for AvtaltPrisPerTimeOppfolgingPerDeltaker blir opprettet med tilskuddstype for driftstilskudd") {
            MulighetsrommetTestDomain(
                avtaler = listOf(AvtaleFixtures.oppfolging),
                gjennomforinger = listOf(Oppfolging1),
            ).initialize(database.api)

            val service = createUtbetalingService()

            val utbetaling = service.opprettUtbetaling(
                ArrangorflateOpprettUtbetaling(
                    gjennomforingId = Oppfolging1.id,
                    periode = periode,
                    kidNummer = null,
                    pris = 750.NOK,
                    vedlegg = emptyList(),
                ),
            ).shouldBeRight()

            utbetaling.tilskuddstype shouldBe Tilskuddstype.TILTAK_DRIFTSTILSKUDD
            utbetaling.status shouldBe UtbetalingStatusType.TIL_BEHANDLING
            utbetaling.beregning.shouldBeTypeOf<UtbetalingBeregningPrisPerTimeOppfolging>()
        }

        test("returnerer feil for prismodeller som ikke støttes") {
            val prisPerUkesverk = PrismodellFixtures.createPrismodellDbo(
                type = PrismodellType.AVTALT_PRIS_PER_UKESVERK,
            )
            val prisPerHeleUkesverk = PrismodellFixtures.createPrismodellDbo(
                type = PrismodellType.AVTALT_PRIS_PER_HELE_UKESVERK,
            )

            val service = createUtbetalingService()

            forAll(
                row(PrismodellFixtures.AvtaltPrisPerManedsverk),
                row(PrismodellFixtures.ForhandsgodkjentVtao),
                row(prisPerUkesverk),
                row(prisPerHeleUkesverk),
            ) { prismodell ->
                database.truncateAll()

                MulighetsrommetTestDomain(
                    prismodeller = listOf(prismodell),
                    avtaler = listOf(AvtaleFixtures.gruppeAmo.copy(prismodeller = listOf(prismodell.id))),
                    gjennomforinger = listOf(GjennomforingFixtures.GruppeAmo1.copy(prismodellId = prismodell.id)),
                ).initialize(database.api)

                service.opprettUtbetaling(
                    ArrangorflateOpprettUtbetaling(
                        gjennomforingId = GjennomforingFixtures.GruppeAmo1.id,
                        periode = periode,
                        kidNummer = null,
                        pris = 100.NOK,
                        vedlegg = emptyList(),
                    ),
                ) shouldBeLeft listOf(
                    FieldError.of("Kan ikke opprette utbetaling for denne tiltaksgjennomføringen"),
                )
            }
        }

        test("utbetaling blir journalført når den blir opprettet av Arrangør") {
            MulighetsrommetTestDomain(
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
            ).initialize(database.api)

            val journalforUtbetaling = mockk<JournalforUtbetaling>(relaxed = true)

            val service = createUtbetalingService(journalforUtbetaling = journalforUtbetaling)

            val vedlegg = listOf(
                Vedlegg(Content("text/plain", "test".toByteArray()), "test.txt"),
            )
            val utbetaling = service.opprettUtbetaling(
                ArrangorflateOpprettUtbetaling(
                    gjennomforingId = AFT1.id,
                    periode = periode,
                    kidNummer = null,
                    pris = 1000.NOK,
                    vedlegg = vedlegg,
                ),
            ).shouldBeRight()

            verify(exactly = 1) {
                journalforUtbetaling.schedule(
                    JournalforUtbetaling.TaskData(utbetaling.id, vedlegg),
                    any(),
                    any(),
                )
            }
        }
    }

    context("Automatisert utbetaling når arrangør godkjenner") {
        val utbetaling1Id = utbetaling1.id

        val utbetaling1Forhandsgodkjent = utbetaling1.copy(
            periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
            beregning = getForhandsgodkjentBeregning(
                periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                pris = 1000.NOK,
            ),
        )

        test("kan ikke godkjenne utbetaling hvis perioden ikke er passert") {
            val fremtidigPeriode = Periode(LocalDate.now().plusDays(1), LocalDate.now().plusMonths(2))
            val utbetalingFremtidigPeriode = utbetaling1Forhandsgodkjent.copy(
                periode = fremtidigPeriode,
                beregning = getForhandsgodkjentBeregning(
                    periode = fremtidigPeriode,
                    pris = 1000.NOK,
                ),
            )
            MulighetsrommetTestDomain(
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                utbetalinger = listOf(utbetalingFremtidigPeriode),
            ).initialize(database.api)

            val service = createUtbetalingService()

            service.godkjentAvArrangor(utbetaling1Id, kid = null) shouldBeLeft listOf(
                FieldError.of("Utbetalingen kan ikke godkjennes før perioden er passert"),
            )
        }

        test("kan ikke godkjenne utbetaling uten betalingsinformasjon") {
            val utbetalingUtenKonto = utbetaling1Forhandsgodkjent.copy(
                betalingsinformasjon = null,
            )
            MulighetsrommetTestDomain(
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                utbetalinger = listOf(utbetalingUtenKonto),
            ).initialize(database.api)

            val service = createUtbetalingService()

            service.godkjentAvArrangor(utbetaling1Id, kid = null) shouldBeLeft listOf(
                FieldError.of("Utbetalingen kan ikke godkjennes fordi kontonummer mangler."),
            )
        }

        test("kan ikke godkjenne utbetaling med blokkeringer") {
            MulighetsrommetTestDomain(
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                utbetalinger = listOf(utbetaling1Forhandsgodkjent),
            ) {
                queries.utbetaling.setBlokkeringer(utbetaling1Id, setOf(Utbetaling.Blokkering.UBEHANDLET_FORSLAG))
            }.initialize(database.api)

            val service = createUtbetalingService()

            service.godkjentAvArrangor(utbetaling1Id, kid = null) shouldBeLeft listOf(
                FieldError.of(
                    "Det finnes advarsler på deltakere som påvirker utbetalingen. Disse må fikses før utbetalingen kan sendes inn.",
                ),
            )
        }

        test("utbetaling blir journalført når arrangør godkjenner") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
                utbetalinger = listOf(utbetaling1Forhandsgodkjent),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT)
            }.initialize(database.api)

            val journalforUtbetaling = mockk<JournalforUtbetaling>(relaxed = true)
            val service = createUtbetalingService(journalforUtbetaling = journalforUtbetaling)

            service.godkjentAvArrangor(utbetaling1Id, kid = null).shouldBeRight()

            database.run {
                queries.utbetaling.getOrError(utbetaling1Id).innsending.shouldNotBeNull().tidspunkt.toLocalDate() shouldBe LocalDate.now()
            }

            verify(exactly = 1) {
                journalforUtbetaling.schedule(
                    JournalforUtbetaling.TaskData(utbetaling1Forhandsgodkjent.id, listOf()),
                    any(),
                    any(),
                )
            }
        }

        test("utbetaling kan ikke godkjennes flere ganger samtidig") {
            val utbetaling = utbetaling1Forhandsgodkjent.copy(
                beregning = getForhandsgodkjentBeregning(
                    periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                    pris = 1.NOK,
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
            }.initialize(database.api)

            val service = createUtbetalingService()

            val job1 = async(Dispatchers.Default) {
                service.godkjentAvArrangor(utbetaling1Id, kid = null)
            }
            val job2 = async(Dispatchers.Default) {
                service.godkjentAvArrangor(utbetaling1Id, kid = null)
            }

            listOf(job1.await(), job2.await()) shouldContainExactlyInAnyOrder listOf(
                AutomatisertUtbetalingResult.GODKJENT.right(),
                FieldError.of("Utbetaling er allerede godkjent").nel().left(),
            )
        }

        test("utbetales ikke hvis det allerede finnes en utbetalingslinje når arrangør godkjenner") {
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
            }.initialize(database.api)

            val service = createUtbetalingService()

            service.godkjentAvArrangor(utbetaling1Id, kid = null).shouldBeRight(
                AutomatisertUtbetalingResult.UTBETALINGLINJER_ALLEREDE_OPPRETTET,
            )
        }

        test("utbetales når det finnes et enkelt tilsagn med nok midler og det er godkjent") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(
                    Tilsagn1.copy(
                        beregning = getTilsagnBeregning(
                            pris = 1000.NOK,
                        ),
                    ),
                ),
                utbetalinger = listOf(utbetaling1Forhandsgodkjent),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT)
            }.initialize(database.api)

            val service = createUtbetalingService()

            service.godkjentAvArrangor(utbetaling1Id, kid = null).shouldBeRight(
                AutomatisertUtbetalingResult.GODKJENT,
            )

            database.run {
                val linje = queries.utbetalingLinje.getByUtbetalingId(utbetaling1Id).shouldHaveSize(1).first().also {
                    it.status shouldBe UtbetalingLinjeStatus.OVERFORT_TIL_UTBETALING
                    it.pris shouldBe 1000.NOK
                }

                queries.totrinnskontroll.getOrError(linje.id, TotrinnskontrollType.UTBETALING_LINJE_OPPRETTELSE)
                    .should {
                        it.behandletAv shouldBe Tiltaksadministrasjon
                        it.besluttetAv shouldBe Tiltaksadministrasjon
                    }

                queries.tilsagn.getOrError(Tilsagn1.id).should {
                    it.belopBrukt shouldBe 1000.NOK
                }

                val records = queries.kafkaProducerRecord.getRecords(50, listOf(BESTILLING_TOPIC))
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
            }.initialize(database.api)

            val service = createUtbetalingService()

            service.godkjentAvArrangor(utbetaling1Id, kid = null).shouldBeRight(
                AutomatisertUtbetalingResult.GODKJENT,
            )

            service.godkjentAvArrangor(utbetaling1Id, kid = null).shouldBeLeft(
                listOf(FieldError.of("Utbetaling er allerede godkjent")),
            )
        }

        test("ingen utbetaling hvis tilsagn ikke er godkjent") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
                utbetalinger = listOf(utbetaling1Forhandsgodkjent),
            ).initialize(database.api)

            val service = createUtbetalingService()

            service.godkjentAvArrangor(utbetaling1Id, kid = null).shouldBeRight(
                AutomatisertUtbetalingResult.FEIL_ANTALL_TILSAGN,
            )

            database.run {
                queries.utbetaling.getOrError(utbetaling1.id).status shouldBe UtbetalingStatusType.TIL_BEHANDLING
                queries.utbetalingLinje.getByUtbetalingId(utbetaling1Id).shouldBeEmpty()
            }
        }

        test("ingen utbetaling hvis ingen tilsagn") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                utbetalinger = listOf(utbetaling1Forhandsgodkjent),
            ).initialize(database.api)

            val service = createUtbetalingService()

            service.godkjentAvArrangor(utbetaling1Id, kid = null).shouldBeRight(
                AutomatisertUtbetalingResult.FEIL_ANTALL_TILSAGN,
            )

            database.run {
                queries.utbetaling.getOrError(utbetaling1.id).status shouldBe UtbetalingStatusType.TIL_BEHANDLING
                queries.utbetalingLinje.getByUtbetalingId(utbetaling1Id).shouldBeEmpty()
            }
        }

        test("ingen utbetaling hvis flere tilsagn") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1, Tilsagn2.copy(periode = Tilsagn1.periode)),
                utbetalinger = listOf(utbetaling1Forhandsgodkjent),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT)
                setTilsagnStatus(Tilsagn2, TilsagnStatus.GODKJENT)
            }.initialize(database.api)

            val service = createUtbetalingService()

            service.godkjentAvArrangor(utbetaling1Id, kid = null).shouldBeRight(
                AutomatisertUtbetalingResult.FEIL_ANTALL_TILSAGN,
            )

            database.run {
                queries.utbetaling.getOrError(utbetaling1.id).status shouldBe UtbetalingStatusType.TIL_BEHANDLING
                queries.utbetalingLinje.getByUtbetalingId(utbetaling1Id).shouldBeEmpty()
            }
        }

        test("ingen utbetaling hvis tilsagn ikke har nok penger") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(
                    Tilsagn1.copy(
                        beregning = getTilsagnBeregning(
                            pris = 1.NOK,
                        ),
                    ),
                ),
                utbetalinger = listOf(utbetaling1Forhandsgodkjent),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT)
            }.initialize(database.api)

            val service = createUtbetalingService()

            service.godkjentAvArrangor(utbetaling1Id, kid = null).shouldBeRight(
                AutomatisertUtbetalingResult.IKKE_NOK_PENGER,
            )

            database.run {
                queries.utbetaling.getOrError(utbetaling1.id).status shouldBe UtbetalingStatusType.TIL_BEHANDLING
                queries.utbetalingLinje.getByUtbetalingId(utbetaling1Id).shouldBeEmpty()
            }
        }

        test("ingen utbetaling når prismodell er fri") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
                utbetalinger = listOf(
                    utbetaling1Forhandsgodkjent.copy(
                        beregning = UtbetalingBeregningFri(
                            input = UtbetalingBeregningFri.Input(1.NOK),
                            output = UtbetalingBeregningFri.Output(1.NOK),
                        ),
                    ),
                ),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT)
            }.initialize(database.api)

            val service = createUtbetalingService()
            service.godkjentAvArrangor(utbetaling1Id, kid = null).shouldBeRight(
                AutomatisertUtbetalingResult.FEIL_PRISMODELL,
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
                            pris = 100.NOK,
                        ),
                    ),
                ),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT)
            }.initialize(database.api)

            val service = createUtbetalingService()

            service.godkjentAvArrangor(utbetaling1Id, kid = null).shouldBeRight(
                AutomatisertUtbetalingResult.GODKJENT,
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

        test("Tilsagn gjøres opp når siste dato i tilsagnsperioden er inkludert i utbetalingsperioden") {
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
                            pris = 100.NOK,
                        ),
                    ),
                ),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT)
            }.initialize(database.api)

            val service = createUtbetalingService()

            service.godkjentAvArrangor(utbetaling1.id, kid = null).shouldBeRight(
                AutomatisertUtbetalingResult.GODKJENT,
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
                            pris = 100.NOK,
                        ),
                    ),
                ),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT)
            }.initialize(database.api)

            val tilsagnService: TilsagnService = spyk(createTilsagnService())
            coEvery {
                with(any<TransactionalQueryContext>()) {
                    tilsagnService.gjorOppTilsagn(any(), any(), any())
                }
            } answers {
                FieldError.of("Noe feil skjedde").nel().left()
            }
            val service = createUtbetalingService(tilsagnService = tilsagnService)

            service.godkjentAvArrangor(
                utbetaling1.id,
                kid = null,
            ) shouldBeRight AutomatisertUtbetalingResult.VALIDERINGSFEIL

            database.run {
                queries.utbetaling.getOrError(utbetaling1.id).status shouldBe UtbetalingStatusType.TIL_BEHANDLING
                queries.utbetalingLinje.getByUtbetalingId(utbetaling1.id).shouldBeEmpty()
                queries.tilsagn.getOrError(Tilsagn1.id).status shouldBe TilsagnStatus.GODKJENT
            }
        }

        test("automatiserer utbetaling med FastSatsPerAvtaltTiltaksplassPerManed når arrangør godkjenner") {
            val januar = Periode.forMonthOf(LocalDate.of(2025, 1, 1))

            val tilsagnForAvtaltSats = Tilsagn1.copy(
                id = UUID.randomUUID(),
                gjennomforingId = GjennomforingFixtures.VTAO.id,
                periode = januar,
                belopBrukt = 0.NOK,
                beregning = TilsagnBeregningFastSatsPerTiltaksplassPerManed(
                    input = TilsagnBeregningFastSatsPerTiltaksplassPerManed.Input(
                        periode = januar,
                        sats = 7_321.NOK,
                        antallPlasser = 1,
                        stengt = setOf(),
                    ),
                    output = TilsagnBeregningFastSatsPerTiltaksplassPerManed.Output(pris = 7_321.NOK),
                ),
            )

            val utbetalingForAvtaltSats = utbetaling1.copy(
                gjennomforingId = GjennomforingFixtures.VTAO.id,
                periode = januar,
                beregning = UtbetalingBeregningFastSatsPerAvtaltTiltaksplassPerManed(
                    input = UtbetalingBeregningFastSatsPerAvtaltTiltaksplassPerManed.Input(
                        tilsagn = listOf(
                            UtbetalingBeregningFastSatsPerAvtaltTiltaksplassPerManed.TilsagnInput(
                                tilsagnId = tilsagnForAvtaltSats.id,
                                periode = januar,
                                beregnetBelop = 7_321.NOK,
                                gjenstaendeBelop = 7_321.NOK,
                            ),
                        ),
                    ),
                    output = UtbetalingBeregningFastSatsPerAvtaltTiltaksplassPerManed.Output(
                        pris = 7_321.NOK,
                        tilsagnBidrag = listOf(
                            UtbetalingBeregningFastSatsPerAvtaltTiltaksplassPerManed.TilsagnBidrag(
                                tilsagnId = tilsagnForAvtaltSats.id,
                                periode = januar,
                                bidrag = 7_321.NOK,
                            ),
                        ),
                    ),
                ),
                betalingsinformasjon = Betalingsinformasjon.BBan(Kontonummer("11111111111"), null),
            )

            MulighetsrommetTestDomain(
                tiltakstyper = listOf(TiltakstypeFixtures.VTAO),
                arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
                avtaler = listOf(AvtaleFixtures.VTAO),
                gjennomforinger = listOf(GjennomforingFixtures.VTAO),
                prismodeller = listOf(PrismodellFixtures.ForhandsgodkjentVtao),
                tilsagn = listOf(tilsagnForAvtaltSats),
                utbetalinger = listOf(utbetalingForAvtaltSats),
            ) {
                setTilsagnStatus(tilsagnForAvtaltSats, TilsagnStatus.GODKJENT)
            }.initialize(database.api)

            val service = createUtbetalingService()

            service.godkjentAvArrangor(utbetalingForAvtaltSats.id, kid = null).shouldBeRight(
                AutomatisertUtbetalingResult.GODKJENT,
            )
        }
    }

    context("avbrytUtbetaling") {
        val innsendtUtbetaling = utbetaling1.copy(
            innsendtAvArrangorTidspunkt = LocalDateTime.of(2025, 1, 31, 12, 0),
        )

        test("kan avbryte utbetaling når status er TIL_BEHANDLING") {
            MulighetsrommetTestDomain(
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                utbetalinger = listOf(innsendtUtbetaling.copy(status = UtbetalingStatusType.TIL_BEHANDLING)),
            ).initialize(database.api)

            val service = createUtbetalingService()

            service.avbrytUtbetaling(
                utbetalingId = utbetaling1.id,
                begrunnelse = "Feil opplysninger",
            ).shouldBeRight()

            val utbetaling = database.api.session { queries.arrangorflate.utbetaling.getOrError(utbetaling1.id) }
            utbetaling.should {
                it.status shouldBe UtbetalingStatusType.AVBRUTT
            }
        }

        test("kan avbryte utbetaling med status RETURNERT") {
            MulighetsrommetTestDomain(
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                utbetalinger = listOf(innsendtUtbetaling.copy(status = UtbetalingStatusType.RETURNERT)),
            ).initialize(database.api)

            val service = createUtbetalingService()

            service.avbrytUtbetaling(
                utbetalingId = utbetaling1.id,
                begrunnelse = "Trukket tilbake",
            ).shouldBeRight()
            val utbetaling = database.api.session { queries.arrangorflate.utbetaling.getOrError(utbetaling1.id) }
            utbetaling.should {
                it.status shouldBe UtbetalingStatusType.AVBRUTT
            }
        }

        test("kan ikke avbryte utbetaling med andre statuser") {
            MulighetsrommetTestDomain(
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                utbetalinger = listOf(innsendtUtbetaling),
            ).initialize(database.api)

            val service = createUtbetalingService()

            forAll(
                row(UtbetalingStatusType.GENERERT),
                row(UtbetalingStatusType.TIL_ATTESTERING),
                row(UtbetalingStatusType.FERDIG_BEHANDLET),
                row(UtbetalingStatusType.UTBETALT),
                row(UtbetalingStatusType.AVBRUTT),
                row(UtbetalingStatusType.DELVIS_UTBETALT),
            ) { status ->
                database.run {
                    queries.utbetaling.setStatus(utbetaling1.id, status)
                }

                service.avbrytUtbetaling(
                    utbetalingId = utbetaling1.id,
                    begrunnelse = "Forsøk på avbryting",
                ) shouldBeLeft listOf(
                    FieldError.of("Utbetalingen kan ikke avbrytes"),
                )
            }
        }
    }

    context("regenererUtbetaling") {
        val avbruttUtbetaling = utbetalingDto1.copy(
            status = UtbetalingStatusType.AVBRUTT,
            innsending = Utbetaling.Innsending(LocalDateTime.of(2025, 1, 31, 12, 0)),
            beregning = getForhandsgodkjentBeregning(
                periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                pris = 1000.NOK,
            ),
        )

        val avbruttArrangorflateUtbetaling = arrangorflateUtbetalingDto1.copy(
            status = UtbetalingStatusType.AVBRUTT,
            innsending = ArrangorflateUtbetaling.Innsending(LocalDateTime.of(2025, 1, 31, 12, 0)),
            beregning = getForhandsgodkjentBeregning(
                periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                pris = 1000.NOK,
            ),
        )

        test("kan regenerere avbrutt utbetaling med støttet beregningstype") {
            val genererUtbetalingService = mockk<GenererUtbetalingService>()
            coEvery { genererUtbetalingService.regenererUtbetaling(utbetalingDto1.id) } returns avbruttUtbetaling.copy(
                status = UtbetalingStatusType.GENERERT,
                innsending = null,
            )

            val service = createUtbetalingService(genererUtbetalingService = genererUtbetalingService)

            service.regenererUtbetaling(avbruttArrangorflateUtbetaling).shouldBeRight()
        }

        test("kan ikke regenerere utbetaling med status som ikke er AVBRUTT") {
            val service = createUtbetalingService()

            forAll(
                row(UtbetalingStatusType.GENERERT),
                row(UtbetalingStatusType.TIL_BEHANDLING),
                row(UtbetalingStatusType.TIL_ATTESTERING),
                row(UtbetalingStatusType.RETURNERT),
                row(UtbetalingStatusType.FERDIG_BEHANDLET),
                row(UtbetalingStatusType.UTBETALT),
                row(UtbetalingStatusType.DELVIS_UTBETALT),
            ) { status ->
                service.regenererUtbetaling(avbruttArrangorflateUtbetaling.copy(status = status)) shouldBeLeft listOf(
                    FieldError.of("Utbetalingen kan bare regenereres når den er avbrutt"),
                )
            }
        }

        test("kan ikke regenerere utbetaling uten innsending") {
            val service = createUtbetalingService()

            service.regenererUtbetaling(avbruttArrangorflateUtbetaling.copy(innsending = null)) shouldBeLeft listOf(
                FieldError.of("Utbetalingen kan bare regenereres når den er innsendt"),
            )
        }

        test("kan ikke regenerere utbetaling med beregningstype som ikke støttes") {
            val service = createUtbetalingService()

            val beregning = UtbetalingBeregningFri(
                input = UtbetalingBeregningFri.Input(1.NOK),
                output = UtbetalingBeregningFri.Output(1.NOK),
            )

            service.regenererUtbetaling(avbruttArrangorflateUtbetaling.copy(beregning = beregning)) shouldBeLeft listOf(
                FieldError.of("Utbetalingen kan ikke regenereres"),
            )
        }
    }
})

private fun getForhandsgodkjentBeregning(periode: Periode, pris: ValutaBelop) = UtbetalingBeregningFastSatsPerTiltaksplassPerManed(
    input = UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Input(
        satser = setOf(SatsPeriode(periode, 20205.NOK)),
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
)
