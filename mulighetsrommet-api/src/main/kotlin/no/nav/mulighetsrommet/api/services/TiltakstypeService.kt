package no.nav.mulighetsrommet.api.services

import kotliquery.queryOf
import no.nav.mulighetsrommet.api.database.Database
import no.nav.mulighetsrommet.api.utils.DatabaseMapper
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.domain.Tiltakstype
import org.slf4j.Logger

class TiltakstypeService(private val db: Database, private val logger: Logger) {

    fun getTiltakstypeByTiltakskode(tiltakskode: Tiltakskode): Tiltakstype? {
        val query = """
            select id, navn, innsatsgruppe_id, sanity_id, tiltakskode, fra_dato, til_dato from tiltakstype where tiltakskode::text = ?
        """.trimIndent()
        val queryResult = queryOf(query, tiltakskode.name).map { DatabaseMapper.toTiltakstype(it) }.asSingle
        return db.session.run(queryResult)
    }

    fun createTiltakstype(tiltakstype: Tiltakstype): Tiltakstype {
        val query = """
            insert into tiltakstype (navn, innsatsgruppe_id, sanity_id, tiltakskode, fra_dato, til_dato) values (?, ?, ?, ?::tiltakskode, ?, ?) returning *
        """.trimIndent()
        val queryResult = queryOf(
            query,
            tiltakstype.navn,
            tiltakstype.innsatsgruppe,
            tiltakstype.sanityId,
            tiltakstype.tiltakskode.name,
            tiltakstype.fraDato,
            tiltakstype.tilDato
        ).asExecute.query.map { DatabaseMapper.toTiltakstype(it) }.asSingle
        return db.session.run(queryResult)!!
    }

    fun updateTiltakstype(tiltakskode: Tiltakskode, tiltakstype: Tiltakstype): Tiltakstype {
        val query = """
            update tiltakstype set navn = ?, innsatsgruppe_id = ?, sanity_id = ?, tiltakskode = ?::tiltakskode, fra_dato = ?, til_dato = ?
            where tiltakskode::text = ?
            returning *
        """.trimIndent()
        val queryResult = queryOf(
            query,
            tiltakstype.navn,
            tiltakstype.innsatsgruppe,
            tiltakstype.sanityId,
            tiltakstype.tiltakskode.name,
            tiltakstype.fraDato,
            tiltakstype.tilDato,
            tiltakskode.name
        ).asExecute.query.map { DatabaseMapper.toTiltakstype(it) }.asSingle
        return db.session.run(queryResult)!!
    }

    fun getTiltakstyper(innsatsgrupper: List<Int>? = null, search: String? = null): List<Tiltakstype> {
        val innsatsgrupperQuery = innsatsgrupper?.toPostgresIntArray()

        val parameters = mapOf(
            "innsatsgrupper" to innsatsgrupperQuery,
            "navn" to "%$search%"
        )

        val query = """
            select id, navn, innsatsgruppe_id, sanity_id, tiltakskode, fra_dato, til_dato from tiltakstype
        """
            .where(innsatsgrupperQuery, "(innsatsgruppe_id = any(:innsatsgrupper))")
            .andWhere(search, "(lower(navn) like lower(:navn))")
            .trimIndent()

        val result = queryOf(query, parameters).map { DatabaseMapper.toTiltakstype(it) }.asList
        return db.session.run(result)
    }

    private fun List<Int>.toPostgresIntArray() = if (isNullOrEmpty()) null else db.session.createArrayOf("int4", this)

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
}
