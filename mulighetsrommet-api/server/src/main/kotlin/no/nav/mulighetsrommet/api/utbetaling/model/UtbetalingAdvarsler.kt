package no.nav.mulighetsrommet.api.utbetaling.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.arrangorflate.model.ArrangorflateUtbetaling
import no.nav.mulighetsrommet.api.domain.deltaker.Deltaker
import no.nav.mulighetsrommet.api.domain.deltaker.DeltakerForslag
import no.nav.mulighetsrommet.model.DeltakerStatusType
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.serializers.UUIDSerializer
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
        return getUtbetalingAdvarsel(utbetaling.periode, utbetaling.beregning, deltakere, forslag)
    }

    fun getAdvarsler(
        utbetaling: ArrangorflateUtbetaling,
        deltakere: List<Deltaker>,
        forslag: Map<UUID, List<DeltakerForslag>>,
    ): List<DeltakerAdvarsel> {
        return getUtbetalingAdvarsel(utbetaling.periode, utbetaling.beregning, deltakere, forslag)
    }

    private fun getUtbetalingAdvarsel(
        periode: Periode,
        beregning: UtbetalingBeregning,
        deltakere: List<Deltaker>,
        forslag: Map<UUID, List<DeltakerForslag>>,
    ): List<DeltakerAdvarsel> {
        return relevanteForslag(periode, beregning, forslag) + deltakereMedFeilSluttDato(deltakere, LocalDate.now())
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
            forslag.endring,
            utbetalingPeriode = utbetalingPeriode,
            deltakelsePeriode = deltakelsePeriode,
        )
    }

    fun isForslagRelevantForPeriode(
        endring: DeltakerForslag.Endring,
        utbetalingPeriode: Periode,
        deltakelsePeriode: Periode,
    ): Boolean {
        val deltakelseInclusiveSluttdato = deltakelsePeriode.getLastInclusiveDate()

        return when (endring) {
            is DeltakerForslag.Endring.AvsluttDeltakelse -> {
                val sluttDato = endring.sluttdato
                endring.harDeltatt == false || (sluttDato != null && sluttdatoEndringErRelevant(sluttDato, utbetalingPeriode, deltakelsePeriode))
            }

            is DeltakerForslag.Endring.EndreAvslutning -> {
                val sluttdato = endring.sluttdato
                endring.harDeltatt == false || (sluttdato != null && sluttdatoEndringErRelevant(sluttdato, utbetalingPeriode, deltakelsePeriode))
            }

            is DeltakerForslag.Endring.Deltakelsesmengde -> {
                endring.gyldigFra?.isBefore(deltakelseInclusiveSluttdato) ?: true
            }

            is DeltakerForslag.Endring.ForlengDeltakelse -> {
                sluttdatoEndringErRelevant(endring.sluttdato, utbetalingPeriode, deltakelsePeriode)
            }

            is DeltakerForslag.Endring.Sluttdato -> {
                sluttdatoEndringErRelevant(endring.sluttdato, utbetalingPeriode, deltakelsePeriode)
            }

            is DeltakerForslag.Endring.Startdato -> {
                val sluttdato = endring.sluttdato
                endring.startdato.isAfter(deltakelsePeriode.start) ||
                    (sluttdato != null && sluttdatoEndringErRelevant(sluttdato, utbetalingPeriode, deltakelsePeriode))
            }

            is DeltakerForslag.Endring.Sluttarsak -> false

            is DeltakerForslag.Endring.IkkeAktuell,
            is DeltakerForslag.Endring.FjernOppstartsdato,
            -> true
        }
    }

    fun sluttdatoEndringErRelevant(
        sluttdato: LocalDate,
        utbetalingPeriode: Periode,
        deltakelsePeriode: Periode,
    ): Boolean {
        return sluttdato.isBefore(deltakelsePeriode.getLastInclusiveDate()) ||
            (sluttdato.isAfter(deltakelsePeriode.getLastInclusiveDate()) && deltakelsePeriode.slutt.isBefore(utbetalingPeriode.slutt))
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

@Serializable
data class DeltakerAdvarselDto(
    @Serializable(with = UUIDSerializer::class)
    val deltakerId: UUID,
    val beskrivelse: String,
    val type: DeltakerAdvarselType,
) {
    companion object {
        fun from(advarsel: DeltakerAdvarsel, navn: String?) = DeltakerAdvarselDto(
            deltakerId = advarsel.deltakerId,
            beskrivelse = when (advarsel.type) {
                DeltakerAdvarselType.RelevanteForslag -> "$navn har ubehandlede forslag. Disse må først godkjennes av Nav-veileder før utbetalingen oppdaterer seg"
                DeltakerAdvarselType.FeilSluttDato -> "$navn har avsluttende status og sluttdato frem i tid"
            },
            type = advarsel.type,
        )
    }
}
