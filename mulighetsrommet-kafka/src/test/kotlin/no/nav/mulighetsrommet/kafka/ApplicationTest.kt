package no.nav.mulighetsrommet.kafka

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.junit.Test
import kotlin.test.assertEquals

class ApplicationTest {
    @Test
    fun testRoot() {
        testApplication {
            val response = client.get("/")
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("Hello, World!", response.bodyAsText())
        }
    }
}
