package no.nav.mulighetsrommet.api.utbetaling.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.domain.deltaker.Deltaker
import no.nav.mulighetsrommet.api.domain.deltaker.DeltakerForslag
import no.nav.mulighetsrommet.model.DeltakerStatusType
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDate
import java.util.UUID

object UtbetalingAdvarsler {
    fun relevanteForslag(
        periode: Periode,
        beregning: UtbetalingBeregning,
        forslag: List<DeltakerForslag>,
    ): List<DeltakerAdvarsel> {
        return forslag
            .groupBy { it.deltakerId }
            .mapNotNull { (deltakerId, forslag) ->
                when (forslag.count { isForslagRelevantForUtbetaling(it, periode, beregning) }) {
                    0 -> null
                    else -> DeltakerAdvarsel(deltakerId, type = DeltakerAdvarselType.RelevanteForslag)
                }
            }
    }

    fun getAdvarsler(
        periode: Periode,
        beregning: UtbetalingBeregning,
        deltakere: List<Deltaker>,
        forslag: List<DeltakerForslag>,
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

    // TODO: er denne sjekken redundant nå? Burde den evt. også persisteres som en blokkering?
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

fun QueryContext.hentDeltakerAdvarslerForUtbetaling(
    status: UtbetalingStatusType,
    gjennomforingId: UUID,
    periode: Periode,
    beregning: UtbetalingBeregning,
): List<DeltakerAdvarsel> {
    val dektakerIds = beregning.input.deltakelser().mapTo(mutableSetOf()) { it.deltakelseId }
    return when (status) {
        UtbetalingStatusType.GENERERT -> {
            val forslag = repository.deltakerForslag.getByGjennomforing(gjennomforingId)
            // TODO: optimalisere denne? Noen AFT/VTA-gjennomføringer har pågått lenge og har dermed mange deltakere som aldri vil være relevante for utbetalingene
            //  Det kan hende at den underliggende sjekken (altså [fun deltakereMedFeilSluttDato]) ikke lengre er nødvendig å ta høyde for siden dette burde være fikset i datagrunnlaget
            val deltakere = repository.deltaker.getByGjennomforing(gjennomforingId).filter { it.id in dektakerIds }
            UtbetalingAdvarsler.getAdvarsler(periode, beregning, deltakere, forslag)
        }

        UtbetalingStatusType.TIL_BEHANDLING,
        UtbetalingStatusType.TIL_ATTESTERING,
        UtbetalingStatusType.RETURNERT,
        UtbetalingStatusType.FERDIG_BEHANDLET,
        UtbetalingStatusType.DELVIS_UTBETALT,
        UtbetalingStatusType.UTBETALT,
        UtbetalingStatusType.AVBRUTT,
        -> emptyList()
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
