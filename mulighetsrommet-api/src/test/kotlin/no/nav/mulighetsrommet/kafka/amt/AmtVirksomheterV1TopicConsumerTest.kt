package no.nav.mulighetsrommet.kafka.amt

import arrow.core.right
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.domain.dto.VirksomhetDto
import no.nav.mulighetsrommet.api.repositories.VirksomhetRepository
import no.nav.mulighetsrommet.api.services.VirksomhetService
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.kafka.consumers.amt.AmtVirksomhetV1Dto
import no.nav.mulighetsrommet.kafka.consumers.amt.AmtVirksomheterV1TopicConsumer

class AmtVirksomheterV1TopicConsumerTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    context("consume virksomheter") {
        val amtVirksomhet = AmtVirksomhetV1Dto(
            navn = "REMA 1000 AS",
            organisasjonsnummer = "982254604",
            overordnetEnhetOrganisasjonsnummer = null,
        )

        val amtUnderenhet = AmtVirksomhetV1Dto(
            navn = "REMA 1000 underenhet",
            organisasjonsnummer = "9923354699",
            overordnetEnhetOrganisasjonsnummer = amtVirksomhet.organisasjonsnummer,
        )

        val underenhetDto = VirksomhetDto(
            navn = amtUnderenhet.navn,
            organisasjonsnummer = amtUnderenhet.organisasjonsnummer,
            overordnetEnhet = amtVirksomhet.organisasjonsnummer,
            postnummer = "1000",
            poststed = "Andeby",
        )

        val virksomhetDto = VirksomhetDto(
            organisasjonsnummer = amtVirksomhet.organisasjonsnummer,
            navn = amtVirksomhet.navn,
            overordnetEnhet = null,
            underenheter = listOf(underenhetDto),
            postnummer = "1000",
            poststed = "Andeby",
        )

        val virksomhetRepository = VirksomhetRepository(database.db)

        val virksomhetService: VirksomhetService = mockk()
        coEvery { virksomhetService.getVirksomhet(amtVirksomhet.organisasjonsnummer) } returns virksomhetDto.right()
        coEvery { virksomhetService.getVirksomhet(amtUnderenhet.organisasjonsnummer) } returns underenhetDto.right()

        val virksomhetConsumer = AmtVirksomheterV1TopicConsumer(
            config = KafkaTopicConsumer.Config(id = "virksomheter", topic = "virksomheter"),
            virksomhetRepository = virksomhetRepository,
            virksomhetService = virksomhetService,
        )

        test("ignorer virksomheter når de ikke allerede er lagret i database") {
            virksomhetConsumer.consume(amtVirksomhet.organisasjonsnummer, Json.encodeToJsonElement(amtVirksomhet))
            virksomhetConsumer.consume(amtUnderenhet.organisasjonsnummer, Json.encodeToJsonElement(amtUnderenhet))

            virksomhetRepository.getAll().shouldBeRight().shouldBeEmpty()
        }

        test("oppdaterer virksomheter når de finnes i database") {
            virksomhetRepository.upsert(virksomhetDto.copy(poststed = "Gåseby")).shouldBeRight()
            virksomhetRepository.upsert(underenhetDto.copy(poststed = "Gåseby")).shouldBeRight()

            virksomhetConsumer.consume(amtVirksomhet.organisasjonsnummer, Json.encodeToJsonElement(amtVirksomhet))
            virksomhetConsumer.consume(amtUnderenhet.organisasjonsnummer, Json.encodeToJsonElement(amtUnderenhet))

            virksomhetRepository.getAll().shouldBeRight().shouldHaveSize(2)
            virksomhetRepository.get(virksomhetDto.organisasjonsnummer).shouldBeRight().shouldBe(virksomhetDto)
            virksomhetRepository.get(underenhetDto.organisasjonsnummer).shouldBeRight().shouldBe(underenhetDto)
        }

        test("delete virksomheter for tombstone messages") {
            virksomhetRepository.upsert(underenhetDto)

            virksomhetConsumer.consume(amtUnderenhet.organisasjonsnummer, JsonNull)

            virksomhetRepository.get(underenhetDto.organisasjonsnummer).shouldBeRight() shouldBe null
        }
    }
})
