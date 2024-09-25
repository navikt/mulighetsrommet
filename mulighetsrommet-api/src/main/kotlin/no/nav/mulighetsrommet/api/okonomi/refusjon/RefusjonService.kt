package no.nav.mulighetsrommet.api.okonomi.refusjon

import no.nav.mulighetsrommet.api.domain.dto.TiltaksgjennomforingDto
import no.nav.mulighetsrommet.api.okonomi.prismodell.Prismodell
import no.nav.mulighetsrommet.api.repositories.DeltakerRepository
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.domain.dbo.DeltakerDbo
import no.nav.mulighetsrommet.domain.dto.Organisasjonsnummer
import java.time.LocalDate
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

    fun genererRefusjonskravForMonth(dayInMonth: LocalDate) {
        val periodeStart = dayInMonth.with(TemporalAdjusters.firstDayOfMonth())
        val periodeSlutt = periodeStart.with(TemporalAdjusters.lastDayOfMonth())

        val krav = tiltaksgjennomforingRepository
            .getGjennomforesInPeriodeUtenRefusjonskrav(periodeStart, periodeSlutt)
            .mapNotNull {
                when (it.tiltakstype.tiltakskode) {
                    Tiltakskode.ARBEIDSFORBEREDENDE_TRENING -> lagAFTRefusjonskrav(
                        it,
                        periodeStart = periodeStart,
                        periodeSlutt = periodeSlutt,
                    )

                    else -> null
                }
            }

        db.transaction { tx ->
            krav.forEach {
                refusjonskravRepository.upsert(it, tx)
            }
        }
    }

    fun lagAFTRefusjonskrav(
        tiltaksgjennomforing: TiltaksgjennomforingDto,
        periodeStart: LocalDate,
        periodeSlutt: LocalDate,
    ): RefusjonskravDbo {
        val beregning = aftRefusjonBeregning(
            tiltaksgjennomforing.id,
            periodeStart,
            periodeSlutt,
        )
        return RefusjonskravDbo(
            id = UUID.randomUUID(),
            tiltaksgjennomforingId = tiltaksgjennomforing.id,
            arrangorId = tiltaksgjennomforing.arrangor.id,
            periodeStart = periodeStart,
            periodeSlutt = periodeSlutt,
            beregning = beregning,
        )
    }

    fun aftRefusjonBeregning(
        tiltaksgjennomforingId: UUID,
        periodeStart: LocalDate,
        periodeSlutt: LocalDate,
    ): Prismodell.RefusjonskravBeregning.AFT {
        val deltakere = deltakereIPeriode(
            // TODO: Her må vi nok hente data fra komet i stedet
            deltakerRepository.getAll(tiltaksgjennomforingId),
            periodeStart = periodeStart,
            periodeSlutt = periodeSlutt,
        )
        val sats = Prismodell.AFT.findSats(periodeStart)
        requireNotNull(sats) { "fant ikke sats" }

        return Prismodell.RefusjonskravBeregning.AFT(
            belop = Prismodell.AFT.beregnRefusjonBelop(
                sats = sats,
                deltakere = deltakere,
                periodeStart = periodeStart,
            ),
            deltakere = deltakere,
            sats = sats,
            periodeStart = periodeStart,
        )
    }
}

// TODO: Disse må endres etterhvert
fun deltakereIPeriode(
    deltakere: List<DeltakerDbo>,
    periodeStart: LocalDate,
    periodeSlutt: LocalDate,
): List<Prismodell.RefusjonskravBeregning.AFT.Deltaker> {
    return deltakere
        .filter {
            it.startDato != null &&
                it.sluttDato != null &&
                it.sluttDato?.isBefore(periodeStart) != true &&
                it.startDato?.isAfter(periodeSlutt) != true
        }
        .map {
            it.toDeltakerIPeriode(periodeStart, periodeSlutt)
        }
}

// TODO: Denne må endres etterhvert. Her mangler vi periodisering av prosent, faktisk prosent i det hele tatt.
fun DeltakerDbo.toDeltakerIPeriode(
    periodeStart: LocalDate,
    periodeSlutt: LocalDate,
): Prismodell.RefusjonskravBeregning.AFT.Deltaker {
    requireNotNull(this.startDato)
    requireNotNull(this.sluttDato)

    val startDato = if (this.startDato!! >= periodeStart) {
        this.startDato!!
    } else {
        periodeStart
    }
    val sluttDato = if (this.sluttDato!! >= periodeSlutt) {
        periodeSlutt
    } else {
        this.sluttDato!!
    }

    return Prismodell.RefusjonskravBeregning.AFT.Deltaker(
        startDato = startDato,
        sluttDato = sluttDato,
        prosentPerioder = listOf(
            Prismodell.RefusjonskravBeregning.AFT.Deltaker.ProsentPeriode(
                startDato = startDato,
                sluttDato = sluttDato,
                prosent = 1.0,
            ),
        ),
    )
}

sealed class RefusjonBeregningInput {
    data class AFT(
        val periodeStart: LocalDate,
        val deltakere: List<Prismodell.RefusjonskravBeregning.AFT.Deltaker>,
        val sats: Int,
    ) : RefusjonBeregningInput()
}
