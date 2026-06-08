package no.nav.mulighetsrommet.api.arrangorflate.service

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.arrangor.model.Betalingsinformasjon
import no.nav.mulighetsrommet.api.arrangorflate.dto.ArrangforflateUtbetalingLinje
import no.nav.mulighetsrommet.api.arrangorflate.dto.ArrangorflateArrangorDto
import no.nav.mulighetsrommet.api.arrangorflate.dto.ArrangorflateBeregning
import no.nav.mulighetsrommet.api.arrangorflate.dto.ArrangorflateGjennomforingDto
import no.nav.mulighetsrommet.api.arrangorflate.dto.ArrangorflateTiltakstypeDto
import no.nav.mulighetsrommet.api.arrangorflate.dto.ArrangorflateUtbetalingDto
import no.nav.mulighetsrommet.api.arrangorflate.model.ArrangorflateUtbetalingStatus
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingAvtale
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingTimeline
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingType
import no.nav.mulighetsrommet.api.utbetaling.api.toDto
import no.nav.mulighetsrommet.api.utbetaling.model.DeltakelseDeltakelsesprosentPerioder
import no.nav.mulighetsrommet.api.utbetaling.model.DeltakelsePeriode
import no.nav.mulighetsrommet.api.utbetaling.model.Deltaker
import no.nav.mulighetsrommet.api.utbetaling.model.DeltakerAdvarsel
import no.nav.mulighetsrommet.api.utbetaling.model.DeltakerAdvarselDto
import no.nav.mulighetsrommet.api.utbetaling.model.SatsPeriode
import no.nav.mulighetsrommet.api.utbetaling.model.StengtPeriode
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregning
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningAvtaltPrisPerBenyttetPlassPerHeleUke
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningAvtaltPrisPerBenyttetPlassPerManed
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningAvtaltPrisPerBenyttetPlassPerUke
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningAvtaltPrisPerTimeOppfolging
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFastSatsPerAvtaltTiltaksplassPerManed
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFastSatsPerBenyttetPlassPerManed
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFri
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningHelpers
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningOutputDeltakelse
import no.nav.mulighetsrommet.api.utbetaling.service.Personalia
import no.nav.mulighetsrommet.api.utils.DatoUtils.formaterDatoTilEuropeiskDatoformat
import no.nav.mulighetsrommet.api.utils.DatoUtils.tilNorskDato
import no.nav.mulighetsrommet.model.DataDetails
import no.nav.mulighetsrommet.model.DataDrivenTableDto
import no.nav.mulighetsrommet.model.DataElement
import no.nav.mulighetsrommet.model.LabeledDataElement
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Periode
import java.math.RoundingMode
import java.time.LocalDate
import java.util.UUID

