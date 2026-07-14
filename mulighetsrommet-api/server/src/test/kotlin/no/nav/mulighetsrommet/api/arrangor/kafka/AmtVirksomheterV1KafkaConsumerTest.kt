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
import no.nav.mulighetsrommet.admin.arrangor.SyncArrangorUseCase
import no.nav.mulighetsrommet.admin.enhetsregister.EnhetsregisterGateway
import no.nav.mulighetsrommet.admin.enhetsregister.Hovedenhet
import no.nav.mulighetsrommet.admin.enhetsregister.Underenhet
import no.nav.mulighetsrommet.admin.enhetsregister.VirksomhetOppslag
import no.nav.mulighetsrommet.api.domain.arrangor.Arrangor
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import java.time.LocalDate
import java.util.UUID

class AmtVirksomheterV1KafkaConsumerTest : FunSpec({
    val database = extension(ApiDatabaseTestListener())

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

        val underenhet = Underenhet(
            navn = amtUnderenhet.navn,
            organisasjonsnummer = amtUnderenhet.organisasjonsnummer,
            organisasjonsform = "BEDR",
            overordnetEnhet = amtVirksomhet.organisasjonsnummer,
        )

        val hovedenhet = Hovedenhet(
            organisasjonsnummer = amtVirksomhet.organisasjonsnummer,
            organisasjonsform = "AS",
            navn = amtVirksomhet.navn,
        )

        val enhetsregister: EnhetsregisterGateway = mockk {
            coEvery { hentVirksomhet(amtVirksomhet.organisasjonsnummer) } answers {
                VirksomhetOppslag.Funnet(hovedenhet).right()
            }
            coEvery { hentVirksomhet(amtUnderenhet.organisasjonsnummer) } answers {
                VirksomhetOppslag.Funnet(underenhet).right()
            }
        }

        val syncArrangor = SyncArrangorUseCase(database.admin, enhetsregister)
        val virksomhetConsumer = AmtVirksomheterV1KafkaConsumer(database.api, syncArrangor)

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
                queries.arrangor.save(
                    Arrangor.Norsk.opprett(
                        id = id,
                        organisasjonsnummer = hovedenhet.organisasjonsnummer,
                        organisasjonsform = hovedenhet.organisasjonsform,
                        navn = "Kiwi",
                    ),
                )
            }

            virksomhetConsumer.consume(amtVirksomhet.organisasjonsnummer.value, Json.encodeToJsonElement(amtVirksomhet))
            virksomhetConsumer.consume(amtUnderenhet.organisasjonsnummer.value, Json.encodeToJsonElement(amtUnderenhet))

            database.run {
                queries.arrangor.getAll().items.shouldHaveSize(1).first().should {
                    it.id shouldBe id
                    it.organisasjonsnummer shouldBe hovedenhet.organisasjonsnummer
                    it.organisasjonsform shouldBe hovedenhet.organisasjonsform
                    it.navn shouldBe "REMA 1000 AS"
                }
            }
        }

        test("håndterer virksomet som er fjernet fra enhetsregisteret") {
            val orgnr = Organisasjonsnummer("433695968")

            database.run {
                queries.arrangor.save(
                    Arrangor.Norsk.opprett(
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

            coEvery { enhetsregister.hentVirksomhet(orgnr) } answers {
                VirksomhetOppslag.FjernetAvJuridiskeArsaker(orgnr, LocalDate.of(2025, 5, 24)).right()
            }

            virksomhetConsumer.consume(orgnr.value, Json.encodeToJsonElement(fjernetVirksomhet))

            database.run {
                repository.arrangor.getByOrganisasjonsnummer(orgnr).shouldNotBeNull().should {
                    it.slettetDato shouldBe LocalDate.of(2025, 5, 24)
                }
            }
        }

        test("delete virksomheter for tombstone messages") {
            database.run {
                queries.arrangor.save(
                    Arrangor.Norsk.opprett(
                        id = UUID.randomUUID(),
                        organisasjonsnummer = underenhet.organisasjonsnummer,
                        organisasjonsform = hovedenhet.organisasjonsform,
                        navn = "Kiwi",
                    ),
                )
                queries.arrangor.getByOrganisasjonsnummer(underenhet.organisasjonsnummer).shouldNotBeNull()
            }

            virksomhetConsumer.consume(amtUnderenhet.organisasjonsnummer.value, JsonNull)

            database.run {
                repository.arrangor.getByOrganisasjonsnummer(underenhet.organisasjonsnummer) shouldBe null
            }
        }
    }
})
