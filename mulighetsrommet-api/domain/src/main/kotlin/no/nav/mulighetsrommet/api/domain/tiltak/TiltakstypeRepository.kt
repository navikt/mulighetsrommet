package no.nav.mulighetsrommet.api.domain.tiltak

import no.nav.mulighetsrommet.model.Tiltakskode
import java.util.UUID

interface TiltakstypeRepository {
    fun save(tiltakstype: Tiltakstype)

    fun get(id: UUID): Tiltakstype?

    fun getByTiltakskode(tiltakskode: Tiltakskode): Tiltakstype

    fun getAll(
        tiltakskoder: Set<Tiltakskode> = emptySet(),
        sortField: TiltakstypeSortField = TiltakstypeSortField.NAVN,
        sortDirection: SortDirection = SortDirection.ASC,
    ): List<Tiltakstype>
}
