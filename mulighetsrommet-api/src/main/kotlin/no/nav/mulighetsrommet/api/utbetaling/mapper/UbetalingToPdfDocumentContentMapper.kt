package no.nav.mulighetsrommet.api.utbetaling.mapper

import no.nav.mulighetsrommet.api.arrangorflate.api.ArrFlateBeregning
import no.nav.mulighetsrommet.api.arrangorflate.api.ArrFlateBeregningDeltakelse
import no.nav.mulighetsrommet.api.arrangorflate.api.ArrFlateUtbetaling
import no.nav.mulighetsrommet.api.arrangorflate.api.ArrFlateUtbetalingStatus
import no.nav.mulighetsrommet.api.pdfgen.Format
import no.nav.mulighetsrommet.api.pdfgen.PdfDocumentContent
import no.nav.mulighetsrommet.api.pdfgen.PdfDocumentContentBuilder
import no.nav.mulighetsrommet.api.pdfgen.TableBlock
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingType
import no.nav.mulighetsrommet.api.utbetaling.api.toReadableName
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.api.utils.DatoUtils.formaterDatoTilEuropeiskDatoformat

object UbetalingToPdfDocumentContentMapper {
    fun toUtbetalingsdetaljerPdfContent(
        utbetaling: ArrFlateUtbetaling,
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

        if (utbetaling.status == ArrFlateUtbetalingStatus.OVERFORT_TIL_UTBETALING || utbetaling.status == ArrFlateUtbetalingStatus.UTBETALT) {
            addUtbetalingsstatusSection(utbetaling)
        }
    }

    fun toJournalpostPdfContent(
        utbetaling: ArrFlateUtbetaling,
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
            is ArrFlateBeregning.Fri -> Unit

            is ArrFlateBeregning.PrisPerManedsverkMedDeltakelsesmengder -> {
                require(utbetaling.beregning.deltakelser.all { it is ArrFlateBeregningDeltakelse.PrisPerManedsverkMedDeltakelsesmengder })
                val casted = utbetaling.beregning.deltakelser.map { it as ArrFlateBeregningDeltakelse.PrisPerManedsverkMedDeltakelsesmengder }
                addDeltakelsesmengderSection(casted)
            }

            is ArrFlateBeregning.PrisPerManedsverk -> {
                addDeltakerperioderSection(utbetaling.beregning.deltakelser)
            }

            is ArrFlateBeregning.PrisPerUkesverk -> {
                addDeltakerperioderSection(utbetaling.beregning.deltakelser)
            }
        }

        when (utbetaling.beregning) {
            is ArrFlateBeregning.Fri -> Unit

            is ArrFlateBeregning.PrisPerManedsverkMedDeltakelsesmengder -> addDeltakelsesfaktorSection(
                sectionHeader = "Beregnet månedsverk",
                deltakelseFaktorColumnName = "Månedsverk",
                deltakelser = utbetaling.beregning.deltakelser,
            )

            is ArrFlateBeregning.PrisPerManedsverk -> addDeltakelsesfaktorSection(
                sectionHeader = "Beregnet månedsverk",
                deltakelseFaktorColumnName = "Månedsverk",
                deltakelser = utbetaling.beregning.deltakelser,
            )

            is ArrFlateBeregning.PrisPerUkesverk -> addDeltakelsesfaktorSection(
                sectionHeader = "Beregnet ukesverk",
                deltakelseFaktorColumnName = "Ukesverk",
                deltakelser = utbetaling.beregning.deltakelser,
            )
        }
    }
}

private fun PdfDocumentContentBuilder.addInnsendingSection(utbetaling: ArrFlateUtbetaling) {
    val utbetalingHeader = when (utbetaling.type) {
        UtbetalingType.KORRIGERING -> "Korrigering"
        UtbetalingType.INVESTERING -> "Utbetaling for investering"
        null -> "Innsending"
    }
    section(utbetalingHeader) {
        descriptionList {
            entry(
                "Arrangør",
                "${utbetaling.arrangor.navn} (${utbetaling.arrangor.organisasjonsnummer.value})",
            )
            utbetaling.godkjentAvArrangorTidspunkt
                ?.let {
                    entry(
                        "Dato innsendt av arrangør",
                        it.toLocalDate().formaterDatoTilEuropeiskDatoformat(),
                    )
                }
                ?: utbetaling.createdAt?.let {
                    entry(
                        "Dato opprettet hos Nav",
                        it.toLocalDate().formaterDatoTilEuropeiskDatoformat(),
                    )
                }
            entry("Tiltaksnavn", utbetaling.gjennomforing.navn)
            entry("Tiltakstype", utbetaling.tiltakstype.navn)
        }
    }
}

