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
    erTolvUkerEtterInnsending: Boolean,
): ArrFlateUtbetaling {
    val beregning = when (val beregning = utbetaling.beregning) {
        is UtbetalingBeregningFri -> ArrFlateBeregning.Fri(
            belop = beregning.output.belop,
            digest = beregning.getDigest(),
        )

        // TODO: forenkle mapping av beregninger som inkluderer deltakelser
        is UtbetalingBeregningPrisPerManedsverkMedDeltakelsesmengder -> {
            val deltakereById = deltakere.associateBy { it.id }
            val perioderById = beregning.input.deltakelser.associateBy { it.deltakelseId }
            val manedsverkById = beregning.output.deltakelser.associateBy { it.deltakelseId }

            val deltakelser = perioderById.map { (id, deltakelse) ->
                val deltaker = deltakereById[id]
                val manedsverk = manedsverkById.getValue(id).manedsverk
                val person = if (erTolvUkerEtterInnsending) {
                    null
                } else {
                    deltaker?.norskIdent?.let { personerByNorskIdent[it] }
                }

                val forstePeriode = deltakelse.perioder.first()
                val sistePeriode = deltakelse.perioder.last()

                ArrFlateUtbetalingDeltakelse(
                    id = id,
                    deltakerStartDato = deltaker?.startDato,
                    periodeStartDato = forstePeriode.periode.start,
                    periodeSluttDato = sistePeriode.periode.getLastInclusiveDate(),
                    faktor = manedsverk,
                    perioderMedDeltakelsesmengde = deltakelse.perioder,
                    person = person,
                )
            }.sortedWith(compareBy(nullsLast()) { it.person?.navn })

            val antallManedsverk = deltakelser
                .map { BigDecimal(it.faktor) }
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

                ArrFlateUtbetalingDeltakelse(
                    id = id,
                    deltakerStartDato = deltaker?.startDato,
                    periodeStartDato = deltakelse.periode.start,
                    periodeSluttDato = deltakelse.periode.getLastInclusiveDate(),
                    faktor = manedsverk,
                    // TODO: deltakelsesmengder er egentlig ikke relevant for denne beregningen
                    perioderMedDeltakelsesmengde = listOf(DeltakelsesprosentPeriode(deltakelse.periode, 100.0)),
                    person = person,
                )
            }.sortedWith(compareBy(nullsLast()) { it.person?.navn })

            val antallUkesverk = deltakelser
                .map { BigDecimal(it.faktor) }
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

                ArrFlateUtbetalingDeltakelse(
                    id = id,
                    deltakerStartDato = deltaker?.startDato,
                    periodeStartDato = deltakelse.periode.start,
                    periodeSluttDato = deltakelse.periode.getLastInclusiveDate(),
                    faktor = ukesverk,
                    // TODO: deltakelsesmengder er egentlig ikke relevant for denne beregningen
                    perioderMedDeltakelsesmengde = listOf(DeltakelsesprosentPeriode(deltakelse.periode, 100.0)),
                    person = person,
                )
            }.sortedWith(compareBy(nullsLast()) { it.person?.navn })

            val antallUkesverk = deltakelser
                .map { BigDecimal(it.faktor) }
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
        erTolvUkerEtterInnsending = erTolvUkerEtterInnsending,
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
