package no.nav.mulighetsrommet.api.tilskudd

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.database.requireSingle
import org.intellij.lang.annotations.Language
import java.util.UUID

class TilskuddQueries(private val session: Session) {
    fun getOrError(id: UUID): Tilskudd {
        @Language("PostgreSQL")
        val query = """
            select * from tilskudd
            where id = :id
        """.trimIndent()

        return session.requireSingle(queryOf(query, id)) { it.toTilskudd() }
    }

    fun getAll(): List<Tilskudd> {
        @Language("PostgreSQL")
        val query = """
            select * from tilskudd
        """.trimIndent()

        return session.list(queryOf(query)) { it.toTilskudd() }
    }

    fun Row.toTilskudd(): Tilskudd = Tilskudd(
        id = uuid("id"),
        navn = string("navn"),
    )
}
