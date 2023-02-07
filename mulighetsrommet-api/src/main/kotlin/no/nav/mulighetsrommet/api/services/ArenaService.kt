package no.nav.mulighetsrommet.api.services

import no.nav.mulighetsrommet.api.producers.TiltaksgjennomforingKafkaProducer
import no.nav.mulighetsrommet.api.producers.TiltakstypeKafkaProducer
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.repositories.TiltakshistorikkRepository
import no.nav.mulighetsrommet.api.repositories.TiltakstypeRepository
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.domain.Tiltakskoder.isGruppetiltak
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingDbo
import no.nav.mulighetsrommet.domain.dbo.TiltakshistorikkDbo
import no.nav.mulighetsrommet.domain.dbo.TiltakstypeDbo
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingDto
import java.util.*

class ArenaService(
    private val tiltakstyper: TiltakstypeRepository,
    private val tiltaksgjennomforinger: TiltaksgjennomforingRepository,
    private val deltakere: TiltakshistorikkRepository,
    private val tiltaksgjennomforingKafkaProducer: TiltaksgjennomforingKafkaProducer,
    private val tiltakstypeKafkaProducer: TiltakstypeKafkaProducer
) {
    fun upsert(tiltakstype: TiltakstypeDbo): QueryResult<TiltakstypeDbo> {
        return tiltakstyper.upsert(tiltakstype).tap {
            tiltakstyper.get(tiltakstype.id)?.let {
                tiltakstypeKafkaProducer.publish(it)
            }
        }
    }

    fun removeTiltakstype(id: UUID): QueryResult<Unit> {
        return tiltakstyper.delete(id).tap {
            tiltakstypeKafkaProducer.retract(id)
        }
    }

    fun upsert(tiltaksgjennomforing: TiltaksgjennomforingDbo): QueryResult<TiltaksgjennomforingDbo> {
        return tiltaksgjennomforinger.upsert(tiltaksgjennomforing)
            .tap {
                tiltaksgjennomforinger.get(tiltaksgjennomforing.id)?.let {
                    tiltaksgjennomforingKafkaProducer.publish(TiltaksgjennomforingDto.from(it))
                }
            }
    }

    fun removeTiltaksgjennomforing(id: UUID): QueryResult<Unit> {
        return tiltaksgjennomforinger.delete(id)
            .tap {
                tiltaksgjennomforingKafkaProducer.retract(id)
            }
    }

    fun upsert(tiltakshistorikk: TiltakshistorikkDbo): QueryResult<TiltakshistorikkDbo> {
        return deltakere.upsert(tiltakshistorikk)
    }

    fun removeTiltakshistorikk(id: UUID): QueryResult<Unit> {
        return deltakere.delete(id)
    }
}
