package no.nav.mulighetsrommet.api.repositories

import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.database.utils.query
import no.nav.mulighetsrommet.domain.models.Tiltaksgjennomforing
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.util.*

class TiltaksgjennomforingRepository(private val db: Database) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun save(tiltaksgjennomforing: Tiltaksgjennomforing): QueryResult<Tiltaksgjennomforing> = query {
        logger.info("Lagrer tiltaksgjennomføring id=${tiltaksgjennomforing.id}")

        @Language("PostgreSQL")
        val query = """
            insert into tiltaksgjennomforing (id, navn, tiltakstype_id, tiltaksnummer, virksomhetsnummer)
            values (:id::uuid, :navn, :tiltakstype_id::uuid, :tiltaksnummer, :virksomhetsnummer)
            on conflict (id)
                do update set navn              = excluded.navn,
                              tiltakstype_id    = excluded.tiltakstype_id,
                              tiltaksnummer     = excluded.tiltaksnummer,
                              virksomhetsnummer = excluded.virksomhetsnummer
            returning *
        """.trimIndent()

        queryOf(query, tiltaksgjennomforing.toSqlParameters())
            .map { it.toTiltaksgjennomforing() }
            .asSingle
            .let { db.run(it)!! }
    }

    fun delete(id: UUID): QueryResult<Unit> = query {
        logger.info("Sletter tiltaksgjennomføring id=$id")

        @Language("PostgreSQL")
        val query = """
            delete from tiltaksgjennomforing
            where id = ?::uuid
        """.trimIndent()

        queryOf(query, id)
            .asExecute
            .let { db.run(it) }
    }

    private fun Tiltaksgjennomforing.toSqlParameters() = mapOf(
        "id" to id,
        "navn" to navn,
        "tiltakstype_id" to tiltakstypeId,
        "tiltaksnummer" to tiltaksnummer,
        "virksomhetsnummer" to virksomhetsnummer,
    )

    private fun Row.toTiltaksgjennomforing() = Tiltaksgjennomforing(
        id = uuid("id"),
        navn = string("navn"),
        tiltakstypeId = uuid("tiltakstype_id"),
        tiltaksnummer = string("tiltaksnummer"),
        virksomhetsnummer = string("virksomhetsnummer")
    )
}
