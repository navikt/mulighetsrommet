package no.nav.mulighetsrommet.api.utbetaling.api

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.arrangorflate.api.beregningSatsPeriodeDetaljerMedFaktor
import no.nav.mulighetsrommet.api.arrangorflate.api.beregningSatsPeriodeDetaljerUtenFaktor
import no.nav.mulighetsrommet.api.avtale.model.PrismodellType
import no.nav.mulighetsrommet.api.navenhet.NavRegionDto
import no.nav.mulighetsrommet.api.utbetaling.DeltakerPersonaliaMedGeografiskEnhet
import no.nav.mulighetsrommet.api.utbetaling.model.DeltakelseDeltakelsesprosentPerioder
import no.nav.mulighetsrommet.api.utbetaling.model.SatsPeriode
import no.nav.mulighetsrommet.api.utbetaling.model.StengtPeriode
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
import no.nav.mulighetsrommet.model.DataDetails
import no.nav.mulighetsrommet.model.DataDrivenTableDto
import no.nav.mulighetsrommet.model.DataElement
import no.nav.mulighetsrommet.model.LabeledDataElement
import no.nav.mulighetsrommet.model.Periode
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

@Serializable
data class UtbetalingBeregningDto(
    val heading: String,
    val deltakerRegioner: List<NavRegionDto>,
    val deltakerTableData: DataDrivenTableDto?,
    val belop: Int,
    val satsDetaljer: List<DataDetails>,
) {
    companion object {
        fun from(
            beregning: UtbetalingBeregning,
            personaliaById: Map<UUID, DeltakerPersonaliaMedGeografiskEnhet>,
            regioner: List<NavRegionDto>,
            utbetalingPeriode: Periode,
        ): UtbetalingBeregningDto {
            return when (beregning) {
                is UtbetalingBeregningFri -> UtbetalingBeregningDto(
                    heading = PrismodellType.ANNEN_AVTALT_PRIS.navn,
                    deltakerRegioner = regioner,
                    deltakerTableData = null,
                    belop = beregning.output.belop,
                    satsDetaljer = emptyList(),
                )

                is UtbetalingBeregningFastSatsPerTiltaksplassPerManed -> {
                    val deltakere = getUtbetalingBeregningDeltaker(beregning.output.deltakelser(), personaliaById)
                    val belop =
                        UtbetalingBeregningHelpers.calculateBelopForDeltakelser(deltakere.map { it.deltakelse }.toSet())
                    val satser = beregning.input.satser.sortedBy { it.periode.start }
                    UtbetalingBeregningDto(
                        heading = PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK.navn,
                        deltakerRegioner = regioner,
                        deltakerTableData = deltakelseFastSatsPerTiltaksplassPerManedTable(
                            utbetalingPeriode = utbetalingPeriode,
                            stengt = beregning.input.stengt.sortedBy { it.periode.start },
                            deltakere,
                            beregning.input.deltakelser,
                        ),
                        belop = belop,
                        beregningSatsPeriodeDetaljerMedFaktor(
                            satser,
                            satsLabel = "Sats",
                            deltakelser = beregning.output.deltakelser(),
                            faktorLabel = "Antall månedsverk",
                        ),
                    )
                }

                is UtbetalingBeregningPrisPerManedsverk -> {
                    val deltakere = getUtbetalingBeregningDeltaker(beregning.output.deltakelser(), personaliaById)
                    val belop =
                        UtbetalingBeregningHelpers.calculateBelopForDeltakelser(deltakere.map { it.deltakelse }.toSet())
                    val satser = beregning.input.satser.sortedBy { it.periode.start }
                    UtbetalingBeregningDto(
                        heading = PrismodellType.AVTALT_PRIS_PER_MANEDSVERK.navn,
                        deltakerRegioner = regioner,
                        deltakerTableData = deltakelsePrisPerManedsverkTable(
                            utbetalingPeriode = utbetalingPeriode,
                            stengt = beregning.input.stengt.sortedBy { it.periode.start },
                            deltakere,
                        ),
                        belop = belop,
                        beregningSatsPeriodeDetaljerMedFaktor(
                            satser,
                            satsLabel = "Avtalt månedspris per tiltaksplass",
                            deltakelser = beregning.output.deltakelser(),
                            faktorLabel = "Antall månedsverk",
                        ),
                    )
                }

                is UtbetalingBeregningPrisPerUkesverk -> {
                    val deltakere = getUtbetalingBeregningDeltaker(beregning.output.deltakelser(), personaliaById)
                    val belop =
                        UtbetalingBeregningHelpers.calculateBelopForDeltakelser(deltakere.map { it.deltakelse }.toSet())
                    val satser = beregning.input.satser.sortedBy { it.periode.start }
                    UtbetalingBeregningDto(
                        heading = PrismodellType.AVTALT_PRIS_PER_UKESVERK.navn,
                        deltakerRegioner = regioner,
                        deltakerTableData = deltakelsePrisPerUkesverkTable(
                            utbetalingPeriode = utbetalingPeriode,
                            stengt = beregning.input.stengt.sortedBy { it.periode.start },
                            deltakere,
                        ),
                        belop = belop,
                        beregningSatsPeriodeDetaljerMedFaktor(
                            satser,
                            satsLabel = "Avtalt ukespris per tiltaksplass",
                            deltakelser = beregning.output.deltakelser(),
                            faktorLabel = "Antall ukesverk",
                        ),
                    )
                }

                is UtbetalingBeregningPrisPerHeleUkesverk -> {
                    val deltakere = getUtbetalingBeregningDeltaker(beregning.output.deltakelser(), personaliaById)
                    val belop =
                        UtbetalingBeregningHelpers.calculateBelopForDeltakelser(deltakere.map { it.deltakelse }.toSet())
                    val satser = beregning.input.satser.sortedBy { it.periode.start }
                    UtbetalingBeregningDto(
                        heading = PrismodellType.AVTALT_PRIS_PER_HELE_UKESVERK.navn,
                        deltakerRegioner = regioner,
                        deltakerTableData = deltakelsePrisPerUkesverkTable(
                            utbetalingPeriode = utbetalingPeriode,
                            stengt = beregning.input.stengt.sortedBy { it.periode.start },
                            deltakere,
                        ),
                        belop = belop,
                        satsDetaljer = beregningSatsPeriodeDetaljerMedFaktor(
                            satser,
                            satsLabel = "Avtalt ukespris per tiltaksplass",
                            deltakelser = beregning.output.deltakelser(),
                            faktorLabel = "Antall ukesverk",
                        ),
                    )
                }

                is UtbetalingBeregningPrisPerTimeOppfolging -> {
                    val belop = beregning.input.belop
                    val satser = beregning.input.satser.sortedBy { it.periode.start }
                    UtbetalingBeregningDto(
                        heading = PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER.navn,
                        deltakerRegioner = regioner,
                        deltakerTableData = deltakelsePrisPerTimeOppfolgingTable(personaliaById),
                        belop = belop,
                        satsDetaljer = beregningSatsPeriodeDetaljerUtenFaktor(
                            satser,
                            "Avtalt pris per time oppfølging",
                        ),
                    )
                }
            }
        }
    }
}

