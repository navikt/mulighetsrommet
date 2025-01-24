package no.nav.mulighetsrommet.tiltakshistorikk.repositories

import kotliquery.queryOf
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.model.TiltaksgjennomforingEksternV1Dto
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.util.*

class GruppetiltakRepository(private val db: Database) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun upsert(tiltak: TiltaksgjennomforingEksternV1Dto) = db.session { session ->
        logger.info("Lagrer tiltak id=${tiltak.id}")

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

        queryOf(query, params).asExecute.runWithSession(session)
    }

    fun delete(id: UUID) = db.session { session ->
        logger.info("Sletter tiltak id=$id")

        @Language("PostgreSQL")
        val query = """
            delete from gruppetiltak
            where id = ?::uuid
        """.trimIndent()

        queryOf(query, id).asExecute.runWithSession(session)
    }
}
