package no.nav.mulighetsrommet.api.utbetaling.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import no.nav.mulighetsrommet.api.navenhet.NavEnhetService
import no.nav.mulighetsrommet.api.utbetaling.PersonEnhetOgRegion
import no.nav.mulighetsrommet.api.utbetaling.model.*
import no.nav.mulighetsrommet.api.utils.DatoUtils.formaterDatoTilEuropeiskDatoformat
import no.nav.mulighetsrommet.model.DataDrivenTableDto

@Serializable
sealed class UtbetalingBeregningDto {
    abstract val belop: Int
    abstract val heading: String
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
        override val heading = "Pris per månedsverk"

        companion object {
            fun manedsverkTable(deltakelsePersoner: List<Pair<UtbetalingBeregningDeltakelse, PersonEnhetOgRegion?>>, sats: Int) = DataDrivenTableDto(
                columns = manedsverkDeltakelseColumns(),
                rows = deltakelsePersoner.map { (deltakelse, person) ->
                    manedsverkDeltakelseRow(deltakelse.faktor, sats, person)
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

            private fun manedsverkDeltakelseRow(manedsverk: Double, sats: Int, person: PersonEnhetOgRegion?) = Fri.friDeltakelseRow(person).plus(
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
        override val heading = "Pris per ukesverk"

        companion object {
            fun ukesverkTable(deltakelsePersoner: List<Pair<UtbetalingBeregningDeltakelse, PersonEnhetOgRegion?>>, sats: Int) = DataDrivenTableDto(
                columns = ukesverkDeltakelseColumns(),
                rows = deltakelsePersoner.map { (deltakelse, person) ->
                    ukesverkDeltakelseRow(deltakelse.faktor, sats, person)
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

            private fun ukesverkDeltakelseRow(ukesverk: Double, sats: Int, person: PersonEnhetOgRegion?) = Fri.friDeltakelseRow(person).plus(
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
        override val heading = "Annen avtalt pris"

        companion object {
            fun friTable(deltakelsePersoner: List<Pair<UtbetalingBeregningDeltakelse, PersonEnhetOgRegion?>>) = DataDrivenTableDto(
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

            fun friDeltakelseRow(person: PersonEnhetOgRegion?) = mapOf<String, JsonElement>(
                "navn" to JsonPrimitive(person?.person?.navn),
                "geografiskEnhet" to JsonPrimitive(person?.geografiskEnhet?.navn),
                "region" to JsonPrimitive(person?.region?.navn),
                "foedselsdato" to JsonPrimitive(person?.person?.foedselsdato?.formaterDatoTilEuropeiskDatoformat()),
            )
        }
    }
    companion object {
        fun from(
            utbetaling: Utbetaling,
            deltakelsePersoner: List<Pair<UtbetalingBeregningDeltakelse, PersonEnhetOgRegion?>>,
            regioner: List<NavEnhetService.NavRegionDto>,
        ): UtbetalingBeregningDto {
            return when (utbetaling.beregning) {
                is UtbetalingBeregningFri -> Fri(
                    belop = utbetaling.beregning.output.belop,
                    deltakerRegioner = regioner,
                    deltakerTableData = Fri.friTable(deltakelsePersoner),
                )

                is UtbetalingBeregningPrisPerManedsverkMedDeltakelsesmengder,
                is UtbetalingBeregningPrisPerManedsverk,
                -> {
                    val sats = getSats(utbetaling.beregning.input)
                    val manedsverkTotal = deltakelsePersoner.sumOf { (deltakelse) -> deltakelse.faktor }
                    PrisPerManedsverk(
                        belop = utbetaling.beregning.output.belop,
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
                        belop = utbetaling.beregning.output.belop,
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
        is UtbetalingBeregningPrisPerManedsverkMedDeltakelsesmengder.Input -> input.sats
        is UtbetalingBeregningPrisPerManedsverk.Input -> input.sats
        is UtbetalingBeregningPrisPerUkesverk.Input -> input.sats
        is UtbetalingBeregningFri.Input -> throw IllegalArgumentException("UtbetalingBeregningFri har ingen sats")
    }
}
