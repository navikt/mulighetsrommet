package no.nav.mulighetsrommet.api.pdfgen

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.accept
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json

class PdfGenClient(
    clientEngine: HttpClientEngine = CIO.create(),
    private val baseUrl: String,
) {
    private val client = HttpClient(clientEngine) {
        install(ContentNegotiation) {
            json()
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
        }
    }

    suspend fun getPdfDocument(
        content: PdfDocumentContent,
    ): Either<PdfGenError, ByteArray> {
        return downloadPdf(
            app = "block-content",
            template = "document",
            body = content,
        )
    }

    private suspend inline fun <reified T> downloadPdf(
        app: String,
        template: String,
        body: T,
    ): Either<PdfGenError, ByteArray> {
        val response = client.post {
            url("$baseUrl/api/v1/genpdf/$app/$template")
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Pdf)
            setBody(body)
        }

        return if (response.status.isSuccess()) {
            response.bodyAsBytes().right()
        } else {
            PdfGenError(response.status.value, response.bodyAsText()).left()
        }
    }
}

data class PdfGenError(val statusCode: Int, val message: String)
