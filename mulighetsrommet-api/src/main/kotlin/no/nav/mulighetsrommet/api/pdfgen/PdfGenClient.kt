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
import no.nav.mulighetsrommet.api.arrangorflate.api.Beregning
import no.nav.mulighetsrommet.api.tilsagn.api.TilsagnDto
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

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
        utbetalingsdetaljerPdf: UtbetalingsdetaljerPdf,
    ): ByteArray {
        @Serializable
        data class PdfData(
            val utbetalingsdetaljerPdf: UtbetalingsdetaljerPdf,
        )

        val updatedUtbetaling = subtractSluttPerioder(utbetalingsdetaljerPdf.utbetaling)

        return downloadPdf(
            app = "utbetaling",
            template = "utbetalingsdetaljer",
            body = PdfData(
                utbetalingsdetaljerPdf = UtbetalingsdetaljerPdf(
                    utbetaling = UtbetalingPdfDto(
                        status = updatedUtbetaling.status,
                        periode = updatedUtbetaling.periode,
                        arrangor = updatedUtbetaling.arrangor,
                        godkjentArrangorTidspunkt = updatedUtbetaling.godkjentArrangorTidspunkt,
                        createdAt = updatedUtbetaling.createdAt,
                        fristForGodkjenning = updatedUtbetaling.fristForGodkjenning,
                        gjennomforing = updatedUtbetaling.gjennomforing,
                        tiltakstype = updatedUtbetaling.tiltakstype,
                        beregning = updatedUtbetaling.beregning,
                        betalingsinformasjon = updatedUtbetaling.betalingsinformasjon,
                        linjer = updatedUtbetaling.linjer,
                    ),
                ),
            ),
        )
    }

    suspend fun utbetalingJournalpost(
        utbetaling: UtbetalingPdfDto,
    ): ByteArray {
        @Serializable
        data class PdfData(
            val utbetaling: UtbetalingPdfDto,
        )

        val updatedUtbetaling = subtractSluttPerioder(utbetaling)

        return downloadPdf(
            app = "utbetaling",
            template = "journalpost",
            body = PdfData(
                utbetaling = UtbetalingPdfDto(
                    status = updatedUtbetaling.status,
                    periode = updatedUtbetaling.periode,
                    arrangor = updatedUtbetaling.arrangor,
                    godkjentArrangorTidspunkt = updatedUtbetaling.godkjentArrangorTidspunkt,
                    createdAt = updatedUtbetaling.createdAt,
                    fristForGodkjenning = updatedUtbetaling.fristForGodkjenning,
                    gjennomforing = updatedUtbetaling.gjennomforing,
                    tiltakstype = updatedUtbetaling.tiltakstype,
                    beregning = updatedUtbetaling.beregning,
                    betalingsinformasjon = updatedUtbetaling.betalingsinformasjon,
                    linjer = updatedUtbetaling.linjer,
                ),
            ),
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
        utbetaling: UtbetalingPdfDto,
    ): UtbetalingPdfDto {
        val updatedUtbetaling = utbetaling.copy(
            periode = utbetaling.periode.copy(slutt = utbetaling.periode.slutt.minusDays(1)),
            beregning = getBeregningWithSubtractedSluttperiode(utbetaling),
        )

        return updatedUtbetaling
    }

    private fun getBeregningWithSubtractedSluttperiode(utbetaling: UtbetalingPdfDto): Beregning = when (utbetaling.beregning) {
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

@Serializable
data class UtbetalingsdetaljerPdf(
    val utbetaling: UtbetalingPdfDto,
)

@Serializable
data class UtbetalingPdfDto(
    val status: String,
    val periode: Periode,
    val arrangor: Utbetaling.Arrangor,
    @Serializable(with = LocalDateTimeSerializer::class)
    val godkjentArrangorTidspunkt: LocalDateTime?,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime?,
    @Serializable(with = LocalDateSerializer::class)
    val fristForGodkjenning: LocalDate,
    val gjennomforing: Utbetaling.Gjennomforing,
    val tiltakstype: Utbetaling.Tiltakstype,
    val beregning: Beregning,
    val betalingsinformasjon: Utbetaling.Betalingsinformasjon,
    val linjer: List<UtbetalingslinjerPdfDto>?,
)

@Serializable
data class UtbetalingslinjerPdfDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val tilsagn: TilsagnDto,
    val status: String,
    @Serializable(with = LocalDateTimeSerializer::class)
    val statusSistOppdatert: LocalDateTime?,
    val belop: Int,
)
