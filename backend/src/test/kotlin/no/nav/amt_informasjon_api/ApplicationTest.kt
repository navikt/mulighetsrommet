package no.nav.amt_informasjon_api

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.server.testing.*
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
