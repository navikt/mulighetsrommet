package no.nav.mulighetsrommet.api.domain.redaksjoneltinnhold

import java.util.UUID

interface RedaksjoneltInnholdLenkeRepository {
    fun getAll(): List<RedaksjoneltInnholdLenke>
    fun get(id: UUID): RedaksjoneltInnholdLenke?
    fun upsert(lenke: RedaksjoneltInnholdLenke): RedaksjoneltInnholdLenke
    fun delete(id: UUID): Int
}
