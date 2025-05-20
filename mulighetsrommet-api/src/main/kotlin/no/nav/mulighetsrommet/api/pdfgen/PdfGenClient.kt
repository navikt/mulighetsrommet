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
import no.nav.mulighetsrommet.api.tilsagn.api.TilsagnDto
import no.nav.mulighetsrommet.api.utbetaling.model.DeltakelsePeriode
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
        utbetaling: UtbetalingPdfDto,
    ): ByteArray {
        @Serializable
        data class PdfData(
            val utbetaling: UtbetalingPdfDto,
        )

        return downloadPdf(
            app = "utbetaling",
            template = "utbetalingsdetaljer",
            body = PdfData(utbetaling),
        )
    }

    suspend fun utbetalingJournalpost(
        utbetaling: UtbetalingPdfDto,
    ): ByteArray {
        @Serializable
        data class PdfData(
            val utbetaling: UtbetalingPdfDto,
        )

        return downloadPdf(
            app = "utbetaling",
            template = "journalpost",
            body = PdfData(utbetaling),
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
data class UtbetalingPdfDto(
    val status: String,
    @Serializable(with = LocalDateSerializer::class)
    val periodeStart: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val periodeSlutt: LocalDate,
    val arrangor: ArrangorPdf,
    @Serializable(with = LocalDateTimeSerializer::class)
    val godkjentArrangorTidspunkt: LocalDateTime?,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime?,
    @Serializable(with = LocalDateSerializer::class)
    val fristForGodkjenning: LocalDate,
    val gjennomforing: GjennomforingPdf,
    val tiltakstype: TiltakstypePdf,
    val beregning: BeregningPdf,
    val betalingsinformasjon: Utbetaling.Betalingsinformasjon,
    val linjer: List<UtbetalingslinjerPdfDto>?,
)

@Serializable
data class ArrangorPdf(
    val organisasjonsnummer: String,
    val navn: String,
)

@Serializable
data class GjennomforingPdf(
    val navn: String,
)

@Serializable
data class TiltakstypePdf(
    val navn: String,
)

@Serializable
data class BeregningPdf(
    val antallManedsverk: Double?,
    val belop: Int,
    val deltakelser: List<DeltakerPdf>,
    val stengt: List<StengtPeriodePdf>,
)

@Serializable
data class DeltakerPdf(
    @Serializable(with = LocalDateSerializer::class)
    val startDato: LocalDate?,
    @Serializable(with = LocalDateSerializer::class)
    val sluttDato: LocalDate?,
    val perioder: List<DeltakelsePeriode>,
    val manedsverk: Double,
    val person: PersonPdf?,
)

@Serializable
data class PersonPdf(
    val navn: String,
    @Serializable(with = LocalDateSerializer::class)
    val fodselsdato: LocalDate?,
    val fodselsaar: Int?,
)

@Serializable
data class StengtPeriodePdf(
    val periode: Periode,
    val beskrivelse: String,
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
