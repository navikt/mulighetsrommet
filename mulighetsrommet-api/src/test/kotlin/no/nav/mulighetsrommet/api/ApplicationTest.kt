package no.nav.mulighetsrommet.api

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.security.mock.oauth2.withMockOAuth2Server

class ApplicationTest : FunSpec({
    context("liveness") {
        test("should respond with 200 OK") {
            withMockOAuth2Server {
                withMulighetsrommetApp {
                    val response = client.get("/internal/liveness")

                    response.status shouldBe HttpStatusCode.OK
                }
            }
        }
    }
})
