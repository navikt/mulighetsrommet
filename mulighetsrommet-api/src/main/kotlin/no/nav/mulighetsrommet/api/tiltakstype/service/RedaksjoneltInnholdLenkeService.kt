package no.nav.mulighetsrommet.api.tiltakstype.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.tiltakstype.model.RedaksjoneltInnholdLenke
import java.util.UUID

class RedaksjoneltInnholdLenkeService(
    private val db: ApiDatabase,
) {

    fun getAll(): List<RedaksjoneltInnholdLenke> = db.session {
        queries.regelverklenke.getAll()
    }

    fun getById(id: UUID): RedaksjoneltInnholdLenke? = db.session {
        queries.regelverklenke.get(id)
    }

    fun upsert(lenke: RedaksjoneltInnholdLenke): RedaksjoneltInnholdLenke = db.session {
        queries.regelverklenke.upsert(lenke)
    }

    fun delete(id: UUID): Either<List<FieldError>, Unit> = db.session {
        val referencedBy = queries.regelverklenke.getReferencingTiltakstyper(id)
        if (referencedBy.isNotEmpty()) {
            return referencedBy.map { navn -> FieldError.root("Lenken er i bruk av tiltakstypen «$navn»") }.left()
        }

        queries.regelverklenke.delete(id)
        Unit.right()
    }
}