fun mapUtbetalingToArrangorflateUtbetaling(
    utbetaling: Utbetaling,
    gjennomforing: GjennomforingAvtale,
    deltakereById: Map<UUID, Deltaker>,
    personaliaById: Map<UUID, Personalia>,
    advarsler: List<DeltakerAdvarsel>,
    linjer: List<ArrangforflateUtbetalingLinje>,
    kanViseBeregning: Boolean,
    kanAvbrytes: ArrangorAvbrytStatus,
    kanRegenereres: Boolean,
    regenerertId: UUID?,
): ArrangorflateUtbetalingDto {
    val beregning = ArrangorflateBeregning(
        pris = utbetaling.beregning.output.pris,
        deltakelser = beregningDeltakerTable(utbetaling, deltakereById, personaliaById),
        stengt = beregningStengt(utbetaling.beregning),
        displayName = beregningDisplayName(utbetaling.beregning),
        satsDetaljer = beregningSatsDetaljer(utbetaling.beregning),
    )

    val kanViseBeregningMedDeltakelse = beregning.deltakelser?.let { kanViseBeregning } ?: false

    val innsendtAvArrangorDato = utbetaling.innsending?.tidspunkt?.toLocalDate()
    return ArrangorflateUtbetalingDto(
        id = utbetaling.id,
        status = ArrangorflateUtbetalingStatus.fromUtbetaling(utbetaling),
        innsendtAvArrangorDato = innsendtAvArrangorDato,
        utbetalesTidligstDato = utbetaling.utbetalesTidligstTidspunkt?.tilNorskDato(),
        kanViseBeregning = kanViseBeregningMedDeltakelse,
        createdAt = utbetaling.createdAt,
        updatedAt = utbetaling.updatedAt,
        tiltakstype = ArrangorflateTiltakstypeDto(
            navn = utbetaling.tiltakstype.navn,
            tiltakskode = utbetaling.tiltakstype.tiltakskode,
        ),
        gjennomforing = ArrangorflateGjennomforingDto(
            id = gjennomforing.id,
            lopenummer = gjennomforing.lopenummer,
            navn = gjennomforing.navn,
        ),
        arrangor = ArrangorflateArrangorDto(
            id = utbetaling.arrangor.id,
            organisasjonsnummer = utbetaling.arrangor.organisasjonsnummer,
            navn = utbetaling.arrangor.navn,
        ),
        periode = utbetaling.periode,
        valuta = utbetaling.valuta,
        beregning = beregning,
        betalingsinformasjon = when (utbetaling.betalingsinformasjon) {
            is Betalingsinformasjon.BBan -> utbetaling.betalingsinformasjon
            is Betalingsinformasjon.IBan -> throw IllegalStateException("IBan funnet for norsk arrangor med id: ${utbetaling.arrangor.id}")
            null -> null
        },
        type = UtbetalingType.from(utbetaling).toDto(),
        linjer = linjer,
        innsendingsDetaljer = getInnsendingsDetaljer(utbetaling, gjennomforing, innsendtAvArrangorDato),
        advarsler = advarsler.map { advarsel ->
            DeltakerAdvarselDto.from(advarsel, personaliaById[advarsel.deltakerId]?.navn() ?: "-")
        },
        kanAvbrytes = kanAvbrytes,
        avbruttDato = utbetaling.avbruttTidspunkt?.tilNorskDato(),
        kanRegenereres = kanRegenereres,
        regenerertId = regenerertId,
    )
}

private fun getInnsendingsDetaljer(
    utbetaling: Utbetaling,
    gjennomforing: GjennomforingAvtale,
    innsendtAvArrangorDato: LocalDate?,
): List<LabeledDataElement> {
    return listOfNotNull(
        if (innsendtAvArrangorDato != null) {
            LabeledDataElement.date("Dato innsendt", innsendtAvArrangorDato)
        } else {
            LabeledDataElement.date("Dato opprettet hos Nav", utbetaling.createdAt.toLocalDate())
        },
        LabeledDataElement.text(
            "Tiltaksnavn",
            "${gjennomforing.navn} (${gjennomforing.lopenummer})",
        ),
        LabeledDataElement.text("Tiltakstype", utbetaling.tiltakstype.navn),
        if (utbetaling.arrangorInnsendtAnnenAvtaltPris()) {
            LabeledDataElement.text(
                "Tiltaksperiode",
                Periode.formatPeriode(gjennomforing.startDato, gjennomforing.sluttDato),
            )
        } else {
            null
        },
    )
}

@Serializable
data class ArrangorflatePersonalia(
    val norskIdent: NorskIdent?,
    val navn: String?,
)

data class ArrangorflateBeregningDeltakelse(
    val personalia: Personalia?,
    val beregningOutput: UtbetalingBeregningOutputDeltakelse,
    val deltaker: Deltaker?,
)

fun deltakelseCommonColumns() = listOf(
    DataDrivenTableDto.Column("navn", "Navn"),
    DataDrivenTableDto.Column("identitetsnummer", "Fødselsnr."),
    DataDrivenTableDto.Column("tiltakStart", "Startdato i tiltaket"),
    DataDrivenTableDto.Column("periodeStart", "Startdato i perioden"),
    DataDrivenTableDto.Column("periodeSlutt", "Sluttdato i perioden"),
)

private fun deltakelseCommonCells(deltaker: ArrangorflateBeregningDeltakelse) = deltakelseCommonCells(
    deltaker.personalia,
    deltaker.deltaker?.startDato,
    Periode.fromRange(deltaker.beregningOutput.perioder.map { it.periode }),
)

fun deltakelseCommonCells(
    personalia: Personalia?,
    startDato: LocalDate?,
    periode: Periode,
): Map<String, DataElement?> = mapOf(
    "navn" to DataElement.text(personalia?.navn()),
    "identitetsnummer" to DataElement.text(personalia?.norskIdent()?.value),
    "tiltakStart" to DataElement.date(startDato),
    "periodeStart" to DataElement.date(periode.start),
    "periodeSlutt" to DataElement.date(periode.getLastInclusiveDate()),
)

