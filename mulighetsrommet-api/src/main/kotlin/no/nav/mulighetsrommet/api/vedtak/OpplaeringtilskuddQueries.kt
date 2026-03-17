package no.nav.mulighetsrommet.api.vedtak

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.database.requireSingle
import org.intellij.lang.annotations.Language
import java.util.UUID

class OpplaeringtilskuddQueries(private val session: Session) {
    fun getOrError(id: UUID): Opplaeringtilskudd {
        @Language("PostgreSQL")
        val query = """
            select * from tilskudd_opplaering
            where id = :id
        """.trimIndent()

        return session.requireSingle(queryOf(query, id)) { it.toTilskudd() }
    }

    fun getAll(): List<Opplaeringtilskudd> {
        @Language("PostgreSQL")
        val query = """
            select * from tilskudd_opplaering
            order by kode;
        """.trimIndent()

        return session.list(queryOf(query)) { it.toTilskudd() }
    }

    fun Row.toTilskudd(): Opplaeringtilskudd = Opplaeringtilskudd(
        id = uuid("id"),
        navn = string("navn"),
        kode = string("kode").let { Opplaeringtilskudd.Kode.valueOf(it) },
    )
}
