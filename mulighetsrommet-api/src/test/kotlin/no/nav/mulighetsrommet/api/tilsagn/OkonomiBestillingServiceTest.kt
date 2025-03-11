package no.nav.mulighetsrommet.api.tilsagn

import com.github.kagkarlsson.scheduler.Scheduler
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.*
import kotlinx.serialization.json.Json
import no.nav.common.kafka.producer.KafkaProducerClient
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.fixtures.TilsagnFixtures.setTilsagnStatus
import no.nav.mulighetsrommet.api.fixtures.UtbetalingFixtures.setDelutbetalingStatus
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningFri
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFri
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.tasks.DbSchedulerKotlinSerializer
import no.nav.tiltak.okonomi.OkonomiBestillingMelding
import no.nav.tiltak.okonomi.OkonomiPart
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class OkonomiBestillingServiceTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    val kafkaProducerClient = mockk<KafkaProducerClient<String, String>>(relaxed = true)

    lateinit var service: OkonomiBestillingService

    lateinit var scheduler: Scheduler

    beforeContainer {
        service = OkonomiBestillingService(
            config = OkonomiBestillingService.Config(topic = "okonomi.bestilling.v1"),
            db = database.db,
            kafkaProducerClient = kafkaProducerClient,
        )

        scheduler = Scheduler
            .create(database.db.getDatasource(), service.task)
            .serializer(DbSchedulerKotlinSerializer())
            .pollingInterval(100.milliseconds.toJavaDuration())
            .build()

        scheduler.start()
    }

    afterContainer {
        scheduler.stop()
    }

    context("skedulering av oppgaver for økonomi") {
        val bestillingsnummer = "A-2025/1-1"

        val tilsagn = TilsagnFixtures.Tilsagn1.copy(
            bestillingsnummer = bestillingsnummer,
            periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 7, 1)),
            beregning = TilsagnBeregningFri(
                input = TilsagnBeregningFri.Input(1000),
                output = TilsagnBeregningFri.Output(1000),
            ),
        )

        val utbetaling1 = UtbetalingFixtures.utbetaling1.copy(
            periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 4, 1)),
            beregning = UtbetalingBeregningFri(
                input = UtbetalingBeregningFri.Input(500),
                output = UtbetalingBeregningFri.Output(500),
            ),
        )
        val utbetaling2 = UtbetalingFixtures.utbetaling2.copy(
            periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 4, 1)),
            beregning = UtbetalingBeregningFri(
                input = UtbetalingBeregningFri.Input(500),
                output = UtbetalingBeregningFri.Output(500),
            ),
        )
        val delutbetaling1 = UtbetalingFixtures.delutbetaling1
        val delutbetaling2 = UtbetalingFixtures.delutbetaling1
            .copy(
                id = UUID.randomUUID(),
                utbetalingId = utbetaling2.id,
                lopenummer = 2,
                fakturanummer = "$bestillingsnummer-2",
            )

        afterEach {
            database.truncateAll()
        }

        test("godkjent tilsagn blir omsider sendt som bestilling på kafka") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.ansatt1, NavAnsattFixture.ansatt2),
                arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(tilsagn),
                utbetalinger = listOf(utbetaling1, utbetaling2),
                delutbetalinger = listOf(delutbetaling1, delutbetaling2),
            ) {
                setTilsagnStatus(tilsagn, TilsagnStatus.GODKJENT)
            }.initialize(database.db)

            database.run {
                service.scheduleBehandleGodkjentTilsagn(tilsagn.id, session)
            }

            eventually(10.seconds) {
                verify {
                    kafkaProducerClient.sendSync(
                        match {
                            it.topic() shouldBe "okonomi.bestilling.v1"

                            it.key() shouldBe tilsagn.bestillingsnummer

                            val bestilling = Json.decodeFromString<OkonomiBestillingMelding>(it.value()!!)
                                .shouldBeTypeOf<OkonomiBestillingMelding.Bestilling>()
                                .payload

                            bestilling.tiltakskode shouldBe Tiltakskode.ARBEIDSFORBEREDENDE_TRENING
                            bestilling.arrangor.hovedenhet shouldBe ArrangorFixtures.hovedenhet.organisasjonsnummer
                            bestilling.arrangor.underenhet shouldBe ArrangorFixtures.underenhet1.organisasjonsnummer
                            bestilling.kostnadssted shouldBe NavEnhetNummer(NavEnhetFixtures.Innlandet.enhetsnummer)
                            bestilling.bestillingsnummer shouldBe tilsagn.bestillingsnummer
                            bestilling.belop shouldBe tilsagn.beregning.output.belop

                            bestilling.behandletAv shouldBe OkonomiPart.NavAnsatt(NavAnsattFixture.ansatt1.navIdent)
                            bestilling.besluttetAv shouldBe OkonomiPart.NavAnsatt(NavAnsattFixture.ansatt2.navIdent)

                            true
                        },
                    )
                }
            }
        }

        test("annullert tilsagn blir omsider sendt som annullering på kafka") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.ansatt1, NavAnsattFixture.ansatt2),
                arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(tilsagn),
                utbetalinger = listOf(utbetaling1, utbetaling2),
                delutbetalinger = listOf(delutbetaling1, delutbetaling2),
            ) {
                setTilsagnStatus(tilsagn, TilsagnStatus.ANNULLERT)
            }.initialize(database.db)

            database.run {
                service.scheduleBehandleAnnullertTilsagn(tilsagn.id, session)
            }

            eventually(10.seconds) {
                verify {
                    kafkaProducerClient.sendSync(
                        match {
                            it.topic() shouldBe "okonomi.bestilling.v1"

                            it.key() shouldBe bestillingsnummer

                            val annullering = Json.decodeFromString<OkonomiBestillingMelding>(it.value()!!)
                                .shouldBeTypeOf<OkonomiBestillingMelding.Annullering>()
                                .payload

                            annullering.bestillingsnummer shouldBe bestillingsnummer
                            annullering.behandletAv shouldBe OkonomiPart.NavAnsatt(NavAnsattFixture.ansatt1.navIdent)
                            annullering.besluttetAv shouldBe OkonomiPart.NavAnsatt(NavAnsattFixture.ansatt2.navIdent)

                            true
                        },
                    )
                }
            }
        }

        test("godkjent utbetaling blir omsider sendt som faktura på kafka") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.ansatt1, NavAnsattFixture.ansatt2),
                arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(tilsagn),
                utbetalinger = listOf(utbetaling1, utbetaling2),
                delutbetalinger = listOf(delutbetaling1, delutbetaling2),
            ) {
                setDelutbetalingStatus(delutbetaling1, UtbetalingFixtures.DelutbetalingStatus.GODKJENT)
                setTilsagnStatus(tilsagn, TilsagnStatus.GODKJENT)
            }
                .initialize(database.db)
            database.run {
                service.scheduleBehandleGodkjenteUtbetalinger(tilsagn.id, session)
            }

            eventually(10.seconds) {
                verify {
                    kafkaProducerClient.sendSync(
                        match {
                            it.topic() shouldBe "okonomi.bestilling.v1"

                            it.key() shouldBe bestillingsnummer

                            val faktura = Json.decodeFromString<OkonomiBestillingMelding>(it.value()!!)
                                .shouldBeTypeOf<OkonomiBestillingMelding.Faktura>()
                                .payload

                            faktura.bestillingsnummer shouldBe tilsagn.bestillingsnummer
                            faktura.fakturanummer shouldBe delutbetaling1.fakturanummer
                            faktura.periode shouldBe delutbetaling1.periode
                            faktura.belop shouldBe delutbetaling1.belop
                            faktura.behandletAv shouldBe OkonomiPart.NavAnsatt(NavAnsattFixture.ansatt1.navIdent)
                            faktura.besluttetAv shouldBe OkonomiPart.NavAnsatt(NavAnsattFixture.ansatt2.navIdent)

                            true
                        },
                    )
                }
            }
        }

        test("første godkjente delutbetaling blir sendt først selv om jobb krasjer første gang") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.ansatt1, NavAnsattFixture.ansatt2),
                arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(tilsagn),
                utbetalinger = listOf(utbetaling1, utbetaling2),
                delutbetalinger = listOf(delutbetaling1, delutbetaling2),
            ) {
                setTilsagnStatus(tilsagn, TilsagnStatus.GODKJENT)
                setDelutbetalingStatus(
                    delutbetaling1,
                    UtbetalingFixtures.DelutbetalingStatus.GODKJENT,
                    besluttetTidspunkt = LocalDateTime.of(2025, 1, 1, 10, 0, 0),
                )
                setDelutbetalingStatus(
                    delutbetaling2,
                    UtbetalingFixtures.DelutbetalingStatus.GODKJENT,
                    besluttetTidspunkt = LocalDateTime.of(2025, 1, 1, 20, 0, 0),
                )
            }.initialize(database.db)
            every { kafkaProducerClient.sendSync(any()) } throws Exception()
            shouldThrow<Exception> {
                service.behandleGodkjentUtbetalinger(tilsagn.id)
            }
            database.run {
                queries.delutbetaling.getSkalSendesTilOkonomi(tilsagn.id) shouldHaveSize 2
            }
            clearAllMocks()
            service.behandleGodkjentUtbetalinger(tilsagn.id)

            verifySequence {
                kafkaProducerClient.sendSync(
                    match {
                        it.topic() shouldBe "okonomi.bestilling.v1"
                        it.key() shouldBe bestillingsnummer

                        val faktura = Json.decodeFromString<OkonomiBestillingMelding>(it.value()!!)
                            .shouldBeTypeOf<OkonomiBestillingMelding.Faktura>()
                            .payload
                        faktura.fakturanummer shouldBe delutbetaling1.fakturanummer
                        true
                    },
                )
                kafkaProducerClient.sendSync(
                    match {
                        it.topic() shouldBe "okonomi.bestilling.v1"
                        it.key() shouldBe bestillingsnummer

                        val faktura = Json.decodeFromString<OkonomiBestillingMelding>(it.value()!!)
                            .shouldBeTypeOf<OkonomiBestillingMelding.Faktura>()
                            .payload
                        faktura.fakturanummer shouldBe delutbetaling2.fakturanummer
                        true
                    },
                )
            }
        }
    }
})
