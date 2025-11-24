package no.nav.tiltak.historikk.db.queries

import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.database.requireSingle
import org.intellij.lang.annotations.Language

data class TiltakstypeDbo(
    val navn: String,
    val tiltakskode: String?,
    val arenaTiltakskode: String?,
)

class TiltakstypeQueries(private val session: Session) {
    fun upsert(tiltakstype: TiltakstypeDbo) {
        @Language("PostgreSQL")
        val query = """
            insert into tiltakstype(arena_tiltakskode, tiltakskode, navn)
            values (?, ?, ?)
            on conflict (tiltakskode) do update
                set arena_tiltakskode = excluded.arena_tiltakskode,
                    navn = excluded.navn
        """.trimIndent()
        session.execute(queryOf(query, tiltakstype.arenaTiltakskode, tiltakstype.tiltakskode, tiltakstype.navn))
    }

    fun getByTiltakskode(tiltakskode: String): TiltakstypeDbo {
        @Language("PostgreSQL")
        val query = """
            select navn, tiltakskode, arena_tiltakskode
            from tiltakstype
            where tiltakskode = ?
        """.trimIndent()
        return session.requireSingle(queryOf(query, tiltakskode)) { row ->
            TiltakstypeDbo(
                navn = row.string("navn"),
                tiltakskode = row.string("tiltakskode"),
                arenaTiltakskode = row.stringOrNull("arena_tiltakskode"),
            )
        }
    }
}
