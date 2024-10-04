package no.nav.mulighetsrommet.api.okonomi.refusjon

import no.nav.mulighetsrommet.api.clients.tiltakshistorikk.TiltakshistorikkClient
import no.nav.mulighetsrommet.api.okonomi.models.DeltakelsePeriode
import no.nav.mulighetsrommet.api.okonomi.models.RefusjonskravDeltakelsePerioder
import no.nav.mulighetsrommet.api.okonomi.prismodell.Prismodell
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.domain.dto.Organisasjonsnummer
import no.nav.mulighetsrommet.domain.dto.amt.AmtDeltakerStatus
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.*

class RefusjonService(
    private val tiltaksgjennomforingRepository: TiltaksgjennomforingRepository,
    private val refusjonskravRepository: RefusjonskravRepository,
    private val tiltakshistorikk: TiltakshistorikkClient,
    private val db: Database,
) {
    fun getByOrgnr(orgnr: List<Organisasjonsnummer>): List<RefusjonskravDto> {
        return refusjonskravRepository.getByOrgnr(orgnr)
    }

    fun getById(id: UUID): RefusjonskravDto? {
        return refusjonskravRepository.get(id)
    }

    suspend fun genererRefusjonskravForMonth(dayInMonth: LocalDate) {
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

    suspend fun createRefusjonskravAft(
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
        val beregning = beregnRefusjonAft(periodeStart, periodeSlutt, sats, deltakere)

        return RefusjonskravDbo(
            id = refusjonskravId,
            tiltaksgjennomforingId = gjennomforingId,
            periodeStart = periodeStart,
            periodeSlutt = periodeSlutt,
            beregning = beregning,
        )
    }

    private fun beregnRefusjonAft(
        periodeStart: LocalDate,
        periodeSlutt: LocalDate,
        sats: Int,
        deltakere: Set<RefusjonskravDeltakelsePerioder>,
    ): Prismodell.RefusjonskravBeregning.AFT {
        val beregning = Prismodell.AFT.beregnRefusjonBelop(
            periodeStart = periodeStart.atStartOfDay(),
            periodeSlutt = periodeSlutt.atStartOfDay(),
            sats = sats,
            deltakelser = deltakere,
        )
        return Prismodell.RefusjonskravBeregning.AFT(
            belop = beregning.belop.toInt(),
            sats = sats,
            deltakere = deltakere,
        )
    }

    private suspend fun getDeltakelser(
        gjennomforingId: UUID,
        periodeStart: LocalDate,
        periodeSlutt: LocalDate,
    ): Set<RefusjonskravDeltakelsePerioder> {
        // TODO: hent deltakelser fra komet i stedet for tiltakshistorikk
        val response = tiltakshistorikk.getDeltakelser(gjennomforingId)

        return response.deltakelser
            .filter {
                it.startDato != null && !it.startDato!!.isAfter(periodeSlutt)
            }
            .filter {
                // TODO: hva er riktig logikk for å filtrere ut relevante deltakere?
                it.status.type in listOf(
                    AmtDeltakerStatus.Type.AVBRUTT,
                    AmtDeltakerStatus.Type.DELTAR,
                    AmtDeltakerStatus.Type.HAR_SLUTTET,
                    AmtDeltakerStatus.Type.FULLFORT,
                )
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

                RefusjonskravDeltakelsePerioder(
                    deltakelseId = deltakelse.id,
                    perioder = perioder,
                )
            }
            .toSet()
    }
}
