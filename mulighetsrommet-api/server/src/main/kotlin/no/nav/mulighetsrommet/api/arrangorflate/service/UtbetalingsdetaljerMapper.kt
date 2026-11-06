package no.nav.mulighetsrommet.api.arrangorflate.service

import no.nav.mulighetsrommet.api.arrangorflate.dto.ArrangforflateUtbetalingLinje
import no.nav.mulighetsrommet.api.arrangorflate.dto.ArrangorflatePris
import no.nav.mulighetsrommet.api.arrangorflate.dto.Avbrytelse
import no.nav.mulighetsrommet.api.arrangorflate.dto.UtbetalingsdetaljerDto
import no.nav.mulighetsrommet.api.arrangorflate.model.ArrangorflateUtbetaling
import no.nav.mulighetsrommet.api.arrangorflate.model.ArrangorflateUtbetalingStatus
import no.nav.mulighetsrommet.api.domain.arrangor.Betalingsreferanse
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingType
import no.nav.mulighetsrommet.api.utbetaling.api.toDto
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningType
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingLinjeStatus
import no.nav.mulighetsrommet.api.utils.DatoUtils.tilNorskDato
import no.nav.mulighetsrommet.model.DataDetails
import no.nav.mulighetsrommet.model.DataElement
import no.nav.mulighetsrommet.model.LabeledDataElement
import no.nav.mulighetsrommet.model.LabeledDataElementType
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.withValuta

fun mapUtbetalingsdetaljer(
    utbetaling: ArrangorflateUtbetaling,
    linjer: List<ArrangforflateUtbetalingLinje>,
): UtbetalingsdetaljerDto {
    val status = ArrangorflateUtbetalingStatus.fromUtbetaling(utbetaling.status, utbetaling.blokkeringer, utbetaling.avbrytelse)
    val type = UtbetalingType.from(utbetaling.korreksjon?.gjelderUtbetalingId, utbetaling.tilskuddstype).toDto()
    val pris = ArrangorflatePris.from(UtbetalingBeregningType.from(utbetaling.beregning), status, utbetaling.beregning.output.pris)
    val avbrytelse = Avbrytelse.fromStatus(status, utbetaling.arrangorAvbrutt, utbetaling.avbrytelse)
    val visTilsagn = status in setOf(ArrangorflateUtbetalingStatus.OVERFORT_TIL_UTBETALING, ArrangorflateUtbetalingStatus.UTBETALT) &&
        linjer.isNotEmpty()
    val satsDetaljer = beregningSatsDetaljer(utbetaling.beregning)
    val referanse =
        when (utbetaling.betalingsinformasjon?.referanse) {
            is Betalingsreferanse.KidNummer -> LabeledDataElement.text("KID-nummer", (utbetaling.betalingsinformasjon.referanse as Betalingsreferanse.KidNummer).kid.value)
            is Betalingsreferanse.MeldingTilMottaker -> LabeledDataElement.text("Referanse", (utbetaling.betalingsinformasjon.referanse as Betalingsreferanse.MeldingTilMottaker).melding)
            null -> LabeledDataElement.text("KID-nummer", "-")
        }

    return UtbetalingsdetaljerDto(
        arrangorNavn = utbetaling.arrangor.navn,
        tiltakstypeNavn = utbetaling.tiltakstype.navn,
        innsending = DataDetails(
            header = type.displayNameLong ?: type.displayName,
            entries = listOfNotNull(
                LabeledDataElement.text("Arrangør", "${utbetaling.arrangor.navn} (${utbetaling.arrangor.organisasjonsnummer.value})"),
                utbetaling.innsending?.let { LabeledDataElement.date("Dato innsendt", it.tidspunkt.toLocalDate()) }
                    ?: LabeledDataElement.date("Dato opprettet hos Nav", utbetaling.createdAt.toLocalDate()),
                LabeledDataElement.text("Tiltaksnavn", "${utbetaling.gjennomforing.navn} (${utbetaling.gjennomforing.lopenummer})"),
                LabeledDataElement.text("Tiltakstype", utbetaling.tiltakstype.navn),
                if (utbetaling.arrangorInnsendtAnnenAvtaltPris()) {
                    LabeledDataElement.text("Tiltaksperiode", Periode.formatPeriode(utbetaling.gjennomforing.startDato, utbetaling.gjennomforing.sluttDato))
                } else {
                    null
                },
                utbetaling.utbetalesTidligstTidspunkt?.let { LabeledDataElement.date("Utbetales tidligst", it.tilNorskDato()) },
            ),
        ),
        utbetaling = DataDetails(
            header = "Utbetaling",
            entries = listOf(
                LabeledDataElement.text("Utbetalingsperiode", utbetaling.periode.formatPeriode()),
                when (pris) {
                    is ArrangorflatePris.Beregnet -> LabeledDataElement.money("Beløp", pris.pris)
                    ArrangorflatePris.KreverRegistrering -> LabeledDataElement.text("Beløp", "Ikke registrert")
                },
            ),
        ),
        satsDetaljer = satsDetaljer.map { it.copy(header = it.header.takeIf { satsDetaljer.size > 1 }) },
        betalingsinformasjon = DataDetails(
            header = "Betalingsinformasjon",
            entries = listOfNotNull(
                LabeledDataElement.text(
                    "Kontonummer",
                    utbetaling.betalingsinformasjon?.kontonummer?.value?.let { "${it.take(4)} ${it.substring(4, 6)} ${it.takeLast(5)}" },
                ),
                referanse,
            ),
        ),
        status = DataDetails(
            header = "Utbetalingsstatus",
            entries = listOfNotNull(
                status.toDataElement().label("Status"),
                when (avbrytelse) {
                    is Avbrytelse.Arrangor -> LabeledDataElement.date("Status endret", avbrytelse.avbruttDato)
                    is Avbrytelse.Nav -> LabeledDataElement.date("Status endret", avbrytelse.avbruttDato)
                    null -> null
                },
                if (visTilsagn) {
                    LabeledDataElement.money("Godkjent beløp til utbetaling", linjer.fold(0.withValuta(utbetaling.valuta)) { sum, linje -> sum + linje.pris })
                } else {
                    null
                },
            ),
        ),
        avbrytelse = avbrytelse?.let {
            DataDetails(
                header = "Begrunnelse for avbrytelse",
                entries = when (it) {
                    is Avbrytelse.Arrangor -> listOf(LabeledDataElement.text("Begrunnelse", it.begrunnelse, LabeledDataElementType.MULTILINE))

                    is Avbrytelse.Nav -> listOf(
                        LabeledDataElement.text(
                            "Årsaker",
                            it.aarsaker.joinToString("\n") { aarsak ->
                                when (aarsak) {
                                    "TILSAGN_GJORT_OPP" -> "Tilsagnet er gjort opp"
                                    "ANNET" -> "Annet"
                                    else -> aarsak
                                }
                            },
                            LabeledDataElementType.MULTILINE,
                        ),
                        LabeledDataElement.text("Begrunnelse", it.begrunnelse, LabeledDataElementType.MULTILINE),
                    )
                },
            )
        },
        tilsagnHeader = "Tilsagn som er brukt til utbetaling",
        tilsagn = linjer.filter { visTilsagn }.sortedBy { it.tilsagn.bestillingsnummer }.map { linje ->
            DataDetails(
                header = "Tilsagn ${linje.tilsagn.bestillingsnummer}",
                entries = listOf(
                    DataElement.Link(
                        text = linje.tilsagn.bestillingsnummer,
                        href = "/${utbetaling.arrangor.organisasjonsnummer.value}/tilsagn/${linje.tilsagn.id}",
                    ).label("Tilsagn"),
                    LabeledDataElement.money("Beløp til utbetaling", linje.pris),
                    linje.status.toArrangorDataElement().label("Status"),
                    LabeledDataElement.date("Status endret", linje.statusSistOppdatert?.toLocalDate()),
                ),
            )
        },
    )
}