private fun getUtbetalingBeregningDeltaker(
    deltakelser: Set<UtbetalingBeregningOutputDeltakelse>,
    personaliaById: Map<UUID, DeltakerPersonaliaMedGeografiskEnhet>,
): List<UtbetalingBeregningDeltaker> = personaliaById.mapNotNull { (deltakelseId, personalia) ->
    deltakelser.find { it.deltakelseId == deltakelseId }
        ?.let { deltakelse -> UtbetalingBeregningDeltaker(personalia, deltakelse) }
}

private fun deltakelseFastSatsPerTiltaksplassPerManedTable(
    utbetalingPeriode: Periode,
    stengt: List<StengtPeriode>,
    deltakere: List<UtbetalingBeregningDeltaker>,
    deltakerInput: Set<DeltakelseDeltakelsesprosentPerioder>,
): DataDrivenTableDto {
    return DataDrivenTableDto(
        columns = deltakelsePersonaliaColumns() + deltakelseFaktorColumns("Månedsverk"),
        rows = deltakere.map { deltaker ->
            val antallManeder = deltaker.deltakelse.perioder.sumOf { it.faktor }
            val belop = UtbetalingBeregningHelpers.calculateBelopForDeltakelser(setOf(deltaker.deltakelse))
            val input = requireNotNull(deltakerInput.find { it.deltakelseId == deltaker.deltakelse.deltakelseId })
            DataDrivenTableDto.Row(
                cells = deltakelsePersonaliaCells(deltaker.personalia) + deltakelseFaktorCells(antallManeder, belop),
                content = UtbetalingTimeline.deltakelseTimeline(
                    utbetalingPeriode,
                    stengt,
                    UtbetalingTimeline.fastSatsPerTiltaksplassPerManedRow(
                        deltaker.deltakelse,
                        input.perioder,
                    ),
                ),
            )
        },
    )
}

private fun deltakelsePrisPerManedsverkTable(
    utbetalingPeriode: Periode,
    stengt: List<StengtPeriode>,
    deltakere: List<UtbetalingBeregningDeltaker>,
): DataDrivenTableDto {
    return DataDrivenTableDto(
        columns = deltakelsePersonaliaColumns() + deltakelseFaktorColumns("Månedsverk"),
        rows = deltakere.map { deltaker ->
            val antallManeder = deltaker.deltakelse.perioder.sumOf { it.faktor }
            val belop = UtbetalingBeregningHelpers.calculateBelopForDeltakelser(setOf(deltaker.deltakelse))
            DataDrivenTableDto.Row(
                cells = deltakelsePersonaliaCells(deltaker.personalia) + deltakelseFaktorCells(antallManeder, belop),
                content = UtbetalingTimeline.deltakelseTimeline(
                    utbetalingPeriode,
                    stengt,
                    UtbetalingTimeline.manedsverkBeregningRow(deltaker.deltakelse),
                ),
            )
        },
    )
}

