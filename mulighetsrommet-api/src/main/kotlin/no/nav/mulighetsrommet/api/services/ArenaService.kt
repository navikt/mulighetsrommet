package no.nav.mulighetsrommet.api.services

import no.nav.mulighetsrommet.api.repositories.DeltakerRepository
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.repositories.TiltakstypeRepository
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.domain.dbo.DeltakerDbo
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingDbo
import no.nav.mulighetsrommet.domain.dbo.TiltakstypeDbo

class ArenaService(
    private val tiltakstypeRepository: TiltakstypeRepository,
    private val tiltaksgjennomforingRepository: TiltaksgjennomforingRepository,
    private val deltakerRepository: DeltakerRepository
) {

    fun upsert(tiltaksgjennomforing: TiltaksgjennomforingDbo): QueryResult<TiltaksgjennomforingDbo> {
        return tiltaksgjennomforingRepository.upsert(tiltaksgjennomforing)
    }

    fun upsert(tiltakstype: TiltakstypeDbo): QueryResult<TiltakstypeDbo> {
        return tiltakstypeRepository.upsert(tiltakstype)
    }

    fun upsert(deltaker: DeltakerDbo): QueryResult<DeltakerDbo> {
        return deltakerRepository.upsert(deltaker)
    }

    fun remove(tiltaksgjennomforing: TiltaksgjennomforingDbo): QueryResult<Unit> {
        return tiltaksgjennomforingRepository.delete(tiltaksgjennomforing.id)
    }

    fun remove(tiltakstype: TiltakstypeDbo): QueryResult<Unit> {
        return tiltakstypeRepository.delete(tiltakstype.id)
    }

    fun remove(deltaker: DeltakerDbo): QueryResult<Unit> {
        return deltakerRepository.delete(deltaker.id)
    }
}
