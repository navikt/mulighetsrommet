package no.nav.mulighetsrommet.api.services

import kotliquery.queryOf
import no.nav.mulighetsrommet.api.repositories.ArenaRepository
import no.nav.mulighetsrommet.api.utils.DatabaseMapper
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.database.utils.query
import no.nav.mulighetsrommet.domain.adapter.AdapterSak
import no.nav.mulighetsrommet.domain.adapter.AdapterTiltak
import no.nav.mulighetsrommet.domain.adapter.AdapterTiltakdeltaker
import no.nav.mulighetsrommet.domain.adapter.AdapterTiltaksgjennomforing
import no.nav.mulighetsrommet.domain.models.Tiltakstype
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory

class ArenaService(private val arenaRepository: ArenaRepository) {

    fun createOrUpdate(tiltaksgjennomforing: AdapterTiltaksgjennomforing) = arenaRepository.upsertTiltaksgjennomforing(tiltaksgjennomforing)

    fun createOrUpdate(tiltakstype: AdapterTiltak) = arenaRepository.upsertTiltakstype(tiltakstype)

    fun createOrUpdate(deltaker: AdapterTiltakdeltaker) = arenaRepository.upsertDeltaker(deltaker)

    fun remove(tiltaksgjennomforing: AdapterTiltaksgjennomforing) = arenaRepository.deleteTiltaksgjennomforing(tiltaksgjennomforing)

    fun remove(tiltakstype: AdapterTiltak) = arenaRepository.deleteTiltakstype(tiltakstype)

    fun remove(deltaker: AdapterTiltakdeltaker) = arenaRepository.deleteDeltaker(deltaker)

    fun setTiltaksnummerFor(tiltaksgjennomforing: AdapterTiltaksgjennomforing, sak: AdapterSak) = arenaRepository.updateTiltaksgjennomforingWithSak(sak)

    fun removeTiltaksnummerFor(tiltaksgjennomforing: AdapterTiltaksgjennomforing, sak: AdapterSak) = arenaRepository.unsetSakOnTiltaksgjennomforing(sak)
}