private fun deltakelseFaktorColumns(faktorLabel: String) = listOf(
    DataDrivenTableDto.Column(
        "faktor",
        faktorLabel,
        align = DataDrivenTableDto.Column.Align.RIGHT,
    ),
)

private fun deltakelseFaktorCells(faktor: Double) = mapOf(
    "faktor" to DataElement.number(faktor),
)

private fun deltakelsePrisPerUkesverkTable(
    periode: Periode,
    deltakere: List<ArrangorflateBeregningDeltakelse>,
    stengt: List<StengtPeriode>,
): DataDrivenTableDto {
    return DataDrivenTableDto(
        columns = deltakelseCommonColumns() + deltakelseFaktorColumns("Ukesverk"),
        rows = deltakere.map { deltaker ->
            val faktor = deltaker.beregningOutput.perioder.sumOf { it.faktor }
            DataDrivenTableDto.Row(
                cells = deltakelseCommonCells(deltaker) + deltakelseFaktorCells(faktor),
                content = UtbetalingTimeline.deltakelseTimeline(
                    periode,
                    stengt,
                    UtbetalingTimeline.ukesverkBeregningRow(deltaker.beregningOutput),
                ),
            )
        },
    )
}

private fun deltakelsePrisPerManedsverkTable(
    periode: Periode,
    deltakere: List<ArrangorflateBeregningDeltakelse>,
    stengt: List<StengtPeriode>,
): DataDrivenTableDto {
    return DataDrivenTableDto(
        columns = deltakelseCommonColumns() + deltakelseFaktorColumns("Månedsverk"),
        rows = deltakere.map {
            val faktor = it.beregningOutput.perioder.sumOf { it.faktor }
            DataDrivenTableDto.Row(
                cells = deltakelseCommonCells(it) + deltakelseFaktorCells(faktor),
                content = UtbetalingTimeline.deltakelseTimeline(
                    periode,
                    stengt,
                    UtbetalingTimeline.manedsverkBeregningRow(it.beregningOutput),
                ),
            )
        },
    )
}

private fun deltakelseFastSatsPerTiltaksplassPerManedTable(
    periode: Periode,
    deltakere: List<ArrangorflateBeregningDeltakelse>,
    deltakerInput: Set<DeltakelseDeltakelsesprosentPerioder>,
    stengt: List<StengtPeriode>,
): DataDrivenTableDto {
    return DataDrivenTableDto(
        columns = deltakelseCommonColumns() + deltakelseFaktorColumns("Månedsverk"),
        rows = deltakere.map { deltaker ->
            val input = requireNotNull(deltakerInput.find { it.deltakelseId == deltaker.beregningOutput.deltakelseId })
            val faktor = deltaker.beregningOutput.perioder.sumOf { it.faktor }
            DataDrivenTableDto.Row(
                cells = deltakelseCommonCells(deltaker) + deltakelseFaktorCells(faktor),
                content = UtbetalingTimeline.deltakelseTimeline(
                    periode,
                    stengt,
                    UtbetalingTimeline.fastSatsPerTiltaksplassPerManedRow(
                        deltaker.beregningOutput,
                        input.perioder,
                    ),
                ),
            )
        },
    )
}

private fun deltakelsePrisPerTimeOppfolgingTable(
    deltakelser: Set<DeltakelsePeriode>,
    deltakereById: Map<UUID, Deltaker>,
    personaliaById: Map<UUID, Personalia?>,
) = DataDrivenTableDto(
    columns = deltakelseCommonColumns(),
    rows = deltakelser.map { deltakelse ->
        val deltaker = deltakereById[deltakelse.deltakelseId]
        val personalia = personaliaById[deltakelse.deltakelseId]
        DataDrivenTableDto.Row(
            cells = deltakelseCommonCells(personalia, deltaker?.startDato, deltakelse.periode),
        )
    },
)

fun beregningDisplayName(beregning: UtbetalingBeregning) = when (beregning) {
    is UtbetalingBeregningFastSatsPerBenyttetPlassPerManed ->
        "Fast sats per tiltaksplass per måned"

    is UtbetalingBeregningFastSatsPerAvtaltTiltaksplassPerManed ->
        "Fast sats per avtalt tiltaksplass"

    is UtbetalingBeregningFri ->
        "Annen avtalt pris"

    is UtbetalingBeregningAvtaltPrisPerBenyttetPlassPerManed ->
        "Avtalt månedspris per tiltaksplass"

    is UtbetalingBeregningAvtaltPrisPerTimeOppfolging ->
        "Avtalt pris per time oppfølging per deltaker"

    is UtbetalingBeregningAvtaltPrisPerBenyttetPlassPerUke,
    is UtbetalingBeregningAvtaltPrisPerBenyttetPlassPerHeleUke,
    -> "Avtalt ukespris per tiltaksplass"
}

