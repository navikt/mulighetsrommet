package no.nav.mulighetsrommet.kafka.amt

import io.kotest.core.spec.style.FunSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.domain.dbo.DeltakerDbo
import no.nav.mulighetsrommet.api.domain.dto.DeltakerDto
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures
import no.nav.mulighetsrommet.api.repositories.DeltakerRepository
import no.nav.mulighetsrommet.api.repositories.TiltakstypeRepository
import no.nav.mulighetsrommet.api.services.TiltakstypeService
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.truncateAll
import no.nav.mulighetsrommet.domain.dto.DeltakerStatus
import no.nav.mulighetsrommet.domain.dto.NorskIdent
import no.nav.mulighetsrommet.domain.dto.amt.AmtDeltakerV1Dto
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.kafka.consumers.amt.AmtDeltakerV1KafkaConsumer
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import java.util.*

class AmtDeltakerV1KafkaConsumerTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    fun createConsumer(period: Period = Period.ofDays(1)) = AmtDeltakerV1KafkaConsumer(
        config = KafkaTopicConsumer.Config(id = "deltaker", topic = "deltaker"),
        relevantDeltakerSluttDatoPeriod = period,
        tiltakstyper = TiltakstypeService(TiltakstypeRepository(database.db), listOf()),
        deltakere = DeltakerRepository(database.db),
    )

    context("konsumering av deltakere") {
        val deltakere = DeltakerRepository(database.db)

        val amtDeltaker1 = createAmtDeltakerV1Dto(
            gjennomforingId = TiltaksgjennomforingFixtures.Oppfolging1.id,
            status = DeltakerStatus.Type.VENTER_PA_OPPSTART,
            personIdent = "12345678910",
        )

        val amtDeltaker2 = createAmtDeltakerV1Dto(
            gjennomforingId = TiltaksgjennomforingFixtures.Oppfolging1.id,
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
            stillingsprosent = amtDeltaker1.prosentStilling?.toDouble(),
            status = amtDeltaker1.status,
        )
        val deltaker2Dbo = deltaker1Dbo.copy(
            id = amtDeltaker2.id,
        )

        val domain = MulighetsrommetTestDomain(
            gjennomforinger = listOf(
                TiltaksgjennomforingFixtures.Oppfolging1,
                TiltaksgjennomforingFixtures.AFT1,
                TiltaksgjennomforingFixtures.VTA1,
            ),
        )

        beforeTest {
            domain.initialize(database.db)
        }

        afterEach {
            database.db.truncateAll()
        }

        test("lagrer deltakere fra topic") {
            val deltakerConsumer = createConsumer()

            deltakerConsumer.consume(amtDeltaker1.id, Json.encodeToJsonElement(amtDeltaker1))
            deltakerConsumer.consume(amtDeltaker2.id, Json.encodeToJsonElement(amtDeltaker2))

            deltakere.getAll().shouldContainExactlyInAnyOrder(deltaker1Dbo.toDto(), deltaker2Dbo.toDto())
        }

        test("delete deltakere for tombstone messages") {
            val deltakerConsumer = createConsumer()
            deltakere.upsert(deltaker1Dbo)

            deltakerConsumer.consume(amtDeltaker1.id, JsonNull)

            deltakere.getAll().shouldBeEmpty()
        }

        test("sletter deltakere med status FEILREGISTRERT") {
            val deltakerConsumer = createConsumer()
            deltakere.upsert(deltaker1Dbo)

            val feilregistrertDeltaker1 = amtDeltaker1.copy(
                status = DeltakerStatus(
                    type = DeltakerStatus.Type.FEILREGISTRERT,
                    aarsak = null,
                    opprettetDato = LocalDateTime.now(),
                ),
            )
            deltakerConsumer.consume(feilregistrertDeltaker1.id, Json.encodeToJsonElement(feilregistrertDeltaker1))

            deltakere.getAll().shouldBeEmpty()
        }

        test("tolker stillingsprosent som 100 hvis den mangler for forhåndsgodkjente tiltak") {
            val deltakerConsumer = createConsumer()
            deltakerConsumer.consume(
                amtDeltaker1.id,
                Json.encodeToJsonElement(amtDeltaker1.copy(gjennomforingId = TiltaksgjennomforingFixtures.AFT1.id)),
            )
            deltakerConsumer.consume(
                amtDeltaker2.id,
                Json.encodeToJsonElement(amtDeltaker2.copy(gjennomforingId = TiltaksgjennomforingFixtures.VTA1.id)),
            )

            deltakere.getAll().shouldContainExactlyInAnyOrder(
                deltaker1Dbo
                    .copy(gjennomforingId = TiltaksgjennomforingFixtures.AFT1.id, stillingsprosent = 100.0)
                    .toDto(),
                deltaker2Dbo
                    .copy(gjennomforingId = TiltaksgjennomforingFixtures.VTA1.id, stillingsprosent = 100.0)
                    .toDto(),
            )
        }
    }

    context("deltakelser for refusjonskrav") {
        val deltakere = DeltakerRepository(database.db)

        val amtDeltaker1 = createAmtDeltakerV1Dto(
            gjennomforingId = TiltaksgjennomforingFixtures.AFT1.id,
            status = DeltakerStatus.Type.DELTAR,
            personIdent = "12345678910",
        )

        val domain = MulighetsrommetTestDomain(
            gjennomforinger = listOf(TiltaksgjennomforingFixtures.AFT1),
        )

        beforeEach {
            domain.initialize(database.db)
        }

        afterEach {
            database.db.truncateAll()
        }

        test("lagrer fødselsnummer på deltakere i AFT med relevant status") {
            val deltakerConsumer = createConsumer()

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

                deltakere.get(deltaker.id).shouldNotBeNull().norskIdent shouldBe expectedNorskIdent
            }
        }

        test("lagrer ikke fødselsnummer når deltakelsen har en sluttdato før konfigurert periode") {
            val deltakerConsumer = createConsumer(Period.ofDays(1))

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

                deltakere.get(deltaker.id).shouldNotBeNull().norskIdent.shouldBe(expectedNorskIdent)
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
)

fun DeltakerDbo.toDto() = DeltakerDto(
    id = id,
    gjennomforingId = gjennomforingId,
    norskIdent = null,
    startDato = null,
    sluttDato = null,
    registrertTidspunkt = registrertTidspunkt,
    endretTidspunkt = endretTidspunkt,
    stillingsprosent = stillingsprosent,
    status = status,
)
