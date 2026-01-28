package no.nav.mulighetsrommet.api.utbetaling.kafka

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.amt.model.AmtDeltakerV1Dto
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.EnkeltplassFixtures.EnkelAmo1
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.Oppfolging1
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.VTA1
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.utbetaling.GenererUtbetalingService
import no.nav.mulighetsrommet.api.utbetaling.model.Deltaker
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.DeltakerStatus
import no.nav.mulighetsrommet.model.DeltakerStatusType
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class ReplicateDeltakerKafkaConsumerTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    fun createConsumer(
        oppdaterUtbetaling: GenererUtbetalingService = mockk(relaxed = true),
    ): ReplicateDeltakerKafkaConsumer {
        return ReplicateDeltakerKafkaConsumer(
            db = database.db,
            genererUtbetalingService = oppdaterUtbetaling,
        )
    }

    context("konsumering av deltakere") {
        val opprettetDato = LocalDateTime.of(2023, 3, 1, 0, 0, 0)

        val amtDeltaker1 = createAmtDeltakerV1Dto(
            gjennomforingId = Oppfolging1.id,
            status = DeltakerStatusType.VENTER_PA_OPPSTART,
            personIdent = "12345678910",
            opprettetDato = opprettetDato,
        )

        val amtDeltaker2 = createAmtDeltakerV1Dto(
            gjennomforingId = Oppfolging1.id,
            status = DeltakerStatusType.VENTER_PA_OPPSTART,
            personIdent = "12345678911",
            opprettetDato = opprettetDato,
        )

        val domain = MulighetsrommetTestDomain(
            avtaler = listOf(AvtaleFixtures.oppfolging, AvtaleFixtures.AFT, AvtaleFixtures.VTA),
            gjennomforinger = listOf(Oppfolging1, AFT1, VTA1),
            enkeltplasser = listOf(EnkelAmo1),
        )

        beforeEach {
            domain.initialize(database.db)
        }

        afterEach {
            database.truncateAll()
        }

        test("lagrer deltakere fra topic") {
            val deltakerConsumer = createConsumer()

            deltakerConsumer.consume(amtDeltaker1.id, Json.encodeToJsonElement(amtDeltaker1))
            deltakerConsumer.consume(amtDeltaker2.id, Json.encodeToJsonElement(amtDeltaker2))

            database.run {
                queries.deltaker.getAll().shouldContainExactlyInAnyOrder(
                    Deltaker(
                        id = amtDeltaker1.id,
                        gjennomforingId = Oppfolging1.id,
                        startDato = null,
                        sluttDato = null,
                        status = DeltakerStatus(
                            type = DeltakerStatusType.VENTER_PA_OPPSTART,
                            aarsak = null,
                            opprettetDato = opprettetDato,
                        ),
                        registrertDato = LocalDate.of(2023, 3, 1),
                        endretTidspunkt = opprettetDato,
                        deltakelsesmengder = listOf(),
                    ),
                    Deltaker(
                        id = amtDeltaker2.id,
                        gjennomforingId = Oppfolging1.id,
                        startDato = null,
                        sluttDato = null,
                        status = DeltakerStatus(
                            type = DeltakerStatusType.VENTER_PA_OPPSTART,
                            aarsak = null,
                            opprettetDato = opprettetDato,
                        ),
                        registrertDato = LocalDate.of(2023, 3, 1),
                        endretTidspunkt = opprettetDato,
                        deltakelsesmengder = listOf(),
                    ),
                )
            }
        }

        test("lagrer deltakere for enkeltplass-tiltak") {
            val deltakerConsumer = createConsumer()

            deltakerConsumer.consume(
                amtDeltaker1.id,
                Json.encodeToJsonElement(amtDeltaker1.copy(gjennomforingId = EnkelAmo1.id)),
            )

            database.run {
                queries.deltaker.getAll().shouldHaveSize(1).first().should {
                    it.id shouldBe amtDeltaker1.id
                    it.gjennomforingId shouldBe EnkelAmo1.id
                }
            }
        }

        test("sletter deltakere ved tombstone-meldinger") {
            database.run {
                queries.deltaker.upsert(amtDeltaker1.toDeltakerDbo())
            }

            val deltakerConsumer = createConsumer()
            deltakerConsumer.consume(amtDeltaker1.id, JsonNull)

            database.run {
                queries.deltaker.getAll().shouldBeEmpty()
            }
        }

        test("sletter deltakere med status FEILREGISTRERT") {
            database.run {
                queries.deltaker.upsert(amtDeltaker1.toDeltakerDbo())
            }

            val deltakerConsumer = createConsumer()
            val feilregistrertDeltaker1 = amtDeltaker1.copy(
                status = DeltakerStatus(
                    type = DeltakerStatusType.FEILREGISTRERT,
                    aarsak = null,
                    opprettetDato = LocalDateTime.now(),
                ),
            )
            deltakerConsumer.consume(feilregistrertDeltaker1.id, Json.encodeToJsonElement(feilregistrertDeltaker1))

            database.run {
                queries.deltaker.getAll().shouldBeEmpty()
            }
        }
    }

    context("deltakelser for utbetaling") {
        val oppdaterUtbetaling: GenererUtbetalingService = mockk()

        val amtDeltaker1 = createAmtDeltakerV1Dto(
            gjennomforingId = AFT1.id,
            status = DeltakerStatusType.DELTAR,
            personIdent = "12345678910",
        )

        val domain = MulighetsrommetTestDomain(
            avtaler = listOf(AvtaleFixtures.AFT),
            gjennomforinger = listOf(AFT1),
        )

        beforeEach {
            domain.initialize(database.db)

            coEvery { oppdaterUtbetaling.skedulerOppdaterUtbetalingerForGjennomforing(any(), any()) } returns Unit
        }

        afterEach {
            database.truncateAll()

            clearAllMocks()
        }

        test("trigger at utbetaling for aktuell gjennomføring beregnes på nytt") {
            val deltakerConsumer = createConsumer(oppdaterUtbetaling = oppdaterUtbetaling)

            deltakerConsumer.consume(amtDeltaker1.id, Json.encodeToJsonElement(amtDeltaker1))

            coVerify(exactly = 1) {
                oppdaterUtbetaling.skedulerOppdaterUtbetalingerForGjennomforing(AFT1.id, any())
            }
        }

        test("trigger at utbetaling for aktuell gjennomføring beregnes på nytt ved feilregistrert deltaker") {
            val deltakerConsumer = createConsumer(oppdaterUtbetaling = oppdaterUtbetaling)

            val feilregistrert = amtDeltaker1.copy(
                status = DeltakerStatus(
                    type = DeltakerStatusType.FEILREGISTRERT,
                    aarsak = null,
                    opprettetDato = LocalDateTime.now(),
                ),
            )
            deltakerConsumer.consume(feilregistrert.id, Json.encodeToJsonElement(feilregistrert))

            coVerify(exactly = 1) {
                oppdaterUtbetaling.skedulerOppdaterUtbetalingerForGjennomforing(AFT1.id, any())
            }
        }

        test("trigger at utbetaling for aktuell gjennomføring beregnes på nytt ved ikke aktuell deltaker") {
            val deltakerConsumer = createConsumer(oppdaterUtbetaling = oppdaterUtbetaling)

            val ikkeAktuell = amtDeltaker1.copy(
                status = DeltakerStatus(
                    type = DeltakerStatusType.IKKE_AKTUELL,
                    aarsak = null,
                    opprettetDato = LocalDateTime.now(),
                ),
            )
            deltakerConsumer.consume(ikkeAktuell.id, Json.encodeToJsonElement(ikkeAktuell))

            coVerify(exactly = 1) {
                oppdaterUtbetaling.skedulerOppdaterUtbetalingerForGjennomforing(AFT1.id, any())
            }
        }

        test("trigger ikke ny beregning når deltaker er uendret") {
            database.run {
                queries.deltaker.upsert(amtDeltaker1.toDeltakerDbo())
            }

            val deltakerConsumer = createConsumer(oppdaterUtbetaling = oppdaterUtbetaling)

            deltakerConsumer.consume(amtDeltaker1.id, Json.encodeToJsonElement(amtDeltaker1))

            coVerify(exactly = 0) {
                oppdaterUtbetaling.skedulerOppdaterUtbetalingerForGjennomforing(any(), any())
            }
        }
    }
})

private fun createAmtDeltakerV1Dto(
    gjennomforingId: UUID,
    status: DeltakerStatusType,
    personIdent: String,
    opprettetDato: LocalDateTime = LocalDateTime.of(2023, 3, 1, 0, 0, 0),
) = AmtDeltakerV1Dto(
    id = UUID.randomUUID(),
    gjennomforingId = gjennomforingId,
    personIdent = personIdent,
    startDato = null,
    sluttDato = null,
    status = DeltakerStatus(
        type = status,
        aarsak = null,
        opprettetDato = opprettetDato,
    ),
    registrertDato = opprettetDato,
    endretDato = opprettetDato,
    dagerPerUke = 2.5f,
    prosentStilling = null,
    deltakelsesmengder = listOf(),
)
