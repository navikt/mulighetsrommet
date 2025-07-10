package no.nav.mulighetsrommet.api.arrangorflate.api

import no.nav.mulighetsrommet.api.utbetaling.api.ArrangorUtbetalingLinje
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingType
import no.nav.mulighetsrommet.api.utbetaling.model.*
import no.nav.mulighetsrommet.model.NorskIdent
import java.math.BigDecimal
import java.math.RoundingMode

fun mapUtbetalingToArrFlateUtbetaling(
    utbetaling: Utbetaling,
    status: ArrFlateUtbetalingStatus,
    deltakere: List<Deltaker>,
    personerByNorskIdent: Map<NorskIdent, UtbetalingDeltakelsePerson>,
    linjer: List<ArrangorUtbetalingLinje>,
): ArrFlateUtbetaling {
    val beregning = when (val beregning = utbetaling.beregning) {
        is UtbetalingBeregningFri -> ArrFlateBeregning.Fri(
            belop = beregning.output.belop,
            digest = beregning.getDigest(),
        )

        is UtbetalingBeregningPrisPerManedsverkMedDeltakelsesmengder -> {
            val deltakereById = deltakere.associateBy { it.id }
            val perioderById = beregning.input.deltakelser.associateBy { it.deltakelseId }
            val manedsverkById = beregning.output.deltakelser.associateBy { it.deltakelseId }

            val deltakelser = perioderById.map { (id, deltakelse) ->
                val deltaker = deltakereById[id]
                val manedsverk = manedsverkById.getValue(id).manedsverk
                val person = deltaker?.norskIdent?.let { personerByNorskIdent[it] }

                val forstePeriode = deltakelse.perioder.first()
                val sistePeriode = deltakelse.perioder.last()

                UtbetalingDeltakelseManedsverk(
                    id = id,
                    startDato = deltaker?.startDato,
                    forstePeriodeStartDato = forstePeriode.periode.start,
                    sistePeriodeSluttDato = sistePeriode.periode.getLastInclusiveDate(),
                    sistePeriodeDeltakelsesprosent = sistePeriode.deltakelsesprosent,
                    manedsverk = manedsverk,
                    perioder = deltakelse.perioder,
                    person = person,
                )
            }.sortedWith(compareBy(nullsLast()) { it.person?.navn })

            val antallManedsverk = deltakelser
                .map { BigDecimal(it.manedsverk) }
                .sumOf { it }
                .setScale(2, RoundingMode.HALF_UP)
                .toDouble()

            ArrFlateBeregning.PrisPerManedsverkMedDeltakelsesmengder(
                stengt = beregning.input.stengt.toList().sortedBy { it.periode.start },
                antallManedsverk = antallManedsverk,
                belop = beregning.output.belop,
                digest = beregning.getDigest(),
                deltakelser = deltakelser,
            )
        }

        is UtbetalingBeregningPrisPerManedsverk -> {
            val deltakereById = deltakere.associateBy { it.id }
            val perioderById = beregning.input.deltakelser.associateBy { it.deltakelseId }
            val ukesverkById = beregning.output.deltakelser.associateBy { it.deltakelseId }

            val deltakelser = perioderById.map { (id, deltakelse) ->
                val deltaker = deltakereById[id]
                val manedsverk = ukesverkById.getValue(id).manedsverk
                val person = deltaker?.norskIdent?.let { personerByNorskIdent[it] }

                UtbetalingDeltakelseManedsverk2(
                    id = id,
                    deltakerStartDato = deltaker?.startDato,
                    periodeStartDato = deltakelse.periode.start,
                    periodeSluttDato = deltakelse.periode.getLastInclusiveDate(),
                    manedsverk = manedsverk,
                    person = person,
                )
            }.sortedWith(compareBy(nullsLast()) { it.person?.navn })

            val antallUkesverk = deltakelser
                .map { BigDecimal(it.manedsverk) }
                .sumOf { it }
                .setScale(2, RoundingMode.HALF_UP)
                .toDouble()

            ArrFlateBeregning.PrisPerManedsverk(
                belop = beregning.output.belop,
                digest = beregning.getDigest(),
                deltakelser = deltakelser,
                stengt = beregning.input.stengt.toList().sortedBy { it.periode.start },
                antallManedsverk = antallUkesverk,
            )
        }

        is UtbetalingBeregningPrisPerUkesverk -> {
            val deltakereById = deltakere.associateBy { it.id }
            val perioderById = beregning.input.deltakelser.associateBy { it.deltakelseId }
            val ukesverkById = beregning.output.deltakelser.associateBy { it.deltakelseId }

            val deltakelser = perioderById.map { (id, deltakelse) ->
                val deltaker = deltakereById[id]
                val ukesverk = ukesverkById.getValue(id).ukesverk
                val person = deltaker?.norskIdent?.let { personerByNorskIdent[it] }

                UtbetalingDeltakelseUkesverk(
                    id = id,
                    deltakerStartDato = deltaker?.startDato,
                    periodeStartDato = deltakelse.periode.start,
                    periodeSluttDato = deltakelse.periode.getLastInclusiveDate(),
                    ukesverk = ukesverk,
                    person = person,
                )
            }.sortedWith(compareBy(nullsLast()) { it.person?.navn })

            val antallUkesverk = deltakelser
                .map { BigDecimal(it.ukesverk) }
                .sumOf { it }
                .setScale(2, RoundingMode.HALF_UP)
                .toDouble()

            ArrFlateBeregning.PrisPerUkesverk(
                belop = beregning.output.belop,
                digest = beregning.getDigest(),
                deltakelser = deltakelser,
                stengt = beregning.input.stengt.toList().sortedBy { it.periode.start },
                antallUkesverk = antallUkesverk,
            )
        }
    }

    return ArrFlateUtbetaling(
        id = utbetaling.id,
        status = status,
        godkjentAvArrangorTidspunkt = utbetaling.godkjentAvArrangorTidspunkt,
        createdAt = utbetaling.createdAt,
        tiltakstype = utbetaling.tiltakstype,
        gjennomforing = utbetaling.gjennomforing,
        arrangor = utbetaling.arrangor,
        periode = utbetaling.periode,
        beregning = beregning,
        betalingsinformasjon = utbetaling.betalingsinformasjon,
        type = UtbetalingType.from(utbetaling),
        linjer = linjer,
    )
}
