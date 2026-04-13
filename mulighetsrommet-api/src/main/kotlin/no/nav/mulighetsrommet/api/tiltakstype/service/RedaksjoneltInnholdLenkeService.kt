package no.nav.mulighetsrommet.api.tiltakstype.service

import no.nav.mulighetsrommet.api.ApiDatabase
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

    fun delete(id: UUID): Boolean = db.session {
        queries.regelverklenke.delete(id) > 0
    }
}
