package no.nav.mulighetsrommet.admin.testing

import no.nav.mulighetsrommet.api.domain.arrangor.Arrangor
import no.nav.mulighetsrommet.api.domain.arrangor.ArrangorRepository
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import java.util.UUID

class FakeArrangorRepository : ArrangorRepository {
    private val store = mutableMapOf<UUID, Arrangor>()

    override fun save(arrangor: Arrangor) {
        store[arrangor.id] = arrangor
    }

    override fun get(id: UUID): Arrangor {
        return requireNotNull(store[id])
    }

    override fun getByOrganisasjonsnummer(orgnr: Organisasjonsnummer): Arrangor? {
        return store.values.find { it.organisasjonsnummer == orgnr }
    }

    override fun delete(orgnr: Organisasjonsnummer) {
        store.values.find { it.organisasjonsnummer == orgnr }?.also { store.remove(it.id) }
    }
}
