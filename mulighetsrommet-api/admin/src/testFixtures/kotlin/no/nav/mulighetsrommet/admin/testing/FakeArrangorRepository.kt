package no.nav.mulighetsrommet.admin.testing

import no.nav.mulighetsrommet.api.domain.arrangor.Arrangor
import no.nav.mulighetsrommet.api.domain.arrangor.ArrangorRepository
import no.nav.mulighetsrommet.api.domain.arrangor.UtenlandskArrangor
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import java.util.UUID

class FakeArrangorRepository : ArrangorRepository {
    private val store = mutableMapOf<UUID, Arrangor>()
    private val utenlandskStore = mutableMapOf<UUID, UtenlandskArrangor>()

    override fun save(arrangor: Arrangor) {
        store[arrangor.id] = arrangor
    }

    override fun get(id: UUID): Arrangor? {
        return store[id]
    }

    override fun getByOrganisasjonsnummer(orgnr: Organisasjonsnummer): Arrangor? {
        return store.values.find { it.organisasjonsnummer == orgnr }
    }

    override fun delete(orgnr: Organisasjonsnummer) {
        store.values.find { it.organisasjonsnummer == orgnr }?.also { store.remove(it.id) }
    }

    override fun getUtenlandskArrangor(arrangorId: UUID): UtenlandskArrangor? {
        return utenlandskStore[arrangorId]
    }

    /**
     * Test-only seeding av utenlandsk bankinformasjon. Det finnes ikke noen funksjon for dette i produksjon.
     * I produksjon settes disse dataene manuelt direkte i databasen.
     */
    fun saveUtenlandsk(arrangorId: UUID, utenlandsk: UtenlandskArrangor) {
        utenlandskStore[arrangorId] = utenlandsk
    }
}
