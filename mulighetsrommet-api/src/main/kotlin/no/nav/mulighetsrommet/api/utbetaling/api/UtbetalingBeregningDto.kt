package no.nav.mulighetsrommet.api.utbetaling.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive
import no.nav.mulighetsrommet.api.navenhet.NavRegionDto
import no.nav.mulighetsrommet.api.utbetaling.PersonEnhetOgRegion
import no.nav.mulighetsrommet.api.utbetaling.model.*
import no.nav.mulighetsrommet.model.DataDrivenTableDto
import no.nav.mulighetsrommet.model.DataElement

@Serializable
sealed class UtbetalingBeregningDto {
    abstract val belop: Int
    abstract val heading: String
    abstract val deltakerTableData: DataDrivenTableDto
    abstract val deltakerRegioner: List<NavRegionDto>

    @Serializable
    @SerialName("FAST_SATS_PER_TILTAKSPLASS_PER_MANED")
    data class FastSatsPerTiltaksplassPerManed(
        val sats: Int,
        val manedsverkTotal: Double,
        override val belop: Int,
        override val deltakerRegioner: List<NavRegionDto>,
        override val deltakerTableData: DataDrivenTableDto,
    ) : UtbetalingBeregningDto() {
        override val heading = "Fast sats per tiltaksplass per måned"

        companion object {
            fun manedsverkTable(
                deltakelsePersoner: List<Pair<UtbetalingBeregningOutputDeltakelse, PersonEnhetOgRegion?>>,
                sats: Int,
            ) = PrisPerManedsverk.manedsverkTable(deltakelsePersoner, sats)
        }
    }

    @Serializable
    @SerialName("PRIS_PER_MANEDSVERK")
    data class PrisPerManedsverk(
        val sats: Int,
        val manedsverkTotal: Double,
        override val belop: Int,
        override val deltakerRegioner: List<NavRegionDto>,
        override val deltakerTableData: DataDrivenTableDto,
    ) : UtbetalingBeregningDto() {
        override val heading = "Pris per månedsverk"

        companion object {
            fun manedsverkTable(
                deltakelsePersoner: List<Pair<UtbetalingBeregningOutputDeltakelse, PersonEnhetOgRegion?>>,
                sats: Int,
            ) = DataDrivenTableDto(
                columns = Fri.friDeltakelseColumns() + manedsverkDeltakelseColumns(),
                rows = deltakelsePersoner.map { (deltakelse, person) ->
                    DataDrivenTableDto.Row(
                        Fri.friDeltakelseCells(person) + manedsverkDeltakelseCells(deltakelse.faktor, sats),
                    )
                },
            )

            private fun manedsverkDeltakelseColumns() = listOf(
                DataDrivenTableDto.Column(
                    "manedsverk",
                    "Månedsverk",
                    true,
                    DataDrivenTableDto.Column.Align.RIGHT,
                ),
                DataDrivenTableDto.Column(
                    "belop",
                    "Beløp",
                    true,
                    DataDrivenTableDto.Column.Align.RIGHT,
                ),
            )

            private fun manedsverkDeltakelseCells(manedsverk: Double, sats: Int) = listOf(
                DataDrivenTableDto.Cell(
                    "manedsverk",
                    DataElement.Text(manedsverk.toString(), DataElement.Text.Format.NUMBER),
                ),
                DataDrivenTableDto.Cell(
                    "belop",
                    DataElement.Text((sats.toDouble() * manedsverk).toString(), DataElement.Text.Format.NOK),
                ),
            )
        }
    }

    @Serializable
    @SerialName("PRIS_PER_UKESVERK")
    data class PrisPerUkesverk(
        val sats: Int,
        val ukesverkTotal: Double,
        override val belop: Int,
        override val deltakerRegioner: List<NavRegionDto>,
        override val deltakerTableData: DataDrivenTableDto,
    ) : UtbetalingBeregningDto() {
        override val heading = "Pris per ukesverk"

        companion object {
            fun ukesverkTable(
                deltakelsePersoner: List<Pair<UtbetalingBeregningOutputDeltakelse, PersonEnhetOgRegion?>>,
                sats: Int,
            ) = DataDrivenTableDto(
                columns = Fri.friDeltakelseColumns() + ukesverkDeltakelseColumns(),
                rows = deltakelsePersoner.map { (deltakelse, person) ->
                    DataDrivenTableDto.Row(
                        Fri.friDeltakelseCells(person) + ukesverkDeltakelseCells(deltakelse.faktor, sats),
                    )
                },
            )

            private fun ukesverkDeltakelseColumns() = listOf(
                DataDrivenTableDto.Column(
                    "ukesverk",
                    "Ukesverk",
                    true,
                    DataDrivenTableDto.Column.Align.RIGHT,
                ),
                DataDrivenTableDto.Column(
                    "belop",
                    "Beløp",
                    true,
                    DataDrivenTableDto.Column.Align.RIGHT,
                ),
            )

            private fun ukesverkDeltakelseCells(ukesverk: Double, sats: Int) = listOf(
                DataDrivenTableDto.Cell(
                    "ukesverk",
                    DataElement.Text(ukesverk.toString(), DataElement.Text.Format.NUMBER),
                ),
                DataDrivenTableDto.Cell(
                    "belop",
                    DataElement.Text((sats.toDouble() * ukesverk).toString(), DataElement.Text.Format.NOK),
                ),
            )
        }
    }

