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
                    val deltakelser = deltakere.map { it.deltakelse }.toSet()
                    UtbetalingBeregningDto(
                        heading = PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK.navn,
                        deltakerRegioner = regioner,
                        deltakerTableData = deltakelsePrisPerManedsverkTable(deltakere),
                        regnestykke = getRegnestykkeManedsverk(deltakelser),
                    )
                }

                is UtbetalingBeregningPrisPerManedsverk -> {
                    val deltakelser = deltakere.map { it.deltakelse }.toSet()
                    UtbetalingBeregningDto(
                        heading = PrismodellType.AVTALT_PRIS_PER_MANEDSVERK.navn,
                        deltakerRegioner = regioner,
                        deltakerTableData = deltakelsePrisPerManedsverkTable(deltakere),
                        regnestykke = getRegnestykkeManedsverk(deltakelser),
                    )
                }

                is UtbetalingBeregningPrisPerUkesverk -> {
                    val deltakelser = deltakere.map { it.deltakelse }.toSet()
                    UtbetalingBeregningDto(
                        heading = PrismodellType.AVTALT_PRIS_PER_UKESVERK.navn,
                        deltakerRegioner = regioner,
                        deltakerTableData = deltakelsePrisPerUkesverkTable(deltakere),
                        regnestykke = getRegnestykkeUkesverk(deltakelser),
                    )
                }

                is UtbetalingBeregningPrisPerHeleUkesverk -> {
                    val deltakelser = deltakere.map { it.deltakelse }.toSet()
                    UtbetalingBeregningDto(
                        heading = PrismodellType.AVTALT_PRIS_PER_HELE_UKESVERK.navn,
                        deltakerRegioner = regioner,
                        deltakerTableData = deltakelsePrisPerUkesverkTable(deltakere),
                        regnestykke = getRegnestykkeUkesverk(deltakelser),
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

private fun deltakelsePrisPerManedsverkTable(
    deltakere: List<UtbetalingBeregningDeltaker>,
): DataDrivenTableDto {
    return DataDrivenTableDto(
        columns = deltakelsePersonaliaColumns() + deltakelseFaktorColumns("Månedsverk"),
        rows = deltakere.map { deltaker ->
            val antallManeder = deltaker.deltakelse.perioder.sumOf { it.faktor }
            val belop = UtbetalingBeregningHelpers.calculateBelopForDeltakelser(setOf(deltaker.deltakelse))
            deltakelsePersonaliaCells(deltaker.personalia) + deltakelseFaktorCells(antallManeder, belop)
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
    deltakere: List<UtbetalingBeregningDeltaker>,
): DataDrivenTableDto {
    return DataDrivenTableDto(
        columns = deltakelsePersonaliaColumns() + deltakelseFaktorColumns("Ukesverk"),
        rows = deltakere.map { deltaker ->
            val antallUker = deltaker.deltakelse.perioder.sumOf { it.faktor }
            val belop = UtbetalingBeregningHelpers.calculateBelopForDeltakelser(setOf(deltaker.deltakelse))
            deltakelsePersonaliaCells(deltaker.personalia) + deltakelseFaktorCells(antallUker, belop)
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
