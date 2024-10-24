package no.nav.mulighetsrommet.kafka.amt

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.domain.dbo.DeltakerDbo
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures
import no.nav.mulighetsrommet.api.repositories.DeltakerRepository
import no.nav.mulighetsrommet.api.repositories.TiltakstypeRepository
import no.nav.mulighetsrommet.api.services.TiltakstypeService
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.truncateAll
import no.nav.mulighetsrommet.domain.dto.DeltakerStatus
import no.nav.mulighetsrommet.domain.dto.amt.AmtDeltakerV1Dto
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.kafka.consumers.amt.AmtDeltakerV1KafkaConsumer
import java.time.LocalDateTime
import java.util.*

class AmtDeltakerV1KafkaConsumerTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    context("consume deltakere") {
        beforeTest {
            MulighetsrommetTestDomain(
                gjennomforinger = listOf(
                    TiltaksgjennomforingFixtures.Oppfolging1,
                    TiltaksgjennomforingFixtures.AFT1,
                    TiltaksgjennomforingFixtures.VTA1,
                ),
            ).initialize(database.db)
        }

        afterEach {
            database.db.truncateAll()
        }

        val deltakere = DeltakerRepository(database.db)
        val deltakerConsumer = AmtDeltakerV1KafkaConsumer(
            config = KafkaTopicConsumer.Config(id = "deltaker", topic = "deltaker"),
            tiltakstyper = TiltakstypeService(TiltakstypeRepository(database.db)),
            deltakere = deltakere,
        )

        val deltakelsesdato = LocalDateTime.of(2023, 3, 1, 0, 0, 0)

        val amtDeltaker1 = AmtDeltakerV1Dto(
            id = UUID.randomUUID(),
            gjennomforingId = TiltaksgjennomforingFixtures.Oppfolging1.id,
            personIdent = "10101010100",
            startDato = null,
            sluttDato = null,
            status = DeltakerStatus(
                type = DeltakerStatus.Type.VENTER_PA_OPPSTART,
                aarsak = null,
                opprettetDato = deltakelsesdato,
            ),
            registrertDato = deltakelsesdato,
            endretDato = deltakelsesdato,
            dagerPerUke = 2.5f,
            prosentStilling = null,
        )
        val amtDeltaker2 = amtDeltaker1.copy(
            id = UUID.randomUUID(),
            personIdent = "10101010101",
            dagerPerUke = 1f,
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

        test("upsert deltakere from topic") {
            deltakerConsumer.consume(amtDeltaker1.id, Json.encodeToJsonElement(amtDeltaker1))
            deltakerConsumer.consume(amtDeltaker2.id, Json.encodeToJsonElement(amtDeltaker2))

            deltakere.getAll().shouldContainExactly(deltaker1Dbo, deltaker2Dbo)
        }

        test("delete deltakere for tombstone messages") {
            deltakere.upsert(deltaker1Dbo)

            deltakerConsumer.consume(amtDeltaker1.id, JsonNull)

            deltakere.getAll().shouldBeEmpty()
        }

        test("delete deltakere that have status FEILREGISTRERT") {
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
            deltakerConsumer.consume(
                amtDeltaker1.id,
                Json.encodeToJsonElement(amtDeltaker1.copy(gjennomforingId = TiltaksgjennomforingFixtures.AFT1.id)),
            )
            deltakerConsumer.consume(
                amtDeltaker2.id,
                Json.encodeToJsonElement(amtDeltaker2.copy(gjennomforingId = TiltaksgjennomforingFixtures.VTA1.id)),
            )

            deltakere.getAll().shouldContainExactly(
                deltaker1Dbo.copy(gjennomforingId = TiltaksgjennomforingFixtures.AFT1.id, stillingsprosent = 100.0),
                deltaker2Dbo.copy(gjennomforingId = TiltaksgjennomforingFixtures.VTA1.id, stillingsprosent = 100.0),
            )
        }
    }
})
