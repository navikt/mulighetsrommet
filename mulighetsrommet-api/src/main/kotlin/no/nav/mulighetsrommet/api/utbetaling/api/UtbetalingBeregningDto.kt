package no.nav.mulighetsrommet.api.utbetaling.api

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.avtale.model.PrismodellType
import no.nav.mulighetsrommet.api.navenhet.NavRegionDto
import no.nav.mulighetsrommet.api.utbetaling.DeltakerPersonaliaMedGeografiskEnhet
import no.nav.mulighetsrommet.api.utbetaling.model.*
import no.nav.mulighetsrommet.model.DataDrivenTableDto
import no.nav.mulighetsrommet.model.DataElement

@Serializable
data class UtbetalingBeregningDto(
    val heading: String,
    val deltakerRegioner: List<NavRegionDto>,
    val deltakerTableData: DataDrivenTableDto?,
    val regnestykke: List<DataElement>,
) {
    companion object {
        fun from(
            beregning: UtbetalingBeregning,
            deltakere: List<UtbetalingBeregningDeltaker>,
            regioner: List<NavRegionDto>,
        ): UtbetalingBeregningDto {
            return when (beregning) {
                is UtbetalingBeregningFri -> UtbetalingBeregningDto(
                    heading = PrismodellType.ANNEN_AVTALT_PRIS.navn,
                    deltakerRegioner = regioner,
                    deltakerTableData = null,
                    regnestykke = listOf(
                        DataElement.text("Innsendt beløp"),
                        DataElement.MathOperator(DataElement.MathOperator.Type.EQUALS),
                        DataElement.nok(beregning.output.belop),
                    ),
                )

                is UtbetalingBeregningFastSatsPerTiltaksplassPerManed -> {
                    val satser = getSatser(beregning.input)
                    val manedsverkTotal = getTotalFaktor(deltakere)
                    val belop = UtbetalingBeregningHelpers.calculateBelopForDeltakelser(
                        deltakere.map { it.deltakelse }.toSet(),
                        satser,
                    )
                    UtbetalingBeregningDto(
                        heading = PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK.navn,
                        deltakerRegioner = regioner,
                        deltakerTableData = deltakelsePrisPerManedsverkTable(deltakere, satser),
                        regnestykke = getRegnestykkeManedsverk(manedsverkTotal, satser, belop),
                    )
                }

                is UtbetalingBeregningPrisPerManedsverk -> {
                    val satser = getSatser(beregning.input)
                    val manedsverkTotal = getTotalFaktor(deltakere)
                    val belop = UtbetalingBeregningHelpers.calculateBelopForDeltakelser(
                        deltakere.map { it.deltakelse }.toSet(),
                        satser,
                    )
                    UtbetalingBeregningDto(
                        heading = PrismodellType.AVTALT_PRIS_PER_MANEDSVERK.navn,
                        deltakerRegioner = regioner,
                        deltakerTableData = deltakelsePrisPerManedsverkTable(deltakere, satser),
                        regnestykke = getRegnestykkeManedsverk(manedsverkTotal, satser, belop),
                    )
                }

                is UtbetalingBeregningPrisPerUkesverk -> {
                    val satser = getSatser(beregning.input)
                    val ukesverkTotal = getTotalFaktor(deltakere)
                    val belop = UtbetalingBeregningHelpers.calculateBelopForDeltakelser(
                        deltakere.map { it.deltakelse }.toSet(),
                        satser,
                    )
                    UtbetalingBeregningDto(
                        heading = PrismodellType.AVTALT_PRIS_PER_UKESVERK.navn,
                        deltakerRegioner = regioner,
                        deltakerTableData = deltakelsePrisPerUkesverkTable(deltakere, satser),
                        regnestykke = listOf(
                            DataElement.number(ukesverkTotal),
                            DataElement.text("uker"),
                            DataElement.MathOperator(DataElement.MathOperator.Type.MULTIPLY),
                            // TODO: en rad per sats
                            DataElement.nok(satser.first().sats),
                            DataElement.text("per tiltaksplass per uke"),
                            DataElement.MathOperator(DataElement.MathOperator.Type.EQUALS),
                            DataElement.nok(belop),
                        ),
                    )
                }

                is UtbetalingBeregningPrisPerHeleUkesverk -> {
                    val satser = getSatser(beregning.input)
                    val ukesverkTotal = getTotalFaktor(deltakere)
                    val belop = UtbetalingBeregningHelpers.calculateBelopForDeltakelser(
                        deltakere.map { it.deltakelse }.toSet(),
                        satser,
                    )
                    UtbetalingBeregningDto(
                        heading = PrismodellType.AVTALT_PRIS_PER_HELE_UKESVERK.navn,
                        deltakerRegioner = regioner,
                        deltakerTableData = deltakelsePrisPerUkesverkTable(deltakere, satser),
                        regnestykke = listOf(
                            DataElement.number(ukesverkTotal),
                            DataElement.text("uker"),
                            DataElement.MathOperator(DataElement.MathOperator.Type.MULTIPLY),
                            // TODO: en rad per sats
                            DataElement.nok(satser.first().sats),
                            DataElement.text("per tiltaksplass per uke"),
                            DataElement.MathOperator(DataElement.MathOperator.Type.EQUALS),
                            DataElement.nok(belop),
                        ),
                    )
                }

                is UtbetalingBeregningPrisPerTimeOppfolging ->
                    UtbetalingBeregningDto(
                        heading = PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER.navn,
                        deltakerRegioner = regioner,
                        deltakerTableData = deltakelsePrisPerTimeOppfolgingTable(deltakere),
                        regnestykke = listOf(
                            DataElement.number(beregning.output.belop),
                        ),
                    )
            }
        }
    }
}

