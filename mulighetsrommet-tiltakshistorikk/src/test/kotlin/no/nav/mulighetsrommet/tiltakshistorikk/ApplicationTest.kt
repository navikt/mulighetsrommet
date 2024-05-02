package no.nav.mulighetsrommet.tiltakshistorikk

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.http.*

class AuthenticationTest : FunSpec({
    context("liveness") {
        test("should respond with 200 OK") {
            withTestApplication {
                val response = client.get("/internal/liveness")

                response.status shouldBe HttpStatusCode.OK
            }
        }
    }

    context("readiness") {
        test("should respond with 200 OK") {
            withTestApplication {
                val response = client.get("/internal/readiness")

                response.status shouldBe HttpStatusCode.OK
            }
        }
    }
})
