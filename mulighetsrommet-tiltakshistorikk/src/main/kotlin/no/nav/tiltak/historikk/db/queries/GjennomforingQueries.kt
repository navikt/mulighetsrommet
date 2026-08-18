package no.nav.tiltak.historikk.db.queries

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.tiltak.historikk.model.Gjennomforing
import org.intellij.lang.annotations.Language
import java.util.UUID

class GjennomforingQueries(private val session: Session) {

    fun upsert(gjennomforing: Gjennomforing) {
        @Language("PostgreSQL")
        val query = """
            insert into gjennomforing(
                id,
                gjennomforing_type,
                tiltakskode,
                arrangor_organisasjonsnummer,
                navn,
                deltidsprosent
            ) values (
                :id::uuid,
                :gjennomforing_type::gjennomforing_type,
                :tiltakskode,
                :arrangor_organisasjonsnummer,
                :navn,
                :deltidsprosent
            )
            on conflict (id) do update set
                gjennomforing_type = excluded.gjennomforing_type,
                tiltakskode = excluded.tiltakskode,
                arrangor_organisasjonsnummer = excluded.arrangor_organisasjonsnummer,
                navn = excluded.navn,
                deltidsprosent = excluded.deltidsprosent
        """.trimIndent()

        val params = mapOf(
            "id" to gjennomforing.id,
            "gjennomforing_type" to gjennomforing.type.name,
            "tiltakskode" to gjennomforing.tiltakskode.name,
            "arrangor_organisasjonsnummer" to gjennomforing.arrangorOrganisasjonsnummer,
            "navn" to gjennomforing.navn,
            "deltidsprosent" to gjennomforing.deltidsprosent,
        )

        session.execute(queryOf(query, params))
    }

    fun delete(id: UUID) {
        @Language("PostgreSQL")
        val query = """
            delete from gjennomforing
            where id = ?::uuid
        """.trimIndent()

        session.execute(queryOf(query, id))
    }

    fun get(id: UUID): Gjennomforing? {
        @Language("PostgreSQL")
        val query = """
            select *
            from gjennomforing
            where id = ?::uuid
        """.trimIndent()

        return session.single(queryOf(query, id)) { it.toGjennomforing() }
    }
}

private fun Row.toGjennomforing() = Gjennomforing(
    id = uuid("id"),
    type = Gjennomforing.Type.valueOf(string("gjennomforing_type")),
    tiltakskode = Tiltakskode.valueOf(string("tiltakskode")),
    arrangorOrganisasjonsnummer = string("arrangor_organisasjonsnummer"),
    navn = stringOrNull("navn"),
    deltidsprosent = doubleOrNull("deltidsprosent"),
)
