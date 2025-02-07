package no.nav.mulighetsrommet.api.utbetaling

import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.tilsagn.model.ForhandsgodkjenteSatser
import no.nav.mulighetsrommet.api.utbetaling.db.UtbetalingDbo
import no.nav.mulighetsrommet.api.utbetaling.model.*
import no.nav.mulighetsrommet.model.DeltakerStatus
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode
import java.time.LocalDate
import java.util.*

class UtbetalingService(
    private val db: ApiDatabase,
) {
    fun genererUtbetalingForMonth(date: LocalDate): List<UtbetalingDto> = db.transaction {
        val periode = Periode.forMonthOf(date)

        queries.gjennomforing
            .getGjennomforesInPeriodeUtenUtbetaling(periode)
            .mapNotNull { gjennomforing ->
                val utbetaling = when (gjennomforing.tiltakstype.tiltakskode) {
                    Tiltakskode.ARBEIDSFORBEREDENDE_TRENING -> createUtbetalingAft(
                        utbetalingId = UUID.randomUUID(),
                        gjennomforingId = gjennomforing.id,
                        periode = periode,
                    )

                    else -> null
                }

                utbetaling?.takeIf { it.beregning.output.belop > 0 }
            }
            .map { utbetaling ->
                queries.utbetaling.upsert(utbetaling)
                requireNotNull(queries.utbetaling.get(utbetaling.id)) { "Utbetaling forventet siden det nettopp ble opprettet" }
            }
    }

    fun recalculateUtbetalingForGjennomforing(id: UUID): Unit = db.transaction {
        queries.utbetaling
            .getByGjennomforing(id, statuser = listOf(UtbetalingStatus.KLAR_FOR_GODKJENNING))
            .mapNotNull { gjeldendeKrav ->
                val nyttKrav = when (gjeldendeKrav.beregning) {
                    is UtbetalingBeregningAft -> createUtbetalingAft(
                        utbetalingId = gjeldendeKrav.id,
                        gjennomforingId = gjeldendeKrav.gjennomforing.id,
                        periode = gjeldendeKrav.beregning.input.periode,
                    )
                    is UtbetalingBeregningFri -> null
                }

                nyttKrav?.takeIf { it.beregning != gjeldendeKrav.beregning }
            }
            .forEach { utbetaling ->
                queries.utbetaling.upsert(utbetaling)
            }
    }

    fun createUtbetalingAft(
        utbetalingId: UUID,
        gjennomforingId: UUID,
        periode: Periode,
    ): UtbetalingDbo {
        val frist = periode.slutt.plusMonths(2)

        val deltakere = getDeltakelser(gjennomforingId, periode)

        // TODO: burde også verifisere at start og slutt har samme pris
        val sats = ForhandsgodkjenteSatser.findSats(Tiltakskode.ARBEIDSFORBEREDENDE_TRENING, periode.start)
            ?: throw IllegalStateException("Sats mangler for periode $periode")

        val input = UtbetalingBeregningAft.Input(
            periode = periode,
            sats = sats,
            deltakelser = deltakere,
        )

        val beregning = UtbetalingBeregningAft.beregn(input)

        val forrigeKrav = db.session {
            queries.utbetaling.getSisteGodkjenteUtbetaling(gjennomforingId)
        }

        return UtbetalingDbo(
            id = utbetalingId,
            fristForGodkjenning = frist.atStartOfDay(),
            gjennomforingId = gjennomforingId,
            beregning = beregning,
            kontonummer = forrigeKrav?.betalingsinformasjon?.kontonummer,
            kid = forrigeKrav?.betalingsinformasjon?.kid,
            periode = periode,
        )
    }

    private fun getDeltakelser(
        gjennomforingId: UUID,
        periode: Periode,
    ): Set<DeltakelsePerioder> {
        val deltakelser = db.session {
            queries.deltaker.getAll(gjennomforingId = gjennomforingId)
        }

        return deltakelser
            .asSequence()
            .filter {
                it.status.type in listOf(
                    DeltakerStatus.Type.AVBRUTT,
                    DeltakerStatus.Type.DELTAR,
                    DeltakerStatus.Type.HAR_SLUTTET,
                    DeltakerStatus.Type.FULLFORT,
                )
            }
            .filter { it.deltakelsesprosent != null }
            .filter {
                it.startDato != null && it.startDato.isBefore(periode.slutt)
            }
            .filter {
                it.sluttDato == null || it.sluttDato.plusDays(1).isAfter(periode.start)
            }
            .map { deltakelse ->
                val start = maxOf(requireNotNull(deltakelse.startDato), periode.start)
                val slutt = minOf(deltakelse.sluttDato?.plusDays(1) ?: periode.slutt, periode.slutt)
                val deltakelsesprosent = requireNotNull(deltakelse.deltakelsesprosent) {
                    "deltakelsesprosent mangler for deltakelse id=${deltakelse.id}"
                }

                // TODO: periodisering av prosent - fra Komet
                val perioder = listOf(DeltakelsePeriode(start, slutt, deltakelsesprosent))

                DeltakelsePerioder(
                    deltakelseId = deltakelse.id,
                    perioder = perioder,
                )
            }
            .toSet()
    }
}
