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
    val deltakerTableData: DataDrivenTableDto,
    val regnestykke: List<DataElement>,
) {
    companion object {
        fun from(
            utbetaling: Utbetaling,
            deltakelsePersoner: List<Pair<UtbetalingBeregningOutputDeltakelse, DeltakerPersonaliaMedGeografiskEnhet?>>,
            regioner: List<NavRegionDto>,
        ): UtbetalingBeregningDto {
            return when (utbetaling.beregning) {
                is UtbetalingBeregningFri -> UtbetalingBeregningDto(
                    heading = PrismodellType.ANNEN_AVTALT_PRIS.navn,
                    deltakerRegioner = regioner,
                    deltakerTableData = friTable(deltakelsePersoner),
                    regnestykke = listOf(
                        DataElement.text("Innsendt beløp"),
                        DataElement.MathOperator(DataElement.MathOperator.Type.EQUALS),
                        DataElement.nok(utbetaling.beregning.output.belop),
                    ),
                )

                is UtbetalingBeregningFastSatsPerTiltaksplassPerManed -> {
                    val sats = getSats(utbetaling.beregning.input)
                    val manedsverkTotal = deltakelsePersoner.sumOf { (deltakelse) -> deltakelse.faktor }
                    val belop = UtbetalingBeregningHelpers.calculateBelopForDeltakelse(
                        deltakelsePersoner.map { it.first }.toSet(),
                        sats,
                    )
                    UtbetalingBeregningDto(
                        heading = PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK.navn,
                        deltakerRegioner = regioner,
                        deltakerTableData = manedsverkTable(deltakelsePersoner, sats),
                        regnestykke = getRegnestykkeManedsverk(manedsverkTotal, sats, belop),
                    )
                }

                is UtbetalingBeregningPrisPerManedsverk -> {
                    val sats = getSats(utbetaling.beregning.input)
                    val manedsverkTotal = deltakelsePersoner.sumOf { (deltakelse) -> deltakelse.faktor }
                    val belop = UtbetalingBeregningHelpers.calculateBelopForDeltakelse(
                        deltakelsePersoner.map { it.first }.toSet(),
                        sats,
                    )
                    UtbetalingBeregningDto(
                        heading = PrismodellType.AVTALT_PRIS_PER_MANEDSVERK.navn,
                        deltakerRegioner = regioner,
                        deltakerTableData = manedsverkTable(deltakelsePersoner, sats),
                        regnestykke = getRegnestykkeManedsverk(manedsverkTotal, sats, belop),
                    )
                }

                is UtbetalingBeregningPrisPerUkesverk -> {
                    val sats = getSats(utbetaling.beregning.input)
                    val ukesverkTotal = deltakelsePersoner.sumOf { (deltakelse) -> deltakelse.faktor }
                    val belop = UtbetalingBeregningHelpers.calculateBelopForDeltakelse(
                        deltakelsePersoner.map { it.first }.toSet(),
                        sats,
                    )
                    UtbetalingBeregningDto(
                        heading = PrismodellType.AVTALT_PRIS_PER_UKESVERK.navn,
                        deltakerRegioner = regioner,
                        deltakerTableData = ukesverkTable(deltakelsePersoner, sats),
                        regnestykke = listOf(
                            DataElement.number(ukesverkTotal),
                            DataElement.text("uker"),
                            DataElement.MathOperator(DataElement.MathOperator.Type.MULTIPLY),
                            DataElement.nok(sats),
                            DataElement.text("per tiltaksplass per uke"),
                            DataElement.MathOperator(DataElement.MathOperator.Type.EQUALS),
                            DataElement.nok(belop),
                        ),
                    )
                }

                is UtbetalingBeregningPrisPerHeleUkesverk -> {
                    val sats = getSats(utbetaling.beregning.input)
                    val ukesverkTotal = deltakelsePersoner.sumOf { (deltakelse) -> deltakelse.faktor }
                    val belop = UtbetalingBeregningHelpers.calculateBelopForDeltakelse(
                        deltakelsePersoner.map { it.first }.toSet(),
                        sats,
                    )
                    UtbetalingBeregningDto(
                        heading = PrismodellType.AVTALT_PRIS_PER_HELE_UKESVERK.navn,
                        deltakerRegioner = regioner,
                        deltakerTableData = ukesverkTable(deltakelsePersoner, sats),
                        regnestykke = listOf(
                            DataElement.number(ukesverkTotal),
                            DataElement.text("uker"),
                            DataElement.MathOperator(DataElement.MathOperator.Type.MULTIPLY),
                            DataElement.nok(sats),
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
                        deltakerTableData = friTable(deltakelsePersoner),
                        regnestykke = listOf(
                            DataElement.number(utbetaling.beregning.output.belop),
                        ),
                    )
            }
        }
    }
}

private fun getSats(input: UtbetalingBeregningInput): Int {
    return when (input) {
        is UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Input -> input.sats
        is UtbetalingBeregningPrisPerManedsverk.Input -> input.sats
        is UtbetalingBeregningPrisPerUkesverk.Input -> input.sats
        is UtbetalingBeregningPrisPerHeleUkesverk.Input -> input.sats
        is UtbetalingBeregningPrisPerTimeOppfolging.Input -> input.sats
        is UtbetalingBeregningFri.Input -> throw IllegalArgumentException("UtbetalingBeregningFri har ingen sats")
    }
}

private fun manedsverkTable(
    deltakelsePersoner: List<Pair<UtbetalingBeregningOutputDeltakelse, DeltakerPersonaliaMedGeografiskEnhet?>>,
    sats: Int,
) = DataDrivenTableDto(
    columns = friDeltakelseColumns() + manedsverkDeltakelseColumns(),
    rows = deltakelsePersoner.map { (deltakelse, person) ->
        friDeltakelseCells(person) + manedsverkDeltakelseCells(deltakelse.faktor, sats)
    },
)

private fun manedsverkDeltakelseColumns() = listOf(
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

private fun manedsverkDeltakelseCells(manedsverk: Double, sats: Int) = mapOf(
    "manedsverk" to DataElement.number(manedsverk),
    "belop" to DataElement.nok(manedsverk * sats),
)

private fun ukesverkTable(
    deltakelsePersoner: List<Pair<UtbetalingBeregningOutputDeltakelse, DeltakerPersonaliaMedGeografiskEnhet?>>,
    sats: Int,
) = DataDrivenTableDto(
    columns = friDeltakelseColumns() + ukesverkDeltakelseColumns(),
    rows = deltakelsePersoner.map { (deltakelse, person) ->
        friDeltakelseCells(person) + ukesverkDeltakelseCells(deltakelse.faktor, sats)
    },
)

private fun ukesverkDeltakelseColumns() = listOf(
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

private fun ukesverkDeltakelseCells(ukesverk: Double, sats: Int) = mapOf(
    "ukesverk" to DataElement.number(ukesverk),
    "belop" to DataElement.nok(ukesverk * sats),
)

private fun friTable(deltakelsePersoner: List<Pair<UtbetalingBeregningOutputDeltakelse, DeltakerPersonaliaMedGeografiskEnhet?>>) = DataDrivenTableDto(
    columns = friDeltakelseColumns(),
    rows = deltakelsePersoner.map { friDeltakelseCells(it.second) },
)

private fun friDeltakelseColumns() = listOf(
    DataDrivenTableDto.Column("navn", "Navn"),
    DataDrivenTableDto.Column("fnr", "Fødselsnr."),
    DataDrivenTableDto.Column("region", "Region"),
    DataDrivenTableDto.Column("geografiskEnhet", "Geografisk enhet"),
    DataDrivenTableDto.Column("oppfolgingEnhet", "Oppfølgingsenhet"),
)

private fun friDeltakelseCells(personalia: DeltakerPersonaliaMedGeografiskEnhet?): Map<String, DataElement?> = mapOf(
    "navn" to personalia?.navn.let { DataElement.text(it) },
    "geografiskEnhet" to personalia?.geografiskEnhet?.navn?.let { DataElement.text(it) },
    "oppfolgingEnhet" to personalia?.oppfolgingEnhet?.navn?.let { DataElement.text(it) },
    "region" to personalia?.region?.navn?.let { DataElement.text(it) },
    "fnr" to personalia?.norskIdent?.let { DataElement.text(it.value) },
)

private fun getRegnestykkeManedsverk(
    manedsverkTotal: Double,
    sats: Int,
    belop: Int,
): List<DataElement> = listOf(
    DataElement.number(manedsverkTotal),
    DataElement.text("plasser"),
    DataElement.MathOperator(DataElement.MathOperator.Type.MULTIPLY),
    DataElement.nok(sats),
    DataElement.text("per tiltaksplass per måned"),
    DataElement.MathOperator(DataElement.MathOperator.Type.EQUALS),
    DataElement.nok(belop),
)
