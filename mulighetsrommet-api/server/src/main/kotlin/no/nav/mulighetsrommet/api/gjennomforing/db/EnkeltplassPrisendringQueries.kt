package no.nav.mulighetsrommet.api.gjennomforing.db

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import org.intellij.lang.annotations.Language
import java.util.UUID

class EnkeltplassPrisendringQueries(private val session: Session) {
    fun insert(dbo: EnkeltplassPrisendringDbo) {
        @Language("PostgreSQL")
        val query = """
            insert into enkeltplass_prisendring (totrinnskontroll_id, gjennomforing_id, prismodell_id)
            values (:totrinnskontroll_id::uuid, :gjennomforing_id::uuid, :prismodell_id::uuid)
        """.trimIndent()

        session.execute(
            queryOf(
                query,
                mapOf(
                    "totrinnskontroll_id" to dbo.totrinnskontrollId,
                    "gjennomforing_id" to dbo.gjennomforingId,
                    "prismodell_id" to dbo.prismodellId,
                ),
            ),
        )
    }

    fun getByGjennomforingId(gjennomforingId: UUID): EnkeltplassPrisendringDbo? {
        @Language("PostgreSQL")
        val query = """
            select totrinnskontroll_id, gjennomforing_id, prismodell_id
            from enkeltplass_prisendring
            where gjennomforing_id = ?::uuid
        """.trimIndent()

        return session.single(queryOf(query, gjennomforingId)) { it.toEnkeltplassPrisendringDbo() }
    }

    fun deleteByTotrinnskontrollId(totrinnskontrollId: UUID) {
        @Language("PostgreSQL")
        val query = """
            delete from enkeltplass_prisendring
            where totrinnskontroll_id = ?::uuid
        """.trimIndent()

        session.execute(queryOf(query, totrinnskontrollId))
    }

    private fun Row.toEnkeltplassPrisendringDbo() = EnkeltplassPrisendringDbo(
        totrinnskontrollId = uuid("totrinnskontroll_id"),
        gjennomforingId = uuid("gjennomforing_id"),
        prismodellId = uuid("prismodell_id"),
    )
}
