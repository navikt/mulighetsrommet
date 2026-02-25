package no.nav.mulighetsrommet.api.utbetaling

import no.nav.amt.model.AmtArrangorMelding
import no.nav.mulighetsrommet.api.utbetaling.db.DeltakerForslag
import no.nav.mulighetsrommet.api.utbetaling.model.Deltaker
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregning
import no.nav.mulighetsrommet.model.DeltakerStatusType
import no.nav.mulighetsrommet.model.Periode
import java.time.LocalDate
import java.util.UUID
import kotlin.collections.component1
import kotlin.collections.component2

object UtbetalingAdvarsler {
    fun relevanteForslag(periode: Periode, beregning: UtbetalingBeregning, forslag: Map<UUID, List<DeltakerForslag>>): List<DeltakerAdvarsel> {
        return forslag
            .mapNotNull { (deltakerId, forslag) ->
                when (forslag.count { isForslagRelevantForUtbetaling(it, periode, beregning) }) {
                    0 -> null
                    else -> DeltakerAdvarsel(deltakerId, type = DeltakerAdvarselType.RelevanteForslag)
                }
            }
    }

    fun getAdvarsler(
        utbetaling: Utbetaling,
        deltakere: List<Deltaker>,
        forslag: Map<UUID, List<DeltakerForslag>>,
    ): List<DeltakerAdvarsel> {
        return relevanteForslag(utbetaling.periode, utbetaling.beregning, forslag) + deltakereMedFeilSluttDato(deltakere, LocalDate.now())
    }

    fun isForslagRelevantForUtbetaling(
        forslag: DeltakerForslag,
        utbetalingPeriode: Periode,
        beregning: UtbetalingBeregning,
    ): Boolean {
        val deltakelsePeriode = beregning.input.deltakelser()
            .find { it.deltakelseId == forslag.deltakerId }
            ?.periode()
            ?: return false
        return isForslagRelevantForPeriode(
            forslag,
            utbetalingPeriode = utbetalingPeriode,
            deltakelsePeriode = deltakelsePeriode,
        )
    }

    fun isForslagRelevantForPeriode(
        forslag: DeltakerForslag,
        utbetalingPeriode: Periode,
        deltakelsePeriode: Periode,
    ): Boolean {
        val deltakerPeriodeSluttDato = deltakelsePeriode.getLastInclusiveDate()

        return when (forslag.endring) {
            is AmtArrangorMelding.Forslag.Endring.AvsluttDeltakelse -> {
                val sluttDato = forslag.endring.sluttdato
                forslag.endring.harDeltatt == false || (sluttDato != null && sluttDato.isBefore(deltakerPeriodeSluttDato))
            }

            is AmtArrangorMelding.Forslag.Endring.Deltakelsesmengde -> {
                forslag.endring.gyldigFra?.isBefore(deltakerPeriodeSluttDato) ?: true
            }

            is AmtArrangorMelding.Forslag.Endring.ForlengDeltakelse -> {
                val sluttdato = forslag.endring.sluttdato
                sluttdato.isAfter(deltakerPeriodeSluttDato) && sluttdato.isBefore(utbetalingPeriode.slutt)
            }

            is AmtArrangorMelding.Forslag.Endring.Sluttdato -> {
                forslag.endring.sluttdato.isBefore(deltakerPeriodeSluttDato)
            }

            is AmtArrangorMelding.Forslag.Endring.Startdato -> {
                forslag.endring.startdato.isAfter(deltakelsePeriode.start)
            }

            is AmtArrangorMelding.Forslag.Endring.Sluttarsak -> false

            is AmtArrangorMelding.Forslag.Endring.IkkeAktuell,
            is AmtArrangorMelding.Forslag.Endring.FjernOppstartsdato,
            is AmtArrangorMelding.Forslag.Endring.EndreAvslutning,
            -> true
        }
    }

    fun deltakereMedFeilSluttDato(
        deltakere: List<Deltaker>,
        today: LocalDate,
    ): List<DeltakerAdvarsel> {
        return deltakere.mapNotNull {
            it.id.takeIf { _ -> harFeilSluttDato(it.status.type, it.sluttDato, today = today) }?.let {
                DeltakerAdvarsel(it, type = DeltakerAdvarselType.FeilSluttDato)
            }
        }
    }

    fun harFeilSluttDato(
        deltakerStatusType: DeltakerStatusType,
        sluttDato: LocalDate?,
        today: LocalDate,
    ): Boolean {
        return deltakerStatusType in listOf(
            DeltakerStatusType.AVBRUTT,
            DeltakerStatusType.FULLFORT,
            DeltakerStatusType.HAR_SLUTTET,
        ) &&
            (sluttDato == null || sluttDato.isAfter(today))
    }
}

data class DeltakerAdvarsel(
    val deltakerId: UUID,
    val type: DeltakerAdvarselType,
)

enum class DeltakerAdvarselType {
    RelevanteForslag,
    FeilSluttDato,
}
