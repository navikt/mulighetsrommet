package no.nav.mulighetsrommet.kafka.amt

import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.mulighetsrommet.api.clients.brreg.BrregClient
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.domain.dto.VirksomhetDto
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.repositories.TiltakstypeRepository
import no.nav.mulighetsrommet.api.repositories.VirksomhetRepository
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.truncateAll
import no.nav.mulighetsrommet.database.utils.getOrThrow
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer

class AmtVirksomheterV1TopicConsumerTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    context("consume virksomheter") {
        beforeTest {
            val tiltak = TiltakstypeRepository(database.db)
            tiltak.upsert(TiltakstypeFixtures.Oppfolging).getOrThrow()

            val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)
            tiltaksgjennomforinger.upsert(TiltaksgjennomforingFixtures.Oppfolging1).getOrThrow()
        }

        afterTest {
            database.db.truncateAll()
        }

        val virksomhetRepository = VirksomhetRepository(database.db)
        val brregClientMock: BrregClient = mockk(relaxed = true)
        val virksomhetConsumer = AmtVirksomheterV1TopicConsumer(
            config = KafkaTopicConsumer.Config(id = "virksomheter", topic = "virksomheter"),
            virksomhetRepository,
            brregClient = brregClientMock,
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
            postnummer = "1000",
            poststed = "Andeby",
        )

        coEvery { brregClientMock.hentEnhet("982254604") } returns VirksomhetDto(
            organisasjonsnummer = "982254604",
            navn = "REMA 1000 AS",
            overordnetEnhet = null,
            underenheter = listOf(underenhetDto),
            postnummer = "1000",
            poststed = "Andeby",
        )

        coEvery { brregClientMock.hentEnhet("9923354699") } returns VirksomhetDto(
            organisasjonsnummer = "9923354699",
            navn = "REMA 1000 underenhet",
            overordnetEnhet = "982254604",
            underenheter = null,
            postnummer = "1000",
            poststed = "Andeby",
        )

        test("upsert virksomheter from topic") {
            virksomhetConsumer.consume(amtVirksomhet1.organisasjonsnummer, Json.encodeToJsonElement(amtVirksomhet1))
            virksomhetConsumer.consume(amtUnderenhet.organisasjonsnummer, Json.encodeToJsonElement(amtUnderenhet))

            virksomhetRepository.get(amtVirksomhet1.organisasjonsnummer).shouldBeRight().should {
                it!!.underenheter shouldContainExactly listOf(underenhetDto)
                it.organisasjonsnummer shouldBe amtVirksomhet1.organisasjonsnummer
                it.navn shouldBe amtVirksomhet1.navn
            }
        }

        test("delete virksomheter for tombstone messages") {
            virksomhetRepository.upsert(underenhetDto)

            virksomhetConsumer.consume(amtUnderenhet.organisasjonsnummer, JsonNull)

            virksomhetRepository.get(underenhetDto.organisasjonsnummer).shouldBeRight() shouldBe null
        }
    }
})
