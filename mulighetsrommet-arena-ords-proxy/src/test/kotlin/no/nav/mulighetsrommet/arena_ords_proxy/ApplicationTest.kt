package no.nav.mulighetsrommet.arena_ords_proxy

import com.sksamuel.hoplite.Masked
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*

class ApplicationTest : FunSpec({
    context("ping") {
        test("should respond with pong") {
            testApplication {
                // TODO: generalize setup
                application {
                    val appConfig = AppConfig(ArenaOrdsConfig("", "", Masked("")))
                    configure(appConfig)
                }

                val response = client.get("/internal/ping")

                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldBe "PONG"
            }
        }
    }
})
