package no.nav.mulighetsrommet.api.services

import kotliquery.queryOf
import no.nav.mulighetsrommet.api.utils.DatabaseMapper
import no.nav.mulighetsrommet.api.utils.PaginationParams
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.domain.models.Tiltaksgjennomforing
import org.intellij.lang.annotations.Language

class TiltaksgjennomforingService(private val db: Database) {

    fun getTiltaksgjennomforingerByTiltakskode(tiltakskode: String): List<Tiltaksgjennomforing> {
        @Language("PostgreSQL")
        val query = """
            select id, navn, tiltaksnummer, tiltakskode, aar, tilgjengelighet
            from tiltaksgjennomforing_valid
            where tiltakskode = ?
        """.trimIndent()
        val queryResult = queryOf(query, tiltakskode).map { DatabaseMapper.toTiltaksgjennomforing(it) }.asList
        return db.run(queryResult)
    }

    fun getTiltaksgjennomforingById(id: Int): Tiltaksgjennomforing? {
        @Language("PostgreSQL")
        val query = """
            select id, navn, tiltaksnummer, tiltakskode, aar, tilgjengelighet
            from tiltaksgjennomforing_valid
            where id = ?
        """.trimIndent()
        val queryResult = queryOf(query, id).map { DatabaseMapper.toTiltaksgjennomforing(it) }.asSingle
        return db.run(queryResult)
    }

    fun getTiltaksgjennomforinger(paginationParams: PaginationParams = PaginationParams()): Pair<Int, List<Tiltaksgjennomforing>> {
        @Language("PostgreSQL")
        val query = """
            select id, navn, tiltaksnummer, tiltakskode, aar, tilgjengelighet, count(*) OVER() AS full_count
            from tiltaksgjennomforing_valid
            limit ?
            offset ?
        """.trimIndent()
        val queryResult = queryOf(query, paginationParams.limit, paginationParams.offset).map {
            it.int("full_count") to DatabaseMapper.toTiltaksgjennomforing(it)
        }.asList
        val results = db.run(queryResult)
        val tiltaksgjennomforinger = results.map { it.second }
        val totaltAntall = results.firstOrNull()?.first ?: 0

        return Pair(totaltAntall, tiltaksgjennomforinger)
    }
}
