package no.nav.mulighetsrommet.arena_ords_proxy

import io.ktor.http.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlin.test.*
import io.ktor.server.testing.*

class ApplicationTest {
    @Test
    fun testRoot() = testApplication {
        client.get("/internal/ping").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("PONG", bodyAsText())
        }
    }
}
