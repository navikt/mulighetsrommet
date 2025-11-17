package no.nav.tiltak.historikk.db.queries

import kotliquery.Session
import kotliquery.queryOf
import no.nav.tiltak.historikk.TiltakshistorikkArenaGjennomforing
import org.intellij.lang.annotations.Language
import java.util.*

class ArenaGjennomforingQueries(private val session: Session) {

    fun upsert(gjennomforing: TiltakshistorikkArenaGjennomforing) {
        @Language("PostgreSQL")
        val query = """
            insert into arena_gjennomforing (id,
                                             arena_tiltakskode,
                                             arena_reg_dato,
                                             arena_mod_dato,
                                             arrangor_organisasjonsnummer,
                                             navn,
                                             deltidsprosent)
            values (:id::uuid,
                    :arena_tiltakskode,
                    :arena_reg_dato,
                    :arena_mod_dato,
                    :arrangor_organisasjonsnummer,
                    :navn,
                    :deltidsprosent)
            on conflict (id) do update set arena_tiltakskode            = excluded.arena_tiltakskode,
                                           arena_reg_dato               = excluded.arena_reg_dato,
                                           arena_mod_dato               = excluded.arena_mod_dato,
                                           arrangor_organisasjonsnummer = excluded.arrangor_organisasjonsnummer,
                                           navn                         = excluded.navn,
                                           deltidsprosent               = excluded.deltidsprosent

        """.trimIndent()

        val params = mapOf(
            "id" to gjennomforing.id,
            "arena_tiltakskode" to gjennomforing.arenaTiltakskode,
            "arena_reg_dato" to gjennomforing.arenaRegDato,
            "arena_mod_dato" to gjennomforing.arenaModDato,
            "navn" to gjennomforing.navn,
            "arrangor_organisasjonsnummer" to gjennomforing.arrangorOrganisasjonsnummer.value,
            "deltidsprosent" to gjennomforing.deltidsprosent,
        )

        session.execute(queryOf(query, params))
    }

    fun delete(id: UUID) {
        @Language("PostgreSQL")
        val query = """
            delete from arena_gjennomforing
            where id = ?::uuid
        """.trimIndent()

        session.execute(queryOf(query, id))
    }
}
