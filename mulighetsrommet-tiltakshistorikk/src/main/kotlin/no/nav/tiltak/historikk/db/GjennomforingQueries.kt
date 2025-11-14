package no.nav.tiltak.historikk.db

import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.model.Tiltakskode
import org.intellij.lang.annotations.Language
import java.util.*

data class GjennomforingDbo(
    val id: UUID,
    val type: GjennomforingType,
    val tiltakskode: Tiltakskode,
    val arrangorOrganisasjonsnummer: String,
    val navn: String?,
    val deltidsprosent: Double?,
)

enum class GjennomforingType {
    GRUPPE,
    ENKELTPLASS,
}

class GjennomforingQueries(private val session: Session) {

    fun upsert(gjennomforing: GjennomforingDbo) {
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
}
