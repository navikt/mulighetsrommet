package no.nav.mulighetsrommet.api.utbetaling

import arrow.core.Either
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.mulighetsrommet.api.OkonomiConfig
import no.nav.mulighetsrommet.api.avtale.model.AvtaltSats
import no.nav.mulighetsrommet.api.avtale.model.PrismodellType
import no.nav.mulighetsrommet.api.clients.kontoregisterOrganisasjon.KontonummerResponse
import no.nav.mulighetsrommet.api.clients.kontoregisterOrganisasjon.KontoregisterOrganisasjonClient
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.fixtures.UtbetalingFixtures.utbetaling1
import no.nav.mulighetsrommet.api.gjennomforing.model.AvbrytGjennomforingAarsak
import no.nav.mulighetsrommet.api.utbetaling.db.DeltakerDbo
import no.nav.mulighetsrommet.api.utbetaling.model.*
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.*
import java.time.LocalDate
import java.time.LocalDateTime

class GenererUtbetalingServiceTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))
    val kontoregisterOrganisasjonClient: KontoregisterOrganisasjonClient = mockk(relaxed = true)

    afterEach {
        database.truncateAll()
    }

    fun createUtbetalingService(
        config: OkonomiConfig = OkonomiConfig(
            gyldigTilsagnPeriode = Tiltakskode.entries.associateWith {
                Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2030, 1, 1))
            },
        ),
    ) = GenererUtbetalingService(
        config = config,
        db = database.db,
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
    val mars = Periode.forMonthOf(LocalDate.of(2025, 3, 1))
    val september = Periode.forMonthOf(LocalDate.of(2025, 9, 1))

    context("utbetalinger for forhåndsgodkjente tiltak") {
        val service = createUtbetalingService()

        val organisasjonsnummer = ArrangorFixtures.underenhet1.organisasjonsnummer

        test("genererer ikke utbetaling når deltakelser mangler") {
            MulighetsrommetTestDomain(
                arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
            ).initialize(database.db)

            service.genererUtbetalingForPeriode(januar).shouldBeEmpty()
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

            service.genererUtbetalingForPeriode(januar).shouldBeEmpty()
        }

        test("genererer ikke utbetaling når alle deltakelser ble avsluttet før utbetalingsperioden") {
            MulighetsrommetTestDomain(
                gjennomforinger = listOf(
                    AFT1.copy(
                        startDato = LocalDate.of(2024, 12, 1),
                        sluttDato = LocalDate.of(2025, 1, 31),
                    ),
                ),
                deltakere = listOf(
                    DeltakerFixtures.createDeltakerMedDeltakelsesmengderDbo(
                        AFT1.id,
                        startDato = LocalDate.of(2024, 12, 1),
                        sluttDato = LocalDate.of(2024, 12, 31),
                        statusType = DeltakerStatusType.HAR_SLUTTET,
                        deltakelsesprosent = 100.0,
                    ),
                ),
            ).initialize(database.db)

            service.genererUtbetalingForPeriode(januar).shouldBeEmpty()
        }

        test("genererer utbetaling med riktig periode, sats og deltakere som input") {
            val domain = MulighetsrommetTestDomain(
                arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
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

            val utbetaling = service.genererUtbetalingForPeriode(januar)
                .shouldHaveSize(1)
                .first()

            utbetaling.gjennomforing.id shouldBe AFT1.id
            utbetaling.betalingsinformasjon.kontonummer shouldBe Kontonummer("12345678901")
            utbetaling.beregning.input shouldBe UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Input(
                periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                sats = 20975,
                stengt = setOf(),
                deltakelser = setOf(
                    DeltakelseDeltakelsesprosentPerioder(
                        deltakelseId = domain.deltakere[0].id,
                        perioder = listOf(
                            DeltakelsesprosentPeriode(
                                periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 2, 1)),
                                deltakelsesprosent = 100.0,
                            ),
                        ),
                    ),
                ),
            )
        }

        test("genererer bare utbetaling når perioden er dekket av konfigurerte tilsagnsperioder") {
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

            val service = createUtbetalingService(
                config = OkonomiConfig(
                    mapOf(
                        Tiltakskode.ARBEIDSFORBEREDENDE_TRENING to februar,
                    ),
                ),
            )

            service.genererUtbetalingForPeriode(januar).shouldBeEmpty()

            service.genererUtbetalingForPeriode(februar).shouldHaveSize(1)
        }

        test("genererer utbetaling med kid-nummer fra forrige godkjente utbetaling fra arrangør") {
            MulighetsrommetTestDomain(
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

            val utbetaling = service.genererUtbetalingForPeriode(januar).first()
            utbetaling.gjennomforing.id shouldBe AFT1.id
            utbetaling.betalingsinformasjon.kontonummer shouldBe Kontonummer("12345678901")
            utbetaling.betalingsinformasjon.kid shouldBe null

            database.run {
                queries.utbetaling.setKid(
                    id = utbetaling.id,
                    kid = Kid.parseOrThrow("006402710013"),
                )
            }

            val sisteKrav = service.genererUtbetalingForPeriode(februar).first()
            sisteKrav.gjennomforing.id shouldBe AFT1.id
            sisteKrav.betalingsinformasjon.kid shouldBe Kid.parseOrThrow("006402710013")
        }

        test("genererer utbetaling med relevante deltakelse-perioder som input") {
            val domain = MulighetsrommetTestDomain(
                gjennomforinger = listOf(AFT1),
                deltakere = listOf(
                    DeltakerFixtures.createDeltakerMedDeltakelsesmengderDbo(
                        AFT1.id,
                        startDato = LocalDate.of(2025, 1, 1),
                        sluttDato = LocalDate.of(2025, 1, 31),
                        statusType = DeltakerStatusType.DELTAR,
                        deltakelsesprosent = 100.0,
                    ),
                    DeltakerFixtures.createDeltakerMedDeltakelsesmengderDbo(
                        AFT1.id,
                        startDato = LocalDate.of(2025, 1, 1),
                        sluttDato = LocalDate.of(2025, 1, 15),
                        statusType = DeltakerStatusType.DELTAR,
                        deltakelsesprosent = 40.0,
                    ),
                    DeltakerFixtures.createDeltakerMedDeltakelsesmengderDbo(
                        AFT1.id,
                        startDato = LocalDate.of(2023, 1, 1),
                        sluttDato = LocalDate.of(2025, 12, 31),
                        statusType = DeltakerStatusType.DELTAR,
                        deltakelsesprosent = 50.0,
                    ),
                    DeltakerFixtures.createDeltakerMedDeltakelsesmengderDbo(
                        AFT1.id,
                        startDato = LocalDate.of(2023, 1, 1),
                        sluttDato = LocalDate.of(2023, 12, 31),
                        statusType = DeltakerStatusType.DELTAR,
                        deltakelsesprosent = 100.0,
                    ),
                    DeltakerFixtures.createDeltakerMedDeltakelsesmengderDbo(
                        AFT1.id,
                        startDato = LocalDate.of(2025, 1, 1),
                        sluttDato = LocalDate.of(2025, 1, 31),
                        statusType = DeltakerStatusType.IKKE_AKTUELL,
                        deltakelsesprosent = 100.0,
                    ),
                    DeltakerFixtures.createDeltakerMedDeltakelsesmengderDbo(
                        AFT1.id,
                        startDato = LocalDate.of(2023, 1, 1),
                        sluttDato = LocalDate.of(2025, 12, 31),
                        statusType = DeltakerStatusType.DELTAR,
                        deltakelsesprosent = 10.0,
                        deltakelsesmengder = listOf(
                            DeltakerDbo.Deltakelsesmengde(
                                gyldigFra = LocalDate.of(2023, 1, 1),
                                deltakelsesprosent = 20.0,
                                opprettetTidspunkt = LocalDateTime.now(),
                            ),
                            DeltakerDbo.Deltakelsesmengde(
                                gyldigFra = LocalDate.of(2025, 1, 10),
                                deltakelsesprosent = 15.0,
                                opprettetTidspunkt = LocalDateTime.now(),
                            ),
                            DeltakerDbo.Deltakelsesmengde(
                                gyldigFra = LocalDate.of(2025, 1, 20),
                                deltakelsesprosent = 10.0,
                                opprettetTidspunkt = LocalDateTime.now(),
                            ),
                            DeltakerDbo.Deltakelsesmengde(
                                gyldigFra = LocalDate.of(2025, 2, 1),
                                deltakelsesprosent = 5.0,
                                opprettetTidspunkt = LocalDateTime.now(),
                            ),
                        ),
                    ),
                ),
            ).initialize(database.db)

            val utbetaling = service.genererUtbetalingForPeriode(januar).first()

            utbetaling.beregning.input.shouldBeTypeOf<UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Input>()
                .should {
                    it.deltakelser shouldBe setOf(
                        DeltakelseDeltakelsesprosentPerioder(
                            deltakelseId = domain.deltakere[0].id,
                            perioder = listOf(
                                DeltakelsesprosentPeriode(
                                    periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 2, 1)),
                                    deltakelsesprosent = 100.0,
                                ),
                            ),
                        ),
                        DeltakelseDeltakelsesprosentPerioder(
                            deltakelseId = domain.deltakere[1].id,
                            perioder = listOf(
                                DeltakelsesprosentPeriode(
                                    periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 16)),
                                    deltakelsesprosent = 40.0,
                                ),
                            ),
                        ),
                        DeltakelseDeltakelsesprosentPerioder(
                            deltakelseId = domain.deltakere[2].id,
                            perioder = listOf(
                                DeltakelsesprosentPeriode(
                                    periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 2, 1)),
                                    deltakelsesprosent = 50.0,
                                ),
                            ),
                        ),
                        DeltakelseDeltakelsesprosentPerioder(
                            deltakelseId = domain.deltakere[5].id,
                            perioder = listOf(
                                DeltakelsesprosentPeriode(
                                    periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 10)),
                                    deltakelsesprosent = 20.0,
                                ),
                                DeltakelsesprosentPeriode(
                                    periode = Periode(LocalDate.of(2025, 1, 10), LocalDate.of(2025, 1, 20)),
                                    deltakelsesprosent = 15.0,
                                ),
                                DeltakelsesprosentPeriode(
                                    periode = Periode(LocalDate.of(2025, 1, 20), LocalDate.of(2025, 2, 1)),
                                    deltakelsesprosent = 10.0,
                                ),
                            ),
                        ),
                    )
                }
        }

        test("overstyrer deltakelse-perioder når det er stengt hos arrangør") {
            MulighetsrommetTestDomain(
                gjennomforinger = listOf(AFT1),
                deltakere = listOf(
                    DeltakerFixtures.createDeltakerMedDeltakelsesmengderDbo(
                        AFT1.id,
                        startDato = LocalDate.of(2025, 1, 1),
                        sluttDato = LocalDate.of(2025, 2, 1),
                        statusType = DeltakerStatusType.DELTAR,
                        deltakelsesprosent = 10.0,
                        deltakelsesmengder = listOf(
                            DeltakerDbo.Deltakelsesmengde(
                                gyldigFra = LocalDate.of(2023, 1, 1),
                                deltakelsesprosent = 20.0,
                                opprettetTidspunkt = LocalDateTime.now(),
                            ),
                            DeltakerDbo.Deltakelsesmengde(
                                gyldigFra = LocalDate.of(2025, 1, 15),
                                deltakelsesprosent = 10.0,
                                opprettetTidspunkt = LocalDateTime.now(),
                            ),
                        ),
                    ),
                ),
            ) {
                queries.gjennomforing.setStengtHosArrangor(
                    AFT1.id,
                    Periode(LocalDate.of(2023, 12, 10), LocalDate.of(2025, 1, 10)),
                    "Ferie 1",
                )
                queries.gjennomforing.setStengtHosArrangor(
                    AFT1.id,
                    Periode(LocalDate.of(2025, 1, 20), LocalDate.of(2025, 2, 20)),
                    "Ferie 2",
                )
                queries.gjennomforing.setStengtHosArrangor(
                    AFT1.id,
                    Periode(LocalDate.of(2025, 2, 20), LocalDate.of(2025, 3, 20)),
                    "Fremtidig ferie",
                )
            }.initialize(database.db)

            val utbetaling = service.genererUtbetalingForPeriode(januar).first()

            utbetaling.beregning.input.shouldBeTypeOf<UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Input>()
                .should {
                    it.stengt shouldBe setOf(
                        StengtPeriode(Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 10)), "Ferie 1"),
                        StengtPeriode(Periode(LocalDate.of(2025, 1, 20), LocalDate.of(2025, 2, 1)), "Ferie 2"),
                    )
                }
        }

        test("genererer utbetaling med beregnet beløp basert på input") {
            val domain = MulighetsrommetTestDomain(
                gjennomforinger = listOf(AFT1),
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

            val utbetaling = service.genererUtbetalingForPeriode(januar).first()

            utbetaling.beregning.output.shouldBeTypeOf<UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Output>()
                .should {
                    it.belop shouldBe 20975
                    it.deltakelser shouldBe setOf(
                        DeltakelseManedsverk(
                            deltakelseId = domain.deltakere[0].id,
                            manedsverk = 1.0,
                        ),
                    )
                }
        }

        test("genererer utbetaling når gjennomføring ble avsluttet i inneværende utbetalingsperiode") {
            val domain = MulighetsrommetTestDomain(
                gjennomforinger = listOf(
                    AFT1.copy(
                        startDato = LocalDate.of(2025, 1, 1),
                        sluttDato = LocalDate.of(2025, 1, 15),
                    ),
                ),
                deltakere = listOf(
                    DeltakerFixtures.createDeltakerMedDeltakelsesmengderDbo(
                        AFT1.id,
                        startDato = LocalDate.of(2025, 1, 1),
                        sluttDato = LocalDate.of(2025, 1, 15),
                        statusType = DeltakerStatusType.HAR_SLUTTET,
                        deltakelsesprosent = 100.0,
                    ),
                ),
            ) {
                queries.gjennomforing.setStatus(
                    AFT1.id,
                    status = GjennomforingStatusType.AVSLUTTET,
                    tidspunkt = LocalDate.of(2025, 1, 16).atStartOfDay(),
                    aarsaker = null,
                    forklaring = null,
                )
            }.initialize(database.db)

            val utbetaling = service.genererUtbetalingForPeriode(januar).first()

            utbetaling.beregning.output.shouldBeTypeOf<UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Output>()
                .should {
                    it.deltakelser shouldBe setOf(DeltakelseManedsverk(domain.deltakere[0].id, 0.48387))
                }
        }

        test("genererer utbetaling når gjennomføring ble avbrutt i inneværende utbetalingsperiode") {
            val domain = MulighetsrommetTestDomain(
                gjennomforinger = listOf(
                    AFT1.copy(
                        sluttDato = LocalDate.of(2025, 2, 1),
                    ),
                ),
                deltakere = listOf(
                    DeltakerFixtures.createDeltakerMedDeltakelsesmengderDbo(
                        AFT1.id,
                        startDato = LocalDate.of(2024, 12, 1),
                        sluttDato = LocalDate.of(2025, 1, 15),
                        statusType = DeltakerStatusType.HAR_SLUTTET,
                        deltakelsesprosent = 100.0,
                    ),
                ),
            ) {
                queries.gjennomforing.setStatus(
                    AFT1.id,
                    status = GjennomforingStatusType.AVBRUTT,
                    tidspunkt = LocalDate.of(2025, 1, 15).atStartOfDay(),
                    aarsaker = listOf(element = AvbrytGjennomforingAarsak.BUDSJETT_HENSYN),
                    forklaring = null,
                )
            }.initialize(database.db)

            val utbetaling = service.genererUtbetalingForPeriode(januar).first()

            utbetaling.beregning.output.shouldBeTypeOf<UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Output>()
                .should {
                    it.deltakelser shouldBe setOf(DeltakelseManedsverk(domain.deltakere[0].id, 0.48387))
                }
        }

        test("genererer ikke utbetaling hvis det allerede finnes en med overlappende periode") {
            MulighetsrommetTestDomain(
                gjennomforinger = listOf(AFT1),
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

            service.genererUtbetalingForPeriode(januar).shouldHaveSize(1)
            database.run { queries.utbetaling.getByArrangorIds(organisasjonsnummer).shouldHaveSize(1) }

            service.genererUtbetalingForPeriode(februar).shouldHaveSize(1)
            database.run { queries.utbetaling.getByArrangorIds(organisasjonsnummer).shouldHaveSize(2) }

            // Februar finnes allerede så ingen nye
            service.genererUtbetalingForPeriode(februar).shouldHaveSize(0)
            database.run { queries.utbetaling.getByArrangorIds(organisasjonsnummer).shouldHaveSize(2) }
        }

        test("deltaker med startDato lik periodeSlutt blir ikke med i kravet") {
            val domain = MulighetsrommetTestDomain(
                gjennomforinger = listOf(AFT1),
                deltakere = listOf(
                    DeltakerFixtures.createDeltakerMedDeltakelsesmengderDbo(
                        AFT1.id,
                        startDato = LocalDate.of(2025, 2, 1),
                        sluttDato = LocalDate.of(2025, 6, 1),
                        statusType = DeltakerStatusType.DELTAR,
                        deltakelsesprosent = 100.0,
                    ),
                    DeltakerFixtures.createDeltakerMedDeltakelsesmengderDbo(
                        AFT1.id,
                        startDato = LocalDate.of(2023, 1, 1),
                        sluttDato = LocalDate.of(2025, 2, 1),
                        statusType = DeltakerStatusType.DELTAR,
                        deltakelsesprosent = 100.0,
                    ),
                ),
            ).initialize(database.db)

            val utbetaling = service.genererUtbetalingForPeriode(januar).first()

            utbetaling.beregning.input
                .shouldBeTypeOf<UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Input>()
                .should {
                    it.deltakelser.shouldHaveSize(1).first().deltakelseId.shouldBe(domain.deltakere[1].id)
                }
        }
    }

    context("gjennomføringer og deltakere med feil i datagrunnlaget") {
        val service = createUtbetalingService()

        test("genererer ikke utbetaling når gjennomføringen ble avbrutt før utbetalingsperioden, selv om deltaker har sluttdato i utbetalingsperioden") {
            MulighetsrommetTestDomain(
                gjennomforinger = listOf(
                    AFT1.copy(
                        sluttDato = LocalDate.of(2025, 1, 31),
                    ),
                ),
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
                queries.gjennomforing.setStatus(
                    AFT1.id,
                    status = GjennomforingStatusType.AVBRUTT,
                    tidspunkt = LocalDate.of(2024, 12, 31).atStartOfDay(),
                    aarsaker = listOf(AvbrytGjennomforingAarsak.BUDSJETT_HENSYN),
                    forklaring = null,
                )
            }.initialize(database.db)

            service.genererUtbetalingForPeriode(januar).shouldBeEmpty()
        }

        test("genererer ikke utbetaling når gjennomføringen ble avsluttet før utbetalingsperioden, selv om deltaker har sluttdato i utbetalingsperioden") {
            MulighetsrommetTestDomain(
                gjennomforinger = listOf(
                    AFT1.copy(
                        sluttDato = LocalDate.of(2025, 1, 31),
                    ),
                ),
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
                queries.gjennomforing.setStatus(
                    AFT1.id,
                    status = GjennomforingStatusType.AVSLUTTET,
                    tidspunkt = LocalDate.of(2024, 12, 31).atStartOfDay(),
                    aarsaker = null,
                    forklaring = null,
                )
            }.initialize(database.db)

            service.genererUtbetalingForPeriode(januar).shouldBeEmpty()
        }

        test("genererer ikke utbetaling når gjennomføringer ble avsluttet før den startet") {
            MulighetsrommetTestDomain(
                gjennomforinger = listOf(
                    AFT1.copy(
                        startDato = LocalDate.of(2025, 1, 1),
                        sluttDato = null,
                    ),
                ),
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
                queries.gjennomforing.setStatus(
                    AFT1.id,
                    status = GjennomforingStatusType.AVLYST,
                    tidspunkt = LocalDate.of(2024, 12, 31).atStartOfDay(),
                    aarsaker = listOf(AvbrytGjennomforingAarsak.BUDSJETT_HENSYN),
                    forklaring = null,
                )
            }.initialize(database.db)

            service.genererUtbetalingForPeriode(januar).shouldBeEmpty()
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

            val utbetaling = service.genererUtbetalingForPeriode(januar).first()

            utbetaling.beregning.input
                .shouldBeTypeOf<UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Input>()
                .should {
                    it.deltakelser.shouldHaveSize(1).first().deltakelseId.shouldBe(domain.deltakere[0].id)
                }
        }
    }

    context("rekalkulering av utbetalinger") {
        val service = createUtbetalingService()

        val avtale = AvtaleFixtures.oppfolging.copy(
            prismodell =
            AvtaleFixtures.oppfolging.prismodell.copy(
                prismodell = PrismodellType.AVTALT_PRIS_PER_MANEDSVERK,
                satser = listOf(
                    AvtaltSats(LocalDate.of(2026, 2, 1), 100),
                ),
            ),
        )

        val gjennomforing = GjennomforingFixtures.Oppfolging1

        val deltaker = DeltakerFixtures.createDeltakerMedDeltakelsesmengderDbo(
            gjennomforing.id,
            startDato = LocalDate.of(2026, 2, 1),
            sluttDato = LocalDate.of(2026, 2, 15),
            statusType = DeltakerStatusType.DELTAR,
            deltakelsesprosent = 100.0,
        )

        val beregning = UtbetalingBeregningPrisPerManedsverk(
            input = UtbetalingBeregningPrisPerManedsverk.Input(
                periode = Periode.forMonthOf(LocalDate.of(2026, 2, 1)),
                sats = 100,
                stengt = setOf(),
                deltakelser = setOf(
                    DeltakelsePeriode(deltaker.id, Periode.forMonthOf(LocalDate.of(2026, 2, 1))),
                ),
            ),
            output = UtbetalingBeregningPrisPerManedsverk.Output(
                belop = 100,
                deltakelser = setOf(
                    DeltakelseManedsverk(deltaker.id, 1.0),
                ),
            ),
        )

        test("oppdaterer beregnet utbetaling når deltakelser endres") {
            MulighetsrommetTestDomain(
                avtaler = listOf(avtale),
                gjennomforinger = listOf(gjennomforing),
                utbetalinger = listOf(
                    utbetaling1.copy(
                        gjennomforingId = gjennomforing.id,
                        periode = beregning.input.periode,
                        beregning = beregning,
                    ),
                ),
                deltakere = listOf(deltaker),
            ).initialize(database.db)

            val utbetaling = service.oppdaterUtbetalingBeregningForGjennomforing(gjennomforing.id)
                .shouldHaveSize(1)
                .first()

            utbetaling.beregning.output.shouldBeTypeOf<UtbetalingBeregningPrisPerManedsverk.Output>().should {
                it.belop shouldBe 50
                it.deltakelser shouldBe setOf(
                    DeltakelseManedsverk(deltaker.id, 0.5),
                )
            }
        }

        test("oppdaterer ikke utbetaling hvis det allerede er godkjent av arrangør") {
            MulighetsrommetTestDomain(
                avtaler = listOf(avtale),
                gjennomforinger = listOf(gjennomforing),
                utbetalinger = listOf(
                    utbetaling1.copy(
                        gjennomforingId = gjennomforing.id,
                        periode = beregning.input.periode,
                        beregning = beregning,
                        status = UtbetalingStatusType.INNSENDT,
                    ),
                ),
                deltakere = listOf(deltaker),
            ) {
                queries.utbetaling.setGodkjentAvArrangor(utbetaling1.id, LocalDateTime.now())
            }.initialize(database.db)

            service.oppdaterUtbetalingBeregningForGjennomforing(gjennomforing.id).shouldBeEmpty()

            database.run {
                val utbetaling = queries.utbetaling.get(utbetaling1.id).shouldNotBeNull()
                utbetaling.beregning.output
                    .shouldBeTypeOf<UtbetalingBeregningPrisPerManedsverk.Output>()
                    .should {
                        it.belop shouldBe 100
                        it.deltakelser shouldBe setOf(
                            DeltakelseManedsverk(deltaker.id, 1.0),
                        )
                    }
            }
        }
    }

    context("utbetalinger for anskaffede tiltak") {
        val service = createUtbetalingService()

        val oppfolging = GjennomforingFixtures.Oppfolging1

        test("genererer en utbetaling for avtalt pris per månedsverk med riktig periode, stengt, sats og deltakere som input") {
            val avtale =
                AvtaleFixtures.oppfolging.copy(
                    prismodell =
                    AvtaleFixtures.oppfolging.prismodell.copy(
                        prismodell = PrismodellType.AVTALT_PRIS_PER_MANEDSVERK,
                        satser = listOf(
                            AvtaltSats(LocalDate.of(2025, 1, 1), 100),
                        ),
                    ),
                )

            val domain = MulighetsrommetTestDomain(
                arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
                avtaler = listOf(avtale),
                gjennomforinger = listOf(oppfolging),
                deltakere = listOf(
                    DeltakerFixtures.createDeltakerDbo(
                        oppfolging.id,
                        startDato = LocalDate.of(2025, 1, 1),
                        sluttDato = LocalDate.of(2025, 1, 31),
                        statusType = DeltakerStatusType.DELTAR,
                    ),
                    DeltakerFixtures.createDeltakerDbo(
                        oppfolging.id,
                        startDato = LocalDate.of(2025, 2, 1),
                        sluttDato = LocalDate.of(2025, 3, 31),
                        statusType = DeltakerStatusType.DELTAR,
                    ),
                ),
            ) {
                queries.gjennomforing.setStengtHosArrangor(
                    oppfolging.id,
                    Periode(LocalDate.of(2025, 1, 20), LocalDate.of(2025, 2, 20)),
                    "Ferie!",
                )
            }.initialize(database.db)

            val utbetaling = service.genererUtbetalingForPeriode(januar)
                .shouldHaveSize(1)
                .first()

            utbetaling.gjennomforing.id shouldBe oppfolging.id
            utbetaling.beregning.input shouldBe UtbetalingBeregningPrisPerManedsverk.Input(
                periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                sats = 100,
                stengt = setOf(
                    StengtPeriode(Periode(LocalDate.of(2025, 1, 20), LocalDate.of(2025, 2, 1)), "Ferie!"),
                ),
                deltakelser = setOf(
                    DeltakelsePeriode(
                        deltakelseId = domain.deltakere[0].id,
                        periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 2, 1)),
                    ),
                ),
            )
        }

        test("genererer en utbetaling for avtalt pris per ukesverk med riktig periode, stengt, sats og deltakere som input") {
            val avtale = AvtaleFixtures.oppfolging.copy(
                prismodell =
                AvtaleFixtures.oppfolging.prismodell.copy(
                    prismodell = PrismodellType.AVTALT_PRIS_PER_UKESVERK,
                    satser = listOf(
                        AvtaltSats(LocalDate.of(2025, 1, 1), 100),
                    ),
                ),
            )

            val domain = MulighetsrommetTestDomain(
                arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
                avtaler = listOf(avtale),
                gjennomforinger = listOf(oppfolging),
                deltakere = listOf(
                    DeltakerFixtures.createDeltakerDbo(
                        oppfolging.id,
                        startDato = LocalDate.of(2025, 1, 1),
                        sluttDato = LocalDate.of(2025, 1, 31),
                        statusType = DeltakerStatusType.DELTAR,
                    ),
                    DeltakerFixtures.createDeltakerDbo(
                        oppfolging.id,
                        startDato = LocalDate.of(2025, 2, 1),
                        sluttDato = LocalDate.of(2025, 3, 31),
                        statusType = DeltakerStatusType.DELTAR,
                    ),
                ),
            ) {
                queries.gjennomforing.setStengtHosArrangor(
                    oppfolging.id,
                    Periode(LocalDate.of(2025, 1, 20), LocalDate.of(2025, 2, 20)),
                    "Ferie!",
                )
            }.initialize(database.db)

            val utbetaling = service.genererUtbetalingForPeriode(januar)
                .shouldHaveSize(1)
                .first()

            utbetaling.gjennomforing.id shouldBe oppfolging.id
            utbetaling.beregning.input shouldBe UtbetalingBeregningPrisPerUkesverk.Input(
                periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                sats = 100,
                stengt = setOf(
                    StengtPeriode(Periode(LocalDate.of(2025, 1, 20), LocalDate.of(2025, 2, 1)), "Ferie!"),
                ),
                deltakelser = setOf(
                    DeltakelsePeriode(
                        deltakelseId = domain.deltakere[0].id,
                        periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 2, 1)),
                    ),
                ),
            )
        }

        test("utbetalinger blir oppdatert med ny beregning når avtalens prismodell endres") {
            val avtale =
                AvtaleFixtures.oppfolging.copy(
                    prismodell =
                    AvtaleFixtures.oppfolging.prismodell.copy(
                        prismodell = PrismodellType.AVTALT_PRIS_PER_MANEDSVERK,
                        satser = listOf(
                            AvtaltSats(LocalDate.of(2025, 1, 1), 100),
                        ),
                    ),
                )

            MulighetsrommetTestDomain(
                arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
                avtaler = listOf(avtale),
                gjennomforinger = listOf(oppfolging),
                deltakere = listOf(
                    DeltakerFixtures.createDeltakerDbo(
                        oppfolging.id,
                        startDato = LocalDate.of(2025, 1, 1),
                        sluttDato = LocalDate.of(2025, 1, 31),
                        statusType = DeltakerStatusType.DELTAR,
                    ),
                ),
            ).initialize(database.db)

            val generertUtbetaling = service.genererUtbetalingForPeriode(januar).shouldHaveSize(1).first()
            generertUtbetaling.beregning.shouldBeTypeOf<UtbetalingBeregningPrisPerManedsverk>()

            database.run {
                queries.avtale.upsertPrismodell(
                    avtale.id,
                    avtale.prismodell.copy(prismodell = PrismodellType.AVTALT_PRIS_PER_UKESVERK),
                )
            }

            val oppdatertUtbetaling = service.oppdaterUtbetalingBeregningForGjennomforing(oppfolging.id)
                .shouldHaveSize(1).first()
            oppdatertUtbetaling.id shouldBe generertUtbetaling.id
            oppdatertUtbetaling.beregning.shouldBeTypeOf<UtbetalingBeregningPrisPerUkesverk>()
        }

        test("innsendt fri utbetaling blir ikke slettet hvis avtalens prismodell endres") {
            val avtale = AvtaleFixtures.oppfolging.copy(
                prismodell =
                AvtaleFixtures.oppfolging.prismodell.copy(
                    prismodell = PrismodellType.ANNEN_AVTALT_PRIS,
                    satser = listOf(
                        AvtaltSats(LocalDate.of(2025, 1, 1), 100),
                    ),
                ),
            )

            val domain = MulighetsrommetTestDomain(
                arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
                avtaler = listOf(avtale),
                gjennomforinger = listOf(oppfolging),
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
            ).initialize(database.db)

            database.run {
                queries.avtale.upsertPrismodell(
                    avtale.id,
                    avtale.prismodell.copy(prismodell = PrismodellType.AVTALT_PRIS_PER_MANEDSVERK),
                )
            }

            service.oppdaterUtbetalingBeregningForGjennomforing(oppfolging.id).shouldHaveSize(0)

            database.run {
                queries.utbetaling.get(domain.utbetalinger[0].id).shouldNotBeNull()
            }
        }

        test("utbetalinger slettes når prismodell ikke lengre kan genereres av systemet") {
            val avtale = AvtaleFixtures.oppfolging.copy(
                prismodell =
                AvtaleFixtures.oppfolging.prismodell.copy(
                    prismodell = PrismodellType.AVTALT_PRIS_PER_MANEDSVERK,
                    satser = listOf(
                        AvtaltSats(LocalDate.of(2025, 1, 1), 100),
                    ),
                ),
            )

            MulighetsrommetTestDomain(
                arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
                avtaler = listOf(avtale),
                gjennomforinger = listOf(oppfolging),
                deltakere = listOf(
                    DeltakerFixtures.createDeltakerDbo(
                        oppfolging.id,
                        startDato = LocalDate.of(2025, 1, 1),
                        sluttDato = LocalDate.of(2025, 1, 31),
                        statusType = DeltakerStatusType.DELTAR,
                    ),
                ),
            ).initialize(database.db)

            val generertUtbetaling = service.genererUtbetalingForPeriode(januar).shouldHaveSize(1).first()
            generertUtbetaling.beregning.shouldBeTypeOf<UtbetalingBeregningPrisPerManedsverk>()

            database.run {
                queries.avtale.upsertPrismodell(
                    avtale.id,
                    avtale.prismodell.copy(prismodell = PrismodellType.ANNEN_AVTALT_PRIS),
                )
            }

            service.oppdaterUtbetalingBeregningForGjennomforing(oppfolging.id).shouldBeEmpty()

            // Sjekk at utbetalingen er fjernet fra databasen etter at avtalen er endret tilbake til ANNEN_AVTALT_PRIS
            database.run {
                queries.utbetaling.get(generertUtbetaling.id).shouldBeNull()
            }
        }
    }

    context("utbetalinger for hele uker") {
        val service = createUtbetalingService()
        val oppfolging = GjennomforingFixtures.Oppfolging1

        test("heleukerPeriode endring") {
            heleUkerPeriode(januar) shouldBe Periode(LocalDate.of(2024, 12, 30), LocalDate.of(2025, 2, 3))

            heleUkerPeriode(februar) shouldBe Periode(LocalDate.of(2025, 2, 3), LocalDate.of(2025, 3, 3))

            heleUkerPeriode(mars) shouldBe Periode(LocalDate.of(2025, 3, 3), LocalDate.of(2025, 3, 31))

            heleUkerPeriode(september) shouldBe Periode(LocalDate.of(2025, 9, 1), LocalDate.of(2025, 9, 29))
        }

        test("utbetaling for avtalt pris per hele ukesverk tar med deltakelse fra 2024 siden uken skal med i januar") {
            val avtale = AvtaleFixtures.oppfolging.copy(
                prismodell =
                AvtaleFixtures.oppfolging.prismodell.copy(
                    prismodell = PrismodellType.AVTALT_PRIS_PER_HELE_UKESVERK,
                    satser = listOf(
                        AvtaltSats(LocalDate.of(2025, 1, 1), 100),
                    ),
                ),
            )

            val domain = MulighetsrommetTestDomain(
                arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
                avtaler = listOf(avtale),
                gjennomforinger = listOf(oppfolging.copy(sluttDato = LocalDate.of(2024, 12, 31))),
                deltakere = listOf(
                    DeltakerFixtures.createDeltakerDbo(
                        oppfolging.id,
                        startDato = LocalDate.of(2024, 12, 1),
                        sluttDato = LocalDate.of(2024, 12, 31),
                        statusType = DeltakerStatusType.HAR_SLUTTET,
                    ),
                ),
            ).initialize(database.db)

            val utbetaling = service.genererUtbetalingForPeriode(januar)
                .shouldHaveSize(1)
                .first()

            utbetaling.gjennomforing.id shouldBe oppfolging.id
            utbetaling.beregning.input shouldBe UtbetalingBeregningPrisPerHeleUkesverk.Input(
                periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                sats = 100,
                stengt = emptySet(),
                deltakelser = setOf(
                    DeltakelsePeriode(
                        deltakelseId = domain.deltakere[0].id,
                        periode = Periode(LocalDate.of(2024, 12, 30), LocalDate.of(2025, 1, 1)),
                    ),
                ),
            )
            utbetaling.beregning.output shouldBe UtbetalingBeregningPrisPerHeleUkesverk.Output(
                belop = 100,
                deltakelser = setOf(
                    DeltakelseUkesverk(
                        deltakelseId = domain.deltakere[0].id,
                        ukesverk = 1.0,
                    ),
                ),
            )
        }

        test("utbetaling for avtalt pris per hele ukesverk genereres ikke for deltakelse 29. sep fordi uken skal med i oktober") {
            val avtale = AvtaleFixtures.oppfolging.copy(
                prismodell =
                AvtaleFixtures.oppfolging.prismodell.copy(
                    prismodell = PrismodellType.AVTALT_PRIS_PER_HELE_UKESVERK,
                    satser = listOf(
                        AvtaltSats(LocalDate.of(2025, 1, 1), 100),
                    ),
                ),
            )

            MulighetsrommetTestDomain(
                arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
                avtaler = listOf(avtale),
                gjennomforinger = listOf(oppfolging),
                deltakere = listOf(
                    DeltakerFixtures.createDeltakerDbo(
                        oppfolging.id,
                        startDato = LocalDate.of(2025, 9, 29),
                        sluttDato = null,
                        statusType = DeltakerStatusType.DELTAR,
                    ),
                ),
            ).initialize(database.db)

            service.genererUtbetalingForPeriode(september).shouldHaveSize(0)
        }

        test("utbetaling for avtalt pris per hele ukesverk genererer kun for hele måneder") {
            val avtale = AvtaleFixtures.oppfolging.copy(
                prismodell =
                AvtaleFixtures.oppfolging.prismodell.copy(
                    prismodell = PrismodellType.AVTALT_PRIS_PER_HELE_UKESVERK,
                    satser = listOf(
                        AvtaltSats(LocalDate.of(2025, 1, 1), 100),
                    ),
                ),
            )

            MulighetsrommetTestDomain(
                arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
                avtaler = listOf(avtale),
                gjennomforinger = listOf(oppfolging),
                deltakere = listOf(
                    DeltakerFixtures.createDeltakerDbo(
                        oppfolging.id,
                        startDato = LocalDate.of(2025, 1, 1),
                        sluttDato = LocalDate.of(2025, 12, 31),
                        statusType = DeltakerStatusType.HAR_SLUTTET,
                    ),
                ),
            ).initialize(database.db)

            service.genererUtbetalingForPeriode(Periode(LocalDate.of(2025, 1, 5), LocalDate.of(2025, 1, 17)))
                .shouldHaveSize(0)
            service.genererUtbetalingForPeriode(Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31)))
                .shouldHaveSize(0)
            service.genererUtbetalingForPeriode(Periode(LocalDate.of(2025, 1, 2), LocalDate.of(2025, 3, 3)))
                .shouldHaveSize(0)
            service.genererUtbetalingForPeriode(Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 3, 31)))
                .shouldHaveSize(0)
            service.genererUtbetalingForPeriode(Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 2, 1)))
                .shouldHaveSize(1)
            service.genererUtbetalingForPeriode(Periode(LocalDate.of(2025, 3, 1), LocalDate.of(2025, 5, 1)))
                .shouldHaveSize(1)
        }
    }

    context("resolve avtalt sats") {
        test("finner sats hvis gjennomføringen og satsen begynner midt i en måned") {
            val oppfolging = GjennomforingFixtures.Oppfolging1
            val service = createUtbetalingService()
            val avtale = AvtaleFixtures.oppfolging.copy(
                prismodell =
                AvtaleFixtures.oppfolging.prismodell.copy(
                    prismodell = PrismodellType.AVTALT_PRIS_PER_MANEDSVERK,
                    satser = listOf(
                        AvtaltSats(LocalDate.of(2025, 1, 15), 100),
                    ),
                ),
            )

            MulighetsrommetTestDomain(
                arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
                avtaler = listOf(avtale),
                gjennomforinger = listOf(oppfolging.copy(startDato = LocalDate.of(2025, 1, 15))),
                deltakere = listOf(
                    DeltakerFixtures.createDeltakerDbo(
                        oppfolging.id,
                        startDato = LocalDate.of(2025, 1, 15),
                        sluttDato = null,
                        statusType = DeltakerStatusType.DELTAR,
                    ),
                ),
            ).initialize(database.db)

            service.genererUtbetalingForPeriode(januar).shouldHaveSize(1)
        }
    }
})
