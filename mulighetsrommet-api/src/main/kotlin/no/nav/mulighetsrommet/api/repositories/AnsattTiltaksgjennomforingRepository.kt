package no.nav.mulighetsrommet.api.repositories

import kotliquery.queryOf
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.utils.query
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.util.*

class AnsattTiltaksgjennomforingRepository(private val db: Database) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun lagreFavoritt(tiltaksgjennomforingId: String, navIdent: String) = query {
        logger.info("Lagrer tiltaksgjennomføring id=$tiltaksgjennomforingId til ansatt")

        @Language("PostgreSQL")
        val query = """
            insert into ansatt_tiltaksgjennomforing (navident, tiltaksgjennomforing_id)
            values (?, ?::uuid)       
        """.trimIndent()

        queryOf(query, navIdent, tiltaksgjennomforingId).asExecute.let { db.run(it) }
    }

    fun fjernFavoritt(tiltaksgjennomforingId: String, navIdent: String) = query {
        logger.info("Fjerner tiltaksgjennomføring id=$tiltaksgjennomforingId for ansatt")

        @Language("PostgreSQL")
        val query = """
            delete from ansatt_tiltaksgjennomforing
            where navident = ? and tiltaksgjennomforing_id = ?::uuid
        """.trimIndent()

        queryOf(query, navIdent, tiltaksgjennomforingId).asExecute
            .let { db.run(it) }
    }
}