fun beregningStengt(beregning: UtbetalingBeregning) = when (beregning) {
    is UtbetalingBeregningFastSatsPerBenyttetPlassPerManed ->
        beregning.input.stengt.sortedBy { it.periode.start }

    is UtbetalingBeregningAvtaltPrisPerBenyttetPlassPerManed ->
        beregning.input.stengt.sortedBy { it.periode.start }

    is UtbetalingBeregningAvtaltPrisPerTimeOppfolging ->
        beregning.input.stengt.sortedBy { it.periode.start }

    is UtbetalingBeregningAvtaltPrisPerBenyttetPlassPerUke ->
        beregning.input.stengt.sortedBy { it.periode.start }

    is UtbetalingBeregningAvtaltPrisPerBenyttetPlassPerHeleUke ->
        beregning.input.stengt.sortedBy { it.periode.start }

    is UtbetalingBeregningFastSatsPerAvtaltTiltaksplassPerManed,
    is UtbetalingBeregningFri,
    -> emptyList()
}

fun beregningSatsDetaljer(beregning: UtbetalingBeregning): List<DataDetails> {
    return when (beregning) {
        is UtbetalingBeregningFastSatsPerBenyttetPlassPerManed,
        -> {
            val satser = beregning.input.satser.sortedBy { it.periode.start }
            beregningSatsPeriodeDetaljerMedFaktor(
                satser,
                satsLabel = "Sats",
                deltakelser = beregning.output.deltakelser(),
                faktorLabel = "Antall månedsverk",
            )
        }

        is UtbetalingBeregningAvtaltPrisPerBenyttetPlassPerManed -> {
            val satser = beregning.input.satser.sortedBy { it.periode.start }
            beregningSatsPeriodeDetaljerMedFaktor(
                satser,
                satsLabel = "Avtalt månedspris per tiltaksplass",
                deltakelser = beregning.output.deltakelser(),
                faktorLabel = "Antall månedsverk",
            )
        }

        is UtbetalingBeregningAvtaltPrisPerBenyttetPlassPerUke -> {
            val satser = beregning.input.satser.sortedBy { it.periode.start }
            beregningSatsPeriodeDetaljerMedFaktor(
                satser,
                satsLabel = "Avtalt ukespris per tiltaksplass",
                deltakelser = beregning.output.deltakelser(),
                faktorLabel = "Antall ukesverk",
            )
        }

        is UtbetalingBeregningAvtaltPrisPerBenyttetPlassPerHeleUke -> {
            val satser = beregning.input.satser.sortedBy { it.periode.start }
            beregningSatsPeriodeDetaljerMedFaktor(
                satser,
                satsLabel = "Avtalt ukespris per tiltaksplass",
                deltakelser = beregning.output.deltakelser(),
                faktorLabel = "Antall ukesverk",
            )
        }

        is UtbetalingBeregningAvtaltPrisPerTimeOppfolging -> {
            val satser = beregning.input.satser.sortedBy { it.periode.start }
            beregningSatsPeriodeDetaljerUtenFaktor(satser, "Avtalt pris per time oppfølging")
        }

        is UtbetalingBeregningFastSatsPerAvtaltTiltaksplassPerManed,
        is UtbetalingBeregningFri,
        -> emptyList()
    }
}

fun beregningSatsPeriodeDetaljerMedFaktor(
    satser: List<SatsPeriode>,
    deltakelser: Set<UtbetalingBeregningOutputDeltakelse>,
    satsLabel: String,
    faktorLabel: String,
): List<DataDetails> {
    return satser.mapNotNull { satsPeriode ->
        deltakelser
            .flatMap { it.perioder }
            .filter { it.sats == satsPeriode.sats }
            .map { it.faktor.toBigDecimal() }
            .sumOf { it }
            .setScale(UtbetalingBeregningHelpers.OUTPUT_PRECISION, RoundingMode.HALF_UP)
            .toDouble()
            .takeIf { it > 0.0 }
            ?.let { faktor ->
                val start = satsPeriode.periode.start.formaterDatoTilEuropeiskDatoformat()
                val slutt = satsPeriode.periode.getLastInclusiveDate().formaterDatoTilEuropeiskDatoformat()
                DataDetails(
                    header = "Periode $start - $slutt",
                    entries = listOf(
                        LabeledDataElement.money(satsLabel, satsPeriode.sats),
                        LabeledDataElement.number(faktorLabel, faktor),
                    ),
                )
            }
    }
}

