package no.nav.mulighetsrommet.tiltak.okonomi

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.call.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.resources.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import no.nav.mulighetsrommet.ktor.createMockEngine
import no.nav.mulighetsrommet.tiltak.okonomi.oebs.OebsBestilling
import no.nav.security.mock.oauth2.MockOAuth2Server

class TiltaksokonomiTest : FunSpec({
    val oauth = MockOAuth2Server()

    beforeSpec {
        oauth.start()
    }

    afterSpec {
        oauth.shutdown()
    }

    context("bestilling") {
        test("unauthroized når token mangler") {
            val mockEngine = mockOebsTiltakApi()

            withTestApplication(oauth, mockEngine) {
                val client = createClient {
                    install(Resources)
                    install(ContentNegotiation) {
                        json()
                    }
                }

                val response = client.get(Bestilling.Id(id = "T-123"))

                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        test("hente status på bestilling") {
            val mockEngine = mockOebsTiltakApi()

            withTestApplication(oauth, mockEngine) {
                val client = createClient {
                    install(Resources)
                    install(ContentNegotiation) {
                        json()
                    }
                }

                val response = client.get(Bestilling.Id(id = "T-123")) {
                    bearerAuth(oauth.issueToken().serialize())
                }

                response.status shouldBe HttpStatusCode.OK

                response.body<OebsBestilling>() shouldBe OebsBestilling(
                    id = "T-123",
                    status = OebsBestilling.Status.BEHANDLET,
                )
            }
        }
    }
})

private fun mockOebsTiltakApi(): MockEngine {
    return createMockEngine(
        "http://oebs-tiltak-api" to { respondOk() },
    )
}
