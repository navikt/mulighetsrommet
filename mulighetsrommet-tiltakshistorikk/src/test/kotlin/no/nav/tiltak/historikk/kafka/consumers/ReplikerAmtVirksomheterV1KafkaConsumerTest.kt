package no.nav.tiltak.historikk.kafka.consumers

import arrow.core.right
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import no.nav.mulighetsrommet.brreg.BrregClient
import no.nav.mulighetsrommet.brreg.BrregUnderenhetDto
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.tiltak.historikk.databaseConfig
import no.nav.tiltak.historikk.db.TiltakshistorikkDatabase
import no.nav.tiltak.historikk.model.Virksomhet
import no.nav.tiltak.historikk.replikering.VirksomhetReplikering

class ReplikerAmtVirksomheterV1KafkaConsumerTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    val organisasjonsnummer = Organisasjonsnummer("876543210")
    var overordnetEnhetOrganisasjonsnummer = Organisasjonsnummer("987654321")
    val melding = buildJsonObject {
        put("organisasjonsnummer", organisasjonsnummer.value)
        put("navn", "Gammelt navn")
        put("overordnetEnhetOrganisasjonsnummer", overordnetEnhetOrganisasjonsnummer.value)
    }

    var virksomhetOverordnetEnhet = Virksomhet(
        organisasjonsnummer = overordnetEnhetOrganisasjonsnummer,
        navn = "Overordnet enhet",
        overordnetEnhetOrganisasjonsnummer = null,
        organisasjonsform = "AS",
        slettetDato = null,
    )
    var virksomhet = Virksomhet(
        organisasjonsnummer = organisasjonsnummer,
        navn = "Gammelt navn",
        overordnetEnhetOrganisasjonsnummer = overordnetEnhetOrganisasjonsnummer,
        organisasjonsform = "AS",
        slettetDato = null,
    )

    context("replikering fra kafka") {
        val db = TiltakshistorikkDatabase(database.db)

        val brreg = mockk<BrregClient>()
        coEvery { brreg.getBrregEnhet(organisasjonsnummer) } returns BrregUnderenhetDto(
            organisasjonsnummer = organisasjonsnummer,
            organisasjonsform = "AS",
            navn = "Nytt navn",
            overordnetEnhet = overordnetEnhetOrganisasjonsnummer,
        ).right()

        val virksomheter = VirksomhetReplikering(db, brreg)

        val consumer = ReplikerAmtVirksomheterV1KafkaConsumer(virksomheter)

        afterEach {
            database.truncateAll()
        }

        test("skal ignorere melding hvis virksomheten ikke finnes i databasen") {
            consumer.consume(organisasjonsnummer.value, melding)

            virksomheter.getVirksomhet(organisasjonsnummer) shouldBe null
        }

        test("skal ignorere ukjent virksomhet uavhengig av meldingsinnhold") {
            consumer.consume(organisasjonsnummer.value, JsonObject(emptyMap()))

            virksomheter.getVirksomhet(organisasjonsnummer) shouldBe null
        }

        test("skal oppdatere virksomhet når melding mottas og virksomheten finnes i databasen") {
            db.session {
                queries.virksomhet.upsert(virksomhetOverordnetEnhet)
                queries.virksomhet.upsert(virksomhet)
            }

            consumer.consume(organisasjonsnummer.value, melding)

            virksomheter.getVirksomhet(organisasjonsnummer)?.navn shouldBe "Nytt navn"
        }

        test("skal slette virksomhet ved tombstone-melding") {
            db.session {
                queries.virksomhet.upsert(virksomhetOverordnetEnhet)
                queries.virksomhet.upsert(virksomhet)
            }

            consumer.consume(organisasjonsnummer.value, JsonNull)

            virksomheter.getVirksomhet(organisasjonsnummer) shouldBe null
        }
    }
})
