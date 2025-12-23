package no.nav.mulighetsrommet.api.arrangorflate.api

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.arrangorflate.ArrangorAvbrytStatus
import no.nav.mulighetsrommet.api.clients.amtDeltaker.DeltakerPersonalia
import no.nav.mulighetsrommet.api.clients.pdl.PdlGradering
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingTimeline
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingType
import no.nav.mulighetsrommet.api.utbetaling.api.toDto
import no.nav.mulighetsrommet.api.utbetaling.model.DeltakelseDeltakelsesprosentPerioder
import no.nav.mulighetsrommet.api.utbetaling.model.DeltakelsePeriode
import no.nav.mulighetsrommet.api.utbetaling.model.Deltaker
import no.nav.mulighetsrommet.api.utbetaling.model.SatsPeriode
import no.nav.mulighetsrommet.api.utbetaling.model.StengtPeriode
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregning
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFastSatsPerTiltaksplassPerManed
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFri
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningHelpers
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningOutputDeltakelse
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningPrisPerHeleUkesverk
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningPrisPerManedsverk
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningPrisPerTimeOppfolging
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningPrisPerUkesverk
import no.nav.mulighetsrommet.api.utils.DatoUtils.formaterDatoTilEuropeiskDatoformat
import no.nav.mulighetsrommet.api.utils.DatoUtils.tilNorskDato
import no.nav.mulighetsrommet.model.DataDetails
import no.nav.mulighetsrommet.model.DataDrivenTableDto
import no.nav.mulighetsrommet.model.DataElement
import no.nav.mulighetsrommet.model.LabeledDataElement
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Periode
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.util.UUID

