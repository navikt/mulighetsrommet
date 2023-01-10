package no.nav.mulighetsrommet.api.services

import no.nav.mulighetsrommet.api.producers.TiltaksgjennomforingKafkaProducer
import no.nav.mulighetsrommet.api.producers.TiltakstypeKafkaProducer
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.repositories.TiltakshistorikkRepository
import no.nav.mulighetsrommet.api.repositories.TiltakstypeRepository
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingDbo
import no.nav.mulighetsrommet.domain.dbo.TiltakshistorikkDbo
import no.nav.mulighetsrommet.domain.dbo.TiltakstypeDbo
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingDto
import no.nav.mulighetsrommet.domain.dto.isGruppetiltak

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

    fun remove(tiltakstype: TiltakstypeDbo): QueryResult<Unit> {
        return tiltakstyper.delete(tiltakstype.id).tap {
            tiltakstypeKafkaProducer.retract(tiltakstype.id)
        }
    }

    fun upsert(tiltaksgjennomforing: TiltaksgjennomforingDbo): QueryResult<TiltaksgjennomforingDbo> {
        return tiltaksgjennomforinger.upsert(tiltaksgjennomforing)
            .tap {
                if (isSupportedTiltaksgjennomforing(tiltaksgjennomforing)) {
                    tiltaksgjennomforinger.get(tiltaksgjennomforing.id)?.let {
                        tiltaksgjennomforingKafkaProducer.publish(TiltaksgjennomforingDto.from(it))
                    }
                }
            }
    }

    fun remove(tiltaksgjennomforing: TiltaksgjennomforingDbo): QueryResult<Unit> {
        return tiltaksgjennomforinger.delete(tiltaksgjennomforing.id)
            .tap {
                if (isSupportedTiltaksgjennomforing(tiltaksgjennomforing)) {
                    tiltaksgjennomforingKafkaProducer.retract(tiltaksgjennomforing.id)
                }
            }
    }

    fun upsert(tiltakshistorikk: TiltakshistorikkDbo): QueryResult<TiltakshistorikkDbo> {
        return deltakere.upsert(tiltakshistorikk)
    }

    fun remove(tiltakshistorikk: TiltakshistorikkDbo): QueryResult<Unit> {
        return deltakere.delete(tiltakshistorikk.id)
    }

    private fun isSupportedTiltaksgjennomforing(tiltaksgjennomforing: TiltaksgjennomforingDbo): Boolean {
        val tiltakstype = tiltakstyper.get(tiltaksgjennomforing.tiltakstypeId)!!
        return isGruppetiltak(tiltakstype.arenaKode)
    }
}