private fun getRegnestykkeManedsverk(
    deltakelser: Set<UtbetalingBeregningOutputDeltakelse>,
): List<DataElement> = getRegnestykkeDeltakelsesfaktor(
    deltakelser,
    faktorLabel = "plasser",
    satsLabel = "per tiltaksplass per måned",
)

private fun deltakelsePrisPerUkesverkTable(
    utbetalingPeriode: Periode,
    stengt: List<StengtPeriode>,
    deltakere: List<UtbetalingBeregningDeltaker>,
): DataDrivenTableDto {
    return DataDrivenTableDto(
        columns = deltakelsePersonaliaColumns() + deltakelseFaktorColumns("Ukesverk"),
        rows = deltakere.map { deltaker ->
            val antallUker = deltaker.deltakelse.perioder.sumOf { it.faktor }
            val belop = UtbetalingBeregningHelpers.calculateBelopForDeltakelser(setOf(deltaker.deltakelse))
            DataDrivenTableDto.Row(
                cells = deltakelsePersonaliaCells(deltaker.personalia) + deltakelseFaktorCells(antallUker, belop),
                content = UtbetalingTimeline.deltakelseTimeline(
                    utbetalingPeriode,
                    stengt,
                    UtbetalingTimeline.ukesverkBeregningRow(deltaker.deltakelse),
                ),
            )
        },
    )
}

private fun getRegnestykkeUkesverk(
    deltakelser: Set<UtbetalingBeregningOutputDeltakelse>,
): List<DataElement> = getRegnestykkeDeltakelsesfaktor(
    deltakelser,
    faktorLabel = "uker",
    satsLabel = "per tiltaksplass per uke",
)

private fun deltakelsePrisPerTimeOppfolgingTable(personalia: Map<UUID, DeltakerPersonaliaMedGeografiskEnhet>) = DataDrivenTableDto(
    columns = deltakelsePersonaliaColumns(),
    rows = personalia.map { (_, personalia) ->
        DataDrivenTableDto.Row(
            cells = deltakelsePersonaliaCells(personalia),
        )
    },
)

private fun deltakelsePersonaliaColumns() = listOf(
    DataDrivenTableDto.Column("navn", "Navn"),
    DataDrivenTableDto.Column("fnr", "Fødselsnr."),
    DataDrivenTableDto.Column("region", "Region"),
    DataDrivenTableDto.Column("oppfolgingEnhet", "Oppfølgingsenhet"),
    DataDrivenTableDto.Column("geografiskEnhet", "Geografisk enhet"),
)

private fun deltakelsePersonaliaCells(personalia: DeltakerPersonaliaMedGeografiskEnhet?): Map<String, DataElement?> = mapOf(
    "navn" to personalia?.navn.let { DataElement.text(it) },
    "geografiskEnhet" to personalia?.geografiskEnhet?.navn?.let { DataElement.text(it) },
    "oppfolgingEnhet" to personalia?.oppfolgingEnhet?.navn?.let { DataElement.text(it) },
    "region" to personalia?.region?.navn?.let { DataElement.text(it) },
    "fnr" to personalia?.norskIdent?.let { DataElement.text(it.value) },
)

private fun deltakelseFaktorColumns(faktorLabel: String) = listOf(
    DataDrivenTableDto.Column(
        "faktor",
        faktorLabel,
        align = DataDrivenTableDto.Column.Align.RIGHT,
    ),
    DataDrivenTableDto.Column(
        "belop",
        "Beløp",
        align = DataDrivenTableDto.Column.Align.RIGHT,
    ),
)

private fun deltakelseFaktorCells(ukesverk: Double, belop: Int) = mapOf(
    "faktor" to DataElement.number(ukesverk),
    "belop" to DataElement.nok(belop),
)

private fun getRegnestykkeDeltakelsesfaktor(
    deltakelser: Set<UtbetalingBeregningOutputDeltakelse>,
    faktorLabel: String,
    satsLabel: String,
): List<DataElement> {
    val belop = UtbetalingBeregningHelpers.calculateBelopForDeltakelser(deltakelser)
    return deltakelser
        .flatMap { it.perioder }
        .groupBy { it.sats }
        .map { (sats, perioder) ->
            val faktor = perioder.sumOf { it.faktor }
            listOf(
                DataElement.number(faktor),
                DataElement.text(faktorLabel),
                DataElement.MathOperator(DataElement.MathOperator.Type.MULTIPLY),
                DataElement.nok(sats),
                DataElement.text(satsLabel),
            )
        }
        .reduce { a, b ->
            a + listOf(DataElement.MathOperator(DataElement.MathOperator.Type.PLUS)) + b
        }
        .plus(DataElement.MathOperator(DataElement.MathOperator.Type.EQUALS))
        .plus(DataElement.nok(belop))
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
