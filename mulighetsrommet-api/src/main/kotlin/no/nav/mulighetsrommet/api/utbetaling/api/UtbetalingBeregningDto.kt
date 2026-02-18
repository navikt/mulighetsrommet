package no.nav.mulighetsrommet.api.utbetaling.api

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.arrangorflate.service.beregningSatsPeriodeDetaljerMedFaktor
import no.nav.mulighetsrommet.api.arrangorflate.service.beregningSatsPeriodeDetaljerUtenFaktor
import no.nav.mulighetsrommet.api.avtale.model.Kontorstruktur
import no.nav.mulighetsrommet.api.avtale.model.PrismodellType
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
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.model.ValutaBelop
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

@Serializable
data class UtbetalingBeregningDto(
    val heading: String,
    val deltakerRegioner: List<Kontorstruktur>,
    val deltakerTableData: DataDrivenTableDto?,
    val pris: ValutaBelop,
    val satsDetaljer: List<DataDetails>,
) {
    companion object {
        fun from(
            beregning: UtbetalingBeregning,
            personaliaById: Map<UUID, DeltakerPersonaliaMedGeografiskEnhet>,
            regioner: List<Kontorstruktur>,
            utbetalingPeriode: Periode,
        ): UtbetalingBeregningDto {
            return when (beregning) {
                is UtbetalingBeregningFri -> UtbetalingBeregningDto(
                    heading = PrismodellType.ANNEN_AVTALT_PRIS.navn,
                    deltakerRegioner = regioner,
                    deltakerTableData = null,
                    pris = beregning.output.pris,
                    satsDetaljer = emptyList(),
                )

                is UtbetalingBeregningFastSatsPerTiltaksplassPerManed -> {
                    val deltakere = getUtbetalingBeregningDeltaker(beregning.output.deltakelser(), personaliaById)
                    val pris =
                        UtbetalingBeregningHelpers.calculateBelopForDeltakelser(
                            beregning.output.pris.valuta,
                            deltakere.map { it.deltakelse }.toSet(),
                        )
                    val satser = beregning.input.satser.sortedBy { it.periode.start }
                    UtbetalingBeregningDto(
                        heading = PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK.navn,
                        deltakerRegioner = regioner,
                        deltakerTableData = deltakelseFastSatsPerTiltaksplassPerManedTable(
                            beregning.output.pris.valuta,
                            utbetalingPeriode = utbetalingPeriode,
                            stengt = beregning.input.stengt.sortedBy { it.periode.start },
                            deltakere,
                            beregning.input.deltakelser,
                        ),
                        pris = pris,
                        satsDetaljer = beregningSatsPeriodeDetaljerMedFaktor(
                            satser,
                            satsLabel = "Sats",
                            deltakelser = beregning.output.deltakelser(),
                            faktorLabel = "Antall månedsverk",
                        ),
                    )
                }

                is UtbetalingBeregningPrisPerManedsverk -> {
                    val deltakere = getUtbetalingBeregningDeltaker(beregning.output.deltakelser(), personaliaById)
                    val valuta = beregning.output.pris.valuta
                    val pris =
                        UtbetalingBeregningHelpers.calculateBelopForDeltakelser(
                            valuta,
                            deltakelser = deltakere.map { it.deltakelse }.toSet(),
                        )
                    val satser = beregning.input.satser.sortedBy { it.periode.start }
                    UtbetalingBeregningDto(
                        heading = PrismodellType.AVTALT_PRIS_PER_MANEDSVERK.navn,
                        deltakerRegioner = regioner,
                        deltakerTableData = deltakelsePrisPerManedsverkTable(
                            valuta,
                            utbetalingPeriode = utbetalingPeriode,
                            stengt = beregning.input.stengt.sortedBy { it.periode.start },
                            deltakere,
                        ),
                        pris = pris,
                        satsDetaljer = beregningSatsPeriodeDetaljerMedFaktor(
                            satser,
                            satsLabel = "Avtalt månedspris per tiltaksplass",
                            deltakelser = beregning.output.deltakelser(),
                            faktorLabel = "Antall månedsverk",
                        ),
                    )
                }

                is UtbetalingBeregningPrisPerUkesverk -> {
                    val deltakere = getUtbetalingBeregningDeltaker(beregning.output.deltakelser(), personaliaById)
                    val valuta = beregning.output.pris.valuta
                    val pris =
                        UtbetalingBeregningHelpers.calculateBelopForDeltakelser(
                            valuta = valuta,
                            deltakelser = deltakere.map { it.deltakelse }.toSet(),
                        )
                    val satser = beregning.input.satser.sortedBy { it.periode.start }
                    UtbetalingBeregningDto(
                        heading = PrismodellType.AVTALT_PRIS_PER_UKESVERK.navn,
                        deltakerRegioner = regioner,
                        deltakerTableData = deltakelsePrisPerUkesverkTable(
                            valuta = valuta,
                            utbetalingPeriode = utbetalingPeriode,
                            stengt = beregning.input.stengt.sortedBy { it.periode.start },
                            deltakere,
                        ),
                        pris = pris,
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
                    val valuta = beregning.output.pris.valuta
                    val pris =
                        UtbetalingBeregningHelpers.calculateBelopForDeltakelser(
                            valuta,
                            deltakelser = deltakere.map { it.deltakelse }.toSet(),
                        )
                    val satser = beregning.input.satser.sortedBy { it.periode.start }
                    UtbetalingBeregningDto(
                        heading = PrismodellType.AVTALT_PRIS_PER_HELE_UKESVERK.navn,
                        deltakerRegioner = regioner,
                        deltakerTableData = deltakelsePrisPerUkesverkTable(
                            valuta,
                            utbetalingPeriode = utbetalingPeriode,
                            stengt = beregning.input.stengt.sortedBy { it.periode.start },
                            deltakere,
                        ),
                        pris = pris,
                        satsDetaljer = beregningSatsPeriodeDetaljerMedFaktor(
                            satser,
                            satsLabel = "Avtalt ukespris per tiltaksplass",
                            deltakelser = beregning.output.deltakelser(),
                            faktorLabel = "Antall ukesverk",
                        ),
                    )
                }

                is UtbetalingBeregningPrisPerTimeOppfolging -> {
                    val pris = beregning.input.pris
                    val satser = beregning.input.satser.sortedBy { it.periode.start }
                    UtbetalingBeregningDto(
                        heading = PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER.navn,
                        deltakerRegioner = regioner,
                        deltakerTableData = deltakelsePrisPerTimeOppfolgingTable(personaliaById),
                        pris = pris,
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
    valuta: Valuta,
    utbetalingPeriode: Periode,
    stengt: List<StengtPeriode>,
    deltakere: List<UtbetalingBeregningDeltaker>,
    deltakerInput: Set<DeltakelseDeltakelsesprosentPerioder>,
): DataDrivenTableDto {
    return DataDrivenTableDto(
        columns = deltakelsePersonaliaColumns() + deltakelseFaktorColumns("Månedsverk"),
        rows = deltakere.map { deltaker ->
            val antallManeder = deltaker.deltakelse.perioder.sumOf { it.faktor }
            val pris = UtbetalingBeregningHelpers.calculateBelopForDeltakelser(valuta, setOf(deltaker.deltakelse))
            val input = requireNotNull(deltakerInput.find { it.deltakelseId == deltaker.deltakelse.deltakelseId })
            DataDrivenTableDto.Row(
                cells = deltakelsePersonaliaCells(deltaker.personalia) + deltakelseFaktorCells(antallManeder, pris),
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
    valuta: Valuta,
    utbetalingPeriode: Periode,
    stengt: List<StengtPeriode>,
    deltakere: List<UtbetalingBeregningDeltaker>,
): DataDrivenTableDto {
    return DataDrivenTableDto(
        columns = deltakelsePersonaliaColumns() + deltakelseFaktorColumns("Månedsverk"),
        rows = deltakere.map { deltaker ->
            val antallManeder = deltaker.deltakelse.perioder.sumOf { it.faktor }
            val belop = UtbetalingBeregningHelpers.calculateBelopForDeltakelser(valuta, setOf(deltaker.deltakelse))
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

private fun deltakelsePrisPerUkesverkTable(
    valuta: Valuta,
    utbetalingPeriode: Periode,
    stengt: List<StengtPeriode>,
    deltakere: List<UtbetalingBeregningDeltaker>,
): DataDrivenTableDto {
    return DataDrivenTableDto(
        columns = deltakelsePersonaliaColumns() + deltakelseFaktorColumns("Ukesverk"),
        rows = deltakere.map { deltaker ->
            val antallUker = deltaker.deltakelse.perioder.sumOf { it.faktor }
            val belop = UtbetalingBeregningHelpers.calculateBelopForDeltakelser(valuta, setOf(deltaker.deltakelse))
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

private fun deltakelseFaktorCells(ukesverk: Double, belop: ValutaBelop) = mapOf(
    "faktor" to DataElement.number(ukesverk),
    "belop" to DataElement.money(belop),
)

private fun getRegnestykkeDeltakelsesfaktor(
    valuta: Valuta,
    deltakelser: Set<UtbetalingBeregningOutputDeltakelse>,
    faktorLabel: String,
    satsLabel: String,
): List<DataElement> {
    val belop = UtbetalingBeregningHelpers.calculateBelopForDeltakelser(valuta, deltakelser)
    return deltakelser
        .flatMap { it.perioder }
        .groupBy { it.sats }
        .map { (sats, perioder) ->
            val faktor = perioder.sumOf { it.faktor }
            listOf(
                DataElement.number(faktor),
                DataElement.text(faktorLabel),
                DataElement.MathOperator(DataElement.MathOperator.Type.MULTIPLY),
                DataElement.money(sats),
                DataElement.text(satsLabel),
            )
        }
        .reduce { a, b ->
            a + listOf(DataElement.MathOperator(DataElement.MathOperator.Type.PLUS)) + b
        }
        .plus(DataElement.MathOperator(DataElement.MathOperator.Type.EQUALS))
        .plus(DataElement.money(belop))
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
                    LabeledDataElement.money(satsLabel, satsPeriode.sats),
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
                LabeledDataElement.money(satsLabel, satsPeriode.sats),
            ),
        )
    }
}
