package no.nav.mulighetsrommet.api.domain.testing

import no.nav.mulighetsrommet.api.domain.deltaker.Deltaker
import no.nav.mulighetsrommet.api.domain.deltaker.DeltakerRepository
import java.util.UUID

class FakeDeltakerRepository : DeltakerRepository {
    private val store = mutableMapOf<UUID, Deltaker>()

    override fun save(deltaker: Deltaker) {
        store[deltaker.id] = deltaker
    }

    override fun get(id: UUID): Deltaker? {
        return store[id]
    }

    override fun getByGjennomforing(gjennomforingId: UUID): List<Deltaker> {
        return store.values.filter { it.gjennomforingId == gjennomforingId }
    }

    override fun delete(id: UUID) {
        store.remove(id)
    }
}
