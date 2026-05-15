package no.nav.mulighetsrommet.api.utbetaling.mapper

import no.nav.mulighetsrommet.api.arrangorflate.dto.ArrangforflateUtbetalingLinje
import no.nav.mulighetsrommet.api.arrangorflate.service.beregningSatsDetaljer
import no.nav.mulighetsrommet.api.arrangorflate.service.beregningStengt
import no.nav.mulighetsrommet.api.domain.arrangor.Betalingsinformasjon
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingAvtale
import no.nav.mulighetsrommet.api.pdfgen.Format
import no.nav.mulighetsrommet.api.pdfgen.PdfDocumentContent
import no.nav.mulighetsrommet.api.pdfgen.PdfDocumentContentBuilder
import no.nav.mulighetsrommet.api.pdfgen.TableBlock
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingType
import no.nav.mulighetsrommet.api.utbetaling.api.toDto
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
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingLinjeStatus
import no.nav.mulighetsrommet.api.utbetaling.service.Gradering
import no.nav.mulighetsrommet.api.utbetaling.service.Personalia
import no.nav.mulighetsrommet.api.utils.DatoUtils.formaterDatoTilEuropeiskDatoformat
import no.nav.mulighetsrommet.api.utils.DatoUtils.tilNorskDato
import no.nav.mulighetsrommet.model.DataElement
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.model.withValuta

object UbetalingToPdfDocumentContentMapper {
    fun toUtbetalingsdetaljerPdfContent(
        utbetaling: Utbetaling,
        linjer: List<ArrangforflateUtbetalingLinje>,
        gjennomforing: GjennomforingAvtale,
    ): PdfDocumentContent = PdfDocumentContent.create(
        title = "Utbetalingsdetaljer",
        subject = "Utbetaling til ${utbetaling.arrangor.navn}",
        description = "Detaljer om utbetaling for gjennomføring av ${utbetaling.tiltakstype.navn}",
        author = "Nav",
    ) {
        mainSection("Detaljer om utbetaling")

        addInnsendingSection(utbetaling, gjennomforing)
        addUtbetalingSection(utbetaling)
        addBetalingsinformasjonSection(utbetaling.betalingsinformasjon)

        if (utbetaling.erFerdigBehandlet()) {
            addUtbetalingsstatusSection(utbetaling.valuta, linjer)
        }
    }

    fun toJournalpostPdfContent(
        utbetaling: Utbetaling,
        personalia: List<Personalia>,
        gjennomforing: GjennomforingAvtale,
    ): PdfDocumentContent = PdfDocumentContent.create(
        title = "Utbetaling",
        subject = "Krav om utbetaling fra ${utbetaling.arrangor.navn}",
        description = "Krav om utbetaling fra ${utbetaling.arrangor.navn}",
        author = "Tiltaksadministrasjon",
    ) {
        mainSection("Innsendt krav om utbetaling")

        addInnsendingSection(utbetaling, gjennomforing)
        addUtbetalingSection(utbetaling)
        addBetalingsinformasjonSection(utbetaling.betalingsinformasjon)
        addStengtHosArrangorSection(utbetaling.beregning)

        when (utbetaling.beregning) {
            is UtbetalingBeregningFri -> Unit

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
            is UtbetalingBeregningFri -> Unit

            is UtbetalingBeregningAvtaltPrisPerTimeOppfolging -> Unit

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

private fun PdfDocumentContentBuilder.addInnsendingSection(
    utbetaling: Utbetaling,
    gjennomforing: GjennomforingAvtale,
) {
    val type = UtbetalingType.from(utbetaling).toDto()
    val utbetalingHeader = type.displayNameLong ?: type.displayName
    section(utbetalingHeader) {
        descriptionList {
            text(
                "Arrangør",
                "${utbetaling.arrangor.navn} (${utbetaling.arrangor.organisasjonsnummer.value})",
            )
            utbetaling.innsending
                ?.let { text("Dato innsendt av arrangør", it.tidspunkt.toLocalDate(), Format.DATE) }
                ?: text("Dato opprettet hos Nav", utbetaling.createdAt.toLocalDate(), Format.DATE)
            text("Tiltakstype", utbetaling.tiltakstype.navn)
            if (utbetaling.arrangorInnsendtAnnenAvtaltPris()) {
                text(
                    "Tiltaksperiode",
                    Periode.formatPeriode(gjennomforing.startDato, gjennomforing.sluttDato),
                )
            }

            text("Løpenummer", gjennomforing.lopenummer.value)
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
            val status = if (linjer.all { it.status == UtbetalingLinjeStatus.UTBETALT }) {
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
