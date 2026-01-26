package no.nav.mulighetsrommet.api.utbetaling.mapper

import no.nav.mulighetsrommet.api.arrangor.model.Betalingsinformasjon
import no.nav.mulighetsrommet.api.arrangorflate.api.ArrangforflateUtbetalingLinje
import no.nav.mulighetsrommet.api.arrangorflate.api.beregningSatsDetaljer
import no.nav.mulighetsrommet.api.arrangorflate.api.beregningStengt
import no.nav.mulighetsrommet.api.clients.amtDeltaker.DeltakerPersonalia
import no.nav.mulighetsrommet.api.pdfgen.Format
import no.nav.mulighetsrommet.api.pdfgen.PdfDocumentContent
import no.nav.mulighetsrommet.api.pdfgen.PdfDocumentContentBuilder
import no.nav.mulighetsrommet.api.pdfgen.TableBlock
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingType
import no.nav.mulighetsrommet.api.utbetaling.api.toDto
import no.nav.mulighetsrommet.api.utbetaling.model.DeltakelsePeriode
import no.nav.mulighetsrommet.api.utbetaling.model.DelutbetalingStatus
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregning
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFastSatsPerTiltaksplassPerManed
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFri
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningOutputDeltakelse
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningPrisPerHeleUkesverk
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningPrisPerManedsverk
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningPrisPerTimeOppfolging
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningPrisPerUkesverk
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatusType
import no.nav.mulighetsrommet.api.utils.DatoUtils.formaterDatoTilEuropeiskDatoformat
import no.nav.mulighetsrommet.api.utils.DatoUtils.tilNorskDato
import no.nav.mulighetsrommet.model.DataElement
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.model.plus
import no.nav.mulighetsrommet.model.withValuta
import java.util.UUID

object UbetalingToPdfDocumentContentMapper {
    fun toUtbetalingsdetaljerPdfContent(
        utbetaling: Utbetaling,
        linjer: List<ArrangforflateUtbetalingLinje>,
    ): PdfDocumentContent = PdfDocumentContent.create(
        title = "Utbetalingsdetaljer",
        subject = "Utbetaling til ${utbetaling.arrangor.navn}",
        description = "Detaljer om utbetaling for gjennomføring av ${utbetaling.tiltakstype.navn}",
        author = "Nav",
    ) {
        mainSection("Detaljer om utbetaling")

        addInnsendingSection(utbetaling)
        addUtbetalingSection(utbetaling)
        addBetalingsinformasjonSection(utbetaling.betalingsinformasjon)

        when (utbetaling.status) {
            UtbetalingStatusType.GENERERT,
            UtbetalingStatusType.INNSENDT,
            UtbetalingStatusType.TIL_ATTESTERING,
            UtbetalingStatusType.RETURNERT,
            UtbetalingStatusType.AVBRUTT,
            -> Unit

            UtbetalingStatusType.FERDIG_BEHANDLET,
            UtbetalingStatusType.DELVIS_UTBETALT,
            UtbetalingStatusType.UTBETALT,
            -> addUtbetalingsstatusSection(utbetaling.valuta, linjer)
        }
    }

    fun toJournalpostPdfContent(
        utbetaling: Utbetaling,
        personalia: Map<UUID, DeltakerPersonalia>,
    ): PdfDocumentContent = PdfDocumentContent.create(
        title = "Utbetaling",
        subject = "Krav om utbetaling fra ${utbetaling.arrangor.navn}",
        description = "Krav om utbetaling fra ${utbetaling.arrangor.navn}",
        author = "Tiltaksadministrasjon",
    ) {
        mainSection("Innsendt krav om utbetaling")

        addInnsendingSection(utbetaling)
        addUtbetalingSection(utbetaling)
        addBetalingsinformasjonSection(utbetaling.betalingsinformasjon)
        addStengtHosArrangorSection(utbetaling.beregning)

        when (utbetaling.beregning) {
            is UtbetalingBeregningFri -> Unit

            is UtbetalingBeregningFastSatsPerTiltaksplassPerManed -> {
                addDeltakelsesmengderSection(utbetaling.beregning, personalia)
            }

            is UtbetalingBeregningPrisPerUkesverk,
            is UtbetalingBeregningPrisPerHeleUkesverk,
            is UtbetalingBeregningPrisPerTimeOppfolging,
            is UtbetalingBeregningPrisPerManedsverk,
            -> {
                addDeltakerperioderSection(utbetaling.beregning.deltakelsePerioder(), personalia)
            }
        }

        when (utbetaling.beregning) {
            is UtbetalingBeregningFri -> Unit

            is UtbetalingBeregningPrisPerTimeOppfolging -> Unit

            is UtbetalingBeregningPrisPerManedsverk,
            is UtbetalingBeregningFastSatsPerTiltaksplassPerManed,
            -> addDeltakelsesfaktorSection(
                sectionHeader = "Beregnet månedsverk",
                deltakelseFaktorColumnName = "Månedsverk",
                deltakelser = utbetaling.beregning.output.deltakelser(),
                personalia = personalia,
            )

            is UtbetalingBeregningPrisPerHeleUkesverk,
            is UtbetalingBeregningPrisPerUkesverk,
            -> addDeltakelsesfaktorSection(
                sectionHeader = "Beregnet ukesverk",
                deltakelseFaktorColumnName = "Ukesverk",
                deltakelser = utbetaling.beregning.output.deltakelser(),
                personalia = personalia,
            )
        }
    }
}

