package no.nav.mulighetsrommet.kafka.amt

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCaseOrder
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
import no.nav.mulighetsrommet.database.utils.getOrThrow
import no.nav.mulighetsrommet.domain.dbo.DeltakerDbo
import no.nav.mulighetsrommet.domain.dbo.Deltakeropphav
import no.nav.mulighetsrommet.domain.dbo.Deltakerstatus
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import java.time.LocalDateTime
import java.util.*

class AmtDeltakerV1TopicConsumerTest : FunSpec({

    testOrder = TestCaseOrder.Sequential

    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    context("consume deltakere") {
        beforeTest {
            database.db.migrate()

            val tiltak = TiltakstypeRepository(database.db)
            tiltak.upsert(TiltakstypeFixtures.Oppfolging).getOrThrow()

            val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)
            tiltaksgjennomforinger.upsert(TiltaksgjennomforingFixtures.Oppfolging1).getOrThrow()
        }

        afterTest {
            database.db.clean()
        }

        val deltakere = DeltakerRepository(database.db)
        val deltakerConsumer = AmtDeltakerV1TopicConsumer(
            config = KafkaTopicConsumer.Config(id = "deltaker", topic = "deltaker"),
            deltakere
        )

        val amtDeltaker1 = AmtDeltakerV1Dto(
            id = UUID.randomUUID(),
            gjennomforingId = TiltaksgjennomforingFixtures.Oppfolging1.id,
            personIdent = "10101010100",
            startDato = null,
            sluttDato = null,
            status = AmtDeltakerV1Dto.Status.VENTER_PA_OPPSTART,
            registrertDato = LocalDateTime.of(2023, 3, 1, 0, 0, 0),
            dagerPerUke = null,
            prosentStilling = null
        )
        val amtDeltaker2 = amtDeltaker1.copy(
            id = UUID.randomUUID(),
            personIdent = "10101010101"
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

        test("delete deltakere for tombstone messages") {
            deltakere.upsert(deltaker1Dbo)

            deltakerConsumer.consume(amtDeltaker1.id, JsonNull)

            deltakere.getAll().shouldBeEmpty()
        }

        test("delete deltakere that have status FEILREGISTRERT") {
            deltakere.upsert(deltaker1Dbo)

            val feilregistrertDeltaker1 = amtDeltaker1.copy(status = AmtDeltakerV1Dto.Status.FEILREGISTRERT)
            deltakerConsumer.consume(feilregistrertDeltaker1.id, Json.encodeToJsonElement(feilregistrertDeltaker1))

            deltakere.getAll().shouldBeEmpty()
        }
    }
})
