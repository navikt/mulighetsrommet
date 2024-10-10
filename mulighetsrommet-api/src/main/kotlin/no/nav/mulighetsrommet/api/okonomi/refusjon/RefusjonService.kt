package no.nav.mulighetsrommet.api.okonomi.refusjon

import no.nav.mulighetsrommet.api.okonomi.models.DeltakelsePeriode
import no.nav.mulighetsrommet.api.okonomi.models.DeltakelsePerioder
import no.nav.mulighetsrommet.api.okonomi.models.RefusjonKravBeregningAft
import no.nav.mulighetsrommet.api.okonomi.models.RefusjonskravDto
import no.nav.mulighetsrommet.api.okonomi.prismodell.Prismodell
import no.nav.mulighetsrommet.api.repositories.DeltakerRepository
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.domain.dto.DeltakerStatus
import no.nav.mulighetsrommet.domain.dto.Organisasjonsnummer
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters
import java.util.*

class RefusjonService(
    private val tiltaksgjennomforingRepository: TiltaksgjennomforingRepository,
    private val deltakerRepository: DeltakerRepository,
    private val refusjonskravRepository: RefusjonskravRepository,
    private val db: Database,
) {
    fun getByOrgnr(orgnr: List<Organisasjonsnummer>): List<RefusjonskravDto> {
        return refusjonskravRepository.getByOrgnr(orgnr)
    }

    fun getById(id: UUID): RefusjonskravDto? {
        return refusjonskravRepository.get(id)
    }

    fun genererRefusjonskravForMonth(dayInMonth: LocalDate) {
        val periodeStart = dayInMonth.with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay()
        val periodeSlutt = periodeStart.with(TemporalAdjusters.lastDayOfMonth()).plusDays(1)

        val krav = tiltaksgjennomforingRepository
            .getGjennomforesInPeriodeUtenRefusjonskrav(periodeStart, periodeSlutt)
            .mapNotNull {
                when (it.tiltakstype.tiltakskode) {
                    Tiltakskode.ARBEIDSFORBEREDENDE_TRENING -> createRefusjonskravAft(
                        gjennomforingId = it.id,
                        periodeStart = periodeStart,
                        periodeSlutt = periodeSlutt,
                    )

                    else -> null
                }
            }

        krav.forEach {
            db.transaction { tx ->
                refusjonskravRepository.upsert(it, tx)
            }
        }
    }

    fun createRefusjonskravAft(
        gjennomforingId: UUID,
        periodeStart: LocalDateTime,
        periodeSlutt: LocalDateTime,
    ): RefusjonskravDbo {
        val refusjonskravId = UUID.randomUUID()

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
            gjennomforingId = gjennomforingId,
            beregning = beregning,
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

                DeltakelsePerioder(
                    deltakelseId = deltakelse.id,
                    perioder = perioder,
                )
            }
            .toSet()
    }
}
