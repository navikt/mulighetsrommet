package no.nav.mulighetsrommet.api.services

import no.nav.mulighetsrommet.api.repositories.DeltakerRepository
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.repositories.TiltakstypeRepository
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.domain.models.Deltaker
import no.nav.mulighetsrommet.domain.models.Tiltaksgjennomforing
import no.nav.mulighetsrommet.domain.models.Tiltakstype

class ArenaService(
    private val tiltakstypeRepository: TiltakstypeRepository,
    private val tiltaksgjennomforingRepository: TiltaksgjennomforingRepository,
    private val deltakerRepository: DeltakerRepository
) {

    fun upsert(tiltaksgjennomforing: Tiltaksgjennomforing): QueryResult<Tiltaksgjennomforing> {
        return tiltaksgjennomforingRepository.upsert(tiltaksgjennomforing)
    }

    fun upsert(tiltakstype: Tiltakstype): QueryResult<Tiltakstype> {
        return tiltakstypeRepository.upsert(tiltakstype)
    }

    fun upsert(deltaker: Deltaker): QueryResult<Deltaker> {
        return deltakerRepository.upsert(deltaker)
    }

    fun remove(tiltaksgjennomforing: Tiltaksgjennomforing): QueryResult<Unit> {
        return tiltaksgjennomforingRepository.delete(tiltaksgjennomforing.id)
    }

    fun remove(tiltakstype: Tiltakstype): QueryResult<Unit> {
        return tiltakstypeRepository.delete(tiltakstype.id)
    }

    fun remove(deltaker: Deltaker): QueryResult<Unit> {
        return deltakerRepository.delete(deltaker.id)
    }
}
