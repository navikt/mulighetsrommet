package no.nav.mulighetsrommet.api.pdfgen

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*

class PdfGenClient(
    clientEngine: HttpClientEngine = CIO.create(),
    private val baseUrl: String,
) {
    private val client = HttpClient(clientEngine) {
        install(ContentNegotiation) {
            json()
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
