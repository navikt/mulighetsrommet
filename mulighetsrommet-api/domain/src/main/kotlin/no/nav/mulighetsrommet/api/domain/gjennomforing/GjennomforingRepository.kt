package no.nav.mulighetsrommet.api.domain.gjennomforing

import java.util.UUID

interface GjennomforingRepository {
    fun getGjennomforing(id: UUID): Gjennomforing?
    fun getGjennomforingOrError(id: UUID): Gjennomforing
    fun getGjennomforingTiltaksadministrasjon(id: UUID): GjennomforingTiltaksadministrasjon
    fun getGjennomforingAvtaleOrError(id: UUID): GjennomforingAvtale
    fun getGjennomforingEnkeltplassOrError(id: UUID): GjennomforingEnkeltplass
    fun getGjennomforingArenaOrError(id: UUID): GjennomforingArena
    fun getByAvtale(avtaleId: UUID): List<GjennomforingAvtale>
}