private fun getSatser(input: UtbetalingBeregningInput): Set<SatsPeriode> {
    return when (input) {
        is UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Input -> input.satser
        is UtbetalingBeregningPrisPerManedsverk.Input -> input.satser
        is UtbetalingBeregningPrisPerUkesverk.Input -> input.satser
        is UtbetalingBeregningPrisPerHeleUkesverk.Input -> input.satser
        is UtbetalingBeregningPrisPerTimeOppfolging.Input -> input.satser
        is UtbetalingBeregningFri.Input -> throw IllegalArgumentException("UtbetalingBeregningFri har ingen satser")
    }
}

private fun getTotalFaktor(deltakere: List<UtbetalingBeregningDeltaker>): Double {
    return deltakere.sumOf { deltaker -> deltaker.deltakelse.perioder.sumOf { it.faktor } }
}

private fun deltakelsePrisPerManedsverkTable(
    deltakere: List<UtbetalingBeregningDeltaker>,
    satser: Set<SatsPeriode>,
): DataDrivenTableDto {
    // TODO: periodisert faktor og sats
    val sats = satser.first().sats
    return DataDrivenTableDto(
        columns = deltakelsePersonaliaColumns() + deltakelseManedsverkColumns(),
        rows = deltakere.map { deltaker ->
            val manedsverk = deltaker.deltakelse.perioder.sumOf { it.faktor }
            deltakelsePersonaliaCells(deltaker.personalia) + deltakelseManedsverkCells(manedsverk, sats)
        },
    )
}

private fun deltakelseManedsverkColumns() = listOf(
    DataDrivenTableDto.Column(
        "manedsverk",
        "Månedsverk",
        align = DataDrivenTableDto.Column.Align.RIGHT,
    ),
    DataDrivenTableDto.Column(
        "belop",
        "Beløp",
        align = DataDrivenTableDto.Column.Align.RIGHT,
    ),
)

private fun deltakelseManedsverkCells(manedsverk: Double, sats: Int) = mapOf(
    "manedsverk" to DataElement.number(manedsverk),
    "belop" to DataElement.nok(manedsverk * sats),
)

private fun deltakelsePrisPerUkesverkTable(
    deltakere: List<UtbetalingBeregningDeltaker>,
    satser: Set<SatsPeriode>,
): DataDrivenTableDto {
    // TODO: periodisert faktor og sats
    val sats = satser.first().sats
    return DataDrivenTableDto(
        columns = deltakelsePersonaliaColumns() + deltakelseUkesverkColumns(),
        rows = deltakere.map { deltaker ->
            val ukesverk = deltaker.deltakelse.perioder.sumOf { it.faktor }
            deltakelsePersonaliaCells(deltaker.personalia) + deltakelseUkesverkCells(ukesverk, sats)
        },
    )
}

private fun deltakelseUkesverkColumns() = listOf(
    DataDrivenTableDto.Column(
        "ukesverk",
        "Ukesverk",
        align = DataDrivenTableDto.Column.Align.RIGHT,
    ),
    DataDrivenTableDto.Column(
        "belop",
        "Beløp",
        align = DataDrivenTableDto.Column.Align.RIGHT,
    ),
)

private fun deltakelseUkesverkCells(ukesverk: Double, sats: Int) = mapOf(
    "ukesverk" to DataElement.number(ukesverk),
    "belop" to DataElement.nok(ukesverk * sats),
)

private fun deltakelsePrisPerTimeOppfolgingTable(deltakere: List<UtbetalingBeregningDeltaker>) = DataDrivenTableDto(
    columns = deltakelsePersonaliaColumns(),
    rows = deltakere.map { deltakelsePersonaliaCells(it.personalia) },
)

private fun deltakelsePersonaliaColumns() = listOf(
    DataDrivenTableDto.Column("navn", "Navn"),
    DataDrivenTableDto.Column("fnr", "Fødselsnr."),
    DataDrivenTableDto.Column("region", "Region"),
    DataDrivenTableDto.Column("geografiskEnhet", "Geografisk enhet"),
    DataDrivenTableDto.Column("oppfolgingEnhet", "Oppfølgingsenhet"),
)

private fun deltakelsePersonaliaCells(personalia: DeltakerPersonaliaMedGeografiskEnhet?): Map<String, DataElement?> = mapOf(
    "navn" to personalia?.navn.let { DataElement.text(it) },
    "geografiskEnhet" to personalia?.geografiskEnhet?.navn?.let { DataElement.text(it) },
    "oppfolgingEnhet" to personalia?.oppfolgingEnhet?.navn?.let { DataElement.text(it) },
    "region" to personalia?.region?.navn?.let { DataElement.text(it) },
    "fnr" to personalia?.norskIdent?.let { DataElement.text(it.value) },
)

private fun getRegnestykkeManedsverk(
    manedsverkTotal: Double,
    satser: Set<SatsPeriode>,
    belop: Int,
): List<DataElement> = listOf(
    DataElement.number(manedsverkTotal),
    DataElement.text("plasser"),
    DataElement.MathOperator(DataElement.MathOperator.Type.MULTIPLY),
    // TODO: en rad per sats
    DataElement.nok(satser.first().sats),
    DataElement.text("per tiltaksplass per måned"),
    DataElement.MathOperator(DataElement.MathOperator.Type.EQUALS),
    DataElement.nok(belop),
)
