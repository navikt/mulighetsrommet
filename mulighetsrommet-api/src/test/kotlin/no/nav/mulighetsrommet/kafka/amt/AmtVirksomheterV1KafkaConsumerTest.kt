package no.nav.mulighetsrommet.kafka.amt

import arrow.core.right
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.mulighetsrommet.api.clients.brreg.BrregClient
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.domain.dto.BrregVirksomhetDto
import no.nav.mulighetsrommet.api.repositories.ArrangorRepository
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.domain.dto.Organisasjonsnummer
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.kafka.consumers.amt.AmtVirksomhetV1Dto
import no.nav.mulighetsrommet.kafka.consumers.amt.AmtVirksomheterV1KafkaConsumer

class AmtVirksomheterV1KafkaConsumerTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    context("consume virksomheter") {
        val amtVirksomhet = AmtVirksomhetV1Dto(
            navn = "REMA 1000 AS",
            organisasjonsnummer = Organisasjonsnummer("982254604"),
            overordnetEnhetOrganisasjonsnummer = null,
        )

        val amtUnderenhet = AmtVirksomhetV1Dto(
            navn = "REMA 1000 underenhet",
            organisasjonsnummer = Organisasjonsnummer("992335469"),
            overordnetEnhetOrganisasjonsnummer = amtVirksomhet.organisasjonsnummer,
        )

        val underenhetDto = BrregVirksomhetDto(
            navn = amtUnderenhet.navn,
            organisasjonsnummer = amtUnderenhet.organisasjonsnummer,
            overordnetEnhet = amtVirksomhet.organisasjonsnummer,
            postnummer = "1000",
            poststed = "Andeby",
        )

        val virksomhetDto = BrregVirksomhetDto(
            organisasjonsnummer = amtVirksomhet.organisasjonsnummer,
            navn = amtVirksomhet.navn,
            overordnetEnhet = null,
            underenheter = listOf(),
            postnummer = "1000",
            poststed = "Andeby",
        )

        val arrangorRepository = ArrangorRepository(database.db)

        val brregClient: BrregClient = mockk()
        coEvery { brregClient.getBrregVirksomhet(amtVirksomhet.organisasjonsnummer) } returns virksomhetDto.right()
        coEvery { brregClient.getBrregVirksomhet(amtUnderenhet.organisasjonsnummer) } returns underenhetDto.right()

        val virksomhetConsumer = AmtVirksomheterV1KafkaConsumer(
            config = KafkaTopicConsumer.Config(id = "virksomheter", topic = "virksomheter"),
            arrangorRepository = arrangorRepository,
            brregClient = brregClient,
        )

        test("ignorer virksomheter når de ikke allerede er lagret i databasen") {
            virksomhetConsumer.consume(amtVirksomhet.organisasjonsnummer.value, Json.encodeToJsonElement(amtVirksomhet))
            virksomhetConsumer.consume(amtUnderenhet.organisasjonsnummer.value, Json.encodeToJsonElement(amtUnderenhet))

            arrangorRepository.getAll().items.shouldBeEmpty()
        }

        test("oppdaterer bare virksomheter som er lagret i databasen") {
            arrangorRepository.upsert(virksomhetDto.copy(navn = "Kiwi", postnummer = "9999", poststed = "Gåseby"))

            virksomhetConsumer.consume(amtVirksomhet.organisasjonsnummer.value, Json.encodeToJsonElement(amtVirksomhet))
            virksomhetConsumer.consume(amtUnderenhet.organisasjonsnummer.value, Json.encodeToJsonElement(amtUnderenhet))

            arrangorRepository.getAll().should {
                it.items.shouldHaveSize(1)
                it.items[0].navn shouldBe "REMA 1000 AS"
                it.items[0].postnummer shouldBe "1000"
                it.items[0].poststed shouldBe "Andeby"
            }
        }

        test("delete virksomheter for tombstone messages") {
            arrangorRepository.upsert(underenhetDto)

            arrangorRepository.get(underenhetDto.organisasjonsnummer).shouldNotBeNull()

            virksomhetConsumer.consume(amtUnderenhet.organisasjonsnummer.value, JsonNull)

            arrangorRepository.get(underenhetDto.organisasjonsnummer) shouldBe null
        }
    }
})
