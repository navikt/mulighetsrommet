package no.nav.mulighetsrommet.api.domain.testing.repository

import no.nav.mulighetsrommet.api.domain.tiltakdokument.TiltakDokument
import no.nav.mulighetsrommet.api.domain.tiltakdokument.TiltakDokumentRepository
import java.util.UUID

class FakeTiltakDokumentRepository : TiltakDokumentRepository {
    private val store = mutableMapOf<UUID, TiltakDokument>()

    override fun save(tiltakDokument: TiltakDokument) {
        store[tiltakDokument.id] = tiltakDokument
    }

    override fun upsertFromArena(tiltakDokument: TiltakDokument) {
        val existing = store.values.find { it.sanityId == tiltakDokument.sanityId && tiltakDokument.sanityId != null }
            ?: store[tiltakDokument.id]
        if (existing != null) {
            store[existing.id] = existing.copy(
                sanityId = tiltakDokument.sanityId,
                navn = tiltakDokument.navn,
                tiltaksnummer = tiltakDokument.tiltaksnummer,
                tiltakstypeId = tiltakDokument.tiltakstypeId,
                arrangorId = tiltakDokument.arrangorId,
            )
        } else {
            store[tiltakDokument.id] = tiltakDokument.copy(
                stedForGjennomforing = null,
                faneinnhold = null,
                beskrivelse = null,
                publisert = false,
                administratorer = emptyList(),
                navEnheter = emptyList(),
                kontaktpersoner = emptyList(),
                arrangorKontaktpersoner = emptyList(),
            )
        }
    }

    override fun get(id: UUID): TiltakDokument? {
        return store[id]
    }

    override fun delete(id: UUID) {
        store.values.find { it.id == id }?.also { store.remove(it.id) }
    }
}
