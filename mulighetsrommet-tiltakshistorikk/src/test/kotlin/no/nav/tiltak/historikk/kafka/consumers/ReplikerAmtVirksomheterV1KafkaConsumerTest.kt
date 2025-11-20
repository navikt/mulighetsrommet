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

    val orgnr = Organisasjonsnummer("876543210")

    val virksomhetDto = AmtVirksomhetV1Dto(
        organisasjonsnummer = orgnr,
        navn = "Gammelt navn",
        overordnetEnhetOrganisasjonsnummer = Organisasjonsnummer("987654321"),
    )

    var dbo = VirksomhetDbo(
        organisasjonsnummer = orgnr,
        navn = "Gammelt navn",
        overordnetEnhetOrganisasjonsnummer = Organisasjonsnummer("987654321"),
        organisasjonsform = "AS",
        slettetDato = null,
    )

    context("replikering fra kafka") {
        val db = TiltakshistorikkDatabase(database.db)

        val brreg = mockk<BrregClient>()
        coEvery { brreg.getBrregEnhet(orgnr) } returns BrregUnderenhetDto(
            organisasjonsnummer = orgnr,
            organisasjonsform = "AS",
            navn = "Nytt navn",
            overordnetEnhet = Organisasjonsnummer("987654321"),
        ).right()

        val virksomheter = VirksomhetService(db, brreg)

        val consumer = ReplikerAmtVirksomheterV1KafkaConsumer(virksomheter)

        afterEach {
            database.truncateAll()
        }

        test("skal ignorere melding hvis virksomheten ikke finnes i databasen") {
            consumer.consume(orgnr.value, Json.encodeToJsonElement(virksomhetDto))

            virksomheter.getVirksomhet(orgnr) shouldBe null
        }

        test("skal oppdatere virksomhet når melding mottas og virksomheten finnes") {
            db.session {
                queries.virksomhet.upsert(dbo)
            }

            consumer.consume(orgnr.value, Json.encodeToJsonElement(virksomhetDto))

            virksomheter.getVirksomhet(orgnr)?.navn shouldBe "Nytt navn"
        }

        test("skal slette virksomhet ved tombstone-melding") {
            db.session {
                queries.virksomhet.upsert(dbo)
            }

            consumer.consume(orgnr.value, JsonNull)

            virksomheter.getVirksomhet(orgnr) shouldBe null
        }
    }
})
