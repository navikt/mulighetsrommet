package no.nav.mulighetsrommet.api

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

class ApplicationTest : FunSpec({
    context("ping") {
        test("should respond with pong") {
            withMulighetsrommetApp {

                val response = client.get("/internal/ping")

                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldBe "PONG"
            }
        }
    }
})
