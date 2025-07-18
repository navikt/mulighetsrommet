package no.nav.mulighetsrommet.api.arrangor.kafka

import arrow.core.left
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
import no.nav.mulighetsrommet.brreg.*
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import java.time.LocalDate
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
            organisasjonsform = "BEDR",
            overordnetEnhet = amtVirksomhet.organisasjonsnummer,
        )

        val virksomhetDto = BrregHovedenhetDto(
            organisasjonsnummer = amtVirksomhet.organisasjonsnummer,
            organisasjonsform = "AS",
            navn = amtVirksomhet.navn,
            overordnetEnhet = null,
            postadresse = null,
            forretningsadresse = null,
        )

        val brregClient: BrregClient = mockk()
        coEvery { brregClient.getBrregEnhet(amtVirksomhet.organisasjonsnummer) } returns virksomhetDto.right()
        coEvery { brregClient.getBrregEnhet(amtUnderenhet.organisasjonsnummer) } returns underenhetDto.right()

        val arrangorService = ArrangorService(
            db = database.db,
            brregClient = brregClient,
        )
        val virksomhetConsumer = AmtVirksomheterV1KafkaConsumer(
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
                        organisasjonsform = virksomhetDto.organisasjonsform,
                        navn = "Kiwi",
                    ),
                )
            }

            virksomhetConsumer.consume(amtVirksomhet.organisasjonsnummer.value, Json.encodeToJsonElement(amtVirksomhet))
            virksomhetConsumer.consume(amtUnderenhet.organisasjonsnummer.value, Json.encodeToJsonElement(amtUnderenhet))

            database.run {
                queries.arrangor.getAll().items.shouldHaveSize(1).first().should {
                    it.id shouldBe id
                    it.organisasjonsnummer shouldBe virksomhetDto.organisasjonsnummer
                    it.organisasjonsform shouldBe virksomhetDto.organisasjonsform
                    it.navn shouldBe "REMA 1000 AS"
                }
            }
        }

        test("håndterer virksomet som er fjernet fra Brreg") {
            val orgnr = Organisasjonsnummer("433695968")

            database.run {
                queries.arrangor.upsert(
                    ArrangorDto(
                        id = UUID.randomUUID(),
                        organisasjonsnummer = orgnr,
                        organisasjonsform = null,
                        navn = "Slottet",
                    ),
                )
            }

            val fjernetVirksomhet = AmtVirksomhetV1Dto(
                navn = "Fjernet virksomhet",
                organisasjonsnummer = orgnr,
                overordnetEnhetOrganisasjonsnummer = null,
            )

            coEvery { brregClient.getBrregEnhet(orgnr) } returns BrregError.FjernetAvJuridiskeArsaker(
                FjernetBrregEnhetDto(orgnr, LocalDate.of(2025, 5, 24)),
            ).left()

            virksomhetConsumer.consume(orgnr.value, Json.encodeToJsonElement(fjernetVirksomhet))

            database.run {
                queries.arrangor.get(orgnr).shouldNotBeNull().slettetDato shouldBe LocalDate.of(2025, 5, 24)
            }
        }

        test("delete virksomheter for tombstone messages") {
            database.run {
                queries.arrangor.upsert(
                    ArrangorDto(
                        id = UUID.randomUUID(),
                        organisasjonsnummer = underenhetDto.organisasjonsnummer,
                        organisasjonsform = virksomhetDto.organisasjonsform,
                        navn = "Kiwi",
                    ),
                )
                queries.arrangor.get(underenhetDto.organisasjonsnummer).shouldNotBeNull()
            }

            virksomhetConsumer.consume(amtUnderenhet.organisasjonsnummer.value, JsonNull)

            database.run {
                queries.arrangor.get(underenhetDto.organisasjonsnummer) shouldBe null
            }
        }
    }
})