    @Serializable
    @SerialName("FRI")
    data class Fri(
        override val belop: Int,
        override val deltakerRegioner: List<NavRegionDto>,
        override val deltakerTableData: DataDrivenTableDto,
    ) : UtbetalingBeregningDto() {
        override val heading = "Annen avtalt pris"

        companion object {
            fun friTable(deltakelsePersoner: List<Pair<UtbetalingBeregningOutputDeltakelse, PersonEnhetOgRegion?>>) = DataDrivenTableDto(
                columns = friDeltakelseColumns(),
                rows = deltakelsePersoner.map {
                    DataDrivenTableDto.Row(friDeltakelseCells(it.second))
                },
            )

            fun friDeltakelseColumns() = listOf(
                DataDrivenTableDto.Column("navn", "Navn", true, DataDrivenTableDto.Column.Align.LEFT),
                DataDrivenTableDto.Column(
                    "foedselsdato",
                    "Fødselsdato",
                    true,
                    DataDrivenTableDto.Column.Align.LEFT,
                ),
                DataDrivenTableDto.Column("region", "Region", true, DataDrivenTableDto.Column.Align.LEFT),
                DataDrivenTableDto.Column(
                    "geografiskEnhet",
                    "Geografisk enhet",
                    true,
                    DataDrivenTableDto.Column.Align.LEFT,
                ),
            )

            fun friDeltakelseCells(person: PersonEnhetOgRegion?) = listOf(
                DataDrivenTableDto.Cell(
                    "navn",
                    person?.person?.navn?.let { DataElement.Text(it) },
                ),
                DataDrivenTableDto.Cell(
                    "geografiskEnhet",
                    person?.geografiskEnhet?.navn?.let { DataElement.Text(it) },
                ),
                DataDrivenTableDto.Cell(
                    "region",
                    person?.region?.navn?.let { DataElement.Text(it) },
                ),
                DataDrivenTableDto.Cell(
                    "foedselsdato",
                    person?.person?.foedselsdato?.let { DataElement.Text(it.toString(), DataElement.Text.Format.DATE) },
                ),
            )
        }
    }

    companion object {
        fun from(
            utbetaling: Utbetaling,
            deltakelsePersoner: List<Pair<UtbetalingBeregningOutputDeltakelse, PersonEnhetOgRegion?>>,
            regioner: List<NavRegionDto>,
        ): UtbetalingBeregningDto {
            return when (utbetaling.beregning) {
                is UtbetalingBeregningFri -> Fri(
                    belop = utbetaling.beregning.output.belop,
                    deltakerRegioner = regioner,
                    deltakerTableData = Fri.friTable(deltakelsePersoner),
                )

                is UtbetalingBeregningFastSatsPerTiltaksplassPerManed,
                -> {
                    val sats = getSats(utbetaling.beregning.input)
                    val manedsverkTotal = deltakelsePersoner.sumOf { (deltakelse) -> deltakelse.faktor }
                    FastSatsPerTiltaksplassPerManed(
                        belop = UtbetalingBeregningHelpers.calculateBelopForDeltakelse(
                            deltakelsePersoner.map { it.first }.toSet(),
                            sats,
                        ),
                        manedsverkTotal = manedsverkTotal,
                        deltakerRegioner = regioner,
                        deltakerTableData = FastSatsPerTiltaksplassPerManed.manedsverkTable(deltakelsePersoner, sats),
                        sats = sats,
                    )
                }

                is UtbetalingBeregningPrisPerManedsverk,
                -> {
                    val sats = getSats(utbetaling.beregning.input)
                    val manedsverkTotal = deltakelsePersoner.sumOf { (deltakelse) -> deltakelse.faktor }
                    PrisPerManedsverk(
                        belop = UtbetalingBeregningHelpers.calculateBelopForDeltakelse(
                            deltakelsePersoner.map { it.first }.toSet(),
                            sats,
                        ),
                        manedsverkTotal = manedsverkTotal,
                        deltakerRegioner = regioner,
                        deltakerTableData = PrisPerManedsverk.manedsverkTable(deltakelsePersoner, sats),
                        sats = sats,
                    )
                }

                is UtbetalingBeregningPrisPerUkesverk -> {
                    val sats = getSats(utbetaling.beregning.input)
                    val ukesverkTotal = deltakelsePersoner.sumOf { (deltakelse) -> deltakelse.faktor }
                    PrisPerUkesverk(
                        belop = UtbetalingBeregningHelpers.calculateBelopForDeltakelse(
                            deltakelsePersoner.map { it.first }.toSet(),
                            sats,
                        ),
                        ukesverkTotal = ukesverkTotal,
                        deltakerRegioner = regioner,
                        deltakerTableData = PrisPerUkesverk.ukesverkTable(deltakelsePersoner, sats),
                        sats = sats,
                    )
                }
            }
        }
    }
}

private fun getSats(input: UtbetalingBeregningInput): Int {
    return when (input) {
        is UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Input -> input.sats
        is UtbetalingBeregningPrisPerManedsverk.Input -> input.sats
        is UtbetalingBeregningPrisPerUkesverk.Input -> input.sats
        is UtbetalingBeregningFri.Input -> throw IllegalArgumentException("UtbetalingBeregningFri har ingen sats")
    }
}