private fun PdfDocumentContentBuilder.addInnsendingSection(utbetaling: Utbetaling) {
    val type = UtbetalingType.from(utbetaling).toDto()
    val utbetalingHeader = type.displayNameLong ?: type.displayName
    section(utbetalingHeader) {
        descriptionList {
            text(
                "Arrangør",
                "${utbetaling.arrangor.navn} (${utbetaling.arrangor.organisasjonsnummer.value})",
            )
            utbetaling.godkjentAvArrangorTidspunkt
                ?.let {
                    text(
                        "Dato innsendt av arrangør",
                        it.toLocalDate().formaterDatoTilEuropeiskDatoformat(),
                    )
                }
                ?: text(
                    "Dato opprettet hos Nav",
                    utbetaling.createdAt.toLocalDate().formaterDatoTilEuropeiskDatoformat(),
                )
            text("Tiltakstype", utbetaling.tiltakstype.navn)
            if (utbetaling.arrangorInnsendtAnnenAvtaltPris()) {
                text(
                    "Tiltaksperiode",
                    Periode.formatPeriode(utbetaling.gjennomforing.start, utbetaling.gjennomforing.slutt),
                )
            }

            text("Løpenummer", utbetaling.gjennomforing.lopenummer.value)
        }
    }
}

private fun PdfDocumentContentBuilder.addUtbetalingSection(utbetaling: Utbetaling) {
    section("Utbetaling") {
        descriptionList {
            text("Utbetalingsperiode", utbetaling.periode.formatPeriode())
            text("Utbetales tidligst", utbetaling.utbetalesTidligstTidspunkt?.tilNorskDato(), Format.DATE)
        }

        val satsDetaljer = beregningSatsDetaljer(utbetaling.beregning)
        satsDetaljer.forEach { satsPeriode ->
            descriptionList {
                if (satsDetaljer.size > 1 && satsPeriode.header != null) {
                    description = satsPeriode.header
                }
                satsPeriode.entries.forEach { entry ->
                    when (val dataElement = entry.value) {
                        is DataElement.Text ->
                            text(
                                entry.label,
                                dataElement.toPdfDocumentValue(),
                                dataElement.format?.toPdfDocumentContentFormat(),
                            )

                        is DataElement.MoneyAmount ->
                            money(entry.label, dataElement)

                        else ->
                            text(entry.label, entry.value?.toPdfDocumentValue())
                    }
                }
            }
        }
        descriptionList {
            money("Beløp", utbetaling.beregning.output.pris)
        }
    }
}

private fun PdfDocumentContentBuilder.addBetalingsinformasjonSection(
    betalingsinformasjon: Betalingsinformasjon?,
) {
    section("Betalingsinformasjon") {
        descriptionList {
            when (betalingsinformasjon) {
                is Betalingsinformasjon.BBan -> {
                    text("Kontonummer", betalingsinformasjon.kontonummer.value)
                    text("KID-nummer", betalingsinformasjon.kid?.value)
                }

                is Betalingsinformasjon.IBan -> {
                    text("IBAN", betalingsinformasjon.iban)
                    text("BIC/SWIFT", betalingsinformasjon.bic)
                    text("Bank landkode", betalingsinformasjon.bankLandKode)
                    text("Banknavn", betalingsinformasjon.bankNavn)
                }

                null -> Unit
            }
        }
    }
}

private fun PdfDocumentContentBuilder.addUtbetalingsstatusSection(
    valuta: Valuta,
    linjer: List<ArrangforflateUtbetalingLinje>,
) {
    section("Utbetalingsstatus") {
        descriptionList {
            val status = if (linjer.all { it.status == DelutbetalingStatus.UTBETALT }) {
                "Utbetalt"
            } else {
                "Overført til utbetaling"
            }
            text("Status", status, Format.STATUS_SUCCESS)

            val totaltUtbetalt = linjer.fold(0.withValuta(valuta)) { acc, linje -> acc + linje.pris }
            money("Godkjent beløp til utbetaling", totaltUtbetalt)
        }
    }

    section("Tilsagn som er brukt til utbetaling") {
        linjer.forEach {
            descriptionList {
                text("Tilsagn", it.tilsagn.bestillingsnummer)
                money("Beløp til utbetaling", it.pris)
                text("Status", it.status.beskrivelse, Format.STATUS_SUCCESS)
                it.statusSistOppdatert?.let { sistEndret ->
                    text(
                        "Status endret",
                        sistEndret.toString(),
                        Format.DATE,
                    )
                }
            }
        }
    }
}

