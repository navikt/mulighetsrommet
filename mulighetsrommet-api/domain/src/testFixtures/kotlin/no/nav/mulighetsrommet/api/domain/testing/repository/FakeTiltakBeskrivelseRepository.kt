package no.nav.mulighetsrommet.api.domain.testing.repository

import no.nav.mulighetsrommet.api.domain.tiltakbeskrivelse.TiltakBeskrivelse
import no.nav.mulighetsrommet.api.domain.tiltakbeskrivelse.TiltakBeskrivelseRepository
import java.util.UUID

class FakeTiltakBeskrivelseRepository : TiltakBeskrivelseRepository {
    private val store = mutableMapOf<UUID, TiltakBeskrivelse>()

    override fun save(tiltakBeskrivelse: TiltakBeskrivelse) {
        store[tiltakBeskrivelse.id] = tiltakBeskrivelse
    }

    override fun upsertFromArena(tiltakBeskrivelse: TiltakBeskrivelse) {
        val existing = store.values.find { it.sanityId == tiltakBeskrivelse.sanityId && tiltakBeskrivelse.sanityId != null }
            ?: store[tiltakBeskrivelse.id]
        if (existing != null) {
            store[existing.id] = existing.copy(
                sanityId = tiltakBeskrivelse.sanityId,
                navn = tiltakBeskrivelse.navn,
                tiltaksnummer = tiltakBeskrivelse.tiltaksnummer,
                tiltakstypeId = tiltakBeskrivelse.tiltakstypeId,
                arrangorId = tiltakBeskrivelse.arrangorId,
            )
        } else {
            store[tiltakBeskrivelse.id] = tiltakBeskrivelse.copy(
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

    override fun get(id: UUID): TiltakBeskrivelse? {
        return store[id]
    }

    override fun delete(id: UUID) {
        store.values.find { it.id == id }?.also { store.remove(it.id) }
    }
}
