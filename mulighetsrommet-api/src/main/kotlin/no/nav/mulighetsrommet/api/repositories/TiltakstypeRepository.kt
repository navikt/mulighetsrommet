package no.nav.mulighetsrommet.api.repositories

import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.utils.PaginationParams
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.database.utils.query
import no.nav.mulighetsrommet.domain.models.Tiltakstype
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.util.*

class TiltakstypeRepository(private val db: Database) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun save(tiltakstype: Tiltakstype): QueryResult<Tiltakstype> = query {
        logger.info("Lagrer tiltakstype id=${tiltakstype.id}")

        @Language("PostgreSQL")
        val query = """
            insert into tiltakstype (id, navn, tiltakskode)
            values (:id::uuid, :navn, :tiltakskode)
            on conflict (id)
                do update set navn        = excluded.navn,
                              tiltakskode = excluded.tiltakskode
            returning *
        """.trimIndent()

        queryOf(query, tiltakstype.toSqlParameters())
            .map { it.toTiltakstype() }
            .asSingle
            .let { db.run(it)!! }
    }

    fun getTiltakstypeById(id: UUID): Tiltakstype? {
        @Language("PostgreSQL")
        val query = """
            select id::uuid, navn, tiltakskode
            from tiltakstype
            where id = ?::uuid
        """.trimIndent()
        val queryResult = queryOf(query, id).map { it.toTiltakstype() }.asSingle
        return db.run(queryResult)
    }

    fun getTiltakstyper(
        search: String? = null,
        paginationParams: PaginationParams = PaginationParams()
    ): Pair<Int, List<Tiltakstype>> {
        val parameters = mapOf(
            "search" to "%$search%",
            "limit" to paginationParams.limit,
            "offset" to paginationParams.offset
        )

        val where = andWhereParameterNotNull(
            search to "(lower(navn) like lower(:search))"
        )

        @Language("PostgreSQL")
        val query = """
            select id, navn, tiltakskode, count(*) OVER() AS full_count
            from tiltakstype
            $where
            limit :limit
            offset :offset
        """.trimIndent()

        val results = queryOf(query, parameters)
            .map {
                it.int("full_count") to it.toTiltakstype()
            }
            .asList
            .let { db.run(it) }
        val tiltakstyper = results.map { it.second }
        val totaltAntall = results.firstOrNull()?.first ?: 0
        return Pair(totaltAntall, tiltakstyper)
    }

    fun delete(id: UUID): QueryResult<Unit> = query {
        logger.info("Sletter tiltakstype id=$id")

        @Language("PostgreSQL")
        val query = """
            delete from tiltakstype
            where id = ?::uuid
        """.trimIndent()

        queryOf(query, id)
            .asExecute
            .let { db.run(it) }
    }

    private fun andWhereParameterNotNull(vararg parts: Pair<Any?, String>): String = parts
        .filter { it.first != null }
        .map { it.second }
        .reduceOrNull { where, part -> "$where and $part" }
        ?.let { "where $it" }
        ?: ""

    private fun Tiltakstype.toSqlParameters() = mapOf(
        "id" to id,
        "navn" to navn,
        "tiltakskode" to tiltakskode
    )

    private fun Row.toTiltakstype() = Tiltakstype(
        id = uuid("id"),
        navn = string("navn"),
        tiltakskode = string("tiltakskode")
    )
}
