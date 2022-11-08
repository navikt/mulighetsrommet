package no.nav.mulighetsrommet.api.services

import no.nav.mulighetsrommet.api.repositories.ArenaRepository
import no.nav.mulighetsrommet.domain.adapter.AdapterSak
import no.nav.mulighetsrommet.domain.adapter.AdapterTiltak
import no.nav.mulighetsrommet.domain.adapter.AdapterTiltakdeltaker
import no.nav.mulighetsrommet.domain.adapter.AdapterTiltaksgjennomforing

class ArenaService(private val arenaRepository: ArenaRepository) {

    fun createOrUpdate(tiltaksgjennomforing: AdapterTiltaksgjennomforing) = arenaRepository.upsertTiltaksgjennomforing(tiltaksgjennomforing)

    fun createOrUpdate(tiltakstype: AdapterTiltak) = arenaRepository.upsertTiltakstype(tiltakstype)

    fun createOrUpdate(deltaker: AdapterTiltakdeltaker) = arenaRepository.upsertDeltaker(deltaker)

    fun remove(tiltaksgjennomforing: AdapterTiltaksgjennomforing) = arenaRepository.deleteTiltaksgjennomforing(tiltaksgjennomforing)

    fun remove(tiltakstype: AdapterTiltak) = arenaRepository.deleteTiltakstype(tiltakstype)

    fun remove(deltaker: AdapterTiltakdeltaker) = arenaRepository.deleteDeltaker(deltaker)

    fun setTiltaksnummerWith(sak: AdapterSak) = arenaRepository.updateTiltaksgjennomforingWithSak(sak)

    fun removeTiltaksnummerWith(sak: AdapterSak) = arenaRepository.unsetSakOnTiltaksgjennomforing(sak)
}
