package no.nav.mulighetsrommet.api

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode

class ApplicationTest : FunSpec({
    context("liveness") {
        test("should respond with 200 OK") {
            withTestApplication {
                val response = client.get("/internal/liveness")

                response.status shouldBe HttpStatusCode.OK
            }
        }
    }
})