private fun PdfDocumentContentBuilder.addStengtHosArrangorSection(
    beregning: UtbetalingBeregning,
) {
    val stengt = beregningStengt(beregning)
    if (stengt.isNotEmpty()) {
        section("Stengt hos arrangør") {
            itemList {
                description = "Det er registrert stengt hos arrangør i følgende perioder:"
                stengt.forEach {
                    val start = it.periode.start.formaterDatoTilEuropeiskDatoformat()
                    val slutt = it.periode.getLastInclusiveDate().formaterDatoTilEuropeiskDatoformat()
                    item("$start - $slutt: ${it.beskrivelse}")
                }
            }
        }
    }
}

private fun PdfDocumentContentBuilder.addDeltakelsesmengderSection(
    beregning: UtbetalingBeregningFastSatsPerTiltaksplassPerManed,
    personalia: Map<UUID, DeltakerPersonalia>,
) {
    section("Deltakerperioder") {
        table {
            column("Navn")
            column("Fødselsnr.", TableBlock.Table.Column.Align.RIGHT)
            column("Startdato i perioden", TableBlock.Table.Column.Align.RIGHT)
            column("Sluttdato i perioden", TableBlock.Table.Column.Align.RIGHT)
            column("Deltakelsesprosent", TableBlock.Table.Column.Align.RIGHT)

            beregning.input.deltakelser.forEach { deltakelse ->
                val person = personalia[deltakelse.deltakelseId]

                deltakelse.perioder.forEach { (periode, prosent) ->
                    val erSkjermet = person?.erSkjermet == true
                    row(
                        TableBlock.Table.Cell(
                            if (erSkjermet) "Skjermet" else person?.navn,
                        ),
                        TableBlock.Table.Cell(
                            if (erSkjermet) null else person?.norskIdent?.value,
                        ),
                        TableBlock.Table.Cell(
                            periode.start.formaterDatoTilEuropeiskDatoformat(),
                        ),
                        TableBlock.Table.Cell(
                            periode.getLastInclusiveDate().formaterDatoTilEuropeiskDatoformat(),
                        ),
                        TableBlock.Table.Cell(
                            prosent.toString(),
                            Format.PERCENT,
                        ),
                    )
                }
            }
        }
    }
}

private fun PdfDocumentContentBuilder.addDeltakerperioderSection(
    deltakelser: Set<DeltakelsePeriode>,
    personalia: Map<UUID, DeltakerPersonalia>,
) {
    section("Deltakerperioder") {
        table {
            column("Navn")
            column("Fødselsnr.", TableBlock.Table.Column.Align.RIGHT)
            column("Startdato i perioden", TableBlock.Table.Column.Align.RIGHT)
            column("Sluttdato i perioden", TableBlock.Table.Column.Align.RIGHT)

            deltakelser.forEach { deltakelse ->
                val person = personalia[deltakelse.deltakelseId]
                val erSkjermet = person?.erSkjermet == true
                row(
                    TableBlock.Table.Cell(
                        if (erSkjermet) "Skjermet" else person?.navn,
                    ),
                    TableBlock.Table.Cell(
                        if (erSkjermet) null else person?.norskIdent?.value,
                    ),
                    TableBlock.Table.Cell(
                        deltakelse.periode.start.formaterDatoTilEuropeiskDatoformat(),
                    ),
                    TableBlock.Table.Cell(
                        deltakelse.periode.getLastInclusiveDate().formaterDatoTilEuropeiskDatoformat(),
                    ),
                )
            }
        }
    }
}

private fun PdfDocumentContentBuilder.addDeltakelsesfaktorSection(
    sectionHeader: String,
    deltakelseFaktorColumnName: String,
    deltakelser: Set<UtbetalingBeregningOutputDeltakelse>,
    personalia: Map<UUID, DeltakerPersonalia>,
) {
    section(sectionHeader) {
        table {
            column("Navn")
            column("Fødselsnr.", TableBlock.Table.Column.Align.RIGHT)
            column(deltakelseFaktorColumnName, TableBlock.Table.Column.Align.RIGHT)

            deltakelser.forEach { deltakelse ->
                val person = personalia[deltakelse.deltakelseId]
                val erSkjermet = person?.erSkjermet == true
                row(
                    TableBlock.Table.Cell(
                        if (erSkjermet) "Skjermet" else person?.navn,
                    ),
                    TableBlock.Table.Cell(
                        if (erSkjermet) null else person?.norskIdent?.value,
                    ),
                    TableBlock.Table.Cell(
                        deltakelse.perioder.sumOf { it.faktor }.toString(),
                    ),
                )
            }
        }
    }
}

private fun DataElement.Text.Format.toPdfDocumentContentFormat(): Format? = when (this) {
    DataElement.Text.Format.DATE -> Format.DATE
    DataElement.Text.Format.NUMBER -> null
}

private fun DataElement.toPdfDocumentValue(): String? = when (this) {
    is DataElement.Link -> this.text
    is DataElement.MathOperator -> this.operator.toString()
    is DataElement.MultiLinkModal -> this.buttonText
    is DataElement.Periode -> "${this.start} - ${this.slutt}"
    is DataElement.Status -> this.value
    is DataElement.Text -> this.value
    is DataElement.MoneyAmount -> this.value
}
