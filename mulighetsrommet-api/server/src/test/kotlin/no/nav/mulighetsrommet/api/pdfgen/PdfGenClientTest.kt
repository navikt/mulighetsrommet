package no.nav.mulighetsrommet.api.pdfgen

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.serialization.json.JsonPrimitive

class PdfGenClientTest : FunSpec({
    fun createClient(engine: MockEngine) = PdfGenClient(
        clientEngine = engine,
        baseUrl = "https://localhost",
    )

    val content = PdfDocumentContent(
        title = "title",
        subject = "subject",
        description = "description",
        author = "author",
        sections = listOf(
            Section(
                title = Header(text = "section", level = 1),
            ),
        ),
    )

    test("parser problem detail fra feilrespons") {
        val engine = MockEngine {
            respond(
                content = """
                    {
                      "type": "urn:pdfgenrs:error:generation-failed",
                      "title": "Internal Server Error",
                      "status": 500,
                      "detail": "Klarte ikke generere pdf",
                      "trace_id": "trace-1",
                      "request_id": "request-1"
                    }
                """.trimIndent(),
                status = HttpStatusCode.InternalServerError,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.ProblemJson.toString()),
            )
        }

        val error = createClient(engine).getPdfDocument(content).shouldBeLeft()

        error.type shouldBe "urn:pdfgenrs:error:generation-failed"
        error.title shouldBe "Internal Server Error"
        error.status shouldBe 500
        error.detail shouldBe "Klarte ikke generere pdf"
        (error.extensions?.get("trace_id") as JsonPrimitive).content shouldBe "trace-1"
        (error.extensions?.get("request_id") as JsonPrimitive).content shouldBe "request-1"
    }

    test("feil content-type gir generisk problem detail") {
        val engine = MockEngine {
            respond(
                status = HttpStatusCode.InternalServerError,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Text.Plain.toString()),
                content = "Internal Server Error",
            )
        }

        val error = createClient(engine).getPdfDocument(content).shouldBeLeft()

        error.type shouldBe "about:blank"
        error.title shouldBe "Internal Server Error"
        error.status shouldBe 500
        error.detail shouldBe "pdfgen returnerte HTTP 500"
    }
})
