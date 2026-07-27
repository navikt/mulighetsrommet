package no.nav.mulighetsrommet.api.domain.testing

import no.nav.mulighetsrommet.api.domain.tiltakdokument.TiltakDokument
import no.nav.mulighetsrommet.api.domain.tiltakdokument.TiltakDokumentRepository
import java.util.UUID

class FakeTiltakDokumentRepository : TiltakDokumentRepository {
    private val store = mutableMapOf<UUID, TiltakDokument>()

    override fun save(tiltakDokument: TiltakDokument) {
        store[tiltakDokument.id] = tiltakDokument
    }

    override fun get(id: UUID): TiltakDokument? {
        return store[id]
    }

    override fun delete(id: UUID) {
        store.values.find { it.id == id }?.also { store.remove(it.id) }
    }
}
