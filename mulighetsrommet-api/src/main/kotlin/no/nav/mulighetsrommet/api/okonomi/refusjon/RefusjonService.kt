package no.nav.mulighetsrommet.api.okonomi.refusjon

import no.nav.mulighetsrommet.api.okonomi.models.DeltakelsePeriode
import no.nav.mulighetsrommet.api.okonomi.models.RefusjonskravDeltakelse
import no.nav.mulighetsrommet.api.okonomi.prismodell.Prismodell
import no.nav.mulighetsrommet.api.repositories.DeltakerRepository
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.domain.dbo.Deltakerstatus
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

    fun getById(id: UUID): RefusjonskravDto? {
        return refusjonskravRepository.get(id)
    }

    fun genererRefusjonskravForMonth(dayInMonth: LocalDate) {
        val periodeStart = dayInMonth.with(TemporalAdjusters.firstDayOfMonth())
        val periodeSlutt = periodeStart.with(TemporalAdjusters.lastDayOfMonth())

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
        periodeStart: LocalDate,
        periodeSlutt: LocalDate,
    ): RefusjonskravDbo {
        val refusjonskravId = UUID.randomUUID()

        val deltakere = getDeltakelser(
            gjennomforingId,
            periodeStart = periodeStart,
            periodeSlutt = periodeSlutt,
        )
        val sats = Prismodell.AFT.findSats(periodeStart)
        val beregning = beregnRefusjonAft(sats, deltakere)

        return RefusjonskravDbo(
            id = refusjonskravId,
            tiltaksgjennomforingId = gjennomforingId,
            periodeStart = periodeStart,
            periodeSlutt = periodeSlutt,
            beregning = beregning,
        )
    }

    private fun beregnRefusjonAft(
        sats: Int,
        deltakere: Set<RefusjonskravDeltakelse>,
    ): Prismodell.RefusjonskravBeregning.AFT {
        val belop = Prismodell.AFT.beregnRefusjonBelop(
            sats = sats,
            deltakelser = deltakere,
        )
        return Prismodell.RefusjonskravBeregning.AFT(
            belop = belop,
            sats = sats,
            deltakere = deltakere,
        )
    }

    private fun getDeltakelser(
        gjennomforingId: UUID,
        periodeStart: LocalDate,
        periodeSlutt: LocalDate,
    ): Set<RefusjonskravDeltakelse> {
        // TODO: hent deltakelser fra komet i stedet for tiltakshistorikk
        val deltakelser = deltakerRepository.getAll(gjennomforingId)

        return deltakelser
            .filter {
                it.startDato != null && !it.startDato!!.isAfter(periodeSlutt)
            }
            .filter {
                // TODO: hva er riktig logikk for å filtrere ut relevante deltakere?
//                it.status.type in listOf(
//                    AmtDeltakerStatus.Type.AVBRUTT,
//                    AmtDeltakerStatus.Type.DELTAR,
//                    AmtDeltakerStatus.Type.HAR_SLUTTET,
//                    AmtDeltakerStatus.Type.FULLFORT,
//                )
                it.status in listOf(Deltakerstatus.DELTAR, Deltakerstatus.AVSLUTTET)
            }
            .map { deltakelse ->
                val startDato = maxOf(requireNotNull(deltakelse.startDato), periodeStart)
                val sluttDato = minOf(deltakelse.sluttDato ?: periodeSlutt, periodeSlutt)
                val periode = DeltakelsePeriode(
                    start = startDato.atStartOfDay(),
                    slutt = sluttDato.atStartOfDay(),
                    stillingsprosent = 100.00,
                )

                // TODO: periodisering av prosent - fra Komet
                val perioder = listOf(periode)

                RefusjonskravDeltakelse(
                    deltakelseId = deltakelse.id,
                    perioder = perioder,
                )
            }
            .toSet()
    }
}
