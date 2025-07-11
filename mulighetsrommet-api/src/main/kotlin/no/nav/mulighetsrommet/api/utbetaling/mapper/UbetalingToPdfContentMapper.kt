package no.nav.mulighetsrommet.api.utbetaling.mapper

import no.nav.mulighetsrommet.api.arrangorflate.api.ArrFlateBeregning
import no.nav.mulighetsrommet.api.arrangorflate.api.ArrFlateUtbetaling
import no.nav.mulighetsrommet.api.arrangorflate.api.ArrFlateUtbetalingStatus
import no.nav.mulighetsrommet.api.pdfgen.PdfDocumentContent
import no.nav.mulighetsrommet.api.pdfgen.Section
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingType
import no.nav.mulighetsrommet.api.utbetaling.api.toReadableName
import no.nav.mulighetsrommet.api.utils.DatoUtils.formaterDatoTilEuropeiskDatoformat

object UbetalingToPdfContentMapper {
    fun toInnsendtFraArrangorPdfContent(utbetaling: ArrFlateUtbetaling): PdfDocumentContent {
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
                        entries = listOfNotNull(
                            Section.Entry(
                                "Arrangør",
                                "${utbetaling.arrangor.navn} (${utbetaling.arrangor.organisasjonsnummer.value})",
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
                        entries = listOfNotNull(
                            run {
                                val start = utbetaling.periode.start.formaterDatoTilEuropeiskDatoformat()
                                val slutt =
                                    utbetaling.periode.getLastInclusiveDate().formaterDatoTilEuropeiskDatoformat()
                                Section.Entry("Utbetalingsperiode", "$start - $slutt")
                            },

                            when (utbetaling.beregning) {
                                is ArrFlateBeregning.Fri -> null

                                is ArrFlateBeregning.PrisPerManedsverkMedDeltakelsesmengder ->
                                    Section.Entry("Antall månedsverk", utbetaling.beregning.antallManedsverk.toString())

                                is ArrFlateBeregning.PrisPerManedsverk ->
                                    Section.Entry("Antall månedsverk", utbetaling.beregning.antallManedsverk.toString())

                                is ArrFlateBeregning.PrisPerUkesverk ->
                                    Section.Entry("Antall ukesverk", utbetaling.beregning.antallUkesverk.toString())
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

        sections.add(
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

        if (utbetaling.status == ArrFlateUtbetalingStatus.OVERFORT_TIL_UTBETALING) {
            val status = ArrFlateUtbetalingStatus.toReadableName(utbetaling.status)
            val totaltUtbetalt = utbetaling.linjer.fold(0) { acc, linje -> acc + linje.belop }
            sections.add(
                Section(
                    title = Section.Header("Utbetalingsstatus", level = 4),
                    blocks = listOf(
                        Section.Block(
                            entries = listOfNotNull(
                                Section.Entry("Status", status, Section.Format.STATUS_SUCCESS),
                                Section.Entry(
                                    "Godkjent beløp til utbetaling",
                                    totaltUtbetalt.toString(),
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
                    blocks = utbetaling.linjer.map {
                        Section.Block(
                            entries = listOfNotNull(
                                Section.Entry("Tilsagn", it.tilsagn.bestillingsnummer),
                                Section.Entry("Beløp til utbetaling", it.belop.toString(), Section.Format.NOK),
                                Section.Entry("Status", toReadableName(it.status), Section.Format.STATUS_SUCCESS),
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

        return PdfDocumentContent(
            title = "Utbetalingsdetaljer",
            subject = "Utbetaling til ${utbetaling.arrangor.navn}",
            description = "Detaljer om utbetaling for gjennomføring av ${utbetaling.tiltakstype.navn}",
            author = "Nav",
            sections = sections,
        )
    }

    fun toJournalpostPdfContent(utbetaling: ArrFlateUtbetaling): PdfDocumentContent {
        val sections = mutableListOf<Section>()

        sections.add(
            Section(
                title = Section.Header("Innsendt krav om utbetaling", level = 1),
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
                        entries = listOfNotNull(
                            Section.Entry(
                                "Arrangør",
                                "${utbetaling.arrangor.navn} (${utbetaling.arrangor.organisasjonsnummer.value})",
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
                        entries = listOfNotNull(
                            run {
                                val start = utbetaling.periode.start.formaterDatoTilEuropeiskDatoformat()
                                val slutt =
                                    utbetaling.periode.getLastInclusiveDate().formaterDatoTilEuropeiskDatoformat()
                                Section.Entry("Utbetalingsperiode", "$start - $slutt")
                            },

                            when (utbetaling.beregning) {
                                is ArrFlateBeregning.Fri -> null

                                is ArrFlateBeregning.PrisPerManedsverkMedDeltakelsesmengder ->
                                    Section.Entry("Antall månedsverk", utbetaling.beregning.antallManedsverk.toString())

                                is ArrFlateBeregning.PrisPerManedsverk ->
                                    Section.Entry("Antall månedsverk", utbetaling.beregning.antallManedsverk.toString())

                                is ArrFlateBeregning.PrisPerUkesverk ->
                                    Section.Entry("Antall ukesverk", utbetaling.beregning.antallUkesverk.toString())
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
        sections.add(
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

        val stengt = when (utbetaling.beregning) {
            is ArrFlateBeregning.Fri -> listOf()
            is ArrFlateBeregning.PrisPerManedsverkMedDeltakelsesmengder -> utbetaling.beregning.stengt
            is ArrFlateBeregning.PrisPerManedsverk -> utbetaling.beregning.stengt
            is ArrFlateBeregning.PrisPerUkesverk -> utbetaling.beregning.stengt
        }
        if (stengt.isNotEmpty()) {
            sections.add(
                Section(
                    title = Section.Header("Stengt hos arrangør", level = 4),
                    blocks = listOf(
                        Section.Block(
                            description = "Det er registrert stengt hos arrangør i følgende perioder:",
                            values = stengt.map {
                                val start = it.periode.start.formaterDatoTilEuropeiskDatoformat()
                                val slutt = it.periode.getLastInclusiveDate().formaterDatoTilEuropeiskDatoformat()
                                "$start - $slutt: ${it.beskrivelse}"
                            },
                        ),
                    ),
                ),
            )
        }

        when (utbetaling.beregning) {
            is ArrFlateBeregning.Fri -> Unit
            is ArrFlateBeregning.PrisPerManedsverkMedDeltakelsesmengder -> {
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
                                        deltakelse.perioderMedDeltakelsesmengde.map { (periode, prosent) ->
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
                                                        periode.getLastInclusiveDate()
                                                            .formaterDatoTilEuropeiskDatoformat(),
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
            }

            is ArrFlateBeregning.PrisPerManedsverk -> {
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
                                                    deltakelse.periodeStartDato.formaterDatoTilEuropeiskDatoformat(),
                                                ),
                                                Section.Table.Cell(
                                                    deltakelse.periodeSluttDato.formaterDatoTilEuropeiskDatoformat(),
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

            is ArrFlateBeregning.PrisPerUkesverk -> {
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
                                                    deltakelse.periodeStartDato.formaterDatoTilEuropeiskDatoformat(),
                                                ),
                                                Section.Table.Cell(
                                                    deltakelse.periodeSluttDato.formaterDatoTilEuropeiskDatoformat(),
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
        }

        when (utbetaling.beregning) {
            is ArrFlateBeregning.Fri -> Unit

            is ArrFlateBeregning.PrisPerManedsverkMedDeltakelsesmengder -> {
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
                                                    deltakelse.faktor.toString(),
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

            is ArrFlateBeregning.PrisPerManedsverk -> {
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
                                                    deltakelse.faktor.toString(),
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

            is ArrFlateBeregning.PrisPerUkesverk -> {
                sections.add(
                    Section(
                        title = Section.Header("Beregnet ukesverk", level = 4),
                        blocks = listOf(
                            Section.Block(
                                table = Section.Table(
                                    columns = listOf(
                                        Section.Table.Column("Navn"),
                                        Section.Table.Column("Fødselsdato", Section.Table.Column.Align.RIGHT),
                                        Section.Table.Column("Ukesverk", Section.Table.Column.Align.RIGHT),
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
                                                    deltakelse.faktor.toString(),
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
        }

        return PdfDocumentContent(
            title = "Utbetaling",
            subject = "Krav om utbetaling fra ${utbetaling.arrangor.navn}",
            description = "Krav om utbetaling fra ${utbetaling.arrangor.navn}",
            author = "Tiltaksadministrasjon",
            sections = sections,
        )
    }
}
