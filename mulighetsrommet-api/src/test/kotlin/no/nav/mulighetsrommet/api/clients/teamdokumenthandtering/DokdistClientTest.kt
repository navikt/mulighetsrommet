package no.nav.mulighetsrommet.api.clients.teamdokumenthandtering

import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import no.nav.mulighetsrommet.tokenprovider.AccessType

class DokdistClientTest : FunSpec({
    fun createClient(engine: MockEngine) = DokdistClient(engine, "", { "token" })

    test("skal kunne serialisere DokdistRequest") {
        val client = createClient(
            MockEngine { request ->
                request.method shouldBe HttpMethod.Post

                val bodyString = request.body.toByteArray().toString(Charsets.UTF_8)
                bodyString shouldBe """{"journalpostId":"123","batchId":null,"adresse":null,"distribusjonstype":"VIKTIG","distribusjonstidspunkt":"KJERNETID","bestillendeFagsystem":"TILTAKSADMINISTRASJON","dokumentProdApp":"TILTAKSADMINISTRASJON"}"""

                respond(
                    content = """{"bestillingsId":"00000000-0000-0000-0000-000000000000"}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            },
        )
        client.distribuerJournalpost("123", AccessType.M2M, DokdistRequest.DistribusjonsType.VIKTIG, null).shouldBeRight()
    }
})