private fun ArrangorflateUtbetalingStatus.toDataElement(): DataElement.Status = when (this) {
    ArrangorflateUtbetalingStatus.KLAR_FOR_GODKJENNING -> DataElement.Status("Klar for innsending", DataElement.Status.Variant.ALT_1)
    ArrangorflateUtbetalingStatus.BEHANDLES_AV_NAV -> DataElement.Status("Behandles av Nav", DataElement.Status.Variant.WARNING)
    ArrangorflateUtbetalingStatus.UTBETALT -> DataElement.Status("Utbetalt", DataElement.Status.Variant.SUCCESS)
    ArrangorflateUtbetalingStatus.BLOKKERT_FOR_INNSENDING -> DataElement.Status("Blokkert for innsending", DataElement.Status.Variant.WARNING)
    ArrangorflateUtbetalingStatus.OVERFORT_TIL_UTBETALING -> DataElement.Status("Overført til utbetaling", DataElement.Status.Variant.SUCCESS)
    ArrangorflateUtbetalingStatus.DELVIS_UTBETALT -> DataElement.Status("Delvis utbetalt", DataElement.Status.Variant.SUCCESS)
    ArrangorflateUtbetalingStatus.AVBRUTT_AV_NAV -> DataElement.Status("Avbrutt av Nav", DataElement.Status.Variant.NEUTRAL)
    ArrangorflateUtbetalingStatus.AVBRUTT_AV_ARRANGOR -> DataElement.Status("Avbrutt av arrangør", DataElement.Status.Variant.NEUTRAL)
}

private fun UtbetalingLinjeStatus.toArrangorDataElement(): DataElement.Status = when (this) {
    UtbetalingLinjeStatus.OVERFORT_TIL_UTBETALING -> DataElement.Status("Overført til utbetaling", DataElement.Status.Variant.SUCCESS)

    UtbetalingLinjeStatus.UTBETALT -> DataElement.Status("Utbetalt", DataElement.Status.Variant.SUCCESS)

    UtbetalingLinjeStatus.TIL_GODKJENNING,
    UtbetalingLinjeStatus.GODKJENT,
    UtbetalingLinjeStatus.RETURNERT,
    -> DataElement.Status("Behandles av Nav", DataElement.Status.Variant.INFO)

    UtbetalingLinjeStatus.AVBRUTT -> DataElement.Status("Avbrutt", DataElement.Status.Variant.ERROR)
}
