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
        return client
            .post {
                url("$baseUrl/api/v1/genpdf/$app/$template")
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            .bodyAsBytes()
    }
}

@Serializable
data class PdfDocumentContent(
    val title: String,
    val subject: String,
    val description: String,
    val author: String,
    val sections: List<Section>,
)

@Serializable
data class Section(
    val title: Header,
    val blocks: List<Block> = listOf(),
) {
    @Serializable
    data class Header(
        val text: String,
        val level: Int,
    )

    @Serializable
    data class Block(
        val description: String? = null,
        val values: List<String> = listOf(),
        val entries: List<Entry> = listOf(),
        val table: Table? = null,
    )

    @Serializable
    data class Entry(
        val label: String,
        val value: String?,
        val format: Format? = null,
    )

    @Serializable
    data class Table(
        val columns: List<Column>,
        val rows: List<Row>,
    ) {
        @Serializable
        data class Column(
            val title: String,
            val align: Align = Align.LEFT,
        ) {
            enum class Align {
                LEFT,
                RIGHT,
            }
        }

        @Serializable
        data class Row(
            val cells: List<Cell>,
        )

        @Serializable
        data class Cell(
            val value: String?,
            val format: Format? = null,
        )
    }

    enum class Format {
        NOK,
        DATE,
        PERCENT,
        STATUS_SUCCESS,
    }
}
