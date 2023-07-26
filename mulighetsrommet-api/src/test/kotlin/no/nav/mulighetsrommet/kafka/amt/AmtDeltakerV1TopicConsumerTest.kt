package no.nav.mulighetsrommet.kafka.amt

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.repositories.DeltakerRepository
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.repositories.TiltakstypeRepository
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.truncateAll
import no.nav.mulighetsrommet.database.utils.getOrThrow
import no.nav.mulighetsrommet.domain.dbo.DeltakerDbo
import no.nav.mulighetsrommet.domain.dbo.Deltakeropphav
import no.nav.mulighetsrommet.domain.dbo.Deltakerstatus
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import java.time.LocalDateTime
import java.util.*

class AmtDeltakerV1TopicConsumerTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    context("consume deltakere") {
        beforeTest {
            val tiltak = TiltakstypeRepository(database.db)
            tiltak.upsert(TiltakstypeFixtures.Oppfolging).getOrThrow()

            val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)
            tiltaksgjennomforinger.upsert(TiltaksgjennomforingFixtures.Oppfolging1)
        }

        beforeEach {
            database.db.truncateAll()
        }

        val deltakere = DeltakerRepository(database.db)
        val deltakerConsumer = AmtDeltakerV1TopicConsumer(
            config = KafkaTopicConsumer.Config(id = "deltaker", topic = "deltaker"),
            deltakere,
        )

        val deltakelsesdato = LocalDateTime.of(2023, 3, 1, 0, 0, 0)

        val amtDeltaker1 = AmtDeltakerV1Dto(
            id = UUID.randomUUID(),
            gjennomforingId = TiltaksgjennomforingFixtures.Oppfolging1.id,
            personIdent = "10101010100",
            startDato = null,
            sluttDato = null,
            status = AmtDeltakerStatus(
                type = AmtDeltakerStatus.Type.VENTER_PA_OPPSTART,
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
            tiltaksgjennomforingId = amtDeltaker1.gjennomforingId,
            status = Deltakerstatus.VENTER,
            opphav = Deltakeropphav.AMT,
            startDato = null,
            sluttDato = null,
            registrertDato = amtDeltaker1.registrertDato,
        )
        val deltaker2Dbo = deltaker1Dbo.copy(
            id = amtDeltaker2.id,
        )

        test("upsert deltakere from topic") {
            deltakerConsumer.consume(amtDeltaker1.id, Json.encodeToJsonElement(amtDeltaker1))
            deltakerConsumer.consume(amtDeltaker2.id, Json.encodeToJsonElement(amtDeltaker2))

            deltakere.getAll().shouldContainExactly(deltaker1Dbo, deltaker2Dbo)
        }

        test("ignore deltakere with invalid foreign key reference to gjennomforing") {
            val deltakerForUnknownGjennomforing = amtDeltaker1.copy(gjennomforingId = UUID.randomUUID())

            deltakerConsumer.consume(amtDeltaker1.id, Json.encodeToJsonElement(deltakerForUnknownGjennomforing))

            deltakere.getAll().shouldBeEmpty()
        }

        test("delete deltakere for tombstone messages") {
            deltakere.upsert(deltaker1Dbo)

            deltakerConsumer.consume(amtDeltaker1.id, JsonNull)

            deltakere.getAll().shouldBeEmpty()
        }

        test("delete deltakere that have status FEILREGISTRERT") {
            deltakere.upsert(deltaker1Dbo)

            val feilregistrertDeltaker1 = amtDeltaker1.copy(
                status = AmtDeltakerStatus(
                    type = AmtDeltakerStatus.Type.FEILREGISTRERT,
                    aarsak = null,
                    opprettetDato = LocalDateTime.now(),
                ),
            )
            deltakerConsumer.consume(feilregistrertDeltaker1.id, Json.encodeToJsonElement(feilregistrertDeltaker1))

            deltakere.getAll().shouldBeEmpty()
        }
    }
})
