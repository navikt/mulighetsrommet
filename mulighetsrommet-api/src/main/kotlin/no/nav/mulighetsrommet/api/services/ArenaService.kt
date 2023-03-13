package no.nav.mulighetsrommet.api.services

import no.nav.mulighetsrommet.api.producers.TiltaksgjennomforingKafkaProducer
import no.nav.mulighetsrommet.api.producers.TiltakstypeKafkaProducer
import no.nav.mulighetsrommet.api.repositories.*
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.database.utils.query
import no.nav.mulighetsrommet.domain.dbo.*
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingDto
import java.util.*

class ArenaService(
    private val tiltakstyper: TiltakstypeRepository,
    private val avtaler: AvtaleRepository,
    private val tiltaksgjennomforinger: TiltaksgjennomforingRepository,
    private val tiltakshistorikk: TiltakshistorikkRepository,
    private val deltakere: DeltakerRepository,
    private val tiltaksgjennomforingKafkaProducer: TiltaksgjennomforingKafkaProducer,
    private val tiltakstypeKafkaProducer: TiltakstypeKafkaProducer
) {
    fun upsert(tiltakstype: TiltakstypeDbo): QueryResult<TiltakstypeDbo> {
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

    fun upsert(avtale: AvtaleDbo): QueryResult<AvtaleDbo> {
        return avtaler.upsert(avtale)
    }

    fun removeAvtale(id: UUID): QueryResult<Int> {
        return avtaler.delete(id)
    }

    fun upsert(tiltaksgjennomforing: TiltaksgjennomforingDbo): QueryResult<TiltaksgjennomforingDbo> {
        return tiltaksgjennomforinger.upsert(tiltaksgjennomforing)
            .onRight {
                tiltaksgjennomforinger.get(tiltaksgjennomforing.id)?.let {
                    tiltaksgjennomforingKafkaProducer.publish(TiltaksgjennomforingDto.from(it))
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

    fun upsert(tiltakshistorikk: TiltakshistorikkDbo): QueryResult<TiltakshistorikkDbo> {
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
