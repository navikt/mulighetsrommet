package no.nav.mulighetsrommet.api.utbetaling.mapper

import no.nav.mulighetsrommet.api.arrangorflate.dto.UtbetalingsdetaljerDto
import no.nav.mulighetsrommet.api.arrangorflate.service.beregningStengt
import no.nav.mulighetsrommet.api.domain.personalia.Gradering
import no.nav.mulighetsrommet.api.domain.personalia.Personalia
import no.nav.mulighetsrommet.api.pdfgen.DescriptionListBlockBuilder
import no.nav.mulighetsrommet.api.pdfgen.Format
import no.nav.mulighetsrommet.api.pdfgen.PdfDocumentContent
import no.nav.mulighetsrommet.api.pdfgen.PdfDocumentContentBuilder
import no.nav.mulighetsrommet.api.pdfgen.TableBlock
import no.nav.mulighetsrommet.api.utbetaling.model.DeltakelsePeriode
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregning
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningAvtaltPrisPerBenyttetPlassPerHeleUke
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningAvtaltPrisPerBenyttetPlassPerManed
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningAvtaltPrisPerBenyttetPlassPerUke
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningAvtaltPrisPerTimeOppfolging
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFastSatsPerAvtaltTiltaksplassPerManed
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFastSatsPerBenyttetPlassPerManed
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFri
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningOutputDeltakelse
import no.nav.mulighetsrommet.api.utils.DatoUtils.formaterDatoTilEuropeiskDatoformat
import no.nav.mulighetsrommet.model.DataDetails
import no.nav.mulighetsrommet.model.DataElement
import no.nav.mulighetsrommet.model.LabeledDataElement

object UbetalingToPdfDocumentContentMapper {
    fun toUtbetalingsdetaljerPdfContent(
        detaljer: UtbetalingsdetaljerDto,
    ): PdfDocumentContent = PdfDocumentContent.create(
        title = "Utbetalingsdetaljer",
        subject = "Utbetaling til ${detaljer.arrangorNavn}",
        description = "Detaljer om utbetaling for gjennomføring av ${detaljer.tiltakstypeNavn}",
        author = "Nav",
    ) {
        mainSection("Detaljer om utbetaling")

        addCommonSections(detaljer)
        addDetailsSection(detaljer.status)
        detaljer.avbrytelse?.let { addDetailsSection(it) }
        if (detaljer.tilsagn.isNotEmpty()) {
            section(detaljer.tilsagnHeader) {
                detaljer.tilsagn.forEach { linje ->
                    descriptionList {
                        description = linje.header
                        addEntries(linje.entries)
                    }
                }
            }
        }
    }

    fun toJournalpostPdfContent(
        detaljer: UtbetalingsdetaljerDto,
        utbetaling: Utbetaling,
        personalia: List<Personalia>,
    ): PdfDocumentContent = PdfDocumentContent.create(
        title = "Utbetaling",
        subject = "Krav om utbetaling fra ${utbetaling.arrangor.navn}",
        description = "Krav om utbetaling fra ${utbetaling.arrangor.navn}",
        author = "Tiltaksadministrasjon",
    ) {
        mainSection("Innsendt krav om utbetaling")

        addCommonSections(detaljer)
        addStengtHosArrangorSection(utbetaling.beregning)

        when (utbetaling.beregning) {
            is UtbetalingBeregningFastSatsPerBenyttetPlassPerManed -> {
                addDeltakelsesmengderSection(utbetaling.beregning, personalia)
            }

            is UtbetalingBeregningAvtaltPrisPerBenyttetPlassPerUke,
            is UtbetalingBeregningAvtaltPrisPerBenyttetPlassPerHeleUke,
            is UtbetalingBeregningAvtaltPrisPerTimeOppfolging,
            is UtbetalingBeregningAvtaltPrisPerBenyttetPlassPerManed,
            -> {
                addDeltakerperioderSection(utbetaling.beregning.deltakelsePerioder(), personalia)
            }

            is UtbetalingBeregningFri,
            is UtbetalingBeregningFastSatsPerAvtaltTiltaksplassPerManed,
            -> Unit
        }

        when (utbetaling.beregning) {
            is UtbetalingBeregningAvtaltPrisPerBenyttetPlassPerManed,
            is UtbetalingBeregningFastSatsPerBenyttetPlassPerManed,
            -> addDeltakelsesfaktorSection(
                sectionHeader = "Beregnet månedsverk",
                deltakelseFaktorColumnName = "Månedsverk",
                deltakelser = utbetaling.beregning.output.deltakelser(),
                personalia = personalia,
            )

            is UtbetalingBeregningAvtaltPrisPerBenyttetPlassPerHeleUke,
            is UtbetalingBeregningAvtaltPrisPerBenyttetPlassPerUke,
            -> addDeltakelsesfaktorSection(
                sectionHeader = "Beregnet ukesverk",
                deltakelseFaktorColumnName = "Ukesverk",
                deltakelser = utbetaling.beregning.output.deltakelser(),
                personalia = personalia,
            )

            is UtbetalingBeregningFri,
            is UtbetalingBeregningFastSatsPerAvtaltTiltaksplassPerManed,
            is UtbetalingBeregningAvtaltPrisPerTimeOppfolging,
            -> Unit
        }
    }
}

