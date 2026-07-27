package no.nav.mulighetsrommet.api.domain.testing

import no.nav.mulighetsrommet.api.domain.tiltak.SortDirection
import no.nav.mulighetsrommet.api.domain.tiltak.Tiltakstype
import no.nav.mulighetsrommet.api.domain.tiltak.TiltakstypeRepository
import no.nav.mulighetsrommet.api.domain.tiltak.TiltakstypeSortField
import no.nav.mulighetsrommet.model.Tiltakskode
import java.util.UUID

class FakeTiltakstypeRepository : TiltakstypeRepository {
    private val store = mutableMapOf<UUID, Tiltakstype>()

    override fun save(tiltakstype: Tiltakstype) {
        store[tiltakstype.id] = tiltakstype
    }

    override fun get(id: UUID): Tiltakstype? {
        return store[id]
    }

    override fun getByTiltakskode(tiltakskode: Tiltakskode): Tiltakstype {
        return store.values.single { it.tiltakskode == tiltakskode }
    }

    override fun getAll(
        tiltakskoder: Set<Tiltakskode>,
        sortField: TiltakstypeSortField,
        sortDirection: SortDirection,
    ): List<Tiltakstype> {
        return store.values.filter { tiltakskoder.isEmpty() || it.tiltakskode in tiltakskoder }
    }
}
