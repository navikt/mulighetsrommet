package no.nav.mulighetsrommet.api.services

import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.database.Database
import no.nav.mulighetsrommet.api.utils.DatabaseUtils
import no.nav.mulighetsrommet.domain.Deltaker
import no.nav.mulighetsrommet.domain.Deltakerstatus
import org.slf4j.Logger

class DeltakerService(private val db: Database, private val logger: Logger) {

    fun getDeltakere(): List<Deltaker> {
        val query = """
            select id, tiltaksgjennomforing_id, person_id, fra_dato, til_dato, status from deltaker
        """.trimIndent()
        val queryResult = queryOf(query).map { DatabaseUtils.toDeltaker(it) }.asList
        return db.session.run(queryResult)
    }

}
