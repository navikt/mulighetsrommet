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
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingType
import no.nav.mulighetsrommet.api.utbetaling.model.DeltakelsesprosentPeriode
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.api.utils.DatoUtils.formaterDatoTilEuropeiskDatoformat
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
        return downloadPdf(
            app = "utbetaling",
            template = "utbetalingsdetaljer",
            body = toUtbetalingsdetaljer(utbetaling),
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
    val godkjentAvArrangorTidspunkt: LocalDateTime?,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime?,
    val gjennomforing: GjennomforingPdf,
    val tiltakstype: TiltakstypePdf,
    val beregning: BeregningPdf,
    val betalingsinformasjon: Utbetaling.Betalingsinformasjon,
    val type: UtbetalingType?,
    val linjer: List<UtbetalingslinjerPdfDto>?,
    val totaltUtbetalt: Int? = linjer?.fold(0) { acc, linje -> acc + linje.belop },
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
        val entries: List<Entry>,
    )

    @Serializable
    data class Entry(
        val label: String,
        val value: String? = null,
        val format: Format? = null,
    ) {
        enum class Format {
            NOK,
            DATE,
            STATUS_SUCCESS,
        }
    }
}

@Serializable
data class UtbetalingPdfContext(
    val title: String,
    val subject: String,
    val description: String,
    val author: String,
    val sections: List<Section>,
)

fun toUtbetalingsdetaljer(
    utbetaling: UtbetalingPdfDto,
): UtbetalingPdfContext {
    val sections = mutableListOf<Section>()

    sections.add(
        Section(
            title = Section.Header("Detaljer om utbetaling", level = 1),
        ),
    )

    val utbetalingHeader = when (utbetaling.type) {
        UtbetalingType.KORRIGERING -> "Korrigering"
        UtbetalingType.INVESTERING -> "Utbetaling for investering"
        null -> "Innsending"
    }
    sections.add(
        Section(
            title = Section.Header(utbetalingHeader, level = 4),
            blocks = listOf(
                Section.Block(
                    listOfNotNull(
                        Section.Entry(
                            "Arrangør",
                            "${utbetaling.arrangor.navn} (${utbetaling.arrangor.organisasjonsnummer})",
                        ),
                        utbetaling.godkjentAvArrangorTidspunkt
                            ?.let {
                                Section.Entry(
                                    "Dato innsendt av arrangør",
                                    it.toLocalDate().formaterDatoTilEuropeiskDatoformat(),
                                )
                            }
                            ?: utbetaling.createdAt?.let {
                                Section.Entry(
                                    "Dato opprettet hos Nav",
                                    it.toLocalDate().formaterDatoTilEuropeiskDatoformat(),
                                )
                            },
                        Section.Entry("Tiltaksnavn", utbetaling.gjennomforing.navn),
                        Section.Entry("Tiltakstype", utbetaling.tiltakstype.navn),
                    ),
                ),
            ),
        ),
    )

    sections.add(
        Section(
            title = Section.Header("Utbetaling", level = 4),
            blocks = listOf(
                Section.Block(
                    listOfNotNull(
                        Section.Entry(
                            "Utbetalingsperiode",
                            "${utbetaling.periodeStart.formaterDatoTilEuropeiskDatoformat()} - ${utbetaling.periodeSlutt.formaterDatoTilEuropeiskDatoformat()}",
                        ),
                        utbetaling.beregning.antallManedsverk?.let {
                            Section.Entry("Antall månedsverk", it.toString())
                        },
                        Section.Entry(
                            "Beløp",
                            utbetaling.beregning.belop.toString(),
                            Section.Entry.Format.NOK,
                        ),
                    ),
                ),
            ),
        ),
    )

    sections.add(
        Section(
            title = Section.Header("Betalingsinformasjon", level = 4),
            blocks = listOf(
                Section.Block(
                    listOfNotNull(
                        Section.Entry("Kontonummer", utbetaling.betalingsinformasjon.kontonummer?.value),
                        Section.Entry("KID-nummer", utbetaling.betalingsinformasjon.kid?.value),
                    ),
                ),
            ),
        ),
    )

    if (utbetaling.status == "Overført til utbetaling") {
        sections.add(
            Section(
                title = Section.Header("Utbetalingsstatus", level = 4),
                blocks = listOf(
                    Section.Block(
                        listOfNotNull(
                            Section.Entry("Status", utbetaling.status, Section.Entry.Format.STATUS_SUCCESS),
                            Section.Entry(
                                "Godkjent beløp til utbetaling",
                                utbetaling.totaltUtbetalt.toString(),
                                Section.Entry.Format.NOK,
                            ),
                        ),
                    ),
                ),
            ),
        )

        sections.add(
            Section(
                title = Section.Header("Tilsagn som er brukt til utbetaling", level = 4),
                blocks = (utbetaling.linjer ?: listOf()).map {
                    Section.Block(
                        listOfNotNull(
                            Section.Entry("Tilsagn", it.tilsagn.bestillingsnummer),
                            Section.Entry("Beløp til utbetaling", it.belop.toString(), Section.Entry.Format.NOK),
                            Section.Entry("Status", it.status, Section.Entry.Format.STATUS_SUCCESS),
                            it.statusSistOppdatert?.let { sistEndret ->
                                Section.Entry(
                                    "Status endret",
                                    sistEndret.toString(),
                                    Section.Entry.Format.DATE,
                                )
                            },
                        ),
                    )
                },
            ),
        )
    }

    return UtbetalingPdfContext(
        title = "Utbetalingsdetaljer",
        subject = "Utbetaling til ${utbetaling.arrangor.navn}",
        description = "Detaljer om utbetaling for gjennomføring av ${utbetaling.tiltakstype.navn}",
        author = "Nav",
        sections = sections,
    )
}

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
    val perioder: List<DeltakelsesprosentPeriode>,
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
