package no.nav.mulighetsrommet.api

import io.kotest.core.spec.style.FunSpec
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.assertEquals

class ApplicationTest : FunSpec({
    context("ping") {
        test("should respond with pong") {
            withTestApplication(Application::module) {
                handleRequest(HttpMethod.Get, "/internal/ping").apply {
                    assertEquals("PONG", response.content)
                }
            }
        }
    }
})
