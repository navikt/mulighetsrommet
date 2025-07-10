package no.nav.mulighetsrommet.api.utbetaling.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import no.nav.mulighetsrommet.api.navenhet.NavEnhetService
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingBeregningDto.*
import no.nav.mulighetsrommet.api.utbetaling.model.*
import no.nav.mulighetsrommet.api.utils.DatoUtils.formaterDatoTilEuropeiskDatoformat
import no.nav.mulighetsrommet.model.DataDrivenTableDto
import kotlin.math.round

@Serializable
sealed class UtbetalingBeregningDto {
    abstract val belop: Int
    abstract val deltakerTableData: DataDrivenTableDto
    abstract val deltakerRegioner: List<NavEnhetService.NavRegionDto>

    @Serializable
    @SerialName("PRIS_PER_MANEDSVERK")
    data class PrisPerManedsverk(
        val sats: Int,
        val manedsverkTotal: Double,
        override val belop: Int,
        override val deltakerRegioner: List<NavEnhetService.NavRegionDto>,
        override val deltakerTableData: DataDrivenTableDto,
    ) : UtbetalingBeregningDto() {
        companion object {
            fun manedsverkTable(deltakelsePersoner: List<Pair<UtbetalingBeregningDeltakelse, Person?>>, sats: Int) = DataDrivenTableDto(
                columns = manedsverkDeltakelseColumns(),
                rows = deltakelsePersoner.map {
                    deltakerRow(it.first, sats, it.second)
                },
            )

            private fun manedsverkDeltakelseColumns() = Fri.friDeltakelseColumns().plus(
                listOf(
                    DataDrivenTableDto.Column(
                        "manedsverk",
                        "Månedsverk",
                        true,
                        DataDrivenTableDto.Column.Align.RIGHT,
                        null,
                    ),
                    DataDrivenTableDto.Column(
                        "belop",
                        "Beløp",
                        true,
                        DataDrivenTableDto.Column.Align.RIGHT,
                        DataDrivenTableDto.Column.Format.NOK,
                    ),
                ),
            )

            fun manedsverkDeltakelseRow(manedsverk: Double, sats: Int, person: Person?) = Fri.friDeltakelseRow(person).plus(
                mapOf(
                    "manedsverk" to JsonPrimitive(manedsverk),
                    "belop" to JsonPrimitive(sats.toDouble() * manedsverk),
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
        override val deltakerRegioner: List<NavEnhetService.NavRegionDto>,
        override val deltakerTableData: DataDrivenTableDto,
    ) : UtbetalingBeregningDto() {
        companion object {
            fun ukesverkTable(deltakelsePersoner: List<Pair<UtbetalingBeregningDeltakelse, Person?>>, sats: Int) = DataDrivenTableDto(
                columns = ukesverkDeltakelseColumns(),
                rows = deltakelsePersoner.map {
                    deltakerRow(it.first, sats, it.second)
                },
            )

            private fun ukesverkDeltakelseColumns() = Fri.friDeltakelseColumns().plus(
                listOf(
                    DataDrivenTableDto.Column(
                        "ukesverk",
                        "Ukesverk",
                        true,
                        DataDrivenTableDto.Column.Align.RIGHT,
                        null,
                    ),
                    DataDrivenTableDto.Column(
                        "belop",
                        "Beløp",
                        true,
                        DataDrivenTableDto.Column.Align.RIGHT,
                        DataDrivenTableDto.Column.Format.NOK,
                    ),
                ),
            )

            fun ukesverkDeltakelseRow(ukesverk: Double, sats: Int, person: Person?) = Fri.friDeltakelseRow(person).plus(
                mapOf(
                    "ukesverk" to JsonPrimitive(ukesverk),
                    "belop" to JsonPrimitive(sats.toDouble() * ukesverk),
                ),
            )
        }
    }

    @Serializable
    @SerialName("FRI")
    data class Fri(
        override val belop: Int,
        override val deltakerRegioner: List<NavEnhetService.NavRegionDto>,
        override val deltakerTableData: DataDrivenTableDto,
    ) : UtbetalingBeregningDto() {
        companion object {
            fun friTable(deltakelsePersoner: List<Pair<UtbetalingBeregningDeltakelse, Person?>>) = DataDrivenTableDto(
                columns = friDeltakelseColumns(),
                rows = deltakelsePersoner.map {
                    friDeltakelseRow(it.second)
                },
            )

            fun friDeltakelseColumns() = listOf(
                DataDrivenTableDto.Column("navn", "Navn", true, DataDrivenTableDto.Column.Align.LEFT, null),
                DataDrivenTableDto.Column(
                    "foedselsdato",
                    "Fødselsdato",
                    true,
                    DataDrivenTableDto.Column.Align.LEFT,
                    DataDrivenTableDto.Column.Format.DATE,
                ),
                DataDrivenTableDto.Column("region", "Region", true, DataDrivenTableDto.Column.Align.LEFT, null),
                DataDrivenTableDto.Column(
                    "geografiskEnhet",
                    "Geografisk enhet",
                    true,
                    DataDrivenTableDto.Column.Align.LEFT,
                    null,
                ),
            )

            fun friDeltakelseRow(person: Person?) = mapOf<String, JsonElement>(
                "navn" to JsonPrimitive(person?.navn),
                "geografiskEnhet" to JsonPrimitive(person?.geografiskEnhet?.navn),
                "region" to JsonPrimitive(person?.region?.navn),
                "foedselsdato" to JsonPrimitive(person?.foedselsdato.formaterDatoTilEuropeiskDatoformat()),
            )
        }
    }

    companion object {
        fun from(
            utbetaling: Utbetaling,
            deltakelsePersoner: List<Pair<UtbetalingBeregningDeltakelse, Person?>>,
            regioner: List<NavEnhetService.NavRegionDto>,
        ): UtbetalingBeregningDto {
            return when (utbetaling.beregning) {
                is UtbetalingBeregningFri -> Fri(
                    belop = utbetaling.beregning.output.belop,
                    deltakerRegioner = regioner,
                    deltakerTableData = Fri.friTable(deltakelsePersoner),
                )

                is UtbetalingBeregningPrisPerManedsverkMedDeltakelsesmengder -> {
                    val manedsverkTotal = deltakelsePersoner.sumOf { (it.first as DeltakelseManedsverk).manedsverk }
                    PrisPerManedsverk(
                        belop = round(manedsverkTotal * utbetaling.beregning.input.sats.toDouble()).toInt(),
                        manedsverkTotal = manedsverkTotal,
                        deltakerRegioner = regioner,
                        deltakerTableData = PrisPerManedsverk.manedsverkTable(
                            deltakelsePersoner,
                            utbetaling.beregning.input.sats,
                        ),
                        sats = utbetaling.beregning.input.sats,
                    )
                }

                is UtbetalingBeregningPrisPerUkesverk -> {
                    val ukesverkTotal = deltakelsePersoner.sumOf { (it.first as DeltakelseUkesverk).ukesverk }
                    PrisPerUkesverk(
                        belop = round(ukesverkTotal * utbetaling.beregning.input.sats.toDouble()).toInt(),
                        ukesverkTotal = ukesverkTotal,
                        deltakerRegioner = regioner,
                        deltakerTableData = PrisPerUkesverk.ukesverkTable(
                            deltakelsePersoner,
                            utbetaling.beregning.input.sats,
                        ),
                        sats = utbetaling.beregning.input.sats,
                    )
                }
            }
        }
    }
}

fun deltakerRow(deltakelse: UtbetalingBeregningDeltakelse, sats: Int, person: Person?) = when (deltakelse) {
    is UtbetalingBeregningFri.Deltakelse -> Fri.friDeltakelseRow(person)
    is DeltakelseManedsverk -> PrisPerManedsverk.manedsverkDeltakelseRow(deltakelse.manedsverk, sats, person)
    is DeltakelseUkesverk -> PrisPerUkesverk.ukesverkDeltakelseRow(deltakelse.ukesverk, sats, person)
}
