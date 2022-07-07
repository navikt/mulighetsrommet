package no.nav.mulighetsrommet.api

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.security.mock.oauth2.withMockOAuth2Server

class ApplicationTest : FunSpec({
    context("ping") {
        test("should respond with pong") {
            withMockOAuth2Server {
                withMulighetsrommetApp {

                    val response = client.get("/internal/ping")

                    response.status shouldBe HttpStatusCode.OK
                    response.bodyAsText() shouldBe "PONG"
                }
            }
        }
    }
})