private fun PdfDocumentContentBuilder.addUtbetalingSection(utbetaling: ArrFlateUtbetaling) {
    section("Utbetaling") {
        descriptionList {
            val start = utbetaling.periode.start.formaterDatoTilEuropeiskDatoformat()
            val slutt = utbetaling.periode.getLastInclusiveDate().formaterDatoTilEuropeiskDatoformat()
            entry("Utbetalingsperiode", "$start - $slutt")

            when (utbetaling.beregning) {
                is ArrFlateBeregning.Fri -> Unit

                is ArrFlateBeregning.PrisPerManedsverkMedDeltakelsesmengder ->
                    entry("Antall månedsverk", utbetaling.beregning.antallManedsverk.toString())

                is ArrFlateBeregning.PrisPerManedsverk ->
                    entry("Antall månedsverk", utbetaling.beregning.antallManedsverk.toString())

                is ArrFlateBeregning.PrisPerUkesverk ->
                    entry("Antall ukesverk", utbetaling.beregning.antallUkesverk.toString())
            }

            entry(
                "Beløp",
                utbetaling.beregning.belop.toString(),
                Format.NOK,
            )
        }
    }
}

private fun PdfDocumentContentBuilder.addBetalingsinformasjonSection(
    betalingsinformasjon: Utbetaling.Betalingsinformasjon,
) {
    section("Betalingsinformasjon") {
        descriptionList {
            entry("Kontonummer", betalingsinformasjon.kontonummer?.value)
            entry("KID-nummer", betalingsinformasjon.kid?.value)
        }
    }
}

private fun PdfDocumentContentBuilder.addUtbetalingsstatusSection(utbetaling: ArrFlateUtbetaling) {
    section("Utbetalingsstatus") {
        descriptionList {
            val status = ArrFlateUtbetalingStatus.toReadableName(utbetaling.status)
            entry("Status", status, Format.STATUS_SUCCESS)

            val totaltUtbetalt = utbetaling.linjer.fold(0) { acc, linje -> acc + linje.belop }
            entry("Godkjent beløp til utbetaling", totaltUtbetalt.toString(), Format.NOK)
        }
    }

    section("Tilsagn som er brukt til utbetaling") {
        utbetaling.linjer.forEach {
            descriptionList {
                entry("Tilsagn", it.tilsagn.bestillingsnummer)
                entry("Beløp til utbetaling", it.belop.toString(), Format.NOK)
                entry("Status", toReadableName(it.status), Format.STATUS_SUCCESS)
                it.statusSistOppdatert?.let { sistEndret ->
                    entry(
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
    beregning: ArrFlateBeregning,
) {
    val stengt = when (beregning) {
        is ArrFlateBeregning.Fri -> listOf()
        is ArrFlateBeregning.PrisPerManedsverkMedDeltakelsesmengder -> beregning.stengt
        is ArrFlateBeregning.PrisPerManedsverk -> beregning.stengt
        is ArrFlateBeregning.PrisPerUkesverk -> beregning.stengt
    }
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
    deltakelser: List<ArrFlateBeregningDeltakelse.PrisPerManedsverkMedDeltakelsesmengder>,
) {
    section("Deltakerperioder") {
        table {
            column("Navn")
            column("Fødselsdato", TableBlock.Table.Column.Align.RIGHT)
            column("Startdato i perioden", TableBlock.Table.Column.Align.RIGHT)
            column("Sluttdato i perioden", TableBlock.Table.Column.Align.RIGHT)
            column("Deltakelsesprosent", TableBlock.Table.Column.Align.RIGHT)

            deltakelser.forEach { deltakelse ->
                deltakelse.perioderMedDeltakelsesmengde.forEach { (periode, prosent) ->
                    row(
                        TableBlock.Table.Cell(
                            deltakelse.person?.navn,
                        ),
                        TableBlock.Table.Cell(
                            deltakelse.person?.foedselsdato?.formaterDatoTilEuropeiskDatoformat(),
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
    deltakelser: List<ArrFlateBeregningDeltakelse>,
) {
    section("Deltakerperioder") {
        table {
            column("Navn")
            column("Fødselsdato", TableBlock.Table.Column.Align.RIGHT)
            column("Startdato i perioden", TableBlock.Table.Column.Align.RIGHT)
            column("Sluttdato i perioden", TableBlock.Table.Column.Align.RIGHT)

            deltakelser.forEach { deltakelse ->
                row(
                    TableBlock.Table.Cell(
                        deltakelse.person?.navn,
                    ),
                    TableBlock.Table.Cell(
                        deltakelse.person?.foedselsdato?.formaterDatoTilEuropeiskDatoformat(),
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
    deltakelser: List<ArrFlateBeregningDeltakelse>,
) {
    section(sectionHeader) {
        table {
            column("Navn")
            column("Fødselsdato", TableBlock.Table.Column.Align.RIGHT)
            column(deltakelseFaktorColumnName, TableBlock.Table.Column.Align.RIGHT)

            deltakelser.forEach { deltakelse ->
                row(
                    TableBlock.Table.Cell(
                        deltakelse.person?.navn,
                    ),
                    TableBlock.Table.Cell(
                        deltakelse.person?.foedselsdato?.formaterDatoTilEuropeiskDatoformat(),
                    ),
                    TableBlock.Table.Cell(
                        deltakelse.faktor.toString(),
                    ),
                )
            }
        }
    }
}
