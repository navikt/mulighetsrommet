package no.nav.mulighetsrommet.api.services

import kotliquery.queryOf
import no.nav.mulighetsrommet.api.utils.DatabaseMapper
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.domain.Deltaker
import org.slf4j.Logger

class DeltakerService(private val db: Database, private val logger: Logger) {

    fun getDeltakere(): List<Deltaker> {
        val query = """
            select id, tiltaksgjennomforing_id, person_id, fra_dato, til_dato, status from deltaker
        """.trimIndent()
        val queryResult = queryOf(query).map { DatabaseMapper.toDeltaker(it) }.asList
        return db.run(queryResult)
    }
}
