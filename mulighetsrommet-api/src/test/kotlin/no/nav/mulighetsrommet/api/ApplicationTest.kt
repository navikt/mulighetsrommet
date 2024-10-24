package no.nav.mulighetsrommet.api

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.security.mock.oauth2.MockOAuth2Server

class ApplicationTest : FunSpec({
    val oauth = MockOAuth2Server()

    beforeSpec {
        oauth.start()
    }

    afterSpec {
        oauth.shutdown()
    }

    context("liveness") {
        test("should respond with 200 OK") {
            val config = createTestApplicationConfig().copy(
                auth = createAuthConfig(oauth, roles = listOf()),
            )
            withTestApplication(config = config) {
                val response = client.get("/internal/liveness")

                response.status shouldBe HttpStatusCode.OK
            }
        }
    }
})
