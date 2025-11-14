package no.nav.tiltak.historikk.db

import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.model.TiltaksgjennomforingV1Dto
import org.intellij.lang.annotations.Language
import java.util.*

class GruppetiltakQueries(private val session: Session) {

    fun upsert(tiltak: TiltaksgjennomforingV1Dto) {
        @Language("PostgreSQL")
        val query = """
            insert into gruppetiltak(
                id,
                navn,
                tiltakskode,
                arrangor_organisasjonsnummer,
                start_dato,
                slutt_dato,
                status,
                oppstart
            ) values (
                :id::uuid,
                :navn,
                :tiltakskode::gruppetiltak_tiltakskode,
                :arrangor_organisasjonsnummer,
                :start_dato,
                :slutt_dato,
                :status::gruppetiltak_status,
                :oppstart::gruppetiltak_oppstartstype
            )
            on conflict (id) do update set
                navn = excluded.navn,
                tiltakskode = excluded.tiltakskode,
                arrangor_organisasjonsnummer = excluded.arrangor_organisasjonsnummer,
                start_dato = excluded.start_dato,
                slutt_dato = excluded.slutt_dato,
                status = excluded.status,
                oppstart = excluded.oppstart
        """.trimIndent()

        val params = tiltak.run {
            mapOf(
                "id" to id,
                "navn" to navn,
                "tiltakskode" to tiltakstype.tiltakskode.name,
                "arrangor_organisasjonsnummer" to virksomhetsnummer,
                "start_dato" to startDato,
                "slutt_dato" to sluttDato,
                "status" to status.name,
                "oppstart" to oppstart.name,
            )
        }

        session.execute(queryOf(query, params))
    }

    fun delete(id: UUID) {
        @Language("PostgreSQL")
        val query = """
            delete from gruppetiltak
            where id = ?::uuid
        """.trimIndent()

        session.execute(queryOf(query, id))
    }
}
