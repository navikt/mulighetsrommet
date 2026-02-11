package no.nav.mulighetsrommet.api.utbetaling

import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.avtale.mapper.satser
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingAvtale
import no.nav.mulighetsrommet.api.utbetaling.model.DeltakelsePeriode
import no.nav.mulighetsrommet.api.utbetaling.model.Deltaker
import no.nav.mulighetsrommet.api.utbetaling.model.SatsPeriode
import no.nav.mulighetsrommet.api.utbetaling.model.StengtPeriode
import no.nav.mulighetsrommet.model.DeltakerStatusType
import no.nav.mulighetsrommet.model.Periode
import java.time.LocalDate

object UtbetalingInputHelper {
    fun QueryContext.resolveAvtaltPrisPerTimeOppfolgingPerDeltaker(
        gjennomforing: GjennomforingAvtale,
        periode: Periode,
    ): AvtaltPrisPerTimeOppfolgingPerDeltaker {
        val satser = resolveAvtalteSatser(gjennomforing, periode)
        val stengtHosArrangor = resolveStengtHosArrangor(periode, gjennomforing.stengt)
        val deltakere = queries.deltaker.getByGjennomforingId(gjennomforing.id)
        val deltakelsePerioder = resolveDeltakelsePerioder(deltakere, periode)
        return AvtaltPrisPerTimeOppfolgingPerDeltaker(
            satser,
            stengtHosArrangor,
            deltakere,
            deltakelsePerioder,
        )
    }

    data class AvtaltPrisPerTimeOppfolgingPerDeltaker(
        val satser: Set<SatsPeriode>,
        val stengtHosArrangor: Set<StengtPeriode>,
        val deltakere: List<Deltaker>,
        val deltakelsePerioder: Set<DeltakelsePeriode>,
    )

    fun resolveAvtalteSatser(gjennomforing: GjennomforingAvtale, periode: Periode): Set<SatsPeriode> {
        val periodeStart = if (gjennomforing.startDato.isBefore(periode.slutt)) {
            maxOf(gjennomforing.startDato, periode.start)
        } else {
            periode.start
        }
        val avtaltSatsPeriode = Periode(periodeStart, periode.slutt)

        return requireNotNull(gjennomforing.prismodell) { "Gjennomføringen mangler prismodell" }
            .satser()
            .sortedBy { it.gjelderFra }
            .windowed(size = 2, partialWindows = true)
            .mapNotNull { satser ->
                val current = satser[0]

                val start = maxOf(current.gjelderFra, avtaltSatsPeriode.start)
                val slutt = if (satser.size == 2) {
                    minOf(satser[1].gjelderFra, avtaltSatsPeriode.slutt)
                } else {
                    avtaltSatsPeriode.slutt
                }

                if (slutt.isAfter(start)) {
                    SatsPeriode(Periode(start, slutt), current.sats)
                } else {
                    null
                }
            }
            .toSet()
    }

    fun resolveDeltakelsePerioder(
        deltakere: List<Deltaker>,
        periode: Periode,
    ): Set<DeltakelsePeriode> {
        return deltakere
            .asSequence()
            .mapNotNull { deltaker ->
                toDeltakelsePeriode(deltaker, periode)
            }
            .toSet()
    }

    fun toDeltakelsePeriode(
        deltaker: Deltaker,
        periode: Periode,
    ): DeltakelsePeriode? {
        if (!harDeltakerDeltatt(deltaker)) {
            return null
        }

        val startDato = requireNotNull(deltaker.startDato) {
            "Deltaker må ha en startdato når status er ${deltaker.status.type} og den er relevant for utbetaling"
        }
        val sluttDatoInPeriode = getSluttDatoInPeriode(deltaker, periode)
        val overlappingPeriode = Periode.of(startDato, sluttDatoInPeriode)?.intersect(periode) ?: return null
        return DeltakelsePeriode(deltaker.id, overlappingPeriode)
    }

    fun harDeltakerDeltatt(deltaker: Deltaker): Boolean {
        if (deltaker.status.type == DeltakerStatusType.DELTAR) {
            return true
        }

        val avsluttendeStatus = listOf(
            DeltakerStatusType.AVBRUTT,
            DeltakerStatusType.FULLFORT,
            DeltakerStatusType.HAR_SLUTTET,
        )
        return deltaker.status.type in avsluttendeStatus && deltaker.sluttDato != null
    }

    fun getSluttDatoInPeriode(deltaker: Deltaker, periode: Periode): LocalDate {
        return deltaker.sluttDato?.plusDays(1)?.coerceAtMost(periode.slutt) ?: periode.slutt
    }

    fun resolveStengtHosArrangor(
        periode: Periode,
        stengtPerioder: List<GjennomforingAvtale.StengtPeriode>,
    ): Set<StengtPeriode> {
        return stengtPerioder
            .mapNotNull { stengt ->
                Periode.fromInclusiveDates(stengt.start, stengt.slutt).intersect(periode)?.let {
                    StengtPeriode(Periode(it.start, it.slutt), stengt.beskrivelse)
                }
            }
            .toSet()
    }
}
