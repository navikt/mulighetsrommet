package no.nav.mulighetsrommet.api.refusjon

import no.nav.mulighetsrommet.api.gjennomforing.db.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.okonomi.Prismodell
import no.nav.mulighetsrommet.api.refusjon.db.DeltakerRepository
import no.nav.mulighetsrommet.api.refusjon.db.RefusjonskravDbo
import no.nav.mulighetsrommet.api.refusjon.db.RefusjonskravRepository
import no.nav.mulighetsrommet.api.refusjon.model.*
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.domain.dto.DeltakerStatus
import java.time.LocalDate
import java.util.*

class RefusjonService(
    private val db: Database,
    private val tiltaksgjennomforingRepository: TiltaksgjennomforingRepository,
    private val deltakerRepository: DeltakerRepository,
    private val refusjonskravRepository: RefusjonskravRepository,
) {
    fun genererRefusjonskravForMonth(dayInMonth: LocalDate) {
        val periode = RefusjonskravPeriode.fromDayInMonth(dayInMonth)

        tiltaksgjennomforingRepository
            .getGjennomforesInPeriodeUtenRefusjonskrav(periode)
            .mapNotNull { gjennomforing ->
                when (gjennomforing.tiltakstype.tiltakskode) {
                    Tiltakskode.ARBEIDSFORBEREDENDE_TRENING -> createRefusjonskravAft(
                        refusjonskravId = UUID.randomUUID(),
                        gjennomforingId = gjennomforing.id,
                        periode = periode,
                    )

                    else -> null
                }
            }
            .forEach { krav ->
                refusjonskravRepository.upsert(krav)
            }
    }

    fun recalculateRefusjonskravForGjennomforing(id: UUID) = db.transaction { tx ->
        refusjonskravRepository
            .getByGjennomforing(id, status = RefusjonskravStatus.KLAR_FOR_GODKJENNING)
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
                refusjonskravRepository.upsert(krav, tx)
            }
    }

    fun createRefusjonskravAft(
        refusjonskravId: UUID,
        gjennomforingId: UUID,
        periode: RefusjonskravPeriode,
    ): RefusjonskravDbo {
        val frist = periode.slutt.plusMonths(2)

        val deltakere = getDeltakelser(gjennomforingId, periode)

        val input = RefusjonKravBeregningAft.Input(
            periode = periode,
            sats = Prismodell.AFT.findSats(periode.start),
            deltakelser = deltakere,
        )

        val output = Prismodell.AFT.beregnRefusjonBelop(input)

        val beregning = RefusjonKravBeregningAft(input, output)

        val forrigeKrav = refusjonskravRepository.getSisteGodkjenteRefusjonskrav(gjennomforingId)

        return RefusjonskravDbo(
            id = refusjonskravId,
            fristForGodkjenning = frist.atStartOfDay(),
            gjennomforingId = gjennomforingId,
            beregning = beregning,
            kontonummer = forrigeKrav?.betalingsinformasjon?.kontonummer,
            kid = forrigeKrav?.betalingsinformasjon?.kid,
        )
    }

    private fun getDeltakelser(
        gjennomforingId: UUID,
        periode: RefusjonskravPeriode,
    ): Set<DeltakelsePerioder> {
        val deltakelser = deltakerRepository.getAll(gjennomforingId)

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
            .filter { it.stillingsprosent != null }
            .filter {
                it.startDato != null && it.startDato.isBefore(periode.slutt)
            }
            .filter {
                it.sluttDato == null || it.sluttDato.plusDays(1).isAfter(periode.start)
            }
            .map { deltakelse ->
                val start = maxOf(requireNotNull(deltakelse.startDato), periode.start)
                val slutt = minOf(deltakelse.sluttDato?.plusDays(1) ?: periode.slutt, periode.slutt)
                val stillingsprosent = requireNotNull(deltakelse.stillingsprosent) {
                    "stillingsprosent mangler for deltakelse id=${deltakelse.id}"
                }

                // TODO: periodisering av prosent - fra Komet
                val perioder = listOf(DeltakelsePeriode(start, slutt, stillingsprosent))

                DeltakelsePerioder(
                    deltakelseId = deltakelse.id,
                    perioder = perioder,
                )
            }
            .toSet()
    }
}
