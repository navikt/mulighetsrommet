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
import no.nav.mulighetsrommet.api.refusjon.model.RefusjonKravAft
import no.nav.mulighetsrommet.api.tilsagn.model.ArrangorflateTilsagn

class PdfGenClient(
    clientEngine: HttpClientEngine = CIO.create(),
    private val baseUrl: String,
) {
    private val client = HttpClient(clientEngine) {
        install(ContentNegotiation) {
            json()
        }
    }

    suspend fun getRefusjonKvittering(refusjon: RefusjonKravAft, tilsagn: List<ArrangorflateTilsagn>): ByteArray {
        @Serializable
        data class PdfData(
            val refusjon: RefusjonKravAft,
            val tilsagn: List<ArrangorflateTilsagn>,
        )

        return downloadPdf(
            app = "refusjon",
            template = "kvittering",
            body = PdfData(refusjon, tilsagn),
        )
    }

    suspend fun refusjonJournalpost(refusjon: RefusjonKravAft, tilsagn: List<ArrangorflateTilsagn>): ByteArray {
        @Serializable
        data class PdfData(
            val refusjon: RefusjonKravAft,
            val tilsagn: List<ArrangorflateTilsagn>,
        )

        return downloadPdf(
            app = "refusjon",
            template = "journalpost",
            body = PdfData(refusjon, tilsagn),
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
