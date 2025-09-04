package no.nav.mulighetsrommet.api.utbetaling.api

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import no.nav.mulighetsrommet.api.navenhet.NavRegionDto
import no.nav.mulighetsrommet.api.utbetaling.PersonEnhetOgRegion
import no.nav.mulighetsrommet.api.utbetaling.model.*
import no.nav.mulighetsrommet.model.DataDetails
import no.nav.mulighetsrommet.model.DataDrivenTableDto
import no.nav.mulighetsrommet.model.DataElement

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("type")
sealed class UtbetalingBeregningDto {
    abstract val belop: Int
    abstract val heading: String
    abstract val deltakerTableData: DataDrivenTableDto
    abstract val deltakerRegioner: List<NavRegionDto>

    @Serializable
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
    data class PrisPerManedsverk(
        val sats: Int,
        val manedsverkTotal: Double,
        override val belop: Int,
        override val deltakerRegioner: List<NavRegionDto>,
        override val deltakerTableData: DataDrivenTableDto,
    ) : UtbetalingBeregningDto() {
        override val heading = "Avtalt månedpris per tiltaksplass"

        companion object {
            fun manedsverkTable(
                deltakelsePersoner: List<Pair<UtbetalingBeregningOutputDeltakelse, PersonEnhetOgRegion?>>,
                sats: Int,
            ) = DataDrivenTableDto(
                columns = Fri.friDeltakelseColumns() + manedsverkDeltakelseColumns(),
                rows = deltakelsePersoner.map { (deltakelse, person) ->
                    Fri.friDeltakelseCells(person) + manedsverkDeltakelseCells(deltakelse.faktor, sats)
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
        }
    }

    @Serializable
    data class PrisPerUkesverk(
        val sats: Int,
        val ukesverkTotal: Double,
        override val belop: Int,
        override val deltakerRegioner: List<NavRegionDto>,
        override val deltakerTableData: DataDrivenTableDto,
    ) : UtbetalingBeregningDto() {
        override val heading = "Avtalt ukespris per tiltaksplass"

        companion object {
            fun ukesverkTable(
                deltakelsePersoner: List<Pair<UtbetalingBeregningOutputDeltakelse, PersonEnhetOgRegion?>>,
                sats: Int,
            ) = DataDrivenTableDto(
                columns = Fri.friDeltakelseColumns() + ukesverkDeltakelseColumns(),
                rows = deltakelsePersoner.map { (deltakelse, person) ->
                    Fri.friDeltakelseCells(person) + ukesverkDeltakelseCells(deltakelse.faktor, sats)
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
        }
    }

    @Serializable
    data class Fri(
        override val belop: Int,
        override val deltakerRegioner: List<NavRegionDto>,
        override val deltakerTableData: DataDrivenTableDto,
    ) : UtbetalingBeregningDto() {
        override val heading = "Annen avtalt pris"

        companion object {
            fun friTable(deltakelsePersoner: List<Pair<UtbetalingBeregningOutputDeltakelse, PersonEnhetOgRegion?>>) = DataDrivenTableDto(
                columns = friDeltakelseColumns(),
                rows = deltakelsePersoner.map { friDeltakelseCells(it.second) },
            )

            fun friDeltakelseColumns() = listOf(
                DataDrivenTableDto.Column("navn", "Navn"),
                DataDrivenTableDto.Column("foedselsdato", "Fødselsdato"),
                DataDrivenTableDto.Column("region", "Region"),
                DataDrivenTableDto.Column("geografiskEnhet", "Geografisk enhet"),
            )

            fun friDeltakelseCells(person: PersonEnhetOgRegion?) = mapOf(
                "navn" to person?.person?.navn?.let { DataElement.text(it) },
                "geografiskEnhet" to person?.geografiskEnhet?.navn?.let { DataElement.text(it) },
                "region" to person?.region?.navn?.let { DataElement.text(it) },
                "foedselsdato" to person?.person?.foedselsdato?.let { DataElement.date(it) },
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
