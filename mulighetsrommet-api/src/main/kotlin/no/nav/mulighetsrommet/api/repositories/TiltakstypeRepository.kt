package no.nav.mulighetsrommet.api.repositories

import kotliquery.queryOf
import no.nav.mulighetsrommet.api.utils.DatabaseMapper
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.database.utils.query
import no.nav.mulighetsrommet.domain.models.Tiltakstype
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.util.*

class TiltakstypeRepository(private val db: Database) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun save(tiltakstype: Tiltakstype): QueryResult<Tiltakstype> = query {
        logger.info("Lagrer tiltakstype id=${tiltakstype.id}")

        @Language("PostgreSQL")
        val query = """
            insert into tiltakstype (id, navn, tiltakskode)
            values (?, ?, ?)
            on conflict (id)
                do update set navn             = excluded.navn,
                              tiltakskode      = excluded.tiltakskode
            returning *
        """.trimIndent()

        tiltakstype.run { queryOf(query, id, navn, tiltakskode) }
            .map { DatabaseMapper.toTiltakstype(it) }
            .asSingle
            .let { db.run(it)!! }
    }

    fun delete(id: UUID): QueryResult<Unit> = query {
        logger.info("Sletter tiltakstype id=$id")

        @Language("PostgreSQL")
        val query = """
            delete from tiltakstype
            where id = ?
        """.trimIndent()

        run { queryOf(query, id) }
            .asExecute
            .let { db.run(it) }
    }
}
