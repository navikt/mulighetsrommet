package no.nav.mulighetsrommet.api.services

import no.nav.mulighetsrommet.api.repositories.DeltakerRepository
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.repositories.TiltakstypeRepository
import no.nav.mulighetsrommet.domain.models.Deltaker
import no.nav.mulighetsrommet.domain.models.Tiltaksgjennomforing
import no.nav.mulighetsrommet.domain.models.Tiltakstype
import java.util.*

class ArenaService(
    private val tiltakstypeRepository: TiltakstypeRepository,
    private val tiltaksgjennomforingRepository: TiltaksgjennomforingRepository,
    private val deltakerRepository: DeltakerRepository
) {

    fun createOrUpdate(tiltaksgjennomforing: Tiltaksgjennomforing) =
        tiltaksgjennomforingRepository.save(tiltaksgjennomforing)

    fun createOrUpdate(tiltakstype: Tiltakstype) = tiltakstypeRepository.save(tiltakstype)

    fun createOrUpdate(deltaker: Deltaker) = deltakerRepository.save(deltaker)

    fun remove(tiltaksgjennomforing: Tiltaksgjennomforing) =
        tiltakstypeRepository.delete(tiltaksgjennomforing.id)

    fun remove(tiltakstype: Tiltakstype) = tiltakstypeRepository.delete(tiltakstype.id)

    fun remove(deltaker: Deltaker) = deltakerRepository.delete(deltaker.id)
}
