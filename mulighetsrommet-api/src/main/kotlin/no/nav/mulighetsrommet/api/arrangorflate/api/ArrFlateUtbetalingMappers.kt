package no.nav.mulighetsrommet.api.arrangorflate.api

import no.nav.mulighetsrommet.api.utbetaling.api.ArrangorUtbetalingLinje
import no.nav.mulighetsrommet.api.utbetaling.model.Deltaker
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningForhandsgodkjent
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFri
import no.nav.mulighetsrommet.model.NorskIdent
import java.math.BigDecimal
import java.math.RoundingMode

fun mapUtbetalingToArrFlateUtbetaling(
    utbetaling: Utbetaling,
    status: ArrFlateUtbetalingStatus,
    deltakere: List<Deltaker>,
    personerByNorskIdent: Map<NorskIdent, UtbetalingDeltakelse.Person>,
    linjer: List<ArrangorUtbetalingLinje>,
): ArrFlateUtbetaling {
    return when (val beregning = utbetaling.beregning) {
        is UtbetalingBeregningForhandsgodkjent -> {
            val deltakereById = deltakere.associateBy { it.id }
            val perioderById = beregning.input.deltakelser.associateBy { it.deltakelseId }
            val manedsverkById = beregning.output.deltakelser.associateBy { it.deltakelseId }

            val deltakelser = perioderById.map { (id, deltakelse) ->
                val deltaker = deltakereById[id]
                val manedsverk = manedsverkById.getValue(id).manedsverk
                val person = deltaker?.norskIdent?.let { personerByNorskIdent[it] }

                val forstePeriode = deltakelse.perioder.first()
                val sistePeriode = deltakelse.perioder.last()

                UtbetalingDeltakelse(
                    id = id,
                    startDato = deltaker?.startDato,
                    sluttDato = deltaker?.startDato,
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

            ArrFlateUtbetaling(
                id = utbetaling.id,
                status = status,
                godkjentAvArrangorTidspunkt = utbetaling.godkjentAvArrangorTidspunkt,
                createdAt = utbetaling.createdAt,
                tiltakstype = utbetaling.tiltakstype,
                gjennomforing = utbetaling.gjennomforing,
                arrangor = utbetaling.arrangor,
                periode = utbetaling.periode,
                beregning = Beregning.Forhandsgodkjent(
                    stengt = beregning.input.stengt.toList().sortedBy { it.periode.start },
                    antallManedsverk = antallManedsverk,
                    belop = beregning.output.belop,
                    digest = beregning.getDigest(),
                    deltakelser = deltakelser,
                ),
                betalingsinformasjon = utbetaling.betalingsinformasjon,
                linjer = linjer,
            )
        }

        is UtbetalingBeregningFri -> ArrFlateUtbetaling(
            id = utbetaling.id,
            status = status,
            godkjentAvArrangorTidspunkt = utbetaling.godkjentAvArrangorTidspunkt,
            createdAt = utbetaling.createdAt,
            tiltakstype = utbetaling.tiltakstype,
            gjennomforing = utbetaling.gjennomforing,
            arrangor = utbetaling.arrangor,
            periode = utbetaling.periode,
            beregning = Beregning.Fri(
                belop = beregning.output.belop,
                digest = beregning.getDigest(),
            ),
            betalingsinformasjon = utbetaling.betalingsinformasjon,
            linjer = linjer,
        )
    }
}
