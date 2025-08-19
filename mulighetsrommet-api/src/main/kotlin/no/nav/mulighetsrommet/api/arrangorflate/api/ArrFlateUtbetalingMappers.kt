package no.nav.mulighetsrommet.api.arrangorflate.api

import no.nav.mulighetsrommet.api.utbetaling.Person
import no.nav.mulighetsrommet.api.utbetaling.api.ArrangorUtbetalingLinje
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingType
import no.nav.mulighetsrommet.api.utbetaling.model.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

fun mapUtbetalingToArrFlateUtbetaling(
    utbetaling: Utbetaling,
    status: ArrFlateUtbetalingStatus,
    deltakerPersoner: Map<UUID, Pair<Deltaker, Person?>>,
    advarsler: List<DeltakerAdvarsel>,
    linjer: List<ArrangorUtbetalingLinje>,
    kanViseBeregning: Boolean,
): ArrFlateUtbetaling {
    val perioderById = utbetaling.beregning.input.deltakelser().associateBy { it.deltakelseId }
    val ukesverkById = utbetaling.beregning.output.deltakelser().associateBy { it.deltakelseId }

    val deltakelser = perioderById.map { (id, deltakelse) ->
        val (deltaker, person) = deltakerPersoner[id] ?: (null to null)
        toArrFlateBeregningDeltakelse(
            deltakelse,
            ukesverkById.getValue(id),
            deltaker,
            person,
        )
    }.sortedWith(compareBy(nullsLast()) { it.person?.navn })

    val totalFaktor = utbetaling.beregning.output.deltakelser()
        .map { BigDecimal(it.faktor) }
        .sumOf { it }
        .setScale(UtbetalingBeregningHelpers.OUTPUT_PRECISION, RoundingMode.HALF_UP)
        .toDouble()

    val beregning = when (val beregning = utbetaling.beregning) {
        is UtbetalingBeregningFri -> {
            ArrFlateBeregning.Fri(
                belop = beregning.output.belop,
                digest = beregning.getDigest(),
            )
        }

        is UtbetalingBeregningPrisPerManedsverkMedDeltakelsesmengder -> {
            ArrFlateBeregning.PrisPerManedsverkMedDeltakelsesmengder(
                stengt = beregning.input.stengt.toList().sortedBy { it.periode.start },
                antallManedsverk = totalFaktor,
                belop = beregning.output.belop,
                digest = beregning.getDigest(),
                deltakelser = deltakelser,
                sats = beregning.input.sats,
            )
        }

        is UtbetalingBeregningPrisPerManedsverk -> {
            ArrFlateBeregning.PrisPerManedsverk(
                belop = beregning.output.belop,
                digest = beregning.getDigest(),
                deltakelser = deltakelser,
                stengt = beregning.input.stengt.toList().sortedBy { it.periode.start },
                antallManedsverk = totalFaktor,
                sats = beregning.input.sats,
            )
        }

        is UtbetalingBeregningPrisPerUkesverk -> {
            ArrFlateBeregning.PrisPerUkesverk(
                belop = beregning.output.belop,
                digest = beregning.getDigest(),
                deltakelser = deltakelser,
                stengt = beregning.input.stengt.toList().sortedBy { it.periode.start },
                antallUkesverk = totalFaktor,
                sats = beregning.input.sats,
            )
        }
    }

    return ArrFlateUtbetaling(
        id = utbetaling.id,
        status = status,
        godkjentAvArrangorTidspunkt = utbetaling.godkjentAvArrangorTidspunkt,
        kanViseBeregning = kanViseBeregning,
        createdAt = utbetaling.createdAt,
        tiltakstype = ArrangorflateTiltakstype(
            navn = utbetaling.tiltakstype.navn,
            tiltakskode = utbetaling.tiltakstype.tiltakskode,
        ),
        gjennomforing = ArrangorflateGjennomforingInfo(
            id = utbetaling.gjennomforing.id,
            navn = utbetaling.gjennomforing.navn,
        ),
        arrangor = ArrangorflateArrangor(
            id = utbetaling.arrangor.id,
            organisasjonsnummer = utbetaling.arrangor.organisasjonsnummer,
            navn = utbetaling.arrangor.navn,
        ),
        periode = utbetaling.periode,
        beregning = beregning,
        betalingsinformasjon = utbetaling.betalingsinformasjon,
        type = UtbetalingType.from(utbetaling),
        linjer = linjer,
        advarsler = advarsler,
    )
}

fun toArrFlateBeregningDeltakelse(
    input: UtbetalingBeregningInputDeltakelse,
    output: UtbetalingBeregningOutputDeltakelse,
    deltaker: Deltaker?,
    person: Person?,
): ArrFlateBeregningDeltakelse {
    return when (output) {
        is DeltakelseUkesverk -> ArrFlateBeregningDeltakelse.PrisPerUkesverk(
            id = output.deltakelseId,
            deltakerStartDato = deltaker?.startDato,
            person = person?.let { ArrFlateBeregningDeltakelse.ArrFlatePerson.fromPerson(it) },
            periode = input.periode(),
            faktor = output.faktor,
            status = deltaker?.status?.type,
        )

        is DeltakelseManedsverk -> when (input) {
            is DeltakelsePeriode ->
                ArrFlateBeregningDeltakelse.PrisPerManedsverk(
                    id = output.deltakelseId,
                    deltakerStartDato = deltaker?.startDato,
                    person = person?.let { ArrFlateBeregningDeltakelse.ArrFlatePerson.fromPerson(it) },
                    periode = input.periode(),
                    faktor = output.faktor,
                    status = deltaker?.status?.type,
                )

            is DeltakelseDeltakelsesprosentPerioder ->
                ArrFlateBeregningDeltakelse.PrisPerManedsverkMedDeltakelsesmengder(
                    id = output.deltakelseId,
                    deltakerStartDato = deltaker?.startDato,
                    person = person?.let { ArrFlateBeregningDeltakelse.ArrFlatePerson.fromPerson(it) },
                    periode = input.periode(),
                    faktor = output.faktor,
                    perioderMedDeltakelsesmengde = input.perioder,
                    status = deltaker?.status?.type,
                )
        }
    }
}
