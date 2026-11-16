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
            insert into tiltakstype(
                arena_tiltakskode,
                tiltakskode,
                navn,
                tiltakstype_id,
                opprettet_tidspunkt,
                oppdatert_tidspunkt
            ) values (
                :arena_tiltakskode,
                :tiltakskode,
                :navn,
                :tiltakstype_id::uuid,
                :opprettet_tidspunkt,
                :oppdatert_tidspunkt
            )
            on conflict (tiltakstype_id) do update
                set arena_tiltakskode = excluded.arena_tiltakskode,
                    navn = excluded.navn,
                    tiltakskode = excluded.tiltakskode,
                    opprettet_tidspunkt = excluded.opprettet_tidspunkt,
                    oppdatert_tidspunkt = excluded.oppdatert_tidspunkt
        """.trimIndent()
        session.execute(
            queryOf(
                query,
                mapOf(
                    "arena_tiltakskode" to tiltakstype.arenaTiltakskode,
                    "tiltakskode" to tiltakstype.tiltakskode?.name,
                    "navn" to tiltakstype.navn,
                    "tiltakstype_id" to tiltakstype.tiltakstypeId,
                    "opprettet_tidspunkt" to tiltakstype.opprettetTidspunkt,
                    "oppdatert_tidspunkt" to tiltakstype.oppdatertTidspunkt,
                ),
            ),
        )
    }

    fun getByTiltakskode(tiltakstype: Tiltakskode): Tiltakstype {
        @Language("PostgreSQL")
        val query = """
            select navn, tiltakskode, arena_tiltakskode, tiltakstype_id, opprettet_tidspunkt, oppdatert_tidspunkt
            from tiltakstype
            where tiltakskode = ?
        """.trimIndent()
        return session.requireSingle(queryOf(query, tiltakstype.name)) { row ->
            Tiltakstype(
                navn = row.string("navn"),
                tiltakskode = Tiltakskode.valueOf(row.string("tiltakskode")),
                arenaTiltakskode = row.stringOrNull("arena_tiltakskode"),
                tiltakstypeId = row.uuid("tiltakstype_id"),
                opprettetTidspunkt = row.instant("opprettet_tidspunkt"),
                oppdatertTidspunkt = row.instant("oppdatert_tidspunkt"),
            )
        }
    }
}