fun mapUtbetalingToArrangorflateUtbetaling(
    utbetaling: Utbetaling,
    status: ArrangorflateUtbetalingStatus,
    deltakereById: Map<UUID, Deltaker>,
    personaliaById: Map<UUID, ArrangorflatePersonalia?>,
    advarsler: List<DeltakerAdvarsel>,
    linjer: List<ArrangforflateUtbetalingLinje>,
    kanViseBeregning: Boolean,
    kanAvbrytes: ArrangorAvbrytStatus,
): ArrangorflateUtbetalingDto {
    val beregning = ArrangorflateBeregning(
        belop = utbetaling.beregning.output.belop,
        digest = utbetaling.beregning.getDigest(),
        deltakelser = beregningDeltakerTable(utbetaling, deltakereById, personaliaById),
        stengt = beregningStengt(utbetaling.beregning),
        displayName = beregningDisplayName(utbetaling.beregning),
        satsDetaljer = beregningSatsDetaljer(utbetaling.beregning),
    )

    val kanViseBeregningMedDeltakelse = beregning.deltakelser?.let { kanViseBeregning } ?: false

    return ArrangorflateUtbetalingDto(
        id = utbetaling.id,
        status = status,
        innsendtAvArrangorDato = utbetaling.godkjentAvArrangorTidspunkt?.toLocalDate(),
        utbetalesTidligstDato = utbetaling.utbetalesTidligstTidspunkt?.tilNorskDato(),
        kanViseBeregning = kanViseBeregningMedDeltakelse,
        createdAt = utbetaling.createdAt,
        tiltakstype = ArrangorflateTiltakstype(
            navn = utbetaling.tiltakstype.navn,
            tiltakskode = utbetaling.tiltakstype.tiltakskode,
        ),
        gjennomforing = ArrangorflateGjennomforingInfo(
            id = utbetaling.gjennomforing.id,
            lopenummer = utbetaling.gjennomforing.lopenummer,
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
        kanAvbrytes = kanAvbrytes,
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
    personalia: ArrangorflatePersonalia?,
    startDato: LocalDate?,
    periode: Periode,
): Map<String, DataElement?> = mapOf(
    "navn" to DataElement.text(personalia?.navn),
    "identitetsnummer" to DataElement.text(personalia?.norskIdent?.value),
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
    personaliaById: Map<UUID, ArrangorflatePersonalia?>,
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

fun beregningSatsDetaljer(beregning: UtbetalingBeregning): List<DataDetails> {
    return when (beregning) {
        is UtbetalingBeregningFri -> emptyList()

        is UtbetalingBeregningFastSatsPerTiltaksplassPerManed,
        -> {
            val satser = beregning.input.satser.sortedBy { it.periode.start }
            beregningSatsPeriodeDetaljerMedFaktor(
                satser,
                satsLabel = "Sats",
                deltakelser = beregning.output.deltakelser(),
                faktorLabel = "Antall månedsverk",
            )
        }

        is UtbetalingBeregningPrisPerManedsverk -> {
            val satser = beregning.input.satser.sortedBy { it.periode.start }
            beregningSatsPeriodeDetaljerMedFaktor(
                satser,
                satsLabel = "Avtalt månedspris per tiltaksplass",
                deltakelser = beregning.output.deltakelser(),
                faktorLabel = "Antall månedsverk",
            )
        }

        is UtbetalingBeregningPrisPerUkesverk -> {
            val satser = beregning.input.satser.sortedBy { it.periode.start }
            beregningSatsPeriodeDetaljerMedFaktor(
                satser,
                satsLabel = "Avtalt ukespris per tiltaksplass",
                deltakelser = beregning.output.deltakelser(),
                faktorLabel = "Antall ukesverk",
            )
        }

        is UtbetalingBeregningPrisPerHeleUkesverk -> {
            val satser = beregning.input.satser.sortedBy { it.periode.start }
            beregningSatsPeriodeDetaljerMedFaktor(
                satser,
                satsLabel = "Avtalt ukespris per tiltaksplass",
                deltakelser = beregning.output.deltakelser(),
                faktorLabel = "Antall ukesverk",
            )
        }

        is UtbetalingBeregningPrisPerTimeOppfolging -> {
            val satser = beregning.input.satser.sortedBy { it.periode.start }
            beregningSatsPeriodeDetaljerUtenFaktor(satser, "Avtalt pris per time oppfølging")
        }
    }
}

fun beregningSatsPeriodeDetaljerMedFaktor(
    satser: List<SatsPeriode>,
    deltakelser: Set<UtbetalingBeregningOutputDeltakelse>,
    satsLabel: String,
    faktorLabel: String,
): List<DataDetails> {
    return satser.mapNotNull { satsPeriode ->
        val faktor = deltakelser
            .flatMap { it.perioder }
            .filter { it.sats == satsPeriode.sats }
            .map { it.faktor.toBigDecimal() }
            .sumOf { it }
            .setScale(UtbetalingBeregningHelpers.OUTPUT_PRECISION, RoundingMode.HALF_UP)
            .toDouble()

        if (faktor.equals(BigDecimal.ZERO)) {
            null
        } else {
            DataDetails(
                header = "Periode ${satsPeriode.periode.start.formaterDatoTilEuropeiskDatoformat()} - ${
                    satsPeriode.periode.getLastInclusiveDate().formaterDatoTilEuropeiskDatoformat()
                }",
                entries = listOf(
                    LabeledDataElement.nok(satsLabel, satsPeriode.sats),
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
                LabeledDataElement.nok(satsLabel, satsPeriode.sats),
            ),
        )
    }
}

fun beregningDeltakerTable(
    utbetaling: Utbetaling,
    deltakereById: Map<UUID, Deltaker>,
    personaliaById: Map<UUID, ArrangorflatePersonalia?>,
): DataDrivenTableDto? {
    return when (val beregning = utbetaling.beregning) {
        is UtbetalingBeregningFri -> null

        is UtbetalingBeregningFastSatsPerTiltaksplassPerManed -> {
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

        is UtbetalingBeregningPrisPerManedsverk -> {
            val deltakelser = getArrangorflateBeregningDeltakelse(
                utbetaling.beregning.output.deltakelser(),
                deltakereById,
                personaliaById,
            )
            val stengt = beregning.input.stengt.sortedBy { it.periode.start }
            deltakelsePrisPerManedsverkTable(utbetaling.periode, deltakelser, stengt)
        }

        is UtbetalingBeregningPrisPerUkesverk -> {
            val deltakelser = getArrangorflateBeregningDeltakelse(
                utbetaling.beregning.output.deltakelser(),
                deltakereById,
                personaliaById,
            )
            val stengt = beregning.input.stengt.sortedBy { it.periode.start }
            deltakelsePrisPerUkesverkTable(utbetaling.periode, deltakelser, stengt)
        }

        is UtbetalingBeregningPrisPerHeleUkesverk -> {
            val deltakelser = getArrangorflateBeregningDeltakelse(
                utbetaling.beregning.output.deltakelser(),
                deltakereById,
                personaliaById,
            )
            val stengt = beregning.input.stengt.sortedBy { it.periode.start }
            deltakelsePrisPerUkesverkTable(utbetaling.periode, deltakelser, stengt)
        }

        is UtbetalingBeregningPrisPerTimeOppfolging -> {
            val deltakelsePerioder = utbetaling.beregning.deltakelsePerioder()
            deltakelsePrisPerTimeOppfolgingTable(deltakelsePerioder, deltakereById, personaliaById)
        }
    }
}

private fun getArrangorflateBeregningDeltakelse(
    deltakelser: Set<UtbetalingBeregningOutputDeltakelse>,
    deltakereById: Map<UUID, Deltaker>,
    personaliaById: Map<UUID, ArrangorflatePersonalia?>,
): List<ArrangorflateBeregningDeltakelse> = deltakelser.associateBy { it.deltakelseId }
    .map { (id, beregningOutput) ->
        ArrangorflateBeregningDeltakelse(
            deltaker = deltakereById[id],
            personalia = personaliaById[id],
            beregningOutput = beregningOutput,
        )
    }
    .sortedWith(compareBy(nullsLast()) { it.personalia?.navn })
