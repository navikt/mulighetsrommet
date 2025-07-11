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
        return downloadPdf(
            app = "utbetaling",
            template = "journalpost",
            body = toJournalpost(utbetaling),
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

    sections.addCommonUtbetalingSections(utbetaling)

    if (utbetaling.status == "Overført til utbetaling") {
        sections.add(
            Section(
                title = Section.Header("Utbetalingsstatus", level = 4),
                blocks = listOf(
                    Section.Block(
                        entries = listOfNotNull(
                            Section.Entry("Status", utbetaling.status, Section.Format.STATUS_SUCCESS),
                            Section.Entry(
                                "Godkjent beløp til utbetaling",
                                utbetaling.totaltUtbetalt.toString(),
                                Section.Format.NOK,
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
                        entries = listOfNotNull(
                            Section.Entry("Tilsagn", it.tilsagn.bestillingsnummer),
                            Section.Entry("Beløp til utbetaling", it.belop.toString(), Section.Format.NOK),
                            Section.Entry("Status", it.status, Section.Format.STATUS_SUCCESS),
                            it.statusSistOppdatert?.let { sistEndret ->
                                Section.Entry(
                                    "Status endret",
                                    sistEndret.toString(),
                                    Section.Format.DATE,
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

fun toJournalpost(
    utbetaling: UtbetalingPdfDto,
): UtbetalingPdfContext {
    val sections = mutableListOf<Section>()

    sections.add(
        Section(
            title = Section.Header("Innsendt krav om utbetaling", level = 1),
        ),
    )

    sections.addCommonUtbetalingSections(utbetaling)

    if (utbetaling.beregning.stengt.isNotEmpty()) {
        sections.add(
            Section(
                title = Section.Header("Stengt hos arrangør", level = 4),
                blocks = listOf(
                    Section.Block(
                        description = "Det er registrert stengt hos arrangør i følgende perioder:",
                        values = utbetaling.beregning.stengt.map {
                            val start = it.periode.start.formaterDatoTilEuropeiskDatoformat()
                            val slutt = it.periode.getLastInclusiveDate().formaterDatoTilEuropeiskDatoformat()
                            "$start - $slutt: ${it.beskrivelse}"
                        },
                    ),
                ),
            ),
        )
    }

    if (utbetaling.beregning.deltakelser.isNotEmpty()) {
        sections.add(
            Section(
                title = Section.Header("Deltakerperioder", level = 4),
                blocks = listOf(
                    Section.Block(
                        table = Section.Table(
                            columns = listOf(
                                Section.Table.Column("Navn"),
                                Section.Table.Column("Fødselsdato", Section.Table.Column.Align.RIGHT),
                                Section.Table.Column("Startdato i perioden", Section.Table.Column.Align.RIGHT),
                                Section.Table.Column("Sluttdato i perioden", Section.Table.Column.Align.RIGHT),
                                Section.Table.Column("Deltakelsesprosent", Section.Table.Column.Align.RIGHT),
                            ),
                            rows = utbetaling.beregning.deltakelser.flatMap { deltakelse ->
                                deltakelse.perioder.map { (periode, prosent) ->
                                    Section.Table.Row(
                                        cells = listOf(
                                            Section.Table.Cell(
                                                deltakelse.person?.navn,
                                            ),
                                            Section.Table.Cell(
                                                deltakelse.person?.fodselsdato?.formaterDatoTilEuropeiskDatoformat(),
                                            ),
                                            Section.Table.Cell(
                                                periode.start.formaterDatoTilEuropeiskDatoformat(),
                                            ),
                                            Section.Table.Cell(
                                                periode.getLastInclusiveDate().formaterDatoTilEuropeiskDatoformat(),
                                            ),
                                            Section.Table.Cell(
                                                prosent.toString(),
                                                Section.Format.PERCENT,
                                            ),
                                        ),
                                    )
                                }
                            },
                        ),
                    ),
                ),
            ),
        )

        sections.add(
            Section(
                title = Section.Header("Beregnet månedsverk", level = 4),
                blocks = listOf(
                    Section.Block(
                        table = Section.Table(
                            columns = listOf(
                                Section.Table.Column("Navn"),
                                Section.Table.Column("Fødselsdato", Section.Table.Column.Align.RIGHT),
                                Section.Table.Column("Månedsverk", Section.Table.Column.Align.RIGHT),
                            ),
                            rows = utbetaling.beregning.deltakelser.map { deltakelse ->
                                Section.Table.Row(
                                    cells = listOf(
                                        Section.Table.Cell(
                                            deltakelse.person?.navn,
                                        ),
                                        Section.Table.Cell(
                                            deltakelse.person?.fodselsdato?.formaterDatoTilEuropeiskDatoformat(),
                                        ),
                                        Section.Table.Cell(
                                            deltakelse.manedsverk.toString(),
                                        ),
                                    ),
                                )
                            },
                        ),
                    ),
                ),
            ),
        )
    }

    return UtbetalingPdfContext(
        title = "Utbetaling",
        subject = "Krav om utbetaling fra ${utbetaling.arrangor.navn}",
        description = "Krav om utbetaling fra ${utbetaling.arrangor.navn}",
        author = "Tiltaksadministrasjon",
        sections = sections,
    )
}

private fun MutableList<Section>.addCommonUtbetalingSections(utbetaling: UtbetalingPdfDto) {
    val utbetalingHeader = when (utbetaling.type) {
        UtbetalingType.KORRIGERING -> "Korrigering"
        UtbetalingType.INVESTERING -> "Utbetaling for investering"
        null -> "Innsending"
    }
    add(
        Section(
            title = Section.Header(utbetalingHeader, level = 4),
            blocks = listOf(
                Section.Block(
                    entries = listOfNotNull(
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

    add(
        Section(
            title = Section.Header("Utbetaling", level = 4),
            blocks = listOf(
                Section.Block(
                    entries = listOfNotNull(
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
                            Section.Format.NOK,
                        ),
                    ),
                ),
            ),
        ),
    )

    add(
        Section(
            title = Section.Header("Betalingsinformasjon", level = 4),
            blocks = listOf(
                Section.Block(
                    entries = listOfNotNull(
                        Section.Entry("Kontonummer", utbetaling.betalingsinformasjon.kontonummer?.value),
                        Section.Entry("KID-nummer", utbetaling.betalingsinformasjon.kid?.value),
                    ),
                ),
            ),
        ),
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
