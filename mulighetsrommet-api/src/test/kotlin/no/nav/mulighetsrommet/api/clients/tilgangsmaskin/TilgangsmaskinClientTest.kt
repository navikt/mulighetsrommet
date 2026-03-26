package no.nav.mulighetsrommet.api.clients.tilgangsmaskin

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.tokenprovider.AccessType

class TilgangsmaskinClientTest : FunSpec({

    val norskIdent = NorskIdent("12345678901")
    val obo = AccessType.OBO("obo-token")

    fun createClient(engine: MockEngine) = TilgangsmaskinClient(
        baseUrl = "https://localhost",
        tokenProvider = { "token" },
        clientEngine = engine,
    )

    test("skal returnere true når tilgangsmaskinen svarer 204 NoContent") {
        val engine = MockEngine {
            respond(content = "", status = HttpStatusCode.NoContent, headers = headersOf())
        }
        val client = createClient(engine)

        client.komplett(norskIdent, obo) shouldBe true
    }

    test("skal returnere false når tilgangsmaskinen svarer 403 Forbidden") {
        val engine = MockEngine {
            respond(content = "", status = HttpStatusCode.Forbidden, headers = headersOf())
        }
        val client = createClient(engine)

        client.komplett(norskIdent, obo) shouldBe false
    }

    test("skal kaste exception når tilgangsmaskinen svarer 404 NotFound") {
        val engine = MockEngine {
            respond(content = "", status = HttpStatusCode.NotFound, headers = headersOf())
        }
        val client = createClient(engine)

        shouldThrow<Exception> {
            client.komplett(norskIdent, obo)
        }
    }

    test("skal kaste exception ved uventet statuskode") {
        val engine = MockEngine {
            respond(content = """{"error": "noe gikk galt"}""", status = HttpStatusCode.InternalServerError, headers = headersOf())
        }
        val client = createClient(engine)

        shouldThrow<Exception> {
            client.komplett(norskIdent, obo)
        }
    }
})
