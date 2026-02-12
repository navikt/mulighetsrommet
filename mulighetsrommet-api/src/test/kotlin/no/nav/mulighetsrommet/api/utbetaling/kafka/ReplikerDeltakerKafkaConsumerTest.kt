package no.nav.mulighetsrommet.api.utbetaling.kafka

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.amt.model.AmtDeltakerEksternV1Dto
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.EnkelAmo
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.Oppfolging1
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.VTA1
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.utbetaling.GenererUtbetalingService
import no.nav.mulighetsrommet.api.utbetaling.model.Deltaker
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.DeltakerStatus
import no.nav.mulighetsrommet.model.DeltakerStatusType
import java.time.LocalDateTime
import java.util.UUID

class ReplikerDeltakerKafkaConsumerTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    fun createConsumer(
        oppdaterUtbetaling: GenererUtbetalingService = mockk(relaxed = true),
    ): ReplikerDeltakerKafkaConsumer {
        return ReplikerDeltakerKafkaConsumer(
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
            opprettetTidspunkt = opprettetDato,
        )

        val amtDeltaker2 = createAmtDeltakerV1Dto(
            gjennomforingId = Oppfolging1.id,
            status = DeltakerStatusType.VENTER_PA_OPPSTART,
            personIdent = "12345678911",
            opprettetTidspunkt = opprettetDato,
        )

        val domain = MulighetsrommetTestDomain(
            avtaler = listOf(AvtaleFixtures.oppfolging, AvtaleFixtures.AFT, AvtaleFixtures.VTA),
            gjennomforinger = listOf(Oppfolging1, AFT1, VTA1, EnkelAmo),
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
                            opprettetTidspunkt = opprettetDato,
                        ),
                        registrertTidspunkt = opprettetDato,
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
                            opprettetTidspunkt = opprettetDato,
                        ),
                        registrertTidspunkt = opprettetDato,
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
                Json.encodeToJsonElement(amtDeltaker1.copy(gjennomforingId = EnkelAmo.id)),
            )

            database.run {
                queries.deltaker.getAll().shouldHaveSize(1).first().should {
                    it.id shouldBe amtDeltaker1.id
                    it.gjennomforingId shouldBe EnkelAmo.id
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
                status = createStatusDto(DeltakerStatusType.FEILREGISTRERT),
            )
            deltakerConsumer.consume(feilregistrertDeltaker1.id, Json.encodeToJsonElement(feilregistrertDeltaker1))

            database.run {
                queries.deltaker.getAll().shouldBeEmpty()
            }
        }

        test("overskriver ikke deltakelser når tidspunkt for endring er eldre enn det som er lagret i databasen") {
            val deltakerConsumer = createConsumer()

            val id = UUID.randomUUID()

            val deltarTidspunkt = LocalDateTime.of(2023, 2, 1, 0, 0, 0)
            val amtDeltakerDeltar = createAmtDeltakerV1Dto(
                id = id,
                gjennomforingId = AFT1.id,
                personIdent = "12345678910",
                status = DeltakerStatusType.DELTAR,
                opprettetTidspunkt = deltarTidspunkt,
            )

            val avbruttTidspunkt = LocalDateTime.of(2023, 3, 1, 0, 0, 0)
            val amtDeltakerAvbrutt = createAmtDeltakerV1Dto(
                id = id,
                gjennomforingId = AFT1.id,
                personIdent = "12345678910",
                status = DeltakerStatusType.AVBRUTT,
                opprettetTidspunkt = avbruttTidspunkt,
            )

            deltakerConsumer.consume(id, Json.encodeToJsonElement(amtDeltakerAvbrutt))

            database.run {
                queries.deltaker.get(id).shouldNotBeNull().should {
                    it.status.type shouldBe DeltakerStatusType.AVBRUTT
                    it.endretTidspunkt shouldBe avbruttTidspunkt
                }
            }

            deltakerConsumer.consume(id, Json.encodeToJsonElement(amtDeltakerDeltar))

            database.run {
                queries.deltaker.get(id).shouldNotBeNull().should {
                    it.status.type shouldBe DeltakerStatusType.AVBRUTT
                    it.endretTidspunkt shouldBe avbruttTidspunkt
                }
            }
        }

        test("overskriver deltakelser når tidspunkt for endring er det samme som det som er lagret i databasen") {
            val deltakerConsumer = createConsumer()

            val id = UUID.randomUUID()

            val tidspunkt = LocalDateTime.of(2023, 3, 1, 0, 0, 0)
            val amtDeltakerDeltar = createAmtDeltakerV1Dto(
                id = id,
                gjennomforingId = AFT1.id,
                personIdent = "12345678910",
                status = DeltakerStatusType.DELTAR,
                opprettetTidspunkt = tidspunkt,
            )

            val amtDeltakerAvbrutt = createAmtDeltakerV1Dto(
                id = id,
                gjennomforingId = AFT1.id,
                personIdent = "12345678910",
                status = DeltakerStatusType.AVBRUTT,
                opprettetTidspunkt = tidspunkt,
            )

            deltakerConsumer.consume(id, Json.encodeToJsonElement(amtDeltakerDeltar))

            database.run {
                queries.deltaker.get(id).shouldNotBeNull().should {
                    it.status.type shouldBe DeltakerStatusType.DELTAR
                    it.endretTidspunkt shouldBe tidspunkt
                }
            }

            deltakerConsumer.consume(id, Json.encodeToJsonElement(amtDeltakerAvbrutt))

            database.run {
                queries.deltaker.get(id).shouldNotBeNull().should {
                    it.status.type shouldBe DeltakerStatusType.AVBRUTT
                    it.endretTidspunkt shouldBe tidspunkt
                }
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
                status = createStatusDto(DeltakerStatusType.FEILREGISTRERT),
            )
            deltakerConsumer.consume(feilregistrert.id, Json.encodeToJsonElement(feilregistrert))

            coVerify(exactly = 1) {
                oppdaterUtbetaling.skedulerOppdaterUtbetalingerForGjennomforing(AFT1.id, any())
            }
        }

        test("trigger at utbetaling for aktuell gjennomføring beregnes på nytt ved ikke aktuell deltaker") {
            val deltakerConsumer = createConsumer(oppdaterUtbetaling = oppdaterUtbetaling)

            val ikkeAktuell = amtDeltaker1.copy(
                status = createStatusDto(DeltakerStatusType.IKKE_AKTUELL),
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

private fun createStatusDto(type: DeltakerStatusType): AmtDeltakerEksternV1Dto.DeltakerStatusDto = AmtDeltakerEksternV1Dto.DeltakerStatusDto(
    statusType = type,
    statusTekst = type.description,
    aarsakType = null,
    aarsakBeskrivelse = null,
    opprettetTidspunkt = LocalDateTime.now(),
)

private fun createAmtDeltakerV1Dto(
    id: UUID = UUID.randomUUID(),
    gjennomforingId: UUID,
    status: DeltakerStatusType,
    personIdent: String,
    opprettetTidspunkt: LocalDateTime = LocalDateTime.of(2023, 3, 1, 0, 0, 0),
) = AmtDeltakerEksternV1Dto(
    id = id,
    gjennomforingId = gjennomforingId,
    personIdent = personIdent,
    startDato = null,
    sluttDato = null,
    status = AmtDeltakerEksternV1Dto.DeltakerStatusDto(
        statusType = status,
        statusTekst = status.description,
        aarsakType = null,
        aarsakBeskrivelse = null,
        opprettetTidspunkt = opprettetTidspunkt,
    ),
    registrertTidspunkt = opprettetTidspunkt,
    endretTidspunkt = opprettetTidspunkt,
    deltakelsesmengder = listOf(),
    kilde = AmtDeltakerEksternV1Dto.Kilde.KOMET,
    innhold = AmtDeltakerEksternV1Dto.DeltakelsesinnholdDto(
        ledetekst = null,
        innhold = listOf(),
    ),
)
