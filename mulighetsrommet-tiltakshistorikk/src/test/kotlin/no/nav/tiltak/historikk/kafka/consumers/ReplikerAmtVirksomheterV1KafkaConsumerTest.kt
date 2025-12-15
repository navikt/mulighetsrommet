package no.nav.tiltak.historikk.kafka.consumers

import arrow.core.right
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.mulighetsrommet.brreg.BrregClient
import no.nav.mulighetsrommet.brreg.BrregUnderenhetDto
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.tiltak.historikk.databaseConfig
import no.nav.tiltak.historikk.db.TiltakshistorikkDatabase
import no.nav.tiltak.historikk.db.queries.VirksomhetDbo
import no.nav.tiltak.historikk.service.VirksomhetService

class ReplikerAmtVirksomheterV1KafkaConsumerTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    val organisasjonsnummer = Organisasjonsnummer("876543210")
    var overordnetEnhetOrganisasjonsnummer = Organisasjonsnummer("987654321")
    val virksomhetDto = AmtVirksomhetV1Dto(
        organisasjonsnummer = organisasjonsnummer,
        navn = "Gammelt navn",
        overordnetEnhetOrganisasjonsnummer = overordnetEnhetOrganisasjonsnummer,
    )

    var virksomhetOverordnetEnhet = VirksomhetDbo(
        organisasjonsnummer = overordnetEnhetOrganisasjonsnummer,
        navn = "Overordnet enhet",
        overordnetEnhetOrganisasjonsnummer = null,
        organisasjonsform = "AS",
        slettetDato = null,
    )
    var virksomhet = VirksomhetDbo(
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

        val virksomheter = VirksomhetService(db, brreg)

        val consumer = ReplikerAmtVirksomheterV1KafkaConsumer(virksomheter)

        afterEach {
            database.truncateAll()
        }

        test("skal ignorere melding hvis virksomheten ikke finnes i databasen") {
            consumer.consume(organisasjonsnummer.value, Json.encodeToJsonElement(virksomhetDto))

            virksomheter.getVirksomhet(organisasjonsnummer) shouldBe null
        }

        test("skal oppdatere virksomhet når melding mottas og virksomheten finnes i databasen") {
            db.session {
                queries.virksomhet.upsert(virksomhetOverordnetEnhet)
                queries.virksomhet.upsert(virksomhet)
            }

            consumer.consume(organisasjonsnummer.value, Json.encodeToJsonElement(virksomhetDto))

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
