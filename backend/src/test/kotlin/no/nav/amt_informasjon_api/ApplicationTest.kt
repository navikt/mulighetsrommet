package no.nav.amt_informasjon_api

import io.ktor.application.Application
import io.ktor.http.HttpMethod
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import org.junit.Test
import kotlin.test.assertEquals

class ApplicationTest {

    @Test
    fun `should be alive`() {
        withTestApplication(Application::module) {
            handleRequest(HttpMethod.Get, "/internal/ping").apply {
                assertEquals("PONG", response.content)
            }
        }
    }
}
