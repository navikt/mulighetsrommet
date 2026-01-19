package no.nav.mulighetsrommet.api.utbetaling

import arrow.core.Either
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.mulighetsrommet.api.arrangor.model.BankKonto
import no.nav.mulighetsrommet.api.avtale.model.AvtaltSats
import no.nav.mulighetsrommet.api.avtale.model.PrismodellType
import no.nav.mulighetsrommet.api.clients.kontoregisterOrganisasjon.KontonummerResponse
import no.nav.mulighetsrommet.api.clients.kontoregisterOrganisasjon.KontoregisterOrganisasjonClient
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.ArrangorFixtures
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.DeltakerFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.PrismodellFixtures
import no.nav.mulighetsrommet.api.fixtures.UtbetalingFixtures.utbetaling1
import no.nav.mulighetsrommet.api.gjennomforing.model.AvbrytGjennomforingAarsak
import no.nav.mulighetsrommet.api.utbetaling.model.DeltakelsePeriode
import no.nav.mulighetsrommet.api.utbetaling.model.FastSatsPerTiltaksplassPerManedBeregning
import no.nav.mulighetsrommet.api.utbetaling.model.PrisPerHeleUkeBeregning
import no.nav.mulighetsrommet.api.utbetaling.model.PrisPerManedBeregning
import no.nav.mulighetsrommet.api.utbetaling.model.PrisPerUkeBeregning
import no.nav.mulighetsrommet.api.utbetaling.model.SatsPeriode
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFastSatsPerTiltaksplassPerManed
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFri
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningOutputDeltakelse
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningPrisPerHeleUkesverk
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningPrisPerManedsverk
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningPrisPerUkesverk
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatusType
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.DeltakerStatusType
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.Kid
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Valuta
import no.nav.tiltak.okonomi.Tilskuddstype
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

class GenererUtbetalingServiceTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))
    val kontoregisterOrganisasjonClient: KontoregisterOrganisasjonClient = mockk(relaxed = true)

    afterEach {
        database.truncateAll()
    }

    fun createUtbetalingService(
        gyldigTilsagnPeriode: Map<Tiltakskode, Periode> = Tiltakskode.entries.associateWith {
            Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2030, 1, 1))
        },
        tidligstTidspunktForUtbetaling: TidligstTidspunktForUtbetalingCalculator = TidligstTidspunktForUtbetalingCalculator { _, _ -> null },
    ) = GenererUtbetalingService(
        config = GenererUtbetalingService.Config(gyldigTilsagnPeriode, tidligstTidspunktForUtbetaling),
        db = database.db,
        prismodeller = setOf(
            FastSatsPerTiltaksplassPerManedBeregning,
            PrisPerManedBeregning,
            PrisPerUkeBeregning,
            PrisPerHeleUkeBeregning,
        ),
        kontoregisterOrganisasjonClient = kontoregisterOrganisasjonClient,
    )

    coEvery { kontoregisterOrganisasjonClient.getKontonummerForOrganisasjon(Organisasjonsnummer("123456789")) } returns Either.Right(
        KontonummerResponse(
            mottaker = "123456789",
            kontonr = "12345678901",
        ),
    )

    coEvery { kontoregisterOrganisasjonClient.getKontonummerForOrganisasjon(Organisasjonsnummer("976663934")) } returns Either.Right(
        KontonummerResponse(
            mottaker = "976663934",
            kontonr = "12345678901",
        ),
    )

    val januar = Periode.forMonthOf(LocalDate.of(2025, 1, 1))
    val februar = Periode.forMonthOf(LocalDate.of(2025, 2, 1))
    val september = Periode.forMonthOf(LocalDate.of(2025, 9, 1))

    context("konfigurasjon for generering av utbetalinger") {
        beforeEach {
            MulighetsrommetTestDomain(
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                deltakere = listOf(
                    DeltakerFixtures.createDeltakerMedDeltakelsesmengderDbo(
                        AFT1.id,
                        startDato = LocalDate.of(2025, 1, 1),
                        sluttDato = LocalDate.of(2025, 2, 28),
                        statusType = DeltakerStatusType.DELTAR,
                        deltakelsesprosent = 100.0,
                    ),
                ),
            ).initialize(database.db)
        }

        test("genererer bare utbetaling når perioden er dekket av konfigurerte tilsagnsperioder") {
            val service = createUtbetalingService(
                gyldigTilsagnPeriode = mapOf(Tiltakskode.ARBEIDSFORBEREDENDE_TRENING to februar),
            )

            service.genererUtbetalingerForPeriode(januar).shouldBeEmpty()

            service.genererUtbetalingerForPeriode(februar).shouldHaveSize(1)
        }

        test("lagrer tidligste tidspunkt for utbetaling basert på konfigurert kalkulering") {
            var tidligstTidspunktForUtbetaling = TidligstTidspunktForUtbetalingCalculator { _, periode ->
                if (periode == januar) februar.start.atStartOfDay().toInstant(ZoneOffset.UTC) else null
            }
            val service = createUtbetalingService(
                tidligstTidspunktForUtbetaling = tidligstTidspunktForUtbetaling,
            )

            service.genererUtbetalingerForPeriode(januar).shouldHaveSize(1).should { (utbetaling) ->
                utbetaling.utbetalesTidligstTidspunkt shouldBe Instant.parse("2025-02-01T00:00:00.00Z")
            }

            service.genererUtbetalingerForPeriode(februar).shouldHaveSize(1).should { (utbetaling) ->
                utbetaling.utbetalesTidligstTidspunkt.shouldBeNull()
            }
        }
    }

    context("generering av utbetalinger") {
        val service = createUtbetalingService()

        val organisasjonsnummer = ArrangorFixtures.underenhet1.organisasjonsnummer

        val prismodellOppfolging = PrismodellFixtures.createPrismodellDbo(
            type = PrismodellType.AVTALT_PRIS_PER_MANEDSVERK,
            satser = listOf(AvtaltSats(LocalDate.of(2025, 1, 1), 100, Valuta.NOK)),
        )
        val avtaleOppfolging = AvtaleFixtures.oppfolging.copy(prismodeller = listOf(prismodellOppfolging.id))
        val oppfolging = GjennomforingFixtures.Oppfolging1.copy(prismodellId = prismodellOppfolging.id)

        beforeEach {
            MulighetsrommetTestDomain(
                arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
                avtaler = listOf(AvtaleFixtures.AFT, avtaleOppfolging),
                gjennomforinger = listOf(AFT1, oppfolging),
                prismodeller = listOf(PrismodellFixtures.ForhandsgodkjentAft, prismodellOppfolging),
            ).initialize(database.db)
        }

        test("genererer ikke utbetaling når deltakelser mangler") {
            service.genererUtbetalingerForPeriode(januar).shouldBeEmpty()
        }

        test("genererer ikke utbetaling når gjennomføring starter etter utbetalingsperioden") {
            MulighetsrommetTestDomain(
                gjennomforinger = listOf(
                    AFT1.copy(
                        startDato = LocalDate.of(2025, 2, 1),
                        sluttDato = LocalDate.of(2025, 2, 28),
                        status = GjennomforingStatusType.GJENNOMFORES,
                    ),
                ),
                deltakere = listOf(
                    DeltakerFixtures.createDeltakerMedDeltakelsesmengderDbo(
                        AFT1.id,
                        startDato = LocalDate.of(2025, 2, 1),
                        sluttDato = LocalDate.of(2025, 2, 28),
                        statusType = DeltakerStatusType.DELTAR,
                        deltakelsesprosent = 100.0,
                    ),
                ),
            ).initialize(database.db)

            service.genererUtbetalingerForPeriode(januar).shouldBeEmpty()
        }

        test("genererer ikke utbetaling når alle deltakelser ble avsluttet før utbetalingsperioden") {
            MulighetsrommetTestDomain(
                deltakere = listOf(
                    DeltakerFixtures.createDeltakerMedDeltakelsesmengderDbo(
                        AFT1.id,
                        startDato = LocalDate.of(2024, 12, 1),
                        sluttDato = LocalDate.of(2024, 12, 31),
                        statusType = DeltakerStatusType.HAR_SLUTTET,
                        deltakelsesprosent = 100.0,
                    ),
                ),
            ) {
                queries.gjennomforing.upsertGruppetiltak(
                    AFT1.copy(
                        startDato = LocalDate.of(2024, 12, 1),
                        sluttDato = LocalDate.of(2025, 1, 31),
                    ),
                )
            }.initialize(database.db)

            service.genererUtbetalingerForPeriode(januar).shouldBeEmpty()
        }

        test("genererer utbetaling med korrekt beløp for forhåndsgodkjent tiltak") {
            MulighetsrommetTestDomain(
                deltakere = listOf(
                    DeltakerFixtures.createDeltakerMedDeltakelsesmengderDbo(
                        AFT1.id,
                        startDato = LocalDate.of(2025, 1, 1),
                        sluttDato = LocalDate.of(2025, 1, 31),
                        statusType = DeltakerStatusType.DELTAR,
                        deltakelsesprosent = 100.0,
                    ),
                ),
            ).initialize(database.db)

            val utbetaling = service.genererUtbetalingerForPeriode(januar)
                .shouldHaveSize(1)
                .first()

            utbetaling.gjennomforing.id shouldBe AFT1.id
            utbetaling.bankKonto.shouldBeTypeOf<BankKonto.BBan>().kontonummer shouldBe Kontonummer("12345678901")
            utbetaling.beregning.output.belop shouldBe 20975
        }

        test("genererer utbetaling med kid-nummer fra forrige godkjente utbetaling fra arrangør") {
            MulighetsrommetTestDomain(
                deltakere = listOf(
                    DeltakerFixtures.createDeltakerMedDeltakelsesmengderDbo(
                        AFT1.id,
                        startDato = LocalDate.of(2025, 1, 1),
                        sluttDato = LocalDate.of(2025, 2, 28),
                        statusType = DeltakerStatusType.DELTAR,
                        deltakelsesprosent = 100.0,
                    ),
                ),
            ).initialize(database.db)

            val utbetaling = service.genererUtbetalingerForPeriode(januar).first()
            utbetaling.gjennomforing.id shouldBe AFT1.id
            utbetaling.bankKonto.shouldBeTypeOf<BankKonto.BBan>().kontonummer shouldBe Kontonummer("12345678901")
            utbetaling.kid shouldBe null

            database.run {
                queries.utbetaling.setKid(
                    id = utbetaling.id,
                    kid = Kid.parseOrThrow("006402710013"),
                )
            }

            val sisteKrav = service.genererUtbetalingerForPeriode(februar).first()
            sisteKrav.gjennomforing.id shouldBe AFT1.id
            sisteKrav.kid shouldBe Kid.parseOrThrow("006402710013")
        }

        test("genererer ikke utbetaling hvis det allerede finnes en med overlappende periode") {
            MulighetsrommetTestDomain(
                deltakere = listOf(
                    DeltakerFixtures.createDeltakerMedDeltakelsesmengderDbo(
                        AFT1.id,
                        startDato = LocalDate.of(2023, 2, 1),
                        sluttDato = LocalDate.of(2026, 6, 1),
                        statusType = DeltakerStatusType.DELTAR,
                        deltakelsesprosent = 100.0,
                    ),
                    DeltakerFixtures.createDeltakerMedDeltakelsesmengderDbo(
                        AFT1.id,
                        startDato = LocalDate.of(2023, 1, 1),
                        sluttDato = LocalDate.of(2026, 2, 1),
                        statusType = DeltakerStatusType.DELTAR,
                        deltakelsesprosent = 100.0,
                    ),
                ),
            ).initialize(database.db)

            service.genererUtbetalingerForPeriode(januar).shouldHaveSize(1)
            database.run { queries.utbetaling.getByArrangorIds(organisasjonsnummer).shouldHaveSize(1) }

            service.genererUtbetalingerForPeriode(februar).shouldHaveSize(1)
            database.run { queries.utbetaling.getByArrangorIds(organisasjonsnummer).shouldHaveSize(2) }

            // Februar finnes allerede så ingen nye
            service.genererUtbetalingerForPeriode(februar).shouldHaveSize(0)
            database.run { queries.utbetaling.getByArrangorIds(organisasjonsnummer).shouldHaveSize(2) }
        }

        test("genererer utbetaling hvis det allerede finnes en med overlappende periode som er tilskuddstype TILTAK_INVESTERINGER") {
            MulighetsrommetTestDomain(
                deltakere = listOf(
                    DeltakerFixtures.createDeltakerMedDeltakelsesmengderDbo(
                        AFT1.id,
                        startDato = LocalDate.of(2023, 1, 1),
                        sluttDato = LocalDate.of(2026, 2, 1),
                        statusType = DeltakerStatusType.DELTAR,
                        deltakelsesprosent = 100.0,
                    ),
                ),
                utbetalinger = listOf(
                    utbetaling1.copy(tilskuddstype = Tilskuddstype.TILTAK_INVESTERINGER),
                ),
            ).initialize(database.db)

            service.genererUtbetalingerForPeriode(januar).shouldHaveSize(1)
            database.run { queries.utbetaling.getByArrangorIds(organisasjonsnummer).shouldHaveSize(2) }
        }

        test("genererer en utbetaling for avtalt pris per månedsverk med korrekt beløp") {
            MulighetsrommetTestDomain(
                deltakere = listOf(
                    DeltakerFixtures.createDeltakerDbo(
                        oppfolging.id,
                        startDato = LocalDate.of(2025, 1, 1),
                        sluttDato = LocalDate.of(2025, 1, 31),
                        statusType = DeltakerStatusType.DELTAR,
                    ),
                ),
            ).initialize(database.db)

            val utbetaling = service.genererUtbetalingerForPeriode(januar)
                .shouldHaveSize(1)
                .first()

            utbetaling.gjennomforing.id shouldBe oppfolging.id
            utbetaling.beregning.shouldBeTypeOf<UtbetalingBeregningPrisPerManedsverk>().output.belop shouldBe 100
        }

        test("genererer en utbetaling for avtalt pris per ukesverk med korrekt beløp") {
            MulighetsrommetTestDomain(
                deltakere = listOf(
                    DeltakerFixtures.createDeltakerDbo(
                        oppfolging.id,
                        startDato = LocalDate.of(2025, 1, 1),
                        sluttDato = LocalDate.of(2025, 1, 31),
                        statusType = DeltakerStatusType.DELTAR,
                    ),
                ),
                prismodeller = listOf(prismodellOppfolging.copy(type = PrismodellType.AVTALT_PRIS_PER_UKESVERK)),
            ).initialize(database.db)

            val utbetaling = service.genererUtbetalingerForPeriode(januar)
                .shouldHaveSize(1)
                .first()

            utbetaling.gjennomforing.id shouldBe oppfolging.id
            utbetaling.beregning.shouldBeTypeOf<UtbetalingBeregningPrisPerUkesverk>().output.belop shouldBe 460
        }

        test("utbetalinger blir oppdatert med ny beregning når avtalens prismodell endres") {
            MulighetsrommetTestDomain(
                deltakere = listOf(
                    DeltakerFixtures.createDeltakerDbo(
                        oppfolging.id,
                        startDato = LocalDate.of(2025, 1, 1),
                        sluttDato = LocalDate.of(2025, 1, 31),
                        statusType = DeltakerStatusType.DELTAR,
                    ),
                ),
            ).initialize(database.db)

            val generertUtbetaling = service.genererUtbetalingerForPeriode(januar).shouldHaveSize(1).first()
            generertUtbetaling.beregning.shouldBeTypeOf<UtbetalingBeregningPrisPerManedsverk>()

            database.run {
                queries.prismodell.upsert(
                    prismodellOppfolging.copy(type = PrismodellType.AVTALT_PRIS_PER_UKESVERK),
                )
            }

            val oppdatertUtbetaling = service.oppdaterUtbetalingerForGjennomforing(oppfolging.id)
                .shouldHaveSize(1).first()
            oppdatertUtbetaling.id shouldBe generertUtbetaling.id
            oppdatertUtbetaling.beregning.shouldBeTypeOf<UtbetalingBeregningPrisPerUkesverk>()
        }

        test("innsendt fri utbetaling blir ikke slettet hvis avtalens prismodell endres") {
            val domain = MulighetsrommetTestDomain(
                deltakere = listOf(
                    DeltakerFixtures.createDeltakerDbo(
                        oppfolging.id,
                        startDato = LocalDate.of(2025, 1, 1),
                        sluttDato = LocalDate.of(2025, 1, 31),
                        statusType = DeltakerStatusType.DELTAR,
                    ),
                ),
                utbetalinger = listOf(
                    utbetaling1.copy(
                        gjennomforingId = oppfolging.id,
                        innsender = NavIdent("B123456"),
                        status = UtbetalingStatusType.INNSENDT,
                        beregning = UtbetalingBeregningFri(
                            input = UtbetalingBeregningFri.Input(1000),
                            output = UtbetalingBeregningFri.Output(1000),
                        ),
                    ),
                ),
            ) {

                queries.prismodell.upsert(
                    prismodellOppfolging.copy(type = PrismodellType.AVTALT_PRIS_PER_MANEDSVERK),
                )
            }.initialize(database.db)

            service.oppdaterUtbetalingerForGjennomforing(oppfolging.id).shouldHaveSize(0)

            database.run {
                val utbetaling = queries.utbetaling.getOrError(domain.utbetalinger[0].id)
                utbetaling.beregning.shouldBeTypeOf<UtbetalingBeregningFri>()
            }
        }

        test("utbetalinger slettes når prismodell ikke lengre kan genereres av systemet") {
            MulighetsrommetTestDomain(
                deltakere = listOf(
                    DeltakerFixtures.createDeltakerDbo(
                        oppfolging.id,
                        startDato = LocalDate.of(2025, 1, 1),
                        sluttDato = LocalDate.of(2025, 1, 31),
                        statusType = DeltakerStatusType.DELTAR,
                    ),
                ),
            ).initialize(database.db)

            val generertUtbetaling = service.genererUtbetalingerForPeriode(januar).shouldHaveSize(1).first()
            generertUtbetaling.beregning.shouldBeTypeOf<UtbetalingBeregningPrisPerManedsverk>()

            database.run {
                queries.prismodell.upsert(
                    prismodellOppfolging.copy(type = PrismodellType.ANNEN_AVTALT_PRIS),
                )
            }

            service.oppdaterUtbetalingerForGjennomforing(oppfolging.id).shouldBeEmpty()

            // Sjekk at utbetalingen er fjernet fra databasen etter at avtalen er endret tilbake til ANNEN_AVTALT_PRIS
            database.run {
                queries.utbetaling.get(generertUtbetaling.id).shouldBeNull()
            }
        }
    }

    context("gjennomføringer og deltakere med feil i datagrunnlaget") {
        val service = createUtbetalingService()

        beforeEach {
            MulighetsrommetTestDomain(
                arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
            ).initialize(database.db)
        }

        test("genererer ikke utbetaling når gjennomføringen ble avbrutt før utbetalingsperioden, selv om deltaker har sluttdato i utbetalingsperioden") {
            MulighetsrommetTestDomain(
                deltakere = listOf(
                    DeltakerFixtures.createDeltakerMedDeltakelsesmengderDbo(
                        AFT1.id,
                        startDato = LocalDate.of(2024, 12, 1),
                        sluttDato = LocalDate.of(2025, 1, 31),
                        statusType = DeltakerStatusType.HAR_SLUTTET,
                        deltakelsesprosent = 100.0,
                    ),
                ),
            ) {
                queries.gjennomforing.upsertGruppetiltak(AFT1.copy(sluttDato = LocalDate.of(2025, 1, 31)))
                queries.gjennomforing.setStatus(
                    AFT1.id,
                    status = GjennomforingStatusType.AVBRUTT,
                    tidspunkt = LocalDate.of(2024, 12, 31).atStartOfDay(),
                    aarsaker = listOf(AvbrytGjennomforingAarsak.BUDSJETT_HENSYN),
                    forklaring = null,
                )
            }.initialize(database.db)

            service.genererUtbetalingerForPeriode(januar).shouldBeEmpty()
        }

        test("genererer ikke utbetaling når gjennomføringen ble avsluttet før utbetalingsperioden, selv om deltaker har sluttdato i utbetalingsperioden") {
            MulighetsrommetTestDomain(
                deltakere = listOf(
                    DeltakerFixtures.createDeltakerMedDeltakelsesmengderDbo(
                        AFT1.id,
                        startDato = LocalDate.of(2024, 12, 1),
                        sluttDato = LocalDate.of(2025, 1, 31),
                        statusType = DeltakerStatusType.HAR_SLUTTET,
                        deltakelsesprosent = 100.0,
                    ),
                ),
            ) {
                queries.gjennomforing.upsertGruppetiltak(AFT1.copy(sluttDato = LocalDate.of(2025, 1, 31)))
                queries.gjennomforing.setStatus(
                    AFT1.id,
                    status = GjennomforingStatusType.AVSLUTTET,
                    tidspunkt = LocalDate.of(2024, 12, 31).atStartOfDay(),
                    aarsaker = null,
                    forklaring = null,
                )
            }.initialize(database.db)

            service.genererUtbetalingerForPeriode(januar).shouldBeEmpty()
        }

        test("genererer ikke utbetaling når gjennomføringen ble avsluttet før den startet") {
            MulighetsrommetTestDomain(
                deltakere = listOf(
                    DeltakerFixtures.createDeltakerMedDeltakelsesmengderDbo(
                        AFT1.id,
                        startDato = LocalDate.of(2025, 1, 1),
                        sluttDato = LocalDate.of(2025, 1, 1),
                        statusType = DeltakerStatusType.HAR_SLUTTET,
                        deltakelsesprosent = 100.0,
                    ),
                ),
            ) {
                queries.gjennomforing.upsertGruppetiltak(
                    AFT1.copy(
                        startDato = LocalDate.of(2025, 1, 1),
                        sluttDato = null,
                    ),
                )
                queries.gjennomforing.setStatus(
                    AFT1.id,
                    status = GjennomforingStatusType.AVLYST,
                    tidspunkt = LocalDate.of(2024, 12, 31).atStartOfDay(),
                    aarsaker = listOf(AvbrytGjennomforingAarsak.BUDSJETT_HENSYN),
                    forklaring = null,
                )
            }.initialize(database.db)

            service.genererUtbetalingerForPeriode(januar).shouldBeEmpty()
        }

        test("deltaker som har sluttet, men med åpen sluttdato, blir ikke med i utbetalingen") {
            val domain = MulighetsrommetTestDomain(
                gjennomforinger = listOf(AFT1),
                deltakere = listOf(
                    DeltakerFixtures.createDeltakerMedDeltakelsesmengderDbo(
                        AFT1.id,
                        startDato = LocalDate.of(2025, 1, 1),
                        sluttDato = null,
                        statusType = DeltakerStatusType.DELTAR,
                        deltakelsesprosent = 100.0,
                    ),
                    DeltakerFixtures.createDeltakerMedDeltakelsesmengderDbo(
                        AFT1.id,
                        startDato = LocalDate.of(2025, 1, 1),
                        sluttDato = null,
                        statusType = DeltakerStatusType.HAR_SLUTTET,
                        deltakelsesprosent = 100.0,
                    ),
                ),
            ).initialize(database.db)

            val utbetaling = service.genererUtbetalingerForPeriode(januar).first()

            utbetaling.beregning.input
                .shouldBeTypeOf<UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Input>()
                .should {
                    it.deltakelser.shouldHaveSize(1).first().deltakelseId.shouldBe(domain.deltakere[0].id)
                }
        }
    }

    context("rekalkulering av utbetalinger") {
        val service = createUtbetalingService()

        val prismodell = PrismodellFixtures.createPrismodellDbo(
            type = PrismodellType.AVTALT_PRIS_PER_MANEDSVERK,
            satser = listOf(
                AvtaltSats(LocalDate.of(2026, 2, 1), 100, Valuta.NOK),
            ),
        )

        val avtale = AvtaleFixtures.oppfolging.copy(prismodeller = listOf(prismodell.id))

        val gjennomforing = GjennomforingFixtures.Oppfolging1.copy(prismodellId = prismodell.id)

        val deltaker = DeltakerFixtures.createDeltakerMedDeltakelsesmengderDbo(
            gjennomforing.id,
            startDato = LocalDate.of(2026, 2, 1),
            sluttDato = LocalDate.of(2026, 2, 15),
            statusType = DeltakerStatusType.DELTAR,
            deltakelsesprosent = 100.0,
        )

        val periode = Periode.forMonthOf(LocalDate.of(2026, 2, 1))
        val beregning = UtbetalingBeregningPrisPerManedsverk(
            input = UtbetalingBeregningPrisPerManedsverk.Input(
                satser = setOf(SatsPeriode(periode, 100)),
                stengt = setOf(),
                deltakelser = setOf(DeltakelsePeriode(deltaker.id, periode)),
            ),
            output = UtbetalingBeregningPrisPerManedsverk.Output(
                belop = 100,
                deltakelser = setOf(
                    UtbetalingBeregningOutputDeltakelse(
                        deltaker.id,
                        setOf(UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(periode, 1.0, 100)),
                    ),
                ),
            ),
        )

        beforeEach {
            MulighetsrommetTestDomain(
                arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
                prismodeller = listOf(prismodell),
                avtaler = listOf(avtale),
                gjennomforinger = listOf(gjennomforing),
            ).initialize(database.db)
        }

        test("oppdaterer beregnet utbetaling når deltakelser endres") {
            MulighetsrommetTestDomain(
                utbetalinger = listOf(
                    utbetaling1.copy(
                        gjennomforingId = gjennomforing.id,
                        periode = periode,
                        beregning = beregning,
                    ),
                ),
                deltakere = listOf(deltaker),
            ).initialize(database.db)

            val utbetaling = service.oppdaterUtbetalingerForGjennomforing(gjennomforing.id)
                .shouldHaveSize(1)
                .first()

            utbetaling.beregning.output.shouldBeTypeOf<UtbetalingBeregningPrisPerManedsverk.Output>().belop shouldBe 50
        }

        test("oppdaterer ikke utbetaling hvis den allerede er godkjent av arrangør") {
            MulighetsrommetTestDomain(
                utbetalinger = listOf(
                    utbetaling1.copy(
                        gjennomforingId = gjennomforing.id,
                        periode = periode,
                        beregning = beregning,
                        status = UtbetalingStatusType.INNSENDT,
                    ),
                ),
                deltakere = listOf(deltaker),
            ) {
                queries.utbetaling.setGodkjentAvArrangor(utbetaling1.id, LocalDateTime.now())
            }.initialize(database.db)

            service.oppdaterUtbetalingerForGjennomforing(gjennomforing.id).shouldBeEmpty()

            database.run {
                queries.utbetaling.getOrError(utbetaling1.id).beregning.output
                    .shouldBeTypeOf<UtbetalingBeregningPrisPerManedsverk.Output>()
                    .belop shouldBe 100
            }
        }
    }

    context("utbetalinger for avtalt pris per hele ukesverk") {
        val gyldigTilsagnPeriode = Periode(LocalDate.of(2024, 12, 1), LocalDate.of(2027, 1, 1))
        val service = createUtbetalingService(mapOf(Tiltakskode.OPPFOLGING to gyldigTilsagnPeriode))

        val prismodell = PrismodellFixtures.createPrismodellDbo(
            type = PrismodellType.AVTALT_PRIS_PER_HELE_UKESVERK,
            satser = listOf(
                AvtaltSats(LocalDate.of(2024, 1, 1), 100, Valuta.NOK),
            ),
        )

        val avtale = AvtaleFixtures.oppfolging.copy(prismodeller = listOf(prismodell.id))

        val gjennomforing = GjennomforingFixtures.Oppfolging1.copy(
            startDato = LocalDate.of(2024, 1, 1),
            sluttDato = LocalDate.of(2027, 1, 1),
            prismodellId = prismodell.id,
        )

        beforeEach {
            MulighetsrommetTestDomain(
                arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
                prismodeller = listOf(prismodell),
                avtaler = listOf(avtale),
                gjennomforinger = listOf(gjennomforing),
            ).initialize(database.db)
        }

        test("justerer utbetalingsperioden til hele uker og genererer korrekt beløp") {
            MulighetsrommetTestDomain(
                deltakere = listOf(
                    DeltakerFixtures.createDeltakerDbo(
                        gjennomforing.id,
                        startDato = LocalDate.of(2024, 12, 1),
                        sluttDato = LocalDate.of(2024, 12, 31),
                        statusType = DeltakerStatusType.HAR_SLUTTET,
                    ),
                ),
            ) {
                queries.gjennomforing.upsertGruppetiltak(gjennomforing.copy(sluttDato = LocalDate.of(2024, 12, 31)))
            }.initialize(database.db)

            val utbetaling = service.genererUtbetalingerForPeriode(januar)
                .shouldHaveSize(1)
                .first()

            utbetaling.periode shouldBe Periode(LocalDate.of(2024, 12, 30), LocalDate.of(2025, 2, 3))
            utbetaling.gjennomforing.id shouldBe gjennomforing.id
            utbetaling.beregning.shouldBeTypeOf<UtbetalingBeregningPrisPerHeleUkesverk>().output.belop shouldBe 100
        }

        test("ikke for deltakelse 29. sep fordi uken skal med i oktober") {
            MulighetsrommetTestDomain(
                deltakere = listOf(
                    DeltakerFixtures.createDeltakerDbo(
                        gjennomforing.id,
                        startDato = LocalDate.of(2025, 9, 29),
                        sluttDato = null,
                        statusType = DeltakerStatusType.DELTAR,
                    ),
                ),
            ).initialize(database.db)

            service.genererUtbetalingerForPeriode(september).shouldHaveSize(0)
        }

        test("genererer utbetalinger for sammenhengende perioder splittet opp per hele uker") {
            MulighetsrommetTestDomain(
                deltakere = listOf(
                    DeltakerFixtures.createDeltakerDbo(
                        gjennomforing.id,
                        startDato = LocalDate.of(2025, 1, 1),
                        sluttDato = LocalDate.of(2025, 12, 31),
                        statusType = DeltakerStatusType.DELTAR,
                    ),
                ),
            ).initialize(database.db)

            service.genererUtbetalingerForPeriode(Periode.forMonthOf(LocalDate.of(2025, 9, 1)))
                .shouldHaveSize(1).should { (utbetaling) ->
                    utbetaling.periode shouldBe Periode(LocalDate.of(2025, 9, 1), LocalDate.of(2025, 9, 29))
                }
            service.genererUtbetalingerForPeriode(Periode.forMonthOf(LocalDate.of(2025, 10, 1)))
                .shouldHaveSize(1).should { (utbetaling) ->
                    utbetaling.periode shouldBe Periode(LocalDate.of(2025, 9, 29), LocalDate.of(2025, 11, 3))
                }
            service.genererUtbetalingerForPeriode(Periode.forMonthOf(LocalDate.of(2025, 11, 1)))
                .shouldHaveSize(1).should { (utbetaling) ->
                    utbetaling.periode shouldBe Periode(LocalDate.of(2025, 11, 3), LocalDate.of(2025, 12, 1))
                }
            service.genererUtbetalingerForPeriode(Periode.forMonthOf(LocalDate.of(2025, 12, 1)))
                .shouldHaveSize(1).should { (utbetaling) ->
                    utbetaling.periode shouldBe Periode(LocalDate.of(2025, 12, 1), LocalDate.of(2026, 1, 5))
                }
        }
    }

    context("regenerering") {
        val service = createUtbetalingService()

        val prismodell = PrismodellFixtures.createPrismodellDbo(
            type = PrismodellType.AVTALT_PRIS_PER_MANEDSVERK,
            satser = listOf(
                AvtaltSats(LocalDate.of(2025, 1, 1), 100, Valuta.NOK),
            ),
        )

        val avtale = AvtaleFixtures.oppfolging.copy(prismodeller = listOf(prismodell.id))

        val oppfolging = GjennomforingFixtures.Oppfolging1.copy(prismodellId = prismodell.id)

        beforeEach {
            MulighetsrommetTestDomain(
                arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
                avtaler = listOf(avtale),
                gjennomforinger = listOf(oppfolging),
                prismodeller = listOf(prismodell),
            ).initialize(database.db)
        }

        test("regenert er lik forrige") {
            MulighetsrommetTestDomain(
                deltakere = listOf(
                    DeltakerFixtures.createDeltakerDbo(
                        oppfolging.id,
                        startDato = LocalDate.of(2025, 1, 1),
                        sluttDato = LocalDate.of(2025, 1, 31),
                        statusType = DeltakerStatusType.DELTAR,
                    ),
                ),
            ).initialize(database.db)

            var utbetaling = service.genererUtbetalingerForPeriode(januar).shouldHaveSize(1).first()

            utbetaling = database.run {
                queries.utbetaling.setStatus(utbetaling.id, UtbetalingStatusType.AVBRUTT)
                queries.utbetaling.getOrError(utbetaling.id)
            }

            val regenerert = service.regenererUtbetaling(utbetaling).shouldNotBeNull()
            regenerert.id shouldNotBe utbetaling.id
            regenerert.beregning shouldBe utbetaling.beregning
        }

        test("regenering feiler hvis allerede regenerert") {
            MulighetsrommetTestDomain(
                deltakere = listOf(
                    DeltakerFixtures.createDeltakerDbo(
                        oppfolging.id,
                        startDato = LocalDate.of(2025, 1, 1),
                        sluttDato = LocalDate.of(2025, 1, 31),
                        statusType = DeltakerStatusType.DELTAR,
                    ),
                ),
            ).initialize(database.db)

            var utbetaling = service.genererUtbetalingerForPeriode(januar).shouldHaveSize(1).first()

            utbetaling = database.run {
                queries.utbetaling.setStatus(utbetaling.id, UtbetalingStatusType.AVBRUTT)
                queries.utbetaling.getOrError(utbetaling.id)
            }

            service.regenererUtbetaling(utbetaling)
            shouldThrow<IllegalArgumentException> {
                service.regenererUtbetaling(utbetaling).shouldBeNull()
            }
        }
    }
})
