package no.nav.mulighetsrommet.api.services

import kotliquery.queryOf
import no.nav.mulighetsrommet.api.utils.DatabaseMapper
import no.nav.mulighetsrommet.api.utils.PaginationParams
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.domain.models.Tiltaksgjennomforing
import org.intellij.lang.annotations.Language
import java.util.*

class TiltaksgjennomforingService(private val db: Database) {

    fun getTiltaksgjennomforingerByTiltakstypeId(id: UUID): List<Tiltaksgjennomforing> {
        @Language("PostgreSQL")
        val query = """
            select id, navn, tiltakstype_id, tiltaksnummer
            from tiltaksgjennomforing
            where tiltakstype_id = ?
        """.trimIndent()
        val queryResult = queryOf(query, id).map { DatabaseMapper.toTiltaksgjennomforing(it) }.asList
        return db.run(queryResult)
    }

    fun getTiltaksgjennomforingById(id: UUID): Tiltaksgjennomforing? {
        @Language("PostgreSQL")
        val query = """
            select id, navn, tiltakstype_id, tiltaksnummer
            from tiltaksgjennomforing
            where id = ?
        """.trimIndent()
        val queryResult = queryOf(query, id).map { DatabaseMapper.toTiltaksgjennomforing(it) }.asSingle
        return db.run(queryResult)
    }

    fun getTiltaksgjennomforinger(paginationParams: PaginationParams = PaginationParams()): Pair<Int, List<Tiltaksgjennomforing>> {
        @Language("PostgreSQL")
        val query = """
            select id, navn, tiltakstype_id, tiltaksnummer, count(*) OVER() AS full_count
            from tiltaksgjennomforing
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