fun beregningSatsPeriodeDetaljerUtenFaktor(
    satser: List<SatsPeriode>,
    satsLabel: String,
): List<DataDetails> {
    return satser.map { satsPeriode ->
        DataDetails(
            header = "Periode ${satsPeriode.periode.start.formaterDatoTilEuropeiskDatoformat()} - ${
                satsPeriode.periode.getLastInclusiveDate().formaterDatoTilEuropeiskDatoformat()
            }",
            entries = listOf(
                LabeledDataElement.money(satsLabel, satsPeriode.sats),
            ),
        )
    }
}

private fun beregningDeltakerTable(
    utbetaling: Utbetaling,
    deltakereById: Map<UUID, Deltaker>,
    personaliaById: Map<UUID, Personalia?>,
): DataDrivenTableDto? {
    return when (val beregning = utbetaling.beregning) {
        is UtbetalingBeregningFastSatsPerBenyttetPlassPerManed -> {
            val deltakelser = getArrangorflateBeregningDeltakelse(
                utbetaling.beregning.output.deltakelser(),
                deltakereById,
                personaliaById,
            )
            val stengt = beregning.input.stengt.sortedBy { it.periode.start }
            deltakelseFastSatsPerTiltaksplassPerManedTable(
                utbetaling.periode,
                deltakelser,
                deltakerInput = beregning.input.deltakelser,
                stengt,
            )
        }

        is UtbetalingBeregningAvtaltPrisPerBenyttetPlassPerManed -> {
            val deltakelser = getArrangorflateBeregningDeltakelse(
                utbetaling.beregning.output.deltakelser(),
                deltakereById,
                personaliaById,
            )
            val stengt = beregning.input.stengt.sortedBy { it.periode.start }
            deltakelsePrisPerManedsverkTable(utbetaling.periode, deltakelser, stengt)
        }

        is UtbetalingBeregningAvtaltPrisPerBenyttetPlassPerUke -> {
            val deltakelser = getArrangorflateBeregningDeltakelse(
                utbetaling.beregning.output.deltakelser(),
                deltakereById,
                personaliaById,
            )
            val stengt = beregning.input.stengt.sortedBy { it.periode.start }
            deltakelsePrisPerUkesverkTable(utbetaling.periode, deltakelser, stengt)
        }

        is UtbetalingBeregningAvtaltPrisPerBenyttetPlassPerHeleUke -> {
            val deltakelser = getArrangorflateBeregningDeltakelse(
                utbetaling.beregning.output.deltakelser(),
                deltakereById,
                personaliaById,
            )
            val stengt = beregning.input.stengt.sortedBy { it.periode.start }
            deltakelsePrisPerUkesverkTable(utbetaling.periode, deltakelser, stengt)
        }

        is UtbetalingBeregningAvtaltPrisPerTimeOppfolging -> {
            val deltakelsePerioder = utbetaling.beregning.deltakelsePerioder()
            deltakelsePrisPerTimeOppfolgingTable(deltakelsePerioder, deltakereById, personaliaById)
        }

        is UtbetalingBeregningFastSatsPerAvtaltTiltaksplassPerManed,
        is UtbetalingBeregningFri,
        -> null
    }
}

private fun getArrangorflateBeregningDeltakelse(
    deltakelser: Set<UtbetalingBeregningOutputDeltakelse>,
    deltakereById: Map<UUID, Deltaker>,
    personaliaById: Map<UUID, Personalia?>,
): List<ArrangorflateBeregningDeltakelse> = deltakelser.associateBy { it.deltakelseId }
    .map { (id, beregningOutput) ->
        ArrangorflateBeregningDeltakelse(
            deltaker = deltakereById[id],
            personalia = personaliaById[id],
            beregningOutput = beregningOutput,
        )
    }
    .sortedWith(compareBy(nullsLast()) { it.personalia?.navn() })
