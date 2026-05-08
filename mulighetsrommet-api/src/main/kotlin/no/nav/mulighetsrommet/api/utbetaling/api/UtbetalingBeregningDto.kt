package no.nav.mulighetsrommet.api.utbetaling.api

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.arrangorflate.service.beregningSatsPeriodeDetaljerMedFaktor
import no.nav.mulighetsrommet.api.arrangorflate.service.beregningSatsPeriodeDetaljerUtenFaktor
import no.nav.mulighetsrommet.api.avtale.model.PrismodellType
import no.nav.mulighetsrommet.api.navenhet.Kontorstruktur
import no.nav.mulighetsrommet.api.utbetaling.model.DeltakelseDeltakelsesprosentPerioder
import no.nav.mulighetsrommet.api.utbetaling.model.DeltakerAdvarselDto
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
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningType
import no.nav.mulighetsrommet.api.utbetaling.service.Gradering
import no.nav.mulighetsrommet.api.utbetaling.service.Personalia
import no.nav.mulighetsrommet.model.DataDetails
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.TimelineDto
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.model.ValutaBelop
import java.util.UUID

@Serializable
data class UtbetalingBeregningDto(
    val type: UtbetalingBeregningType,
    val heading: String,
    val deltakerRegioner: List<Kontorstruktur>,
    val deltakere: List<BeregningDeltakerDto>,
    val pris: ValutaBelop,
    val satsDetaljer: List<DataDetails>,
    val advarsler: List<DeltakerAdvarselDto>,
) {
    companion object {
        fun from(
            beregning: UtbetalingBeregning,
            personaliaById: Map<UUID, Personalia>,
            regioner: List<Kontorstruktur>,
            utbetalingPeriode: Periode,
            advarsler: List<DeltakerAdvarselDto>,
        ): UtbetalingBeregningDto {
            return when (beregning) {
                is UtbetalingBeregningFri -> UtbetalingBeregningDto(
                    type = UtbetalingBeregningType.FRI,
                    heading = PrismodellType.ANNEN_AVTALT_PRIS.navn,
                    deltakerRegioner = regioner,
                    deltakere = emptyList(),
                    pris = beregning.output.pris,
                    satsDetaljer = emptyList(),
                    advarsler = advarsler,
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
                        type = UtbetalingBeregningType.FAST_SATS_PER_TILTAKSPLASS_PER_MANED,
                        heading = PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK.navn,
                        deltakerRegioner = regioner,
                        deltakere = deltakelseFastSatsPerTiltaksplassPerManedTable(
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
                        advarsler = advarsler,
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
                        type = UtbetalingBeregningType.PRIS_PER_MANEDSVERK,
                        heading = PrismodellType.AVTALT_PRIS_PER_MANEDSVERK.navn,
                        deltakerRegioner = regioner,
                        deltakere = deltakelsePrisPerManedsverkTable(
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
                        advarsler = advarsler,
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
                        type = UtbetalingBeregningType.PRIS_PER_UKESVERK,
                        heading = PrismodellType.AVTALT_PRIS_PER_UKESVERK.navn,
                        deltakerRegioner = regioner,
                        deltakere = deltakelsePrisPerUkesverkTable(
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
                        advarsler = advarsler,
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
                        type = UtbetalingBeregningType.PRIS_PER_HELE_UKESVERK,
                        heading = PrismodellType.AVTALT_PRIS_PER_HELE_UKESVERK.navn,
                        deltakerRegioner = regioner,
                        deltakere = deltakelsePrisPerUkesverkTable(
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
                        advarsler = advarsler,
                    )
                }

                is UtbetalingBeregningPrisPerTimeOppfolging -> {
                    val pris = beregning.input.pris
                    val satser = beregning.input.satser.sortedBy { it.periode.start }
                    UtbetalingBeregningDto(
                        type = UtbetalingBeregningType.PRIS_PER_TIME_OPPFOLGING,
                        heading = PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER.navn,
                        deltakerRegioner = regioner,
                        deltakere = deltakelsePrisPerTimeOppfolgingTable(personaliaById),
                        pris = pris,
                        satsDetaljer = beregningSatsPeriodeDetaljerUtenFaktor(
                            satser,
                            "Avtalt pris per time oppfølging",
                        ),
                        advarsler = advarsler,
                    )
                }
            }
        }
    }
}

@Serializable
data class BeregningDeltakerDto(
    val navn: String,
    val norskIdent: NorskIdent?,
    val gradering: Gradering,
    val faktor: Double?,
    val belop: ValutaBelop?,
    val geografiskEnhet: String?,
    val oppfolgingEnhet: String?,
    val region: String?,
    val content: TimelineDto?,
)

private fun getUtbetalingBeregningDeltaker(
    deltakelser: Set<UtbetalingBeregningOutputDeltakelse>,
    personaliaById: Map<UUID, Personalia>,
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
): List<BeregningDeltakerDto> {
    return deltakere.map { d ->
        val antallManeder = d.deltakelse.perioder.sumOf { it.faktor }
        val pris = UtbetalingBeregningHelpers.calculateBelopForDeltakelser(valuta, setOf(d.deltakelse))
        val input = requireNotNull(deltakerInput.find { it.deltakelseId == d.deltakelse.deltakelseId })
        BeregningDeltakerDto(
            navn = d.personalia.navn(),
            gradering = d.personalia.gradering,
            geografiskEnhet = d.personalia.geografiskEnhet()?.navn,
            oppfolgingEnhet = d.personalia.oppfolgingEnhet()?.navn,
            region = d.personalia.region()?.navn,
            norskIdent = d.personalia.norskIdent(),
            faktor = antallManeder,
            belop = pris,
            content = UtbetalingTimeline.deltakelseTimeline(
                utbetalingPeriode,
                stengt,
                UtbetalingTimeline.fastSatsPerTiltaksplassPerManedRow(
                    d.deltakelse,
                    input.perioder,
                ),
            ),
        )
    }
}

private fun deltakelsePrisPerManedsverkTable(
    valuta: Valuta,
    utbetalingPeriode: Periode,
    stengt: List<StengtPeriode>,
    deltakere: List<UtbetalingBeregningDeltaker>,
): List<BeregningDeltakerDto> {
    return deltakere.map { d ->
        val antallManeder = d.deltakelse.perioder.sumOf { it.faktor }
        val belop = UtbetalingBeregningHelpers.calculateBelopForDeltakelser(valuta, setOf(d.deltakelse))
        BeregningDeltakerDto(
            navn = d.personalia.navn(),
            gradering = d.personalia.gradering,
            geografiskEnhet = d.personalia.geografiskEnhet()?.navn,
            oppfolgingEnhet = d.personalia.oppfolgingEnhet()?.navn,
            region = d.personalia.region()?.navn,
            norskIdent = d.personalia.norskIdent(),
            faktor = antallManeder,
            belop = belop,
            content = UtbetalingTimeline.deltakelseTimeline(
                utbetalingPeriode,
                stengt,
                UtbetalingTimeline.manedsverkBeregningRow(d.deltakelse),
            ),
        )
    }
}

private fun deltakelsePrisPerUkesverkTable(
    valuta: Valuta,
    utbetalingPeriode: Periode,
    stengt: List<StengtPeriode>,
    deltakere: List<UtbetalingBeregningDeltaker>,
): List<BeregningDeltakerDto> {
    return deltakere.map { d ->
        val antallUker = d.deltakelse.perioder.sumOf { it.faktor }
        val belop = UtbetalingBeregningHelpers.calculateBelopForDeltakelser(valuta, setOf(d.deltakelse))
        BeregningDeltakerDto(
            navn = d.personalia.navn(),
            gradering = d.personalia.gradering,
            geografiskEnhet = d.personalia.geografiskEnhet()?.navn,
            oppfolgingEnhet = d.personalia.oppfolgingEnhet()?.navn,
            region = d.personalia.region()?.navn,
            norskIdent = d.personalia.norskIdent(),
            faktor = antallUker,
            belop = belop,
            content = UtbetalingTimeline.deltakelseTimeline(
                utbetalingPeriode,
                stengt,
                UtbetalingTimeline.ukesverkBeregningRow(d.deltakelse),
            ),
        )
    }
}

private fun deltakelsePrisPerTimeOppfolgingTable(personalia: Map<UUID, Personalia>): List<BeregningDeltakerDto> = personalia.map { (_, personalia) ->
    BeregningDeltakerDto(
        navn = personalia.navn(),
        gradering = personalia.gradering,
        geografiskEnhet = personalia.geografiskEnhet()?.navn,
        oppfolgingEnhet = personalia.oppfolgingEnhet()?.navn,
        region = personalia.region()?.navn,
        norskIdent = personalia.norskIdent(),
        faktor = null,
        belop = null,
        content = null,
    )
}
