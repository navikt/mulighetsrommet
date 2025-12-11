package no.nav.mulighetsrommet.api.gjennomforing.db

import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.database.datatypes.toDaterange
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Periode
import org.intellij.lang.annotations.Language
import java.util.*

class GjennomforingQueries(private val session: Session) {
    fun upsert(gjennomforing: GjennomforingDbo) {
        @Language("PostgreSQL")
        val query = """
            insert into gjennomforing (id, tiltakstype_id, arrangor_id)
            values (:id::uuid, :tiltakstype_id::uuid, :arrangor_id::uuid)
            on conflict (id) do update set
                tiltakstype_id = excluded.tiltakstype_id,
                arrangor_id = excluded.arrangor_id
        """.trimIndent()

        val params = mapOf(
            "id" to gjennomforing.id,
            "tiltakstype_id" to gjennomforing.tiltakstypeId,
            "arrangor_id" to gjennomforing.arrangorId,
        )

        session.execute(queryOf(query, params))
    }

    fun setArenaData(dbo: GjennomforingArenaDataDbo) {
        @Language("PostgreSQL")
        val query = """
            update gjennomforing
            set arena_tiltaksnummer = :arena_tiltaksnummer,
                arena_ansvarlig_enhet = :arena_ansvarlig_enhet,
                arena_navn = :arena_navn,
                arena_start_dato = :arena_start_dato,
                arena_slutt_dato = :arena_slutt_dato,
                arena_status = :arena_status::gjennomforing_status
            where id = :id::uuid
        """.trimIndent()

        val params = mapOf(
            "id" to dbo.id,
            "arena_tiltaksnummer" to dbo.tiltaksnummer?.value,
            "arena_ansvarlig_enhet" to dbo.arenaAnsvarligEnhet,
            "arena_navn" to dbo.navn,
            "arena_start_dato" to dbo.startDato,
            "arena_slutt_dato" to dbo.sluttDato,
            "arena_status" to dbo.status?.name,
        )

        session.execute(queryOf(query, params))
    }

    fun setFreeTextSearch(id: UUID, content: List<String>) {
        @Language("PostgreSQL")
        val query = """
            update gjennomforing
            set fts = to_tsvector('norwegian',
                                  concat_ws(' ',
                                            lopenummer,
                                            regexp_replace(lopenummer, '/', ' '),
                                            coalesce(arena_tiltaksnummer, ''),
                                            :content
                                  )
                      )
            where id = :id
        """.trimIndent()

        val params = mapOf("id" to id, "content" to content.joinToString(" "))

        session.execute(queryOf(query, params))
    }

    fun frikobleKontaktpersonFraGjennomforing(kontaktpersonId: UUID, gjennomforingId: UUID) {
        @Language("PostgreSQL")
        val query = """
            delete
            from gjennomforing_arrangor_kontaktperson
            where arrangor_kontaktperson_id = ?::uuid and gjennomforing_id = ?::uuid
        """.trimIndent()

        session.update(queryOf(query, kontaktpersonId, gjennomforingId))
    }

    fun setStengtHosArrangor(
        id: UUID,
        periode: Periode,
        beskrivelse: String,
    ) {
        @Language("PostgreSQL")
        val query = """
            insert into gjennomforing_stengt_hos_arrangor (gjennomforing_id, periode, beskrivelse)
            values (:gjennomforing_id::uuid, :periode::daterange, :beskrivelse)
        """.trimIndent()

        val params = mapOf(
            "gjennomforing_id" to id,
            "periode" to periode.toDaterange(),
            "beskrivelse" to beskrivelse,
        )

        session.execute(queryOf(query, params))
    }

    fun deleteStengtHosArrangor(
        id: Int,
    ) {
        @Language("PostgreSQL")
        val query = """
            delete from gjennomforing_stengt_hos_arrangor
            where id = ?
        """.trimIndent()

        session.execute(queryOf(query, id))
    }

    fun insertKoordinatorForGjennomforing(
        id: UUID,
        navIdent: NavIdent,
        gjennomforingId: UUID,
    ) {
        @Language("PostgreSQL")
        val query = """
            insert into gjennomforing_koordinator(id, nav_ident, gjennomforing_id)
            values(:id::uuid, :nav_ident, :gjennomforing_id::uuid)
            on conflict (nav_ident, gjennomforing_id) do nothing
        """.trimIndent()

        val params = mapOf(
            "id" to id,
            "nav_ident" to navIdent.value,
            "gjennomforing_id" to gjennomforingId,
        )
        session.execute(queryOf(query, params))
    }

    fun deleteKoordinatorForGjennomforing(
        id: UUID,
    ) {
        @Language("PostgreSQL")
        val query = """
            delete from gjennomforing_koordinator
            where id = ?::uuid
        """.trimIndent()

        session.execute(queryOf(query, id))
    }

    fun delete(id: UUID): Int {
        @Language("PostgreSQL")
        val query = """
            delete from gjennomforing
            where id = ?::uuid
        """.trimIndent()

        return session.update(queryOf(query, id))
    }
}
