package no.nav.mulighetsrommet.api.okonomi.refusjon

import no.nav.mulighetsrommet.api.okonomi.models.DeltakelsePeriode
import no.nav.mulighetsrommet.api.okonomi.models.DeltakelsePerioder
import no.nav.mulighetsrommet.api.okonomi.models.RefusjonKravBeregningAft
import no.nav.mulighetsrommet.api.okonomi.prismodell.Prismodell
import no.nav.mulighetsrommet.api.repositories.DeltakerRepository
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.domain.dto.DeltakerStatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters
import java.util.*

class RefusjonService(
    private val db: Database,
    private val tiltaksgjennomforingRepository: TiltaksgjennomforingRepository,
    private val deltakerRepository: DeltakerRepository,
    private val refusjonskravRepository: RefusjonskravRepository,
) {

    fun genererRefusjonskravForMonth(dayInMonth: LocalDate) {
        val periodeStart = dayInMonth.with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay()
        val periodeSlutt = periodeStart.with(TemporalAdjusters.lastDayOfMonth()).plusDays(1)

        tiltaksgjennomforingRepository
            .getGjennomforesInPeriodeUtenRefusjonskrav(periodeStart, periodeSlutt)
            .mapNotNull { gjennomforing ->
                when (gjennomforing.tiltakstype.tiltakskode) {
                    Tiltakskode.ARBEIDSFORBEREDENDE_TRENING -> createRefusjonskravAft(
                        refusjonskravId = UUID.randomUUID(),
                        gjennomforingId = gjennomforing.id,
                        periodeStart = periodeStart,
                        periodeSlutt = periodeSlutt,
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
                        periodeStart = gjeldendeKrav.beregning.input.periodeStart,
                        periodeSlutt = gjeldendeKrav.beregning.input.periodeSlutt,
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
        periodeStart: LocalDateTime,
        periodeSlutt: LocalDateTime,
    ): RefusjonskravDbo {
        val frist = periodeSlutt.plusMonths(2)

        val deltakere = getDeltakelser(
            gjennomforingId,
            periodeStart = periodeStart,
            periodeSlutt = periodeSlutt,
        )

        val input = RefusjonKravBeregningAft.Input(
            periodeStart = periodeStart,
            periodeSlutt = periodeSlutt,
            sats = Prismodell.AFT.findSats(periodeStart.toLocalDate()),
            deltakelser = deltakere,
        )

        val output = Prismodell.AFT.beregnRefusjonBelop(input)

        val beregning = RefusjonKravBeregningAft(input, output)

        return RefusjonskravDbo(
            id = refusjonskravId,
            fristForGodkjenning = frist,
            gjennomforingId = gjennomforingId,
            beregning = beregning,
            kontonummer = null,
            kid = null,
        )
    }

    private fun getDeltakelser(
        gjennomforingId: UUID,
        periodeStart: LocalDateTime,
        periodeSlutt: LocalDateTime,
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
                it.startDato != null && !it.startDato.atStartOfDay().isAfter(periodeSlutt)
            }
            .filter {
                it.sluttDato == null || it.sluttDato.plusDays(1).atStartOfDay().isAfter(periodeStart)
            }
            .map { deltakelse ->
                val start = maxOf(requireNotNull(deltakelse.startDato).atStartOfDay(), periodeStart)
                val slutt = minOf(deltakelse.sluttDato?.plusDays(1)?.atStartOfDay() ?: periodeSlutt, periodeSlutt)
                val stillingsprosent = requireNotNull(deltakelse.stillingsprosent) {
                    "stillingsprosent mangler for deltakelse id=${deltakelse.id}"
                }

                // TODO: periodisering av prosent - fra Komet
                val perioder = listOf(DeltakelsePeriode(start, slutt, stillingsprosent))
                    .filter { it.start.isBefore(it.slutt) }

                DeltakelsePerioder(
                    deltakelseId = deltakelse.id,
                    perioder = perioder,
                )
            }
            .filter { it.perioder.isNotEmpty() }
            .toSet()
    }
}
