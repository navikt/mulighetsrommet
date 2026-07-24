package no.nav.mulighetsrommet.api.domain.tiltak

import java.util.UUID

interface AvtaleRepository {
    fun save(avtale: Avtale)
    fun get(id: UUID): Avtale?
    fun getOrError(id: UUID): Avtale
}
