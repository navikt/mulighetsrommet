package no.nav.mulighetsrommet.api.services

import kotliquery.queryOf
import no.nav.mulighetsrommet.api.database.Database
import no.nav.mulighetsrommet.api.utils.DatabaseMapper
import no.nav.mulighetsrommet.domain.Tiltaksgjennomforing
import org.slf4j.Logger

class TiltaksgjennomforingService(private val db: Database, private val logger: Logger) {

    fun getTiltaksgjennomforingerByTiltakskode(tiltakskode: String): List<Tiltaksgjennomforing> {
        val query = """
            select id, navn, tiltaksnummer, tiltakskode, aar
            from tiltaksgjennomforing_valid
            where tiltakskode = ?
        """.trimIndent()
        val queryResult = queryOf(query, tiltakskode).map { DatabaseMapper.toTiltaksgjennomforing(it) }.asList
        return db.run(queryResult)
    }

    fun getTiltaksgjennomforingById(id: Int): Tiltaksgjennomforing? {
        val query = """
            select id, navn, tiltaksnummer, tiltakskode, aar
            from tiltaksgjennomforing_valid
            where id = ?
        """.trimIndent()
        val queryResult = queryOf(query, id).map { DatabaseMapper.toTiltaksgjennomforing(it) }.asSingle
        return db.run(queryResult)
    }

    fun getTiltaksgjennomforinger(): List<Tiltaksgjennomforing> {
        val query = """
            select id, navn, tiltaksnummer, tiltakskode, aar
            from tiltaksgjennomforing_valid
        """.trimIndent()
        val queryResult = queryOf(query).map { DatabaseMapper.toTiltaksgjennomforing(it) }.asList
        return db.run(queryResult)
    }
}
