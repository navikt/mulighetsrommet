package no.nav.mulighetsrommet.api.pdfgen

import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.arrangorflate.api.ArrFlateUtbetaling
import no.nav.mulighetsrommet.api.arrangorflate.api.ArrangorflateTilsagnDto

class PdfGenClient(
    clientEngine: HttpClientEngine = CIO.create(),
    private val baseUrl: String,
) {
    private val client = HttpClient(clientEngine) {
        install(ContentNegotiation) {
            json()
        }
    }

    suspend fun getUtbetalingKvittering(utbetaling: ArrFlateUtbetaling, tilsagn: List<ArrangorflateTilsagnDto>): ByteArray {
        @Serializable
        data class PdfData(
            val utbetaling: ArrFlateUtbetaling,
            val tilsagn: List<ArrangorflateTilsagnDto>,
        )

        return downloadPdf(
            app = "utbetaling",
            template = "kvittering",
            body = PdfData(utbetaling, tilsagn),
        )
    }

    suspend fun utbetalingJournalpost(utbetaling: ArrFlateUtbetaling, tilsagn: List<ArrangorflateTilsagnDto>): ByteArray {
        @Serializable
        data class PdfData(
            val utbetaling: ArrFlateUtbetaling,
            val tilsagn: List<ArrangorflateTilsagnDto>,
        )

        return downloadPdf(
            app = "utbetaling",
            template = "journalpost",
            body = PdfData(utbetaling, tilsagn),
        )
    }

    private suspend inline fun <reified T> downloadPdf(app: String, template: String, body: T): ByteArray {
        return client
            .post {
                url("$baseUrl/api/v1/genpdf/$app/$template")
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            .bodyAsBytes()
    }
}
