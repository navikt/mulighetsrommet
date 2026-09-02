package no.nav.tiltak.historikk.db.queries

import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.database.requireSingle
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.tiltak.historikk.model.Tiltakstype
import org.intellij.lang.annotations.Language

class TiltakstypeQueries(private val session: Session) {
    fun upsert(tiltakstype: Tiltakstype) {
        @Language("PostgreSQL")
        val query = """
            insert into tiltakstype(arena_tiltakskode, tiltakskode, navn, tiltakstype_id)
            values (:arena_tiltakskode, :tiltakskode, :navn, :tiltakstype_id::uuid)
            on conflict (tiltakstype_id) do update
                set arena_tiltakskode = excluded.arena_tiltakskode,
                    navn = excluded.navn,
                    tiltakskode = excluded.tiltakskode
        """.trimIndent()
        session.execute(
            queryOf(
                query,
                mapOf(
                    "arena_tiltakskode" to tiltakstype.arenaTiltakskode,
                    "tiltakskode" to tiltakstype.tiltakskode?.name,
                    "navn" to tiltakstype.navn,
                    "tiltakstype_id" to tiltakstype.tiltakstypeId,
                ),
            ),
        )
    }

    fun getByTiltakskode(tiltakstype: Tiltakskode): Tiltakstype {
        @Language("PostgreSQL")
        val query = """
            select navn, tiltakskode, arena_tiltakskode, tiltakstype_id
            from tiltakstype
            where tiltakskode = ?
        """.trimIndent()
        return session.requireSingle(queryOf(query, tiltakstype.name)) { row ->
            Tiltakstype(
                navn = row.string("navn"),
                tiltakskode = Tiltakskode.valueOf(row.string("tiltakskode")),
                arenaTiltakskode = row.stringOrNull("arena_tiltakskode"),
                tiltakstypeId = row.uuid("tiltakstype_id"),
            )
        }
    }
}
