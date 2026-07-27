package no.nav.mulighetsrommet.api.domain.testing

import no.nav.mulighetsrommet.api.domain.navansatt.NavAnsatt
import no.nav.mulighetsrommet.api.domain.navansatt.NavAnsattRepository
import no.nav.mulighetsrommet.model.NavIdent
import java.util.UUID

class FakeNavAnsattRepository : NavAnsattRepository {
    private val store = mutableMapOf<NavIdent, NavAnsatt>()

    override fun save(ansatt: NavAnsatt) {
        store[ansatt.navIdent] = ansatt
    }

    override fun get(navIdent: NavIdent): NavAnsatt? {
        return store[navIdent]
    }

    override fun getOrError(navIdent: NavIdent): NavAnsatt {
        return checkNotNull(get(navIdent)) { "NavAnsatt ikke funnet" }
    }

    override fun getByEntraObjectId(objectId: UUID): NavAnsatt? {
        return store.values.find { it.entraObjectId == objectId }
    }

    override fun getAll(): List<NavAnsatt> {
        return store.values.sortedWith(compareBy({ it.fornavn }, { it.etternavn }))
    }

    override fun deleteByEntraObjectId(objectId: UUID): Int {
        val ansatt = getByEntraObjectId(objectId) ?: return 0
        store.remove(ansatt.navIdent)
        return 1
    }
}
