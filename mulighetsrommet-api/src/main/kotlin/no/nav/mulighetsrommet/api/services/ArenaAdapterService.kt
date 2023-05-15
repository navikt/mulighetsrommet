package no.nav.mulighetsrommet.api.services

import arrow.core.flatMap
import no.nav.mulighetsrommet.api.producers.TiltaksgjennomforingKafkaProducer
import no.nav.mulighetsrommet.api.producers.TiltakstypeKafkaProducer
import no.nav.mulighetsrommet.api.repositories.*
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.database.utils.query
import no.nav.mulighetsrommet.domain.dbo.*
import no.nav.mulighetsrommet.domain.dto.AvtaleAdminDto
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingAdminDto
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingDto
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.*

class ArenaAdapterService(
    private val tiltakstyper: TiltakstypeRepository,
    private val avtaler: AvtaleRepository,
    private val tiltaksgjennomforinger: TiltaksgjennomforingRepository,
    private val tiltakshistorikk: TiltakshistorikkRepository,
    private val deltakere: DeltakerRepository,
    private val tiltaksgjennomforingKafkaProducer: TiltaksgjennomforingKafkaProducer,
    private val tiltakstypeKafkaProducer: TiltakstypeKafkaProducer,
    private val sanityTiltaksgjennomforingService: SanityTiltaksgjennomforingService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun upsertTiltakstype(tiltakstype: TiltakstypeDbo): QueryResult<TiltakstypeDbo> {
        return tiltakstyper.upsert(tiltakstype).onRight {
            tiltakstyper.get(tiltakstype.id)?.let {
                tiltakstypeKafkaProducer.publish(it)
            }
        }
    }

    fun removeTiltakstype(id: UUID): QueryResult<Int> {
        return tiltakstyper.delete(id).onRight { deletedRows ->
            if (deletedRows != 0) {
                tiltakstypeKafkaProducer.retract(id)
            }
        }
    }

    fun upsertAvtale(avtale: AvtaleDbo): QueryResult<AvtaleAdminDto> {
        return avtaler.upsert(avtale)
            .flatMap { avtaler.get(avtale.id) }
            .map { it!! }
    }

    fun removeAvtale(id: UUID): QueryResult<Int> {
        return avtaler.delete(id)
    }

    suspend fun upsertTiltaksgjennomforing(tiltaksgjennomforing: TiltaksgjennomforingDbo): QueryResult<TiltaksgjennomforingAdminDto> {
        return tiltaksgjennomforinger.upsert(tiltaksgjennomforing)
            .flatMap { tiltaksgjennomforinger.get(tiltaksgjennomforing.id) }
            .map { it!! }
            .onRight {
                tiltaksgjennomforingKafkaProducer.publish(TiltaksgjennomforingDto.from(it))
            }
            .onRight {
                if (it.sluttDato == null || it.sluttDato?.isAfter(LocalDate.of(2023, 1, 1)) == true) {
                    try {
                        sanityTiltaksgjennomforingService.opprettSanityTiltaksgjennomforing(it)
                    } catch (t: Throwable) {
                        log.error("Error ved opprettelse av sanity tiltaksgjennomforing: $t")
                    }
                }
            }
    }

    fun removeTiltaksgjennomforing(id: UUID): QueryResult<Int> {
        return tiltaksgjennomforinger.delete(id)
            .onRight { deletedRows ->
                if (deletedRows != 0) {
                    tiltaksgjennomforingKafkaProducer.retract(id)
                }
            }
    }

    fun upsertTiltakshistorikk(tiltakshistorikk: TiltakshistorikkDbo): QueryResult<TiltakshistorikkDbo> {
        return this.tiltakshistorikk.upsert(tiltakshistorikk)
    }

    fun removeTiltakshistorikk(id: UUID): QueryResult<Unit> {
        return tiltakshistorikk.delete(id)
    }

    fun upsertDeltaker(deltaker: DeltakerDbo): QueryResult<DeltakerDbo> {
        return query { deltakere.upsert(deltaker) }
    }

    fun removeDeltaker(id: UUID): QueryResult<Unit> {
        return query { deltakere.delete(id) }
    }
}
