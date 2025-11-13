package no.nav.mulighetsrommet.api.arrangorflate.api

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.clients.amtDeltaker.DeltakerPersonalia
import no.nav.mulighetsrommet.api.clients.pdl.PdlGradering
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingType
import no.nav.mulighetsrommet.api.utbetaling.api.toDto
import no.nav.mulighetsrommet.api.utbetaling.model.*
import no.nav.mulighetsrommet.model.DataDrivenTableDto
import no.nav.mulighetsrommet.model.DataElement
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.TimelineDto
import no.nav.mulighetsrommet.model.TimelineDto.Row.Period
import no.nav.mulighetsrommet.model.TimelineDto.Row.Period.Variant
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*
import kotlin.String
import kotlin.collections.List

fun mapUtbetalingToArrangorflateUtbetaling(
    utbetaling: Utbetaling,
    status: ArrangorflateUtbetalingStatus,
    deltakereById: Map<UUID, Deltaker>,
    personaliaById: Map<UUID, ArrangorflatePersonalia?>,
    advarsler: List<DeltakerAdvarsel>,
    linjer: List<ArrangforflateUtbetalingLinje>,
    kanViseBeregning: Boolean,
): ArrangorflateUtbetalingDto {
    val deltakelseById = utbetaling.beregning.output.deltakelser().associateBy { it.deltakelseId }
    val deltakelser = deltakelseById
        .map { (id, beregningOutput) ->
            ArrangorflateBeregningDeltakelse(
                deltaker = deltakereById[id],
                personalia = personaliaById[id],
                beregningOutput = beregningOutput,
            )
        }
        .sortedWith(compareBy(nullsLast()) { it.personalia?.navn })

    val beregning = ArrangorflateBeregning(
        belop = utbetaling.beregning.output.belop,
        digest = utbetaling.beregning.getDigest(),
        deltakelser = beregningDeltakerTable(utbetaling, deltakelser),
        stengt = beregningStengt(utbetaling.beregning),
        displayName = beregningDisplayName(utbetaling.beregning),
        detaljer = beregningDetails(utbetaling.beregning),
    )

    return ArrangorflateUtbetalingDto(
        id = utbetaling.id,
        status = status,
        godkjentAvArrangorTidspunkt = utbetaling.godkjentAvArrangorTidspunkt,
        kanViseBeregning = kanViseBeregning,
        createdAt = utbetaling.createdAt,
        tiltakstype = ArrangorflateTiltakstype(
            navn = utbetaling.tiltakstype.navn,
            tiltakskode = utbetaling.tiltakstype.tiltakskode,
        ),
        gjennomforing = ArrangorflateGjennomforingInfo(
            id = utbetaling.gjennomforing.id,
            navn = utbetaling.gjennomforing.navn,
        ),
        arrangor = ArrangorflateArrangor(
            id = utbetaling.arrangor.id,
            organisasjonsnummer = utbetaling.arrangor.organisasjonsnummer,
            navn = utbetaling.arrangor.navn,
        ),
        periode = utbetaling.periode,
        beregning = beregning,
        betalingsinformasjon = ArrangorflateBetalingsinformasjon(
            kontonummer = utbetaling.betalingsinformasjon.kontonummer,
            kid = utbetaling.betalingsinformasjon.kid,
        ),
        type = UtbetalingType.from(utbetaling).toDto(),
        linjer = linjer,
        advarsler = advarsler,
    )
}

@Serializable
data class ArrangorflatePersonalia(
    val navn: String,
    val norskIdent: NorskIdent?,
    val erSkjermet: Boolean,
) {
    companion object {
        fun fromPersonalia(personalia: DeltakerPersonalia) = when (personalia.adressebeskyttelse) {
            PdlGradering.UGRADERT -> {
                ArrangorflatePersonalia(
                    navn = personalia.navn,
                    norskIdent = personalia.norskIdent,
                    erSkjermet = personalia.erSkjermet,
                )
            }
            else -> ArrangorflatePersonalia(
                navn = "Adressebeskyttet",
                norskIdent = null,
                erSkjermet = personalia.erSkjermet,
            )
        }
    }
}

data class ArrangorflateBeregningDeltakelse(
    val personalia: ArrangorflatePersonalia?,
    val beregningOutput: UtbetalingBeregningOutputDeltakelse,
    val deltaker: Deltaker?,
)

private fun getSatserDetails(label: String, satser: List<SatsPeriode>): List<DetailsEntry> {
    return satser.singleOrNull()?.let {
        listOf(DetailsEntry.nok(label, it.sats))
    } ?: satser.map {
        DetailsEntry.nok("$label (${it.periode.formatPeriode()})", it.sats)
    }
}

private fun getBelopDetails(belop: Int): List<DetailsEntry> = listOf(DetailsEntry.nok("Beløp", belop))

