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
import no.nav.mulighetsrommet.api.arrangor.ArrangorService
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorDto
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.brreg.BrregClient
import no.nav.mulighetsrommet.brreg.BrregEnhetMedUnderenheterDto
import no.nav.mulighetsrommet.brreg.BrregUnderenhetDto
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import java.util.*

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

        val underenhetDto = BrregUnderenhetDto(
            navn = amtUnderenhet.navn,
            organisasjonsnummer = amtUnderenhet.organisasjonsnummer,
            overordnetEnhet = amtVirksomhet.organisasjonsnummer,
            postnummer = "1000",
            poststed = "Andeby",
        )

        val virksomhetDto = BrregEnhetMedUnderenheterDto(
            organisasjonsnummer = amtVirksomhet.organisasjonsnummer,
            navn = amtVirksomhet.navn,
            underenheter = listOf(),
            postnummer = "1000",
            poststed = "Andeby",
        )

        val brregClient: BrregClient = mockk()
        coEvery { brregClient.getBrregVirksomhet(amtVirksomhet.organisasjonsnummer) } returns virksomhetDto.right()
        coEvery { brregClient.getBrregVirksomhet(amtUnderenhet.organisasjonsnummer) } returns underenhetDto.right()

        val arrangorService = ArrangorService(
            db = database.db,
            brregClient = brregClient,
        )
        val virksomhetConsumer = AmtVirksomheterV1KafkaConsumer(
            config = KafkaTopicConsumer.Config(id = "virksomheter", topic = "virksomheter"),
            arrangorService = arrangorService,
        )

        test("ignorer virksomheter når de ikke allerede er lagret i databasen") {
            virksomhetConsumer.consume(amtVirksomhet.organisasjonsnummer.value, Json.encodeToJsonElement(amtVirksomhet))
            virksomhetConsumer.consume(amtUnderenhet.organisasjonsnummer.value, Json.encodeToJsonElement(amtUnderenhet))

            database.run {
                queries.arrangor.getAll().items.shouldBeEmpty()
            }
        }

        test("oppdaterer bare virksomheter som er lagret i databasen") {
            val id = UUID.randomUUID()
            database.run {
                queries.arrangor.upsert(
                    ArrangorDto(
                        id = id,
                        organisasjonsnummer = virksomhetDto.organisasjonsnummer,
                        navn = "Kiwi",
                        postnummer = "9999",
                        poststed = "Gåseby",
                    ),
                )
            }

            virksomhetConsumer.consume(amtVirksomhet.organisasjonsnummer.value, Json.encodeToJsonElement(amtVirksomhet))
            virksomhetConsumer.consume(amtUnderenhet.organisasjonsnummer.value, Json.encodeToJsonElement(amtUnderenhet))

            database.run {
                queries.arrangor.getAll().items.shouldHaveSize(1).first().should {
                    it.id shouldBe id
                    it.organisasjonsnummer shouldBe virksomhetDto.organisasjonsnummer
                    it.navn shouldBe "REMA 1000 AS"
                    it.postnummer shouldBe "1000"
                    it.poststed shouldBe "Andeby"
                }
            }
        }

        test("delete virksomheter for tombstone messages") {
            database.run {
                database.run {
                    queries.arrangor.upsert(
                        ArrangorDto(
                            id = UUID.randomUUID(),
                            organisasjonsnummer = underenhetDto.organisasjonsnummer,
                            navn = "Kiwi",
                            postnummer = "9999",
                            poststed = "Gåseby",
                        ),
                    )
                    queries.arrangor.get(underenhetDto.organisasjonsnummer).shouldNotBeNull()
                }
            }

            virksomhetConsumer.consume(amtUnderenhet.organisasjonsnummer.value, JsonNull)

            database.run {
                queries.arrangor.get(underenhetDto.organisasjonsnummer) shouldBe null
            }
        }
    }
})
