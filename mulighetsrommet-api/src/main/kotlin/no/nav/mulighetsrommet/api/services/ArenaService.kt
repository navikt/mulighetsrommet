package no.nav.mulighetsrommet.api.services

import no.nav.mulighetsrommet.api.producers.TiltaksgjennomforingKafkaProducer
import no.nav.mulighetsrommet.api.repositories.DeltakerRepository
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.repositories.TiltakstypeRepository
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.domain.dbo.DeltakerDbo
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingDbo
import no.nav.mulighetsrommet.domain.dbo.TiltakstypeDbo
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingDto
import no.nav.mulighetsrommet.domain.dto.TiltakstypeDto

class ArenaService(
    private val tiltakstyper: TiltakstypeRepository,
    private val tiltaksgjennomforinger: TiltaksgjennomforingRepository,
    private val deltakere: DeltakerRepository,
    private val tiltaksgjennomforingKafkaProducer: TiltaksgjennomforingKafkaProducer,
) {
    fun upsert(tiltakstype: TiltakstypeDbo): QueryResult<TiltakstypeDbo> {
        return tiltakstyper.upsert(tiltakstype)
    }

    fun remove(tiltakstype: TiltakstypeDbo): QueryResult<Unit> {
        return tiltakstyper.delete(tiltakstype.id)
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

    fun upsert(deltaker: DeltakerDbo): QueryResult<DeltakerDbo> {
        return deltakere.upsert(deltaker)
    }

    fun remove(deltaker: DeltakerDbo): QueryResult<Unit> {
        return deltakere.delete(deltaker.id)
    }

    private fun isSupportedTiltaksgjennomforing(tiltaksgjennomforing: TiltaksgjennomforingDbo): Boolean {
        val tiltakstype = tiltakstyper.get(tiltaksgjennomforing.tiltakstypeId)!!
        return isGruppetiltak(tiltakstype)
    }

    private fun isGruppetiltak(tiltakstype: TiltakstypeDto): Boolean {
        // Enn så lenge så opererer vi med en hardkodet liste over hvilke gjennomføringer vi anser som gruppetiltak
        val gruppetiltak = listOf(
            "ARBFORB",
            "ARBRRHDAG",
            "AVKLARAG",
            "DIGIOPPARB",
            "FORSAMOGRU",
            "FORSFAGGRU",
            "GRUFAGYRKE",
            "GRUPPEAMO",
            "INDJOBSTOT",
            "INDOPPFAG",
            "INDOPPRF",
            "IPSUNG",
            "JOBBK",
            "UTVAOONAV",
            "UTVOPPFOPL",
            "VASV",
        )
        return tiltakstype.arenaKode in gruppetiltak
    }
}
