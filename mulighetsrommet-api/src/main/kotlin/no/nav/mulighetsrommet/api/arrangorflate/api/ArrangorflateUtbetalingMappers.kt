package no.nav.mulighetsrommet.api.arrangorflate.api

import no.nav.mulighetsrommet.api.utbetaling.Person
import no.nav.mulighetsrommet.api.utbetaling.api.ArrangorUtbetalingLinje
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingType
import no.nav.mulighetsrommet.api.utbetaling.api.toDto
import no.nav.mulighetsrommet.api.utbetaling.model.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

fun mapUtbetalingToArrangorflateUtbetaling(
    utbetaling: Utbetaling,
    status: ArrangorflateUtbetalingStatus,
    deltakerPersoner: Map<UUID, Pair<Deltaker, Person?>>,
    advarsler: List<DeltakerAdvarsel>,
    linjer: List<ArrangorUtbetalingLinje>,
    kanViseBeregning: Boolean,
): ArrangorflateUtbetalingDto {
    val perioderById = utbetaling.beregning.input.deltakelser().associateBy { it.deltakelseId }
    val ukesverkById = utbetaling.beregning.output.deltakelser().associateBy { it.deltakelseId }

    val deltakelser = perioderById.map { (id, deltakelse) ->
        val (deltaker, person) = deltakerPersoner[id] ?: (null to null)
        toArrangorflateBeregningDeltakelse(
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
            ArrangorflateBeregning.Fri(
                belop = beregning.output.belop,
                digest = beregning.getDigest(),
            )
        }

        is UtbetalingBeregningFastSatsPerTiltaksplassPerManed -> {
            ArrangorflateBeregning.FastSatsPerTiltaksplassPerManed(
                stengt = beregning.input.stengt.toList().sortedBy { it.periode.start },
                antallManedsverk = totalFaktor,
                belop = beregning.output.belop,
                digest = beregning.getDigest(),
                deltakelser = deltakelser,
                sats = beregning.input.sats,
            )
        }

        is UtbetalingBeregningPrisPerManedsverk -> {
            ArrangorflateBeregning.PrisPerManedsverk(
                belop = beregning.output.belop,
                digest = beregning.getDigest(),
                deltakelser = deltakelser,
                stengt = beregning.input.stengt.toList().sortedBy { it.periode.start },
                antallManedsverk = totalFaktor,
                sats = beregning.input.sats,
            )
        }

        is UtbetalingBeregningPrisPerUkesverk -> {
            ArrangorflateBeregning.PrisPerUkesverk(
                belop = beregning.output.belop,
                digest = beregning.getDigest(),
                deltakelser = deltakelser,
                stengt = beregning.input.stengt.toList().sortedBy { it.periode.start },
                antallUkesverk = totalFaktor,
                sats = beregning.input.sats,
            )
        }
    }

    return ArrangorflateUtbetalingDto(
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
        type = UtbetalingType.from(utbetaling).toDto(),
        linjer = linjer,
        advarsler = advarsler,
    )
}

fun toArrangorflateBeregningDeltakelse(
    input: UtbetalingBeregningInputDeltakelse,
    output: UtbetalingBeregningOutputDeltakelse,
    deltaker: Deltaker?,
    person: Person?,
): ArrangorflateBeregningDeltakelse {
    return when (output) {
        is DeltakelseUkesverk -> ArrangorflateBeregningDeltakelse.PrisPerUkesverk(
            id = output.deltakelseId,
            deltakerStartDato = deltaker?.startDato,
            person = person?.let { ArrangorflatePerson.fromPerson(it) },
            periode = input.periode(),
            faktor = output.faktor,
            status = deltaker?.status?.type,
        )

        is DeltakelseManedsverk -> when (input) {
            is DeltakelsePeriode ->
                ArrangorflateBeregningDeltakelse.PrisPerManedsverk(
                    id = output.deltakelseId,
                    deltakerStartDato = deltaker?.startDato,
                    person = person?.let { ArrangorflatePerson.fromPerson(it) },
                    periode = input.periode(),
                    faktor = output.faktor,
                    status = deltaker?.status?.type,
                )

            is DeltakelseDeltakelsesprosentPerioder ->
                ArrangorflateBeregningDeltakelse.FastSatsPerTiltaksplassPerManed(
                    id = output.deltakelseId,
                    deltakerStartDato = deltaker?.startDato,
                    person = person?.let { ArrangorflatePerson.fromPerson(it) },
                    periode = input.periode(),
                    faktor = output.faktor,
                    perioderMedDeltakelsesmengde = input.perioder,
                    status = deltaker?.status?.type,
                )
        }
    }
}
