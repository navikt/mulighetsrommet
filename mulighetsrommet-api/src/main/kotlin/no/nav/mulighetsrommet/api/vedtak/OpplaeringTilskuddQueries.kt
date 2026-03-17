package no.nav.mulighetsrommet.api.vedtak

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.database.requireSingle
import org.intellij.lang.annotations.Language
import java.util.UUID

class OpplaeringTilskuddQueries(private val session: Session) {
    fun getOrError(id: UUID): OpplaeringTilskudd {
        @Language("PostgreSQL")
        val query = """
            select * from tilskudd_opplaering
            where id = :id
        """.trimIndent()

        return session.requireSingle(queryOf(query, id)) { it.toTilskudd() }
    }

    fun getAll(): List<OpplaeringTilskudd> {
        @Language("PostgreSQL")
        val query = """
            select * from tilskudd_opplaering
            order by kode;
        """.trimIndent()

        return session.list(queryOf(query)) { it.toTilskudd() }
    }

    fun Row.toTilskudd(): OpplaeringTilskudd = OpplaeringTilskudd(
        id = uuid("id"),
        navn = string("navn"),
        kode = string("kode").let { OpplaeringTilskudd.Kode.valueOf(it) },
    )
}