fun deltakelseCommonColumns() = listOf(
    DataDrivenTableDto.Column("navn", "Navn", sortable = false),
    DataDrivenTableDto.Column("identitetsnummer", "Fødselsnr.", sortable = false),
    DataDrivenTableDto.Column("tiltakStart", "Startdato i tiltaket", sortable = false),
    DataDrivenTableDto.Column("periodeStart", "Startdato i perioden"),
    DataDrivenTableDto.Column("periodeSlutt", "Sluttdato i perioden"),
)

private fun deltakelseCommonCells(deltaker: ArrangorflateBeregningDeltakelse) = deltakelseCommonCells(
    deltaker.personalia,
    deltaker.deltaker,
    Periode.fromRange(deltaker.beregningOutput.perioder.map { it.periode }),
)

fun deltakelseCommonCells(
    personalia: ArrangorflatePersonalia?,
    deltaker: Deltaker?,
    periode: Periode,
): Map<String, DataElement?> = mapOf(
    "navn" to DataElement.text(personalia?.navn),
    "identitetsnummer" to DataElement.text(personalia?.norskIdent?.value),
    "tiltakStart" to DataElement.date(deltaker?.startDato),
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

private fun stengtTimelineRow(stengt: List<StengtPeriode>): TimelineDto.Row? {
    if (stengt.isEmpty()) return null
    return TimelineDto.Row(
        periods = stengt.mapIndexed { index, it ->
            Period(
                start = it.periode.start,
                key = index.toString(),
                end = it.periode.getLastInclusiveDate(),
                status = Variant.WARNING,
                content = "",
            )
        },
        label = "Stengt hos arrangør",
    )
}

private fun deltakelseTimeline(
    periode: Periode,
    stengt: List<StengtPeriode>,
    beregningRow: TimelineDto.Row,
): TimelineDto {
    return TimelineDto(
        startDate = periode.start,
        endDate = periode.getLastInclusiveDate(),
        rows = listOfNotNull(
            stengtTimelineRow(stengt),
            beregningRow,
        ),
    )
}

fun deltakelsePrisPerUkesverkTable(
    periode: Periode,
    deltakere: List<ArrangorflateBeregningDeltakelse>,
    stengt: List<StengtPeriode>,
): DataDrivenTableDto {
    return DataDrivenTableDto(
        columns = deltakelseCommonColumns() + deltakelseFaktorColumns("Ukesverk"),
        rows = deltakere.map {
            val faktor = it.beregningOutput.perioder.sumOf { it.faktor }
            DataDrivenTableDto.Row(
                cells = deltakelseCommonCells(it) + deltakelseFaktorCells(faktor),
                content = deltakelseTimeline(
                    periode,
                    stengt,
                    TimelineDto.Row(
                        label = "Beregning",
                        periods = it.beregningOutput.perioder.mapIndexed { index, it ->
                            Period(
                                start = it.periode.start,
                                key = index.toString(),
                                end = it.periode.getLastInclusiveDate(),
                                status = Variant.INFO,
                                content = "Pris per uke: ${it.sats}, Ukesverk: ${it.faktor}",
                            )
                        },
                    ),
                ),
            )
        },
    )
}

fun deltakelsePrisPerManedsverkTable(
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
                content = deltakelseTimeline(
                    periode,
                    stengt,
                    TimelineDto.Row(
                        label = "Beregning",
                        periods = it.beregningOutput.perioder.mapIndexed { index, it ->
                            Period(
                                start = it.periode.start,
                                key = index.toString(),
                                end = it.periode.getLastInclusiveDate(),
                                status = Variant.INFO,
                                content = "Pris per måned: ${it.sats}, Månedsverk: ${it.faktor}",
                            )
                        },
                    ),
                ),
            )
        },
    )
}

fun deltakelseFastSatsPerTiltaksplassPerManedTable(
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
                content = deltakelseTimeline(
                    periode,
                    stengt,
                    TimelineDto.Row(
                        label = "Beregning",
                        periods = deltaker.beregningOutput.perioder.mapIndexed { index, beregnetPeriode ->
                            Period(
                                start = beregnetPeriode.periode.start,
                                key = index.toString(),
                                end = beregnetPeriode.periode.getLastInclusiveDate(),
                                status = Variant.INFO,
                                content = "Deltakesesprosent: ${input.perioder[index].deltakelsesprosent}, Månedsverk: ${beregnetPeriode.faktor}",
                            )
                        },
                    ),
                ),
            )
        },
    )
}

fun deltakelsePrisPerTimeOppfolgingTable(deltakere: List<ArrangorflateBeregningDeltakelse>) = DataDrivenTableDto(
    columns = deltakelseCommonColumns(),
    rows = deltakere.map {
        DataDrivenTableDto.Row(
            cells = deltakelseCommonCells(it),
        )
    },
)

fun beregningDisplayName(beregning: UtbetalingBeregning) = when (beregning) {
    is UtbetalingBeregningFastSatsPerTiltaksplassPerManed ->
        "Fast sats per tiltaksplass per måned"
    is UtbetalingBeregningFri ->
        "Annen avtalt pris"
    is UtbetalingBeregningPrisPerManedsverk ->
        "Avtalt månedspris per tiltaksplass"
    is UtbetalingBeregningPrisPerTimeOppfolging ->
        "Avtalt pris per time oppfølging per deltaker"
    is UtbetalingBeregningPrisPerUkesverk,
    is UtbetalingBeregningPrisPerHeleUkesverk,
    ->
        "Avtalt ukespris per tiltaksplass"
}

