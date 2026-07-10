package no.nav.mulighetsrommet.admin.testing

import no.nav.mulighetsrommet.api.domain.redaksjoneltinnhold.RedaksjoneltInnholdLenke
import no.nav.mulighetsrommet.api.domain.redaksjoneltinnhold.RedaksjoneltInnholdLenkeRepository
import java.util.UUID

class FakeRedaksjoneltInnholdLenkeRepository : RedaksjoneltInnholdLenkeRepository {
    private val store = mutableMapOf<UUID, RedaksjoneltInnholdLenke>()

    override fun getAll(): List<RedaksjoneltInnholdLenke> = store.values.sortedBy { it.url }

    override fun get(id: UUID): RedaksjoneltInnholdLenke? = store[id]

    override fun upsert(lenke: RedaksjoneltInnholdLenke): RedaksjoneltInnholdLenke = lenke.also { store[it.id] = it }

    override fun delete(id: UUID): Int = if (store.remove(id) != null) 1 else 0
}
