package no.nav.mulighetsrommet.api.utbetaling.kafka

import io.kotest.core.spec.style.FunSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
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
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.Oppfolging1
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.VTA1
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.utbetaling.UtbetalingService
import no.nav.mulighetsrommet.api.utbetaling.db.DeltakerDbo
import no.nav.mulighetsrommet.api.utbetaling.model.DeltakerDto
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.model.DeltakerStatus
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Prismodell
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import java.util.*

class AmtDeltakerV1KafkaConsumerTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    fun createConsumer(
        period: Period = Period.ofDays(1),
        utbetalingService: UtbetalingService = mockk(),
    ): AmtDeltakerV1KafkaConsumer {
        return AmtDeltakerV1KafkaConsumer(
            config = KafkaTopicConsumer.Config(id = "deltaker", topic = "deltaker"),
            db = database.db,
            relevantDeltakerSluttDatoPeriod = period,
            utbetalingService = utbetalingService,
        )
    }

    context("konsumering av deltakere") {
        val amtDeltaker1 = createAmtDeltakerV1Dto(
            gjennomforingId = Oppfolging1.id,
            status = DeltakerStatus.Type.VENTER_PA_OPPSTART,
            personIdent = "12345678910",
        )

        val amtDeltaker2 = createAmtDeltakerV1Dto(
            gjennomforingId = Oppfolging1.id,
            status = DeltakerStatus.Type.VENTER_PA_OPPSTART,
            personIdent = "12345678911",
        )

        val deltaker1Dbo = DeltakerDbo(
            id = amtDeltaker1.id,
            gjennomforingId = amtDeltaker1.gjennomforingId,
            startDato = null,
            sluttDato = null,
            registrertTidspunkt = amtDeltaker1.registrertDato,
            endretTidspunkt = amtDeltaker1.endretDato,
            deltakelsesprosent = amtDeltaker1.prosentStilling?.toDouble(),
            deltakelsesmengder = emptyList(),
            status = amtDeltaker1.status,
        )
        val deltaker2Dbo = deltaker1Dbo.copy(
            id = amtDeltaker2.id,
        )

        val domain = MulighetsrommetTestDomain(
            avtaler = listOf(AvtaleFixtures.oppfolging, AvtaleFixtures.AFT, AvtaleFixtures.VTA),
            gjennomforinger = listOf(Oppfolging1, AFT1, VTA1),
        )

        beforeTest {
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
                queries.deltaker.getAll().shouldContainExactlyInAnyOrder(deltaker1Dbo.toDto(), deltaker2Dbo.toDto())
            }
        }

        test("delete deltakere for tombstone messages") {
            database.run {
                queries.deltaker.upsert(deltaker1Dbo)
            }

            val deltakerConsumer = createConsumer()
            deltakerConsumer.consume(amtDeltaker1.id, JsonNull)

            database.run {
                queries.deltaker.getAll().shouldBeEmpty()
            }
        }

        test("sletter deltakere med status FEILREGISTRERT") {
            database.run {
                queries.deltaker.upsert(deltaker1Dbo)
            }

            val deltakerConsumer = createConsumer()
            val feilregistrertDeltaker1 = amtDeltaker1.copy(
                status = DeltakerStatus(
                    type = DeltakerStatus.Type.FEILREGISTRERT,
                    aarsak = null,
                    opprettetDato = LocalDateTime.now(),
                ),
            )
            deltakerConsumer.consume(feilregistrertDeltaker1.id, Json.encodeToJsonElement(feilregistrertDeltaker1))

            database.run {
                queries.deltaker.getAll().shouldBeEmpty()
            }
        }

        test("tolker deltakelsesprosent som 100 hvis den mangler for tiltak med forhåndsgodkjent prismodell") {
            MulighetsrommetTestDomain(
                avtaler = listOf(
                    AvtaleFixtures.AFT.copy(prismodell = Prismodell.FORHANDSGODKJENT),
                    AvtaleFixtures.VTA.copy(prismodell = null),
                ),
            ).initialize(database.db)

            val deltakerConsumer = createConsumer()
            deltakerConsumer.consume(
                amtDeltaker1.id,
                Json.encodeToJsonElement(amtDeltaker1.copy(gjennomforingId = AFT1.id)),
            )
            deltakerConsumer.consume(
                amtDeltaker2.id,
                Json.encodeToJsonElement(amtDeltaker2.copy(gjennomforingId = VTA1.id)),
            )

            database.run {
                queries.deltaker.getAll().shouldContainExactlyInAnyOrder(
                    deltaker1Dbo
                        .copy(gjennomforingId = AFT1.id, deltakelsesprosent = 100.0)
                        .toDto(),
                    deltaker2Dbo
                        .copy(gjennomforingId = VTA1.id, deltakelsesprosent = null)
                        .toDto(),
                )
            }
        }
    }

    context("deltakelser for utbetaling") {
        val utbetalingService: UtbetalingService = mockk()

        val amtDeltaker1 = createAmtDeltakerV1Dto(
            gjennomforingId = AFT1.id,
            status = DeltakerStatus.Type.DELTAR,
            personIdent = "12345678910",
        )

        val domain = MulighetsrommetTestDomain(
            avtaler = listOf(AvtaleFixtures.AFT.copy(prismodell = Prismodell.FORHANDSGODKJENT)),
            gjennomforinger = listOf(AFT1),
        )

        beforeEach {
            domain.initialize(database.db)

            coEvery { utbetalingService.recalculateUtbetalingForGjennomforing(any()) } returns Unit
        }

        afterEach {
            database.truncateAll()

            clearAllMocks()
        }

        test("lagrer fødselsnummer på deltakere i AFT med relevant status") {
            val deltakerConsumer = createConsumer(utbetalingService = utbetalingService)

            forAll(
                row(DeltakerStatus.Type.VENTER_PA_OPPSTART, null),
                row(DeltakerStatus.Type.IKKE_AKTUELL, null),
                row(DeltakerStatus.Type.DELTAR, NorskIdent("12345678910")),
                row(DeltakerStatus.Type.FULLFORT, NorskIdent("12345678910")),
                row(DeltakerStatus.Type.HAR_SLUTTET, NorskIdent("12345678910")),
                row(DeltakerStatus.Type.AVBRUTT, NorskIdent("12345678910")),
            ) { status, expectedNorskIdent ->
                val deltaker = amtDeltaker1.copy(
                    status = amtDeltaker1.status.copy(type = status),
                )
                deltakerConsumer.consume(deltaker.id, Json.encodeToJsonElement(deltaker))

                database.run {
                    queries.deltaker.get(deltaker.id).shouldNotBeNull().norskIdent shouldBe expectedNorskIdent
                }
            }
        }

        test("lagrer ikke fødselsnummer når deltakelsen har en sluttdato før konfigurert periode") {
            val deltakerConsumer = createConsumer(
                period = Period.ofDays(1),
                utbetalingService = utbetalingService,
            )

            forAll(
                row(LocalDate.now().minusDays(2), null),
                row(LocalDate.now().minusDays(1), NorskIdent("12345678910")),
                row(LocalDate.now(), NorskIdent("12345678910")),
            ) { sluttDato, expectedNorskIdent ->
                val deltaker = amtDeltaker1.copy(
                    startDato = LocalDate.now().minusMonths(1),
                    sluttDato = sluttDato,
                )
                deltakerConsumer.consume(amtDeltaker1.id, Json.encodeToJsonElement(deltaker))

                database.run {
                    queries.deltaker.get(deltaker.id).shouldNotBeNull().norskIdent.shouldBe(expectedNorskIdent)
                }
            }
        }

        test("trigger at utbetaling for aktuell gjennomføring beregnes på nytt") {
            val deltakerConsumer = createConsumer(utbetalingService = utbetalingService)

            deltakerConsumer.consume(amtDeltaker1.id, Json.encodeToJsonElement(amtDeltaker1))

            coVerify(exactly = 1) {
                utbetalingService.recalculateUtbetalingForGjennomforing(AFT1.id)
            }
        }
    }
})

private fun createAmtDeltakerV1Dto(
    gjennomforingId: UUID,
    status: DeltakerStatus.Type,
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

fun DeltakerDbo.toDto() = DeltakerDto(
    id = id,
    gjennomforingId = gjennomforingId,
    norskIdent = null,
    startDato = null,
    sluttDato = null,
    registrertTidspunkt = registrertTidspunkt,
    endretTidspunkt = endretTidspunkt,
    deltakelsesprosent = deltakelsesprosent,
    status = status,
)
