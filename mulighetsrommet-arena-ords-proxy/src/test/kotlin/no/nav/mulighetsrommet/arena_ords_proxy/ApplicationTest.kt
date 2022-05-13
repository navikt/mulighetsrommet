package no.nav.mulighetsrommet.arena_ords_proxy

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.http.*

class ApplicationTest : FunSpec({
    context("liveness") {
        test("should respond with 200 OK") {
            withArenaOrdsProxyApp {
                val response = client.get("/internal/liveness")

                response.status shouldBe HttpStatusCode.OK
            }
        }
    }
})
