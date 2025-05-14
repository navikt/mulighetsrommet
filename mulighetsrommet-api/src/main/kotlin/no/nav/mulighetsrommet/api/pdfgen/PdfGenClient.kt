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
import no.nav.mulighetsrommet.api.arrangorflate.api.Beregning

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
        utbetaling: ArrFlateUtbetaling,
        tilsagn: List<ArrangorflateTilsagnDto>,
    ): ByteArray {
        @Serializable
        data class PdfData(
            val utbetaling: ArrFlateUtbetaling,
            val tilsagn: List<ArrangorflateTilsagnDto>,
        )

        val (updatedutbetaling, updatedTilsagn) = subtractSluttPerioder(utbetaling, tilsagn)

        return downloadPdf(
            app = "utbetaling",
            template = "kvittering",
            body = PdfData(updatedutbetaling, updatedTilsagn),
        )
    }

    suspend fun utbetalingJournalpost(
        utbetaling: ArrFlateUtbetaling,
        tilsagn: List<ArrangorflateTilsagnDto>,
    ): ByteArray {
        @Serializable
        data class PdfData(
            val utbetaling: ArrFlateUtbetaling,
            val tilsagn: List<ArrangorflateTilsagnDto>,
        )

        val (updatedutbetaling, updatedTilsagn) = subtractSluttPerioder(utbetaling, tilsagn)

        return downloadPdf(
            app = "utbetaling",
            template = "journalpost",
            body = PdfData(updatedutbetaling, updatedTilsagn),
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

    private fun subtractSluttPerioder(
        utbetaling: ArrFlateUtbetaling,
        tilsagn: List<ArrangorflateTilsagnDto>,
    ): Pair<ArrFlateUtbetaling, List<ArrangorflateTilsagnDto>> {
        val updatedUtbetaling = utbetaling.copy(
            periode = utbetaling.periode.copy(slutt = utbetaling.periode.slutt.minusDays(1)),
            beregning = getBeregningWithSubtractedSluttperiode(utbetaling),
        )

        val updatedTilsagn = tilsagn.map { it.copy(periode = it.periode.copy(slutt = it.periode.slutt.minusDays(1))) }

        return (updatedUtbetaling to updatedTilsagn)
    }

    private fun getBeregningWithSubtractedSluttperiode(utbetaling: ArrFlateUtbetaling): Beregning = when (utbetaling.beregning) {
        is Beregning.Forhandsgodkjent -> utbetaling.beregning.copy(
            deltakelser = utbetaling.beregning.deltakelser.map {
                it.copy(
                    perioder = it.perioder.map { p ->
                        p.copy(
                            periode = p.periode.copy(
                                slutt = p.periode.slutt.minusDays(1),
                            ),
                        )
                    },
                )
            },
        )

        else -> utbetaling.beregning
    }
}