private fun PdfDocumentContentBuilder.addCommonSections(detaljer: UtbetalingsdetaljerDto) {
    addDetailsSection(detaljer.innsending)
    section(requireNotNull(detaljer.utbetaling.header)) {
        detaljer.satsDetaljer.forEach { sats ->
            descriptionList {
                description = sats.header
                addEntries(sats.entries)
            }
        }
        descriptionList { addEntries(detaljer.utbetaling.entries) }
    }
    addDetailsSection(detaljer.betalingsinformasjon)
}

private fun PdfDocumentContentBuilder.addDetailsSection(details: DataDetails) {
    section(requireNotNull(details.header)) {
        descriptionList { addEntries(details.entries) }
    }
}

private fun DescriptionListBlockBuilder.addEntries(entries: List<LabeledDataElement>) {
    entries.forEach { entry ->
        when (val value = entry.value) {
            is DataElement.MoneyAmount -> money(entry.label, value)
            is DataElement.Text -> text(entry.label, value.value ?: "-", value.format?.toPdfDocumentContentFormat())
            else -> text(entry.label, value?.toPdfDocumentValue() ?: "-")
        }
    }
}

private fun PdfDocumentContentBuilder.addStengtHosArrangorSection(
    beregning: UtbetalingBeregning,
) {
    val stengt = beregningStengt(beregning)
    if (stengt.isNotEmpty()) {
        section("Det er registrert stengt hos arrangør i følgende perioder:") {
            itemList {
                description = null
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
    beregning: UtbetalingBeregningFastSatsPerBenyttetPlassPerManed,
    personalia: List<Personalia>,
) {
    section("Deltakerperioder") {
        table {
            column("Navn")
            column("Fødselsnr.", TableBlock.Table.Column.Align.RIGHT)
            column("Startdato i perioden", TableBlock.Table.Column.Align.RIGHT)
            column("Sluttdato i perioden", TableBlock.Table.Column.Align.RIGHT)
            column("Deltakelsesprosent", TableBlock.Table.Column.Align.RIGHT)

            beregning.input.deltakelser.forEach { deltakelse ->
                val p = personalia.find { it.deltakerId == deltakelse.deltakelseId }

                deltakelse.perioder.forEach { (periode, prosent) ->
                    row(
                        *deltakerNavnOgIdent(p),
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
    personalia: List<Personalia>,
) {
    section("Deltakerperioder") {
        table {
            column("Navn")
            column("Fødselsnr.", TableBlock.Table.Column.Align.RIGHT)
            column("Startdato i perioden", TableBlock.Table.Column.Align.RIGHT)
            column("Sluttdato i perioden", TableBlock.Table.Column.Align.RIGHT)

            deltakelser.forEach { deltakelse ->
                val personalia = personalia.find { it.deltakerId == deltakelse.deltakelseId }
                row(
                    *deltakerNavnOgIdent(personalia),
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
    personalia: List<Personalia>,
) {
    section(sectionHeader) {
        table {
            column("Navn")
            column("Fødselsnr.", TableBlock.Table.Column.Align.RIGHT)
            column(deltakelseFaktorColumnName, TableBlock.Table.Column.Align.RIGHT)

            deltakelser.forEach { deltakelse ->
                val p = personalia.find { it.deltakerId == deltakelse.deltakelseId }
                row(
                    *deltakerNavnOgIdent(p),
                    TableBlock.Table.Cell(
                        deltakelse.perioder.sumOf { it.faktor }.toString(),
                    ),
                )
            }
        }
    }
}

private fun deltakerNavnOgIdent(personalia: Personalia?): Array<TableBlock.Table.Cell> {
    return arrayOf(
        TableBlock.Table.Cell(
            when (personalia?.gradering) {
                Gradering.SKJERMING -> "Skjermet"

                Gradering.STRENGT_FORTROLIG_UTLAND,
                Gradering.STRENGT_FORTROLIG_ADRESSE,
                Gradering.FORTROLIG_ADRESSE,
                -> "Adressebeskyttet"

                else -> personalia?.navn()
            },

        ),
        TableBlock.Table.Cell(
            when (personalia?.gradering) {
                Gradering.UGRADERT -> personalia.norskIdent()?.value
                else -> null
            },
        ),
    )
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
