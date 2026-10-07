package no.nav.mulighetsrommet.api.domain.tiltakbeskrivelse

import java.util.UUID

interface TiltakBeskrivelseRepository {
    fun save(tiltakBeskrivelse: TiltakBeskrivelse)

    fun upsertFromArena(tiltakBeskrivelse: TiltakBeskrivelse)

    fun get(id: UUID): TiltakBeskrivelse?

    fun delete(id: UUID)
}