fun beregningStengt(beregning: UtbetalingBeregning) = when (beregning) {
    is UtbetalingBeregningFastSatsPerTiltaksplassPerManed ->
        beregning.input.stengt.sortedBy { it.periode.start }
    is UtbetalingBeregningPrisPerManedsverk ->
        beregning.input.stengt.sortedBy { it.periode.start }
    is UtbetalingBeregningPrisPerTimeOppfolging ->
        beregning.input.stengt.sortedBy { it.periode.start }
    is UtbetalingBeregningPrisPerUkesverk ->
        beregning.input.stengt.sortedBy { it.periode.start }
    is UtbetalingBeregningPrisPerHeleUkesverk ->
        beregning.input.stengt.sortedBy { it.periode.start }
    is UtbetalingBeregningFri ->
        emptyList()
}

fun beregningDetails(beregning: UtbetalingBeregning): Details {
    val totalFaktor = beregning.output.deltakelser()
        .flatMap { deltakelse -> deltakelse.perioder.map { it.faktor } }
        .map { BigDecimal(it) }
        .sumOf { it }
        .setScale(UtbetalingBeregningHelpers.OUTPUT_PRECISION, RoundingMode.HALF_UP)
        .toDouble()

    return when (beregning) {
        is UtbetalingBeregningFri ->
            Details(entries = getBelopDetails(beregning.output.belop))
        is UtbetalingBeregningFastSatsPerTiltaksplassPerManed -> {
            val satser = beregning.input.satser.sortedBy { it.periode.start }
            Details(
                entries = getSatserDetails("Sats", satser) +
                    DetailsEntry.number("Antall månedsverk", totalFaktor) +
                    getBelopDetails(beregning.output.belop),
            )
        }
        is UtbetalingBeregningPrisPerManedsverk -> {
            val satser = beregning.input.satser.sortedBy { it.periode.start }
            Details(
                entries = getSatserDetails("Avtalt månedspris per tiltaksplass", satser) +
                    DetailsEntry.number("Antall månedsverk", totalFaktor) +
                    getBelopDetails(beregning.output.belop),
            )
        }
        is UtbetalingBeregningPrisPerUkesverk -> {
            val satser = beregning.input.satser.sortedBy { it.periode.start }
            Details(
                entries = getSatserDetails("Avtalt ukespris per tiltaksplass", satser) +
                    DetailsEntry.number("Antall ukesverk", totalFaktor) +
                    getBelopDetails(beregning.output.belop),
            )
        }
        is UtbetalingBeregningPrisPerHeleUkesverk -> {
            val satser = beregning.input.satser.sortedBy { it.periode.start }
            Details(
                entries = getSatserDetails("Avtalt ukespris per tiltaksplass", satser) +
                    DetailsEntry.number("Antall ukesverk", totalFaktor) +
                    getBelopDetails(beregning.output.belop),
            )
        }
        is UtbetalingBeregningPrisPerTimeOppfolging -> {
            val satser = beregning.input.satser.sortedBy { it.periode.start }
            Details(
                entries = getSatserDetails("Avtalt pris per time oppfølging", satser) +
                    getBelopDetails(beregning.output.belop),
            )
        }
    }
}

fun beregningDeltakerTable(
    utbetaling: Utbetaling,
    deltakelser: List<ArrangorflateBeregningDeltakelse>,
) = when (val beregning = utbetaling.beregning) {
    is UtbetalingBeregningFri -> null
    is UtbetalingBeregningFastSatsPerTiltaksplassPerManed -> {
        val stengt = beregning.input.stengt.sortedBy { it.periode.start }
        deltakelseFastSatsPerTiltaksplassPerManedTable(
            utbetaling.periode,
            deltakelser,
            deltakerInput = beregning.input.deltakelser,
            stengt,
        )
    }
    is UtbetalingBeregningPrisPerManedsverk -> {
        val stengt = beregning.input.stengt.sortedBy { it.periode.start }
        deltakelsePrisPerManedsverkTable(utbetaling.periode, deltakelser, stengt)
    }
    is UtbetalingBeregningPrisPerUkesverk -> {
        val stengt = beregning.input.stengt.sortedBy { it.periode.start }
        deltakelsePrisPerUkesverkTable(utbetaling.periode, deltakelser, stengt)
    }
    is UtbetalingBeregningPrisPerHeleUkesverk -> {
        val stengt = beregning.input.stengt.sortedBy { it.periode.start }
        deltakelsePrisPerUkesverkTable(utbetaling.periode, deltakelser, stengt)
    }
    is UtbetalingBeregningPrisPerTimeOppfolging -> {
        deltakelsePrisPerTimeOppfolgingTable(deltakelser)
    }
}
