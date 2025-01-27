package no.nav.mulighetsrommet.api.arrangor.kafka

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
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.brreg.BrregClient
import no.nav.mulighetsrommet.brreg.BrregVirksomhetDto
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.model.Organisasjonsnummer

class AmtVirksomheterV1KafkaConsumerTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

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

        val brregClient: BrregClient = mockk()
        coEvery { brregClient.getBrregVirksomhet(amtVirksomhet.organisasjonsnummer) } returns virksomhetDto.right()
        coEvery { brregClient.getBrregVirksomhet(amtUnderenhet.organisasjonsnummer) } returns underenhetDto.right()

        val virksomhetConsumer = AmtVirksomheterV1KafkaConsumer(
            config = KafkaTopicConsumer.Config(id = "virksomheter", topic = "virksomheter"),
            db = database.db,
            brregClient = brregClient,
        )

        test("ignorer virksomheter når de ikke allerede er lagret i databasen") {
            virksomhetConsumer.consume(amtVirksomhet.organisasjonsnummer.value, Json.encodeToJsonElement(amtVirksomhet))
            virksomhetConsumer.consume(amtUnderenhet.organisasjonsnummer.value, Json.encodeToJsonElement(amtUnderenhet))

            database.run {
                queries.arrangor.getAll().items.shouldBeEmpty()
            }
        }

        test("oppdaterer bare virksomheter som er lagret i databasen") {
            database.run {
                queries.arrangor.upsert(virksomhetDto.copy(navn = "Kiwi", postnummer = "9999", poststed = "Gåseby"))
            }

            virksomhetConsumer.consume(amtVirksomhet.organisasjonsnummer.value, Json.encodeToJsonElement(amtVirksomhet))
            virksomhetConsumer.consume(amtUnderenhet.organisasjonsnummer.value, Json.encodeToJsonElement(amtUnderenhet))

            database.run {
                queries.arrangor.getAll().should {
                    it.items.shouldHaveSize(1)
                    it.items[0].navn shouldBe "REMA 1000 AS"
                    it.items[0].postnummer shouldBe "1000"
                    it.items[0].poststed shouldBe "Andeby"
                }
            }
        }

        test("delete virksomheter for tombstone messages") {
            database.run {
                queries.arrangor.upsert(underenhetDto)
                queries.arrangor.get(underenhetDto.organisasjonsnummer).shouldNotBeNull()
            }

            virksomhetConsumer.consume(amtUnderenhet.organisasjonsnummer.value, JsonNull)

            database.run {
                queries.arrangor.get(underenhetDto.organisasjonsnummer) shouldBe null
            }
        }
    }
})
