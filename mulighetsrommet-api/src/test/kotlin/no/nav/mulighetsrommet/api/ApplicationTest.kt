package no.nav.mulighetsrommet.api

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.http.*
import io.ktor.server.testing.*

class ApplicationTest : FunSpec({
    context("ping") {
        test("should respond with pong") {
            withMulighetsrommetApp {
                handleRequest(HttpMethod.Get, "/internal/ping").run {
                    response.status() shouldBe HttpStatusCode.OK
                    response.content shouldBe "PONG"
                }
            }
        }
    }
})
