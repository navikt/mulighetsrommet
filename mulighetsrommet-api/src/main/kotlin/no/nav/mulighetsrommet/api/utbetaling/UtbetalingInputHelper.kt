package no.nav.mulighetsrommet.api.utbetaling

import java.time.LocalDate
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.avtale.model.Avtale
import no.nav.mulighetsrommet.api.gjennomforing.model.Gjennomforing
import no.nav.mulighetsrommet.api.tilsagn.model.AvtalteSatser
import no.nav.mulighetsrommet.api.utbetaling.model.DeltakelsePeriode
import no.nav.mulighetsrommet.api.utbetaling.model.Deltaker
import no.nav.mulighetsrommet.api.utbetaling.model.SatsPeriode
import no.nav.mulighetsrommet.api.utbetaling.model.StengtPeriode
import no.nav.mulighetsrommet.model.DeltakerStatusType
import no.nav.mulighetsrommet.model.Periode

object UtbetalingInputHelper {
    fun QueryContext.resolveAvtaltPrisPerTimeOppfolgingPerDeltaker(
        gjennomforing: Gjennomforing,
        periode: Periode,
    ): AvtaltPrisPerTimeOppfolgingPerDeltaker {
        val sats = resolveAvtaltSats(gjennomforing, periode)
        val stengtHosArrangor = resolveStengtHosArrangor(periode, gjennomforing.stengt)
        val deltakere = queries.deltaker.getAll(gjennomforingId = gjennomforing.id)
        val deltakelsePerioder = resolveDeltakelsePerioder(deltakere, periode)
        // TODO: støtte satsperioder her også
        return AvtaltPrisPerTimeOppfolgingPerDeltaker(sats, stengtHosArrangor, deltakere, deltakelsePerioder)
    }

    data class AvtaltPrisPerTimeOppfolgingPerDeltaker(val sats: Int, val stengtHosArrangor: Set<StengtPeriode>, val deltakere: List<Deltaker>, val deltakelsePerioder: Set<DeltakelsePeriode>)

    private fun QueryContext.resolveAvtaltSats(gjennomforing: Gjennomforing, periode: Periode): Int {
        val avtale = requireNotNull(queries.avtale.get(gjennomforing.avtaleId!!))
        return resolveAvtaltSats(gjennomforing, avtale, periode)
    }

    fun resolveAvtaltSats(gjennomforing: Gjennomforing, avtale: Avtale, periode: Periode): Int {
        val periodeStart = if (gjennomforing.startDato.isBefore(periode.slutt)) {
            maxOf(gjennomforing.startDato, periode.start)
        } else {
            periode.start
        }
        val avtaltSatsPeriode = Periode(periodeStart, periode.slutt)
        return AvtalteSatser.findSats(avtale, avtaltSatsPeriode)
            ?: throw IllegalStateException("Klarte ikke utlede sats for gjennomføring=${gjennomforing.id} og periode=$avtaltSatsPeriode")
    }

    fun resolveAvtalteSatser(gjennomforing: Gjennomforing, avtale: Avtale, periode: Periode): Set<SatsPeriode> {
        val periodeStart = if (gjennomforing.startDato.isBefore(periode.slutt)) {
            maxOf(gjennomforing.startDato, periode.start)
        } else {
            periode.start
        }
        val avtaltSatsPeriode = Periode(periodeStart, periode.slutt)

        return AvtalteSatser.getAvtalteSatser(avtale)
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

    private fun resolveDeltakelsePerioder(
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

    private fun toDeltakelsePeriode(
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

    private fun harDeltakerDeltatt(deltaker: Deltaker): Boolean {
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

    private fun getSluttDatoInPeriode(deltaker: Deltaker, periode: Periode): LocalDate {
        return deltaker.sluttDato?.plusDays(1)?.coerceAtMost(periode.slutt) ?: periode.slutt
    }

    private fun resolveStengtHosArrangor(
        periode: Periode,
        stengtPerioder: List<Gjennomforing.StengtPeriode>,
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
