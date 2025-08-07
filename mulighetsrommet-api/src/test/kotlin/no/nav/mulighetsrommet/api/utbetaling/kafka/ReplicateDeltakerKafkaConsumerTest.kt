package no.nav.mulighetsrommet.api.utbetaling.kafka

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.amt.model.AmtDeltakerV1Dto
import no.nav.mulighetsrommet.api.avtale.model.Prismodell
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.Oppfolging1
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.VTA1
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.utbetaling.db.DeltakerDbo
import no.nav.mulighetsrommet.api.utbetaling.model.Deltaker
import no.nav.mulighetsrommet.api.utbetaling.task.OppdaterUtbetalingBeregning
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.DeltakerStatus
import no.nav.mulighetsrommet.model.DeltakerStatusType
import no.nav.mulighetsrommet.model.NorskIdent
import java.time.LocalDateTime
import java.time.Period
import java.util.*

class ReplicateDeltakerKafkaConsumerTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    fun createConsumer(
        period: Period = Period.ofDays(1),
        oppdaterUtbetaling: OppdaterUtbetalingBeregning = mockk(relaxed = true),
    ): ReplicateDeltakerKafkaConsumer {
        return ReplicateDeltakerKafkaConsumer(
            db = database.db,
            relevantDeltakerSluttDatoPeriod = period,
            oppdaterUtbetaling = oppdaterUtbetaling,
        )
    }

    context("konsumering av deltakere") {
        val amtDeltaker1 = createAmtDeltakerV1Dto(
            gjennomforingId = Oppfolging1.id,
            status = DeltakerStatusType.VENTER_PA_OPPSTART,
            personIdent = "12345678910",
        )

        val amtDeltaker2 = createAmtDeltakerV1Dto(
            gjennomforingId = Oppfolging1.id,
            status = DeltakerStatusType.VENTER_PA_OPPSTART,
            personIdent = "12345678911",
        )

        val deltaker1Dbo = DeltakerDbo(
            id = amtDeltaker1.id,
            gjennomforingId = amtDeltaker1.gjennomforingId,
            startDato = null,
            sluttDato = null,
            registrertDato = amtDeltaker1.registrertDato.toLocalDate(),
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
                queries.deltaker.getAll()
                    .shouldContainExactlyInAnyOrder(
                        deltaker1Dbo.toDeltaker(NorskIdent(amtDeltaker1.personIdent)),
                        deltaker2Dbo.toDeltaker(NorskIdent(amtDeltaker2.personIdent)),
                    )
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

        test("tolker deltakelsesprosent som 100 hvis den mangler for tiltak med forhåndsgodkjent prismodell") {
            MulighetsrommetTestDomain(
                avtaler = listOf(
                    AvtaleFixtures.AFT,
                    AvtaleFixtures.oppfolging,
                ),
            ).initialize(database.db)

            val deltakerConsumer = createConsumer()
            deltakerConsumer.consume(
                amtDeltaker1.id,
                Json.encodeToJsonElement(amtDeltaker1.copy(gjennomforingId = AFT1.id)),
            )
            deltakerConsumer.consume(
                amtDeltaker2.id,
                Json.encodeToJsonElement(amtDeltaker2.copy(gjennomforingId = Oppfolging1.id)),
            )

            database.run {
                queries.deltaker.getAll().shouldContainExactlyInAnyOrder(
                    deltaker1Dbo
                        .copy(gjennomforingId = AFT1.id, deltakelsesprosent = 100.0)
                        .toDeltaker(NorskIdent(amtDeltaker1.personIdent)),
                    deltaker2Dbo
                        .copy(gjennomforingId = Oppfolging1.id, deltakelsesprosent = null)
                        .toDeltaker(NorskIdent(amtDeltaker2.personIdent)),
                )
            }
        }
    }

    context("deltakelser for utbetaling") {
        val oppdaterUtbetaling: OppdaterUtbetalingBeregning = mockk()

        val amtDeltaker1 = createAmtDeltakerV1Dto(
            gjennomforingId = AFT1.id,
            status = DeltakerStatusType.DELTAR,
            personIdent = "12345678910",
        )

        val domain = MulighetsrommetTestDomain(
            avtaler = listOf(AvtaleFixtures.AFT.copy(prismodell = Prismodell.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK)),
            gjennomforinger = listOf(AFT1),
        )

        beforeEach {
            domain.initialize(database.db)

            coEvery { oppdaterUtbetaling.schedule(any(), any(), any()) } returns Unit
        }

        afterEach {
            database.truncateAll()

            clearAllMocks()
        }

        test("trigger at utbetaling for aktuell gjennomføring beregnes på nytt") {
            val deltakerConsumer = createConsumer(oppdaterUtbetaling = oppdaterUtbetaling)

            deltakerConsumer.consume(amtDeltaker1.id, Json.encodeToJsonElement(amtDeltaker1))

            coVerify(exactly = 1) {
                oppdaterUtbetaling.schedule(AFT1.id, any(), any())
            }
        }

        test("trigger at utbetaling for aktuell gjennomføring beregnes på nytt ved feilregistrert deltaker") {
            val deltakerConsumer = createConsumer(oppdaterUtbetaling = oppdaterUtbetaling)

            deltakerConsumer.consume(
                amtDeltaker1.id,
                Json.encodeToJsonElement(
                    amtDeltaker1.copy(
                        status = DeltakerStatus(
                            type = DeltakerStatusType.FEILREGISTRERT,
                            aarsak = null,
                            opprettetDato = LocalDateTime.now(),
                        ),
                    ),
                ),
            )
            coVerify(exactly = 1) {
                oppdaterUtbetaling.schedule(AFT1.id, any(), any())
            }
        }

        test("trigger at utbetaling for aktuell gjennomføring beregnes på nytt ved ikke aktuell deltaker") {
            val deltakerConsumer = createConsumer(oppdaterUtbetaling = oppdaterUtbetaling)

            deltakerConsumer.consume(
                amtDeltaker1.id,
                Json.encodeToJsonElement(
                    amtDeltaker1.copy(
                        status = DeltakerStatus(
                            type = DeltakerStatusType.IKKE_AKTUELL,
                            aarsak = null,
                            opprettetDato = LocalDateTime.now(),
                        ),
                    ),
                ),
            )
            coVerify(exactly = 1) {
                oppdaterUtbetaling.schedule(AFT1.id, any(), any())
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

fun DeltakerDbo.toDeltaker(norskIdent: NorskIdent) = Deltaker(
    id = id,
    gjennomforingId = gjennomforingId,
    norskIdent = norskIdent,
    startDato = null,
    sluttDato = null,
    registrertDato = registrertDato,
    endretTidspunkt = endretTidspunkt,
    deltakelsesprosent = deltakelsesprosent,
    status = status,
)
