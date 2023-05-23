package no.nav.mulighetsrommet.kafka.amt

import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.domain.dto.VirksomhetDto
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.repositories.TiltakstypeRepository
import no.nav.mulighetsrommet.api.repositories.VirksomhetRepository
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.utils.getOrThrow
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer

class AmtVirksomheterV1TopicConsumerTest : FunSpec({

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

        val virksomhetRepository = VirksomhetRepository(database.db)
        val virksomhetConsumer = AmtVirksomheterV1TopicConsumer(
            config = KafkaTopicConsumer.Config(id = "virksomheter", topic = "virksomheter"),
            virksomhetRepository,
        )

        val amtVirksomhet1 = AmtVirksomhetV1Dto(
            navn = "REMA 1000 AS",
            organisasjonsnummer = "982254604",
            overordnetEnhetOrganisasjonsnummer = null,
        )
        val amtUnderenhet = AmtVirksomhetV1Dto(
            navn = "REMA 1000 underenhet",
            organisasjonsnummer = "9923354699",
            overordnetEnhetOrganisasjonsnummer = amtVirksomhet1.organisasjonsnummer,
        )
        val underenhetDto = VirksomhetDto(
            navn = amtUnderenhet.navn,
            organisasjonsnummer = amtUnderenhet.organisasjonsnummer,
            overordnetEnhet = amtVirksomhet1.organisasjonsnummer,
        )

        test("upsert deltakere from topic") {
            virksomhetConsumer.consume(amtVirksomhet1.organisasjonsnummer, Json.encodeToJsonElement(amtVirksomhet1))
            virksomhetConsumer.consume(amtUnderenhet.organisasjonsnummer, Json.encodeToJsonElement(amtUnderenhet))

            virksomhetRepository.get(amtVirksomhet1.organisasjonsnummer).shouldBeRight().should {
                it!!.underenheter shouldContainExactly listOf(underenhetDto)
                it.organisasjonsnummer shouldBe amtVirksomhet1.organisasjonsnummer
                it.navn shouldBe amtVirksomhet1.navn
            }
        }

        test("delete deltakere for tombstone messages") {
            virksomhetRepository.upsert(underenhetDto)

            virksomhetConsumer.consume(amtUnderenhet.organisasjonsnummer, JsonNull)

            virksomhetRepository.get(underenhetDto.organisasjonsnummer).shouldBeRight() shouldBe null
        }
    }
})
