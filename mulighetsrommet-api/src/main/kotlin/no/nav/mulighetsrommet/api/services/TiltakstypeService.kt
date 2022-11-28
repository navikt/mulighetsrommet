package no.nav.mulighetsrommet.api.services

import kotliquery.queryOf
import no.nav.mulighetsrommet.api.utils.DatabaseMapper
import no.nav.mulighetsrommet.api.utils.PaginationParams
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.domain.models.Tiltakstype
import org.intellij.lang.annotations.Language
import java.util.*

class TiltakstypeService(private val db: Database) {

    fun getTiltakstypeById(id: UUID): Tiltakstype? {
        @Language("PostgreSQL")
        val query = """
            select id, navn, tiltakskode
            from tiltakstype
            where id = ?
        """.trimIndent()
        val queryResult = queryOf(query, id).map { DatabaseMapper.toTiltakstype(it) }.asSingle
        return db.run(queryResult)
    }

    fun getTiltakstyper(
        search: String? = null,
        paginationParams: PaginationParams = PaginationParams()
    ): Pair<Int, List<Tiltakstype>> {

        val parameters = mapOf(
            "navn" to "%$search%",
            "paginationLimit" to paginationParams.limit,
            "paginationOffset" to paginationParams.offset
        )

        @Language("PostgreSQL")
        val query = """
            select id, navn, tiltakskode, count(*) OVER() AS full_count
            from tiltakstype
        """
            .where(search, "(lower(navn) like lower(:navn))")
            .plus(
                """
            limit :paginationLimit
            offset :paginationOffset
        """
            )
            .trimIndent()

        val result = queryOf(
            query,
            parameters
        ).map {
            it.int("full_count") to DatabaseMapper.toTiltakstype(it)
        }.asList
        val results = db.run(result)
        val tiltakstyper = results.map { it.second }
        val totaltAntall = results.firstOrNull()?.first ?: 0
        return Pair(totaltAntall, tiltakstyper)
    }

    private fun List<Int>.toPostgresIntArray() = if (isNullOrEmpty()) null else db.createArrayOf("int4", this)

    private fun <T> String.where(v: T?, query: String): String = if (v != null) "$this where $query" else this

    private fun <T> String.andWhere(v: T?, query: String): String {
        return if (v == null) {
            this
        } else {
            if (this.contains(" where ")) {
                "$this and $query"
            } else {
                this.where(v, query)
            }
        }
    }

    fun getTiltakstypeById(id: Int): Tiltakstype? {
        val query = """
            select id, navn, innsatsgruppe_id, sanity_id, tiltakskode, fra_dato, til_dato from tiltakstype where id = ?
        """.trimIndent()
        val queryResult = queryOf(query, id).map { DatabaseMapper.toTiltakstype(it) }.asSingle
        return db.run(queryResult)
    }
}
