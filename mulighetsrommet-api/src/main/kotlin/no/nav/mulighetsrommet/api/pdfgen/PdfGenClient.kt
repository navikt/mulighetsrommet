package no.nav.mulighetsrommet.api.pdfgen

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

    suspend fun getUtbetalingKvittering(
        utbetaling: PdfDocumentContent,
    ): ByteArray {
        return downloadPdf(
            app = "utbetaling",
            template = "utbetalingsdetaljer",
            body = utbetaling,
        )
    }

    suspend fun utbetalingJournalpost(
        utbetaling: PdfDocumentContent,
    ): ByteArray {
        return downloadPdf(
            app = "utbetaling",
            template = "journalpost",
            body = utbetaling,
        )
    }

    private suspend inline fun <reified T> downloadPdf(app: String, template: String, body: T): ByteArray {
        // TODO: handle errors
        return client
            .post {
                url("$baseUrl/api/v1/genpdf/$app/$template")
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            .bodyAsBytes()
    }
}
