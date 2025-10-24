package no.nav.mulighetsrommet.api.arrangorflate.api

import no.nav.mulighetsrommet.api.arrangorflate.api.ArrangorflateBeregningDeltakelse.PrisPerUkesverk
import no.nav.mulighetsrommet.api.clients.amtDeltaker.DeltakerPersonalia
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingType
import no.nav.mulighetsrommet.api.utbetaling.api.toDto
import no.nav.mulighetsrommet.api.utbetaling.model.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

fun mapUtbetalingToArrangorflateUtbetaling(
    utbetaling: Utbetaling,
    status: ArrangorflateUtbetalingStatus,
    deltakereById: Map<UUID, Deltaker>,
    personaliaById: Map<UUID, DeltakerPersonalia?>,
    advarsler: List<DeltakerAdvarsel>,
    linjer: List<ArrangforflateUtbetalingLinje>,
    kanViseBeregning: Boolean,
): ArrangorflateUtbetalingDto {
    val totalFaktor = utbetaling.beregning.output.deltakelser()
        .flatMap { deltakelse -> deltakelse.perioder.map { it.faktor } }
        .map { BigDecimal(it) }
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
            val perioderById = utbetaling.beregning.input.deltakelser.associateBy { it.deltakelseId }
            val deltakelseById = utbetaling.beregning.output.deltakelser.associateBy { it.deltakelseId }
            val deltakelser = perioderById
                .map { (id, deltakelse) ->
                    val deltaker = deltakereById[id]
                    val personalia = personaliaById[id]
                    val deltakelseOutput = deltakelseById.getValue(id)
                    ArrangorflateBeregningDeltakelse.FastSatsPerTiltaksplassPerManed(
                        id = deltakelseOutput.deltakelseId,
                        deltakerStartDato = deltaker?.startDato,
                        personalia = personalia?.let { ArrangorflatePersonalia.fromPersonalia(it) },
                        periode = deltakelse.periode(),
                        faktor = deltakelseOutput.perioder.sumOf { it.faktor },
                        perioderMedDeltakelsesmengde = deltakelse.perioder,
                        status = deltaker?.status?.type,
                    )
                }
                .sortedWith(compareBy(nullsLast()) { it.personalia?.navn })
            ArrangorflateBeregning.FastSatsPerTiltaksplassPerManed(
                belop = beregning.output.belop,
                digest = beregning.getDigest(),
                deltakelser = deltakelser,
                stengt = beregning.input.stengt.toList().sortedBy { it.periode.start },
                antallManedsverk = totalFaktor,
                // TODO: støtte flere satser
                sats = beregning.input.satser.first().sats,
            )
        }

        is UtbetalingBeregningPrisPerManedsverk -> {
            val perioderById = utbetaling.beregning.input.deltakelser.associateBy { it.deltakelseId }
            val deltakelseById = utbetaling.beregning.output.deltakelser.associateBy { it.deltakelseId }
            val deltakelser = perioderById
                .map { (id, deltakelse) ->
                    val deltaker = deltakereById[id]
                    val personalia = personaliaById[id]
                    val deltakelseOutput = deltakelseById.getValue(id)
                    ArrangorflateBeregningDeltakelse.PrisPerManedsverk(
                        id = deltakelseOutput.deltakelseId,
                        deltakerStartDato = deltaker?.startDato,
                        personalia = personalia?.let { ArrangorflatePersonalia.fromPersonalia(it) },
                        periode = deltakelse.periode(),
                        faktor = deltakelseOutput.perioder.sumOf { it.faktor },
                        status = deltaker?.status?.type,
                    )
                }
                .sortedWith(compareBy(nullsLast()) { it.personalia?.navn })
            ArrangorflateBeregning.PrisPerManedsverk(
                belop = beregning.output.belop,
                digest = beregning.getDigest(),
                deltakelser = deltakelser,
                stengt = beregning.input.stengt.toList().sortedBy { it.periode.start },
                antallManedsverk = totalFaktor,
                // TODO: støtte flere satser
                sats = beregning.input.satser.first().sats,
            )
        }

        is UtbetalingBeregningPrisPerUkesverk -> {
            val perioderById = utbetaling.beregning.input.deltakelser.associateBy { it.deltakelseId }
            val deltakelseById = utbetaling.beregning.output.deltakelser.associateBy { it.deltakelseId }
            val deltakelser = perioderById
                .map { (id, deltakelse) ->
                    val deltaker = deltakereById[id]
                    val personalia = personaliaById[id]
                    val deltakelseOutput = deltakelseById.getValue(id)
                    PrisPerUkesverk(
                        id = deltakelseOutput.deltakelseId,
                        deltakerStartDato = deltaker?.startDato,
                        personalia = personalia?.let { ArrangorflatePersonalia.fromPersonalia(it) },
                        periode = deltakelse.periode(),
                        faktor = deltakelseOutput.perioder.sumOf { it.faktor },
                        status = deltaker?.status?.type,
                    )
                }
                .sortedWith(compareBy(nullsLast()) { it.personalia?.navn })
            ArrangorflateBeregning.PrisPerUkesverk(
                belop = beregning.output.belop,
                digest = beregning.getDigest(),
                deltakelser = deltakelser,
                stengt = beregning.input.stengt.toList().sortedBy { it.periode.start },
                antallUkesverk = totalFaktor,
                // TODO: støtte flere satser
                sats = beregning.input.satser.first().sats,
            )
        }

        is UtbetalingBeregningPrisPerHeleUkesverk -> {
            val perioderById = utbetaling.beregning.input.deltakelser.associateBy { it.deltakelseId }
            val deltakelseById = utbetaling.beregning.output.deltakelser.associateBy { it.deltakelseId }
            val deltakelser = perioderById
                .map { (id, deltakelse) ->
                    val deltaker = deltakereById[id]
                    val personalia = personaliaById[id]
                    val deltakelseOutput = deltakelseById.getValue(id)
                    PrisPerUkesverk(
                        id = deltakelseOutput.deltakelseId,
                        deltakerStartDato = deltaker?.startDato,
                        personalia = personalia?.let { ArrangorflatePersonalia.fromPersonalia(it) },
                        periode = deltakelse.periode(),
                        faktor = deltakelseOutput.perioder.sumOf { it.faktor },
                        status = deltaker?.status?.type,
                    )
                }
                .sortedWith(compareBy(nullsLast()) { it.personalia?.navn })
            ArrangorflateBeregning.PrisPerUkesverk(
                belop = beregning.output.belop,
                digest = beregning.getDigest(),
                deltakelser = deltakelser,
                stengt = beregning.input.stengt.toList().sortedBy { it.periode.start },
                antallUkesverk = totalFaktor,
                // TODO: støtte flere satser
                sats = beregning.input.satser.first().sats,
            )
        }

        is UtbetalingBeregningPrisPerTimeOppfolging -> {
            val perioderById = utbetaling.beregning.input.deltakelser().associateBy { it.deltakelseId }
            val deltakerlser = perioderById
                .map { (id, deltakelse) ->
                    val deltaker = deltakereById[id]
                    val personalia = personaliaById[id]
                    ArrangorflateBeregningDeltakelse.PrisPerTimeOppfolging(
                        id = id,
                        deltakerStartDato = deltaker?.startDato,
                        personalia = personalia?.let { ArrangorflatePersonalia.fromPersonalia(it) },
                        periode = deltakelse.periode(),
                        status = deltaker?.status?.type,
                    )
                }
                .sortedWith(compareBy(nullsLast()) { it.personalia?.navn })
            ArrangorflateBeregning.PrisPerTimeOppfolging(
                belop = beregning.output.belop,
                digest = beregning.getDigest(),
                deltakelser = deltakerlser,
                stengt = beregning.input.stengt.toList().sortedBy { it.periode.start },
                // TODO: støtte flere satser
                sats = beregning.input.satser.first().sats,
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
        betalingsinformasjon = ArrangorflateBetalingsinformasjon(
            kontonummer = utbetaling.betalingsinformasjon.kontonummer,
            kid = utbetaling.betalingsinformasjon.kid,
        ),
        type = UtbetalingType.from(utbetaling).toDto(),
        linjer = linjer,
        advarsler = advarsler,
    )
}
