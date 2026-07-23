package no.nav.mulighetsrommet.api.domain.testing

import no.nav.mulighetsrommet.api.domain.deltaker.DeltakerForslag
import no.nav.mulighetsrommet.api.domain.deltaker.DeltakerForslagRepository
import java.util.UUID

class FakeDeltakerForslagRepository : DeltakerForslagRepository {
    private val store = mutableMapOf<UUID, DeltakerForslag>()

    override fun save(forslag: DeltakerForslag) {
        store[forslag.id] = forslag
    }

    override fun get(id: UUID): DeltakerForslag? {
        return store[id]
    }

    override fun getByGjennomforing(gjennomforingId: UUID): Map<UUID, List<DeltakerForslag>> {
        return store.values
            .filter { it.gjennomforingId == gjennomforingId }
            .groupBy { it.deltakerId }
    }

    override fun delete(id: UUID) {
        store.remove(id)
    }
}
