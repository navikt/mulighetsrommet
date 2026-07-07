package no.nav.mulighetsrommet.admin.redaksjoneltinnhold

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.mulighetsrommet.admin.AdminDatabase
import no.nav.mulighetsrommet.api.domain.redaksjoneltinnhold.RedaksjoneltInnholdLenke
import java.util.UUID

class RedaksjoneltInnholdLenkeService(
    private val db: AdminDatabase,
) {
    fun getAll(): List<RedaksjoneltInnholdLenke> = db.session {
        repository.redaksjoneltInnholdLenke.getAll()
    }

    fun upsert(lenke: RedaksjoneltInnholdLenke): RedaksjoneltInnholdLenke = db.session {
        repository.redaksjoneltInnholdLenke.upsert(lenke)
    }

    fun delete(id: UUID): Either<List<String>, Unit> = db.session {
        val referencedBy = queries.tiltakstype.getNamesReferencingLenke(id)
        if (referencedBy.isNotEmpty()) {
            return@session referencedBy.left()
        }
        repository.redaksjoneltInnholdLenke.delete(id)
        Unit.right()
    }
}
