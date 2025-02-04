package no.nav.mulighetsrommet.api.refusjon

import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.refusjon.db.RefusjonskravDbo
import no.nav.mulighetsrommet.api.refusjon.model.*
import no.nav.mulighetsrommet.api.tilsagn.model.ForhandsgodkjenteSatser
import no.nav.mulighetsrommet.model.DeltakerStatus
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode
import java.time.LocalDate
import java.util.*

class RefusjonService(
    private val db: ApiDatabase,
) {
    fun genererRefusjonskravForMonth(date: LocalDate): List<RefusjonskravDto> = db.transaction {
        val periode = Periode.forMonthOf(date)

        queries.gjennomforing
            .getGjennomforesInPeriodeUtenRefusjonskrav(periode)
            .mapNotNull { gjennomforing ->
                val krav = when (gjennomforing.tiltakstype.tiltakskode) {
                    Tiltakskode.ARBEIDSFORBEREDENDE_TRENING -> createRefusjonskravAft(
                        refusjonskravId = UUID.randomUUID(),
                        gjennomforingId = gjennomforing.id,
                        periode = periode,
                    )

                    else -> null
                }

                krav?.takeIf { it.beregning.output.belop > 0 }
            }
            .map { krav ->
                queries.refusjonskrav.upsert(krav)
                requireNotNull(queries.refusjonskrav.get(krav.id)) { "Refusjonskrav forventet siden det nettopp ble opprettet" }
            }
    }

    fun recalculateRefusjonskravForGjennomforing(id: UUID): Unit = db.transaction {
        queries.refusjonskrav
            .getByGjennomforing(id, statuser = listOf(RefusjonskravStatus.KLAR_FOR_GODKJENNING))
            .mapNotNull { gjeldendeKrav ->
                val nyttKrav = when (gjeldendeKrav.beregning) {
                    is RefusjonKravBeregningAft -> createRefusjonskravAft(
                        refusjonskravId = gjeldendeKrav.id,
                        gjennomforingId = gjeldendeKrav.gjennomforing.id,
                        periode = gjeldendeKrav.beregning.input.periode,
                    )
                }

                nyttKrav.takeIf { it.beregning != gjeldendeKrav.beregning }
            }
            .forEach { krav ->
                queries.refusjonskrav.upsert(krav)
            }
    }

    fun createRefusjonskravAft(
        refusjonskravId: UUID,
        gjennomforingId: UUID,
        periode: Periode,
    ): RefusjonskravDbo {
        val frist = periode.slutt.plusMonths(2)

        val deltakere = getDeltakelser(gjennomforingId, periode)

        // TODO: burde også verifisere at start og slutt har samme pris
        val sats = ForhandsgodkjenteSatser.findSats(Tiltakskode.ARBEIDSFORBEREDENDE_TRENING, periode.start)
            ?: throw IllegalStateException("Sats mangler for periode $periode")

        val input = RefusjonKravBeregningAft.Input(
            periode = periode,
            sats = sats,
            deltakelser = deltakere,
        )

        val beregning = RefusjonKravBeregningAft.beregn(input)

        val forrigeKrav = db.session {
            queries.refusjonskrav.getSisteGodkjenteRefusjonskrav(gjennomforingId)
        }

        return RefusjonskravDbo(
            id = refusjonskravId,
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
